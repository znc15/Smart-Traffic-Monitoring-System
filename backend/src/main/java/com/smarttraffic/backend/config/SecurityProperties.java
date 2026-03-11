package com.smarttraffic.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        String configuredOrigins = StringUtils.hasText(corsAllowedOrigins)
                ? corsAllowedOrigins
                : DEFAULT_CORS_ALLOWED_ORIGINS;

        Set<String> expandedOrigins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        // 本地开发常见会在 localhost / 127.0.0.1 间切换，这里自动补齐别名，
        // 避免 docker-compose 只注入一个 origin 时触发 "Invalid CORS request"。
        expandedOrigins.addAll(expandLocalAliases(expandedOrigins));
        return List.copyOf(expandedOrigins);
    }

    private static Set<String> expandLocalAliases(Set<String> origins) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        for (String origin : origins) {
            try {
                URI uri = new URI(origin);
                String scheme = uri.getScheme();
                String host = uri.getHost();
                int port = uri.getPort();
                if (!StringUtils.hasText(scheme) || !StringUtils.hasText(host)) {
                    continue;
                }

                if ("localhost".equalsIgnoreCase(host)) {
                    aliases.add(buildOriginAlias(scheme, "127.0.0.1", port));
                } else if ("127.0.0.1".equals(host)) {
                    aliases.add(buildOriginAlias(scheme, "localhost", port));
                }
            } catch (URISyntaxException ignored) {
                // Ignore malformed custom origins and keep the original configured value.
            }
        }
        return aliases;
    }

    private static String buildOriginAlias(String scheme, String host, int port) {
        try {
            return new URI(scheme, null, host, port, null, null, null).toString();
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("Failed to build CORS origin alias", ex);
        }
    }
}
