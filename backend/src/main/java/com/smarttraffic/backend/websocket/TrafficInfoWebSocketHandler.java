package com.smarttraffic.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.service.RoadService;
import com.smarttraffic.backend.service.TrafficService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class TrafficInfoWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TrafficInfoWebSocketHandler.class);

    private final TrafficService trafficService;
    private final RoadService roadService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final WebSocketConnectionLimiter connectionLimiter;
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public TrafficInfoWebSocketHandler(TrafficService trafficService,
                                       RoadService roadService,
                                       ObjectMapper objectMapper,
                                       ScheduledExecutorService webSocketScheduler,
                                       WebSocketConnectionLimiter connectionLimiter) {
        this.trafficService = trafficService;
        this.roadService = roadService;
        this.objectMapper = objectMapper;
        this.scheduler = webSocketScheduler;
        this.connectionLimiter = connectionLimiter;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!connectionLimiter.tryAcquire(session)) {
            return;
        }
        String roadName = WebSocketUtils.extractRoadName(session, "/api/v1/ws/info/");
        if (!roadService.getActiveRoads().contains(roadName)) {
            log.warn("Info WebSocket rejected: unknown road '{}', session {}", roadName, session.getId());
            connectionLimiter.release(session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> sendInfo(session, roadName), 0, 200, TimeUnit.MILLISECONDS);
        tasks.put(session.getId(), task);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("Transport error on info session {}: {}", session.getId(), exception.getMessage());
        WebSocketUtils.cancelScheduledTask(session.getId(), tasks);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WebSocketUtils.cancelScheduledTask(session.getId(), tasks);
        connectionLimiter.release(session.getId());
    }

    private void sendInfo(WebSocketSession session, String roadName) {
        if (!session.isOpen()) {
            WebSocketUtils.cancelScheduledTask(session.getId(), tasks);
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(trafficService.info(roadName));
            session.sendMessage(new TextMessage(payload));
        } catch (Exception ex) {
            log.error("Failed to send info for session {}, road '{}': {}", session.getId(), roadName, ex.getMessage(), ex);
            WebSocketUtils.cancelScheduledTask(session.getId(), tasks);
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (Exception closeEx) {
                log.warn("Failed to close session {} after send error: {}", session.getId(), closeEx.getMessage());
            }
        }
    }

    private String extractRoadName(WebSocketSession session, String prefix) {
        return WebSocketUtils.extractRoadName(session, prefix);
    }

    private void cancel(String sessionId) {
        WebSocketUtils.cancelScheduledTask(sessionId, tasks);
    }
}
