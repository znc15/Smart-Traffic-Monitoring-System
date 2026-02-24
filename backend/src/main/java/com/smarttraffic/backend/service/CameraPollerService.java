package com.smarttraffic.backend.service;

import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CameraPollerService {

    private static final Logger log = LoggerFactory.getLogger(CameraPollerService.class);

    // 每个摄像头节点的健康状态
    private final ConcurrentHashMap<String, NodeHealth> nodeHealthMap = new ConcurrentHashMap<>();

    private final CameraRepository cameraRepository;
    private final TrafficService trafficService;
    private final RestClient restClient;

    public CameraPollerService(CameraRepository cameraRepository, TrafficService trafficService) {
        this.cameraRepository = cameraRepository;
        this.trafficService = trafficService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Scheduled(fixedRate = 3000)
    public void poll() {
        for (CameraEntity cam : cameraRepository.findByEnabledTrue()) {
            String url = cam.getStreamUrl();
            if (url == null || url.isBlank()) continue;

            String name = cam.getName();
            NodeHealth health = nodeHealthMap.computeIfAbsent(name, k -> new NodeHealth());
            health.lastPollTime = Instant.now();

            long start = System.nanoTime();
            try {
                pollCamera(name, url);
                long elapsed = (System.nanoTime() - start) / 1_000_000;
                health.online = true;
                health.lastSuccessTime = Instant.now();
                health.latencyMs = elapsed;
                health.consecutiveFailures = 0;
            } catch (Exception e) {
                health.online = false;
                health.errorCount++;
                health.consecutiveFailures++;
                health.lastError = e.getMessage();
                log.debug("轮询摄像头 {} 失败: {}", name, e.getMessage());
            }
        }
    }

    private void pollCamera(String name, String baseUrl) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        // 获取交通数据
        @SuppressWarnings("unchecked")
        Map<String, Object> data = restClient.get()
                .uri(base + "/api/traffic")
                .retrieve()
                .body(Map.class);

        // 获取视频帧
        byte[] frame = null;
        try {
            frame = restClient.get()
                    .uri(base + "/api/frame")
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception ignored) {
        }

        trafficService.updateFromRemote(name, data, frame);
    }

    // 获取所有摄像头节点的健康状态
    public Map<String, Map<String, Object>> getNodeHealthMap() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map.Entry<String, NodeHealth> entry : nodeHealthMap.entrySet()) {
            NodeHealth h = entry.getValue();
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", entry.getKey());
            info.put("online", h.online);
            info.put("lastSuccessTime", h.lastSuccessTime != null ? h.lastSuccessTime.toString() : null);
            info.put("lastPollTime", h.lastPollTime != null ? h.lastPollTime.toString() : null);
            info.put("latencyMs", h.latencyMs);
            info.put("errorCount", h.errorCount);
            info.put("consecutiveFailures", h.consecutiveFailures);
            info.put("lastError", h.lastError);
            result.put(entry.getKey(), info);
        }
        return result;
    }

    // 摄像头节点健康数据
    static class NodeHealth {
        volatile boolean online;
        volatile Instant lastSuccessTime;
        volatile Instant lastPollTime;
        volatile long latencyMs;
        volatile int errorCount;
        volatile int consecutiveFailures;
        volatile String lastError;
    }
}
