package com.smarttraffic.backend.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenExtractionServiceTest {

    private final TokenExtractionService service = new TokenExtractionService();

    @Test
    void extractFromHttpRequest_shouldUseAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer header-token");
        request.setCookies(new Cookie("access_token", "cookie-token"));

        var token = service.extractFromHttpRequest(request);

        assertTrue(token.isPresent());
        assertEquals("header-token", token.get());
    }

    @Test
    void extractFromHttpRequest_shouldUseCookieWhenNoHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access_token", "cookie-token"));

        var token = service.extractFromHttpRequest(request);

        assertTrue(token.isPresent());
        assertEquals("cookie-token", token.get());
    }

    @Test
    void extractFromHttpRequest_shouldIgnoreQueryToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("token", "query-token");

        var token = service.extractFromHttpRequest(request);

        assertTrue(token.isEmpty());
    }

    @Test
    void extractFromWebSocket_shouldRespectQueryTokenSwitch() {
        URI uri = URI.create("ws://localhost:8000/api/v1/ws/info/test?token=query-token");

        var disabled = service.extractFromWebSocket(uri, List.of(), List.of(), false);
        var enabled = service.extractFromWebSocket(uri, List.of(), List.of(), true);

        assertTrue(disabled.isEmpty());
        assertTrue(enabled.isPresent());
        assertEquals("query-token", enabled.get());
    }
}
