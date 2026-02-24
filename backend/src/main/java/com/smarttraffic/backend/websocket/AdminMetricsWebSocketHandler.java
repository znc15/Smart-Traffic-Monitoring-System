package com.smarttraffic.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.service.SystemMetricsService;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class AdminMetricsWebSocketHandler extends TextWebSocketHandler {

    private final SystemMetricsService systemMetricsService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public AdminMetricsWebSocketHandler(SystemMetricsService systemMetricsService, ObjectMapper objectMapper) {
        this.systemMetricsService = systemMetricsService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> sendMetrics(session), 0, 2, TimeUnit.SECONDS);
        tasks.put(session.getId(), task);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        cancel(session.getId());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cancel(session.getId());
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void sendMetrics(WebSocketSession session) {
        if (!session.isOpen()) {
            cancel(session.getId());
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(systemMetricsService.getSystemMetrics());
            session.sendMessage(new TextMessage(payload));
        } catch (Exception ex) {
            cancel(session.getId());
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (Exception ignored) {
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
