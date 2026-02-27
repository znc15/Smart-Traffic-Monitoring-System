package com.smarttraffic.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.repository.ApiClientRepository;
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

    private final ApiClientRepository apiClientRepository;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthenticationFilter(ApiClientRepository apiClientRepository, ObjectMapper objectMapper) {
        this.apiClientRepository = apiClientRepository;
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
        if (key == null || key.isBlank() || apiClientRepository.findByApiKeyAndEnabledTrue(key).isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of("detail", "Invalid API key"));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
