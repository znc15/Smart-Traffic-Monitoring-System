package com.smarttraffic.backend.websocket;

import org.springframework.web.socket.WebSocketSession;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public final class WebSocketUtils {

    private WebSocketUtils() {
    }

    public static String extractRoadName(WebSocketSession session, String prefix) {
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

    public static void cancelScheduledTask(String sessionId, Map<String, ScheduledFuture<?>> tasks) {
        ScheduledFuture<?> task = tasks.remove(sessionId);
        if (task != null) {
            task.cancel(true);
        }
    }
}
