package com.smarttraffic.backend.security;

import com.smarttraffic.backend.config.AppRuntimeProperties;
import com.smarttraffic.backend.config.JwtProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    @Test
    void shouldRejectDefaultSecretOutsideDevelopment() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("change-this-dev-secret-change-this-dev-secret");
        jwtProperties.setAccessTokenExpireDays(7);

        AppRuntimeProperties runtime = new AppRuntimeProperties();
        runtime.setEnv("production");

        assertThrows(IllegalStateException.class, () -> new JwtService(jwtProperties, runtime));
    }

    @Test
    void shouldAllowDefaultSecretInDevelopment() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("change-this-dev-secret-change-this-dev-secret");
        jwtProperties.setAccessTokenExpireDays(7);

        AppRuntimeProperties runtime = new AppRuntimeProperties();
        runtime.setEnv("development");

        assertDoesNotThrow(() -> new JwtService(jwtProperties, runtime));
    }
}
