package com.smarttraffic.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.model.AlertEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AlertWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final WebSocketConnectionLimiter connectionLimiter;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public AlertWebSocketHandler(ObjectMapper objectMapper, WebSocketConnectionLimiter connectionLimiter) {
        this.objectMapper = objectMapper;
        this.connectionLimiter = connectionLimiter;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!connectionLimiter.tryAcquire(session)) {
            return;
        }
        sessions.put(session.getId(), session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("Transport error on alerts session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session.getId());
        connectionLimiter.release(session.getId());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        connectionLimiter.release(session.getId());
    }

    public void broadcast(AlertEntity alert) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(alert);
        } catch (Exception ex) {
            log.error("Failed to serialize alert {}: {}", alert != null ? alert.getId() : null, ex.getMessage(), ex);
            return;
        }

        sessions.values().forEach((session) -> {
            if (!session.isOpen()) {
                sessions.remove(session.getId());
                connectionLimiter.release(session.getId());
                return;
            }
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (Exception ex) {
                log.warn("Failed to send alert to session {}: {}", session.getId(), ex.getMessage());
                sessions.remove(session.getId());
                connectionLimiter.release(session.getId());
                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (Exception closeEx) {
                    log.warn("Failed to close alerts session {} after send error: {}", session.getId(), closeEx.getMessage());
                }
            }
        });
    }
}

