package com.smarttraffic.backend.config;

import com.smarttraffic.backend.security.CurrentUserResolver;
import com.smarttraffic.backend.security.TokenExtractionService;
import com.smarttraffic.backend.websocket.AdminMetricsWebSocketHandler;
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

    private final TrafficInfoWebSocketHandler trafficInfoWebSocketHandler;
    private final FrameWebSocketHandler frameWebSocketHandler;
    private final AdminMetricsWebSocketHandler adminMetricsWebSocketHandler;
    private final TokenExtractionService tokenExtractionService;
    private final CurrentUserResolver currentUserResolver;
    private final SecurityProperties securityProperties;

    public WebSocketConfig(
            TrafficInfoWebSocketHandler trafficInfoWebSocketHandler,
            FrameWebSocketHandler frameWebSocketHandler,
            AdminMetricsWebSocketHandler adminMetricsWebSocketHandler,
            TokenExtractionService tokenExtractionService,
            CurrentUserResolver currentUserResolver,
            SecurityProperties securityProperties
    ) {
        this.trafficInfoWebSocketHandler = trafficInfoWebSocketHandler;
        this.frameWebSocketHandler = frameWebSocketHandler;
        this.adminMetricsWebSocketHandler = adminMetricsWebSocketHandler;
        this.tokenExtractionService = tokenExtractionService;
        this.currentUserResolver = currentUserResolver;
        this.securityProperties = securityProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] allowedOrigins = securityProperties.corsAllowedOriginsAsList().toArray(new String[0]);
        boolean wsAllowQueryToken = securityProperties.isWsAllowQueryToken();

        WebSocketAuthInterceptor userAuth = new WebSocketAuthInterceptor(
                tokenExtractionService,
                currentUserResolver,
                false,
                wsAllowQueryToken
        );
        WebSocketAuthInterceptor adminAuth = new WebSocketAuthInterceptor(
                tokenExtractionService,
                currentUserResolver,
                true,
                wsAllowQueryToken
        );

        registry.addHandler(trafficInfoWebSocketHandler, "/api/v1/ws/info/*")
                .addInterceptors(userAuth)
                .setAllowedOrigins(allowedOrigins);

        registry.addHandler(frameWebSocketHandler, "/api/v1/ws/frames/*")
                .addInterceptors(userAuth)
                .setAllowedOrigins(allowedOrigins);

        registry.addHandler(adminMetricsWebSocketHandler, "/api/v1/admin/ws/resources")
                .addInterceptors(adminAuth)
                .setAllowedOrigins(allowedOrigins);
    }
}
