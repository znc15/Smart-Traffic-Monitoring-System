package com.smarttraffic.backend.service;

import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
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
            String nodeUrl = cam.getNodeUrl();
            if (nodeUrl == null || nodeUrl.isBlank()) continue;

            String roadKey = roadKey(cam);
            NodeHealth health = nodeHealthMap.computeIfAbsent(roadKey, k -> new NodeHealth());
            health.cameraId = cam.getId();
            health.name = cam.getName();
            health.roadName = roadKey;
            health.edgeNodeId = trimToNull(cam.getEdgeNodeId());
            health.nodeUrl = trimToNull(nodeUrl);
            health.lastPollTime = Instant.now();
            boolean wasOnline = health.online;

            long start = System.nanoTime();
            try {
                pollCamera(cam, roadKey, nodeUrl);
                long elapsed = (System.nanoTime() - start) / 1_000_000;
                health.online = true;
                health.lastSuccessTime = Instant.now();
                health.latencyMs = elapsed;
                health.consecutiveFailures = 0;
                if (!wasOnline) {
                    log.info("边缘节点已恢复在线: {} ({} ms)", roadKey, elapsed);
                }
            } catch (Exception e) {
                health.online = false;
                health.errorCount++;
                health.consecutiveFailures++;
                health.lastError = e.getMessage();
                if (wasOnline) {
                    log.warn("边缘节点离线: {} ({})", roadKey, e.getMessage());
                }
                log.debug("轮询摄像头 {} 失败: {}", roadKey, e.getMessage());
            }
        }
    }

    private void pollCamera(CameraEntity camera, String roadKey, String baseUrl) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String edgeNodeId = trimToNull(camera.getEdgeNodeId());
        String edgeKey = trimToNull(camera.getNodeApiKey());

        // 获取交通数据
        @SuppressWarnings("unchecked")
        Map<String, Object> data = restClient.get()
                .uri(base + "/api/traffic")
                .headers(headers -> applyEdgeHeaders(headers, edgeNodeId, edgeKey))
                .retrieve()
                .body(Map.class);

        // 获取视频帧
        byte[] frame = null;
        try {
            frame = restClient.get()
                    .uri(base + "/api/frame")
                    .headers(headers -> applyEdgeHeaders(headers, edgeNodeId, edgeKey))
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception ignored) {
        }

        trafficService.updateFromRemote(roadKey, data, frame);

        // 存储边缘节点性能指标
        if (data != null && data.containsKey("edge_metrics")) {
            NodeHealth h = nodeHealthMap.get(roadKey);
            if (h != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> em = (Map<String, Object>) data.get("edge_metrics");
                h.edgeMetrics = em;
            }
        }
    }

    // 获取所有摄像头节点的健康状态
    public Map<String, Map<String, Object>> getNodeHealthMap() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map.Entry<String, NodeHealth> entry : nodeHealthMap.entrySet()) {
            NodeHealth h = entry.getValue();
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("camera_id", h.cameraId);
            info.put("name", h.name);
            info.put("road_name", h.roadName);
            info.put("edge_node_id", h.edgeNodeId);
            info.put("node_url", h.nodeUrl);
            info.put("online", h.online);
            info.put("last_success_time", h.lastSuccessTime != null ? h.lastSuccessTime.toString() : null);
            info.put("last_poll_time", h.lastPollTime != null ? h.lastPollTime.toString() : null);
            info.put("latency_ms", h.latencyMs);
            info.put("error_count", h.errorCount);
            info.put("consecutive_failures", h.consecutiveFailures);
            info.put("last_error", h.lastError);
            info.put("edge_metrics", h.edgeMetrics);
            result.put(entry.getKey(), info);
        }
        return result;
    }

    // 摄像头节点健康数据
    static class NodeHealth {
        volatile Long cameraId;
        volatile String name;
        volatile String roadName;
        volatile String edgeNodeId;
        volatile String nodeUrl;
        volatile boolean online;
        volatile Instant lastSuccessTime;
        volatile Instant lastPollTime;
        volatile long latencyMs;
        volatile int errorCount;
        volatile int consecutiveFailures;
        volatile String lastError;
        volatile Map<String, Object> edgeMetrics;
    }

    private static void applyEdgeHeaders(HttpHeaders headers, String edgeNodeId, String edgeKey) {
        if (edgeNodeId != null) {
            headers.set("X-Edge-Node-Id", edgeNodeId);
        }
        if (edgeKey != null) {
            headers.set("X-Edge-Key", edgeKey);
        }
    }

    private static String roadKey(CameraEntity camera) {
        String roadName = trimToNull(camera.getRoadName());
        return roadName != null ? roadName : trimToNull(camera.getName());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
