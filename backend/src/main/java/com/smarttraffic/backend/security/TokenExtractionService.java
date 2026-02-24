package com.smarttraffic.backend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Component
public class TokenExtractionService {

    public Optional<String> extractFromHttpRequest(HttpServletRequest request) {
        Optional<String> bearer = extractBearer(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (bearer.isPresent()) {
            return bearer;
        }

        Optional<String> cookie = extractFromCookies(request.getCookies());
        if (cookie.isPresent()) {
            return cookie;
        }

        String queryToken = request.getParameter("token");
        return Optional.ofNullable(StringUtils.hasText(queryToken) ? queryToken : null);
    }

    public Optional<String> extractFromWebSocket(URI uri, List<String> authorizationHeaders, List<String> cookieHeaders) {
        if (authorizationHeaders != null && !authorizationHeaders.isEmpty()) {
            Optional<String> bearer = extractBearer(authorizationHeaders.get(0));
            if (bearer.isPresent()) {
                return bearer;
            }
        }

        Optional<String> cookie = extractFromCookieHeader(cookieHeaders);
        if (cookie.isPresent()) {
            return cookie;
        }

        if (uri == null) {
            return Optional.empty();
        }

        List<String> tokens = UriComponentsBuilder.fromUri(uri).build().getQueryParams().get("token");
        String token = (tokens == null || tokens.isEmpty()) ? null : tokens.get(0);
        return Optional.ofNullable(StringUtils.hasText(token) ? token : null);
    }

    private Optional<String> extractBearer(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return Optional.empty();
        }
        if (authorization.startsWith("Bearer ")) {
            return Optional.of(authorization.substring(7));
        }
        return Optional.empty();
    }

    private Optional<String> extractFromCookies(Cookie[] cookies) {
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if ("access_token".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractFromCookieHeader(List<String> cookieHeaders) {
        if (cookieHeaders == null || cookieHeaders.isEmpty()) {
            return Optional.empty();
        }
        for (String header : cookieHeaders) {
            if (!StringUtils.hasText(header)) {
                continue;
            }
            String[] parts = header.split(";");
            for (String part : parts) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && "access_token".equals(kv[0].trim()) && StringUtils.hasText(kv[1])) {
                    return Optional.of(kv[1].trim());
                }
            }
        }
        return Optional.empty();
    }
}
