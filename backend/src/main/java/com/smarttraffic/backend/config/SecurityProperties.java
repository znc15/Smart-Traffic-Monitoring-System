package com.smarttraffic.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    private String corsAllowedOrigins = "http://localhost:5173";
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
            return List.of("http://localhost:5173");
        }
        return Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}

