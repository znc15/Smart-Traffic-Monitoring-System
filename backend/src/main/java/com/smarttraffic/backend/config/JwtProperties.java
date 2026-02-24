package com.smarttraffic.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private Integer accessTokenExpireDays = 7;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Integer getAccessTokenExpireDays() {
        return accessTokenExpireDays;
    }

    public void setAccessTokenExpireDays(Integer accessTokenExpireDays) {
        this.accessTokenExpireDays = accessTokenExpireDays;
    }
}
