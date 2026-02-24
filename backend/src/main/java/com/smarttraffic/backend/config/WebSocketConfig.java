package com.smarttraffic.backend.config;

import com.smarttraffic.backend.security.JwtService;
import com.smarttraffic.backend.security.TokenExtractionService;
import com.smarttraffic.backend.websocket.AdminMetricsWebSocketHandler;
import com.smarttraffic.backend.websocket.ChatWebSocketHandler;
import com.smarttraffic.backend.websocket.FrameWebSocketHandler;
import com.smarttraffic.backend.websocket.TrafficInfoWebSocketHandler;
import com.smarttraffic.backend.websocket.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final TrafficInfoWebSocketHandler trafficInfoWebSocketHandler;
    private final FrameWebSocketHandler frameWebSocketHandler;
    private final AdminMetricsWebSocketHandler adminMetricsWebSocketHandler;
    private final TokenExtractionService tokenExtractionService;
    private final JwtService jwtService;

    public WebSocketConfig(
            ChatWebSocketHandler chatWebSocketHandler,
            TrafficInfoWebSocketHandler trafficInfoWebSocketHandler,
            FrameWebSocketHandler frameWebSocketHandler,
            AdminMetricsWebSocketHandler adminMetricsWebSocketHandler,
            TokenExtractionService tokenExtractionService,
            JwtService jwtService
    ) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.trafficInfoWebSocketHandler = trafficInfoWebSocketHandler;
        this.frameWebSocketHandler = frameWebSocketHandler;
        this.adminMetricsWebSocketHandler = adminMetricsWebSocketHandler;
        this.tokenExtractionService = tokenExtractionService;
        this.jwtService = jwtService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        WebSocketAuthInterceptor userAuth = new WebSocketAuthInterceptor(tokenExtractionService, jwtService, false);
        WebSocketAuthInterceptor adminAuth = new WebSocketAuthInterceptor(tokenExtractionService, jwtService, true);

        registry.addHandler(chatWebSocketHandler, "/api/v1/ws/chat")
                .addInterceptors(userAuth)
                .setAllowedOriginPatterns("*");

        registry.addHandler(trafficInfoWebSocketHandler, "/api/v1/ws/info/*")
                .addInterceptors(userAuth)
                .setAllowedOriginPatterns("*");

        registry.addHandler(frameWebSocketHandler, "/api/v1/ws/frames/*")
                .addInterceptors(userAuth)
                .setAllowedOriginPatterns("*");

        registry.addHandler(adminMetricsWebSocketHandler, "/api/v1/admin/ws/resources")
                .addInterceptors(adminAuth)
                .setAllowedOriginPatterns("*");
    }
}
