package com.smarttraffic.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.service.CameraPollerService;
import com.smarttraffic.backend.service.SystemMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class AdminMetricsWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AdminMetricsWebSocketHandler.class);

    private final SystemMetricsService systemMetricsService;
    private final CameraPollerService cameraPollerService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final WebSocketConnectionLimiter connectionLimiter;
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public AdminMetricsWebSocketHandler(SystemMetricsService systemMetricsService,
                                        CameraPollerService cameraPollerService,
                                        ObjectMapper objectMapper,
                                        ScheduledExecutorService webSocketScheduler,
                                        WebSocketConnectionLimiter connectionLimiter) {
        this.systemMetricsService = systemMetricsService;
        this.cameraPollerService = cameraPollerService;
        this.objectMapper = objectMapper;
        this.scheduler = webSocketScheduler;
        this.connectionLimiter = connectionLimiter;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (!connectionLimiter.tryAcquire(session)) {
            return;
        }
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> sendMetrics(session), 0, 2, TimeUnit.SECONDS);
        tasks.put(session.getId(), task);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("Transport error on metrics session {}: {}", session.getId(), exception.getMessage());
        cancel(session.getId());
        connectionLimiter.release();
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cancel(session.getId());
        connectionLimiter.release();
    }

    private void sendMetrics(WebSocketSession session) {
        if (!session.isOpen()) {
            cancel(session.getId());
            return;
        }
        try {
            Map<String, Object> combined = new LinkedHashMap<>(systemMetricsService.getSystemMetrics());
            combined.put("nodes", cameraPollerService.getNodeHealthMap());
            String payload = objectMapper.writeValueAsString(combined);
            session.sendMessage(new TextMessage(payload));
        } catch (Exception ex) {
            log.error("Failed to send metrics for session {}: {}", session.getId(), ex.getMessage(), ex);
            cancel(session.getId());
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (Exception closeEx) {
                log.warn("Failed to close session {} after send error: {}", session.getId(), closeEx.getMessage());
            }
        }
    }

    private void cancel(String sessionId) {
        ScheduledFuture<?> task = tasks.remove(sessionId);
        if (task != null) {
            task.cancel(true);
        }
    }
}
