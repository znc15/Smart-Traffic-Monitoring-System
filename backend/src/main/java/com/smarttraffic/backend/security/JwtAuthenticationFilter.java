package com.smarttraffic.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenExtractionService tokenExtractionService;
    private final JwtService jwtService;

    public JwtAuthenticationFilter(TokenExtractionService tokenExtractionService, JwtService jwtService) {
        this.tokenExtractionService = tokenExtractionService;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Optional<String> token = tokenExtractionService.extractFromHttpRequest(request);
        token.flatMap(jwtService::parseToken)
                .ifPresent(currentUser -> SecurityContextHolder.getContext()
                        .setAuthentication(new CurrentUserAuthentication(currentUser, token.get())));
        filterChain.doFilter(request, response);
    }
}
