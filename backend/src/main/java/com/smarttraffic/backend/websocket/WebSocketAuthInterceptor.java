package com.smarttraffic.backend.websocket;

import com.smarttraffic.backend.security.CurrentUser;
import com.smarttraffic.backend.security.CurrentUserResolver;
import com.smarttraffic.backend.security.TokenExtractionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final TokenExtractionService tokenExtractionService;
    private final CurrentUserResolver currentUserResolver;
    private final boolean adminOnly;
    private final boolean allowQueryToken;

    public WebSocketAuthInterceptor(
            TokenExtractionService tokenExtractionService,
            CurrentUserResolver currentUserResolver,
            boolean adminOnly,
            boolean allowQueryToken
    ) {
        this.tokenExtractionService = tokenExtractionService;
        this.currentUserResolver = currentUserResolver;
        this.adminOnly = adminOnly;
        this.allowQueryToken = allowQueryToken;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        List<String> cookieHeaders = request.getHeaders().get(HttpHeaders.COOKIE);
        Optional<String> token = tokenExtractionService.extractFromWebSocket(
                request.getURI(),
                authHeaders,
                cookieHeaders,
                allowQueryToken
        );
        if (token.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Optional<CurrentUser> user = currentUserResolver.resolve(token.get());
        if (user.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        if (adminOnly && !user.get().isAdmin()) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        attributes.put(WebSocketAttributes.CURRENT_USER, user.get());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}
