package com.smarttraffic.backend.security;

import com.smarttraffic.backend.model.ApiClientEntity;
import com.smarttraffic.backend.model.ApiUsageLogEntity;
import com.smarttraffic.backend.repository.ApiClientRepository;
import com.smarttraffic.backend.repository.ApiUsageLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@Order(200)
public class ApiUsageLoggingFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiUsageLogRepository apiUsageLogRepository;
    private final ApiClientRepository apiClientRepository;

    public ApiUsageLoggingFilter(
            ApiUsageLogRepository apiUsageLogRepository,
            ApiClientRepository apiClientRepository) {
        this.apiUsageLogRepository = apiUsageLogRepository;
        this.apiClientRepository = apiClientRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/api/v1/maas/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, wrappedResponse);
        } finally {
            long responseTimeMs = System.currentTimeMillis() - startTime;
            int statusCode = wrappedResponse.getStatus();
            String endpoint = request.getRequestURI();
            String method = request.getMethod();
            String requestIp = resolveClientIp(request);
            String apiKey = request.getHeader(API_KEY_HEADER);

            logUsageAsync(apiKey, endpoint, method, statusCode, responseTimeMs, requestIp);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logUsageAsync(String apiKey, String endpoint, String method,
                                int statusCode, long responseTimeMs, String requestIp) {
        CompletableFuture.runAsync(() -> {
            Long clientId = null;

            if (apiKey != null && !apiKey.isBlank()) {
                Optional<ApiClientEntity> clientOpt = apiClientRepository.findByApiKeyAndEnabledTrue(apiKey);
                if (clientOpt.isPresent()) {
                    ApiClientEntity client = clientOpt.get();
                    clientId = client.getId();
                    client.setLastUsedAt(LocalDateTime.now());
                    apiClientRepository.save(client);
                }
            }

            ApiUsageLogEntity log = new ApiUsageLogEntity();
            log.setApiClientId(clientId);
            log.setEndpoint(endpoint);
            log.setMethod(method);
            log.setStatusCode(statusCode);
            log.setResponseTimeMs(responseTimeMs);
            log.setRequestIp(requestIp);
            apiUsageLogRepository.save(log);
        });
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
