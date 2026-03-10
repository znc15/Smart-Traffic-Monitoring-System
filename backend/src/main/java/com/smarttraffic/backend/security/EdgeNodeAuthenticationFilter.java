package com.smarttraffic.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.repository.CameraRepository;
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
public class EdgeNodeAuthenticationFilter extends OncePerRequestFilter {

    private static final String EDGE_KEY_HEADER = "X-Edge-Key";
    private static final String EDGE_NODE_ID_HEADER = "X-Edge-Node-Id";

    private final CameraRepository cameraRepository;
    private final ObjectMapper objectMapper;

    public EdgeNodeAuthenticationFilter(CameraRepository cameraRepository, ObjectMapper objectMapper) {
        this.cameraRepository = cameraRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !"/api/v1/edge/telemetry".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String edgeNodeId = request.getHeader(EDGE_NODE_ID_HEADER);
        String edgeKey = request.getHeader(EDGE_KEY_HEADER);
        if (edgeNodeId == null || edgeNodeId.isBlank() || edgeKey == null || edgeKey.isBlank()) {
            reject(response, "Missing edge authentication headers");
            return;
        }

        CameraEntity camera = cameraRepository.findFirstByEdgeNodeIdAndNodeApiKeyAndEnabledTrue(edgeNodeId.trim(), edgeKey.trim())
                .orElse(null);
        if (camera == null) {
            reject(response, "Invalid edge credentials");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String detail) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("detail", detail));
    }
}
