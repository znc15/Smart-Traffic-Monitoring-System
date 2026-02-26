package com.smarttraffic.backend.security;

import com.smarttraffic.backend.config.AppRuntimeProperties;
import com.smarttraffic.backend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    private static final String DEFAULT_DEV_SECRET = "change-this-dev-secret-change-this-dev-secret";

    private final JwtProperties jwtProperties;
    private final AppRuntimeProperties appRuntimeProperties;

    public JwtService(JwtProperties jwtProperties, AppRuntimeProperties appRuntimeProperties) {
        this.jwtProperties = jwtProperties;
        this.appRuntimeProperties = appRuntimeProperties;
        validateSecretConfiguration();
    }

    public String createAccessToken(CurrentUser currentUser) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofDays(jwtProperties.getAccessTokenExpireDays()));

        return Jwts.builder()
                .subject(currentUser.email())
                .claims(Map.of(
                        "uid", currentUser.id(),
                        "username", currentUser.username(),
                        "email", currentUser.email(),
                        "phone_number", currentUser.phoneNumber(),
                        "role_id", currentUser.roleId()
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey())
                .compact();
    }

    public Optional<CurrentUser> parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String email = claims.getSubject();
            if (!StringUtils.hasText(email)) {
                return Optional.empty();
            }

            Long id = toLong(claims.get("uid"));
            String username = toStringValue(claims.get("username"));
            String phone = toStringValue(claims.get("phone_number"));
            Integer roleId = toInteger(claims.get("role_id"));

            if (id == null) {
                return Optional.empty();
            }

            return Optional.of(new CurrentUser(id, username, email, phone, roleId));
        } catch (JwtException ex) {
            return Optional.empty();
        }
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    private void validateSecretConfiguration() {
        String secret = jwtProperties.getSecret();
        if (!StringUtils.hasText(secret) || secret.length() < 32) {
            throw new IllegalStateException("JWT secret 长度不足，至少需要 32 个字符");
        }
        if (!appRuntimeProperties.isDevelopment() && DEFAULT_DEV_SECRET.equals(secret)) {
            throw new IllegalStateException("非开发环境禁止使用默认 JWT secret");
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String s && StringUtils.hasText(s)) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String s && StringUtils.hasText(s)) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
