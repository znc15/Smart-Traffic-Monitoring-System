package com.smarttraffic.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.model.ApiClientEntity;
import com.smarttraffic.backend.repository.ApiClientRepository;
import com.smarttraffic.backend.service.analytics.RedisRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final int RATE_LIMIT_WINDOW_SECONDS = 86400; // 1 day

    private final ApiClientRepository apiClientRepository;
    private final RedisRateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthenticationFilter(ApiClientRepository apiClientRepository,
                                      RedisRateLimitService rateLimitService,
                                      ObjectMapper objectMapper) {
        this.apiClientRepository = apiClientRepository;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/api/v1/maas/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getHeader(API_KEY_HEADER);
        if (key == null || key.isBlank()) {
            respondUnauthorized(response, "Missing API key");
            return;
        }

        var clientOpt = apiClientRepository.findByApiKeyAndEnabledTrue(key);
        if (clientOpt.isEmpty()) {
            respondUnauthorized(response, "Invalid API key");
            return;
        }

        ApiClientEntity client = clientOpt.get();
        if (!rateLimitService.isAllowed(key, RATE_LIMIT_WINDOW_SECONDS, client.getRateLimit())) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of("detail", "Rate limit exceeded"));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void respondUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("detail", message));
    }
}