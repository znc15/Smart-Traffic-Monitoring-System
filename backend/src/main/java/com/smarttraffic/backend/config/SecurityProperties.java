package com.smarttraffic.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    private static final String DEFAULT_CORS_ALLOWED_ORIGINS =
            "http://localhost:5173,http://127.0.0.1:5173,http://localhost:5174,http://127.0.0.1:5174";

    private String corsAllowedOrigins = DEFAULT_CORS_ALLOWED_ORIGINS;
    private boolean wsAllowQueryToken = true;

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public boolean isWsAllowQueryToken() {
        return wsAllowQueryToken;
    }

    public void setWsAllowQueryToken(boolean wsAllowQueryToken) {
        this.wsAllowQueryToken = wsAllowQueryToken;
    }

    public List<String> corsAllowedOriginsAsList() {
        if (!StringUtils.hasText(corsAllowedOrigins)) {
            return Arrays.stream(DEFAULT_CORS_ALLOWED_ORIGINS.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        }
        return Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
