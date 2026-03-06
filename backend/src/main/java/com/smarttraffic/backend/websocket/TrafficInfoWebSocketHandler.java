package com.smarttraffic.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.service.TrafficService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class TrafficInfoWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TrafficInfoWebSocketHandler.class);

    private final TrafficService trafficService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final WebSocketConnectionLimiter connectionLimiter;
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public TrafficInfoWebSocketHandler(TrafficService trafficService,
                                       ObjectMapper objectMapper,
                                       ScheduledExecutorService webSocketScheduler,
                                       WebSocketConnectionLimiter connectionLimiter) {
        this.trafficService = trafficService;
        this.objectMapper = objectMapper;
        this.scheduler = webSocketScheduler;
        this.connectionLimiter = connectionLimiter;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (!connectionLimiter.tryAcquire(session)) {
            return;
        }
        String roadName = extractRoadName(session, "/api/v1/ws/info/");
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> sendInfo(session, roadName), 0, 200, TimeUnit.MILLISECONDS);
        tasks.put(session.getId(), task);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("Transport error on info session {}: {}", session.getId(), exception.getMessage());
        cancel(session.getId());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cancel(session.getId());
        connectionLimiter.release(session.getId());
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
            log.error("Failed to send info for session {}, road '{}': {}", session.getId(), roadName, ex.getMessage(), ex);
            cancel(session.getId());
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (Exception closeEx) {
                log.warn("Failed to close session {} after send error: {}", session.getId(), closeEx.getMessage());
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
