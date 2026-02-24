package com.smarttraffic.backend.service;

import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.Map;

@Service
public class CameraPollerService {

    private static final Logger log = LoggerFactory.getLogger(CameraPollerService.class);

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
            try {
                pollCamera(cam.getName(), url);
            } catch (Exception e) {
                log.debug("轮询摄像头 {} 失败: {}", cam.getName(), e.getMessage());
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
}
