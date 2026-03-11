package com.smarttraffic.backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityPropertiesTest {

    @Test
    void corsAllowedOriginsAsList_shouldExpandLocalhostAlias() {
        SecurityProperties properties = new SecurityProperties();
        properties.setCorsAllowedOrigins("http://localhost:5173");

        var origins = properties.corsAllowedOriginsAsList();

        assertEquals(2, origins.size());
        assertTrue(origins.contains("http://localhost:5173"));
        assertTrue(origins.contains("http://127.0.0.1:5173"));
    }

    @Test
    void corsAllowedOriginsAsList_shouldExpandLoopbackAlias() {
        SecurityProperties properties = new SecurityProperties();
        properties.setCorsAllowedOrigins("https://127.0.0.1:8443");

        var origins = properties.corsAllowedOriginsAsList();

        assertEquals(2, origins.size());
        assertTrue(origins.contains("https://127.0.0.1:8443"));
        assertTrue(origins.contains("https://localhost:8443"));
    }

    @Test
    void corsAllowedOriginsAsList_shouldKeepNonLocalOriginsUnchanged() {
        SecurityProperties properties = new SecurityProperties();
        properties.setCorsAllowedOrigins("https://traffic.example.com");

        var origins = properties.corsAllowedOriginsAsList();

        assertEquals(1, origins.size());
        assertTrue(origins.contains("https://traffic.example.com"));
    }
}
