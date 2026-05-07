package com.smarttraffic.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.db")
public class DbRuntimeProperties {

    private boolean redisCacheEnabled = true;
    private long cacheTtlSeconds = 10;

    public boolean isRedisCacheEnabled() {
        return redisCacheEnabled;
    }

    public void setRedisCacheEnabled(boolean redisCacheEnabled) {
        this.redisCacheEnabled = redisCacheEnabled;
    }

    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }
}
