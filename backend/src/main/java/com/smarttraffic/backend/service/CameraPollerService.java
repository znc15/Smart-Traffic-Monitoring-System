package com.smarttraffic.backend.service;

import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CameraPollerService {

    private static final Logger log = LoggerFactory.getLogger(CameraPollerService.class);
    private static final String HEALTH_ONLINE = "online";
    private static final String HEALTH_DEGRADED = "degraded";
    private static final String HEALTH_OFFLINE = "offline";
    private static final String REASON_AUTH_FAILED = "auth_failed";
    private static final String REASON_TIMEOUT = "timeout";
    private static final String REASON_TRAFFIC_FETCH_FAILED = "traffic_fetch_failed";
    private static final String REASON_FRAME_FETCH_FAILED = "frame_fetch_failed";
    private static final String STAGE_TRAFFIC = "traffic";
    private static final String STAGE_FRAME = "frame";

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
            String previousHealthStatus = health.healthStatus;

            long start = System.nanoTime();
            try {
                PollOutcome outcome = pollCamera(cam, roadKey, nodeUrl);
                long elapsed = (System.nanoTime() - start) / 1_000_000;
                health.lastSuccessTime = Instant.now();
                health.latencyMs = elapsed;

                if (outcome.frameFailure() == null) {
                    markHealthy(health);
                    if (!HEALTH_ONLINE.equals(previousHealthStatus)) {
                        log.info("边缘节点已恢复在线: {} ({} ms)", roadKey, elapsed);
                    }
                } else {
                    markDegraded(health, outcome.frameFailure());
                    if (!HEALTH_DEGRADED.equals(previousHealthStatus)) {
                        log.warn("边缘节点在线但已降级: {} ({})", roadKey, outcome.frameFailure().lastError());
                    }
                }
            } catch (Exception e) {
                FailureDescriptor failure = classifyTrafficFailure(e);
                markOffline(health, failure);
                if (!HEALTH_OFFLINE.equals(previousHealthStatus)) {
                    log.warn("边缘节点离线: {} ({})", roadKey, failure.lastError());
                }
                log.debug("轮询摄像头 {} 失败: {}", roadKey, failure.lastError());
            }
        }
    }

    private void markHealthy(NodeHealth health) {
        health.online = true;
        health.healthStatus = HEALTH_ONLINE;
        health.consecutiveFailures = 0;
        health.statusReasonCode = null;
        health.statusReasonMessage = null;
        health.lastErrorStage = null;
        health.lastError = null;
    }

    private void markDegraded(NodeHealth health, FailureDescriptor failure) {
        health.online = true;
        health.healthStatus = failure.healthStatus();
        health.errorCount++;
        health.consecutiveFailures++;
        health.statusReasonCode = failure.reasonCode();
        health.statusReasonMessage = failure.reasonMessage();
        health.lastErrorStage = failure.stage();
        health.lastError = failure.lastError();
    }

    private void markOffline(NodeHealth health, FailureDescriptor failure) {
        health.online = false;
        health.healthStatus = failure.healthStatus();
        health.errorCount++;
        health.consecutiveFailures++;
        health.statusReasonCode = failure.reasonCode();
        health.statusReasonMessage = failure.reasonMessage();
        health.lastErrorStage = failure.stage();
        health.lastError = failure.lastError();
    }

    private PollOutcome pollCamera(CameraEntity camera, String roadKey, String baseUrl) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String edgeNodeId = trimToNull(camera.getEdgeNodeId());
        String edgeKey = trimToNull(camera.getNodeApiKey());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = restClient.get()
                .uri(base + "/api/traffic")
                .headers(headers -> applyEdgeHeaders(headers, edgeNodeId, edgeKey))
                .retrieve()
                .body(Map.class);

        byte[] frame = null;
        FailureDescriptor frameFailure = null;
        try {
            frame = restClient.get()
                    .uri(base + "/api/frame")
                    .headers(headers -> applyEdgeHeaders(headers, edgeNodeId, edgeKey))
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception ex) {
            frameFailure = classifyFrameFailure(ex);
        }

        trafficService.updateFromRemote(roadKey, data, frame);

        if (data != null && data.containsKey("edge_metrics")) {
            NodeHealth h = nodeHealthMap.get(roadKey);
            if (h != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> em = (Map<String, Object>) data.get("edge_metrics");
                h.edgeMetrics = em;
            }
        }

        return new PollOutcome(frameFailure);
    }

    private FailureDescriptor classifyTrafficFailure(Exception ex) {
        if (isAuthFailure(ex)) {
            return new FailureDescriptor(
                    HEALTH_OFFLINE,
                    REASON_AUTH_FAILED,
                    "节点鉴权失败，请检查 edge key / node id",
                    STAGE_TRAFFIC,
                    buildErrorMessage(ex)
            );
        }
        if (isTimeoutFailure(ex)) {
            return new FailureDescriptor(
                    HEALTH_OFFLINE,
                    REASON_TIMEOUT,
                    "节点请求超时",
                    STAGE_TRAFFIC,
                    buildErrorMessage(ex)
            );
        }
        return new FailureDescriptor(
                HEALTH_OFFLINE,
                REASON_TRAFFIC_FETCH_FAILED,
                "交通数据拉取失败",
                STAGE_TRAFFIC,
                buildErrorMessage(ex)
        );
    }

    private FailureDescriptor classifyFrameFailure(Exception ex) {
        return new FailureDescriptor(
                HEALTH_DEGRADED,
                REASON_FRAME_FETCH_FAILED,
                "视频帧拉取失败",
                STAGE_FRAME,
                buildErrorMessage(ex)
        );
    }

    private static boolean isAuthFailure(Throwable ex) {
        if (ex instanceof RestClientResponseException restEx) {
            int status = restEx.getStatusCode().value();
            return status == 401 || status == 403;
        }
        return false;
    }

    private static boolean isTimeoutFailure(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            if (current instanceof ResourceAccessException) {
                String message = current.getMessage();
                if (message != null) {
                    String normalized = message.toLowerCase();
                    if (normalized.contains("timed out") || normalized.contains("timeout")) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("timed out") || normalized.contains("timeout");
    }

    private static String buildErrorMessage(Throwable ex) {
        String message = ex.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return ex.getClass().getSimpleName();
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
            info.put("health_status", h.healthStatus);
            info.put("status_reason_code", h.statusReasonCode);
            info.put("status_reason_message", h.statusReasonMessage);
            info.put("last_error_stage", h.lastErrorStage);
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

    private record PollOutcome(FailureDescriptor frameFailure) {
    }

    private record FailureDescriptor(
            String healthStatus,
            String reasonCode,
            String reasonMessage,
            String stage,
            String lastError
    ) {
    }

    // 摄像头节点健康数据
    static class NodeHealth {
        volatile Long cameraId;
        volatile String name;
        volatile String roadName;
        volatile String edgeNodeId;
        volatile String nodeUrl;
        volatile boolean online;
        volatile String healthStatus = HEALTH_OFFLINE;
        volatile String statusReasonCode;
        volatile String statusReasonMessage;
        volatile String lastErrorStage;
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
