package com.smarttraffic.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.service.TrafficService;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class TrafficInfoWebSocketHandler extends TextWebSocketHandler {

    private final TrafficService trafficService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public TrafficInfoWebSocketHandler(TrafficService trafficService, ObjectMapper objectMapper) {
        this.trafficService = trafficService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String roadName = extractRoadName(session, "/api/v1/ws/info/");
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> sendInfo(session, roadName), 0, 200, TimeUnit.MILLISECONDS);
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

    private void sendInfo(WebSocketSession session, String roadName) {
        if (!session.isOpen()) {
            cancel(session.getId());
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(trafficService.info(roadName));
            session.sendMessage(new TextMessage(payload));
        } catch (Exception ex) {
            cancel(session.getId());
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (Exception ignored) {
            }
        }
    }

    private String extractRoadName(WebSocketSession session, String prefix) {
        if (session.getUri() == null || session.getUri().getPath() == null) {
            return "";
        }
        String path = session.getUri().getPath();
        int index = path.indexOf(prefix);
        if (index < 0) {
            return "";
        }
        String encoded = path.substring(index + prefix.length());
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    private void cancel(String sessionId) {
        ScheduledFuture<?> task = tasks.remove(sessionId);
        if (task != null) {
            task.cancel(true);
        }
    }
}
