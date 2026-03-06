package com.smarttraffic.backend.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WebSocketConnectionLimiter {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConnectionLimiter.class);

    public static final int MAX_CONNECTIONS = 100;
    private static final CloseStatus POLICY_VIOLATION = new CloseStatus(1008, "Too many connections");

    private final AtomicInteger connectionCount = new AtomicInteger(0);

    /**
     * Try to acquire a connection slot. Returns true if accepted, false if limit exceeded
     * (in which case the session is closed automatically).
     */
    public boolean tryAcquire(WebSocketSession session) {
        int current = connectionCount.incrementAndGet();
        if (current > MAX_CONNECTIONS) {
            connectionCount.decrementAndGet();
            log.warn("WebSocket connection rejected (limit {}): session={}, remote={}",
                    MAX_CONNECTIONS, session.getId(), session.getRemoteAddress());
            try {
                session.close(POLICY_VIOLATION);
            } catch (IOException e) {
                log.error("Failed to close rejected WebSocket session {}: {}", session.getId(), e.getMessage());
            }
            return false;
        }
        log.debug("WebSocket connection accepted: session={}, total={}", session.getId(), current);
        return true;
    }

    public void release() {
        int current = connectionCount.decrementAndGet();
        if (current < 0) {
            connectionCount.set(0);
            log.warn("WebSocket connection count went negative, reset to 0");
        }
    }

    public int getConnectionCount() {
        return connectionCount.get();
    }
}
