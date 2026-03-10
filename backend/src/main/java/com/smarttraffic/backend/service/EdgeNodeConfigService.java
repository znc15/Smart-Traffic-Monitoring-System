package com.smarttraffic.backend.service;

import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class EdgeNodeConfigService {

    private final CameraRepository cameraRepository;
    private final RestClient restClient;

    public EdgeNodeConfigService(CameraRepository cameraRepository) {
        this.cameraRepository = cameraRepository;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public Map<String, Object> getConfig(Long cameraId) {
        CameraEntity camera = requireCamera(cameraId);
        return restClient.get()
                .uri(normalizeBaseUrl(camera.getNodeUrl()) + "/api/config")
                .headers(headers -> applyHeaders(headers, camera))
                .retrieve()
                .body(Map.class);
    }

    public Map<String, Object> updateConfig(Long cameraId, Map<String, Object> payload) {
        CameraEntity camera = requireCamera(cameraId);
        return restClient.put()
                .uri(normalizeBaseUrl(camera.getNodeUrl()) + "/api/config")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> applyHeaders(headers, camera))
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    private CameraEntity requireCamera(Long cameraId) {
        CameraEntity camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Camera not found"));
        if (camera.getNodeUrl() == null || camera.getNodeUrl().isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Node URL is not configured");
        }
        return camera;
    }

    private static String normalizeBaseUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static void applyHeaders(org.springframework.http.HttpHeaders headers, CameraEntity camera) {
        String nodeKey = trimToNull(camera.getNodeApiKey());
        if (nodeKey == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Node API key is not configured");
        }
        headers.set("X-Edge-Key", nodeKey);
        String edgeNodeId = trimToNull(camera.getEdgeNodeId());
        if (edgeNodeId != null) {
            headers.set("X-Edge-Node-Id", edgeNodeId);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
