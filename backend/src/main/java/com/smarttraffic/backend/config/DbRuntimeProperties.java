package com.smarttraffic.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.db")
public class DbRuntimeProperties {

    private String primary = "postgres";
    private boolean mirrorWrite = false;
    private boolean redisCacheEnabled = true;
    private long cacheTtlSeconds = 10;

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public boolean isMirrorWrite() {
        return mirrorWrite;
    }

    public void setMirrorWrite(boolean mirrorWrite) {
        this.mirrorWrite = mirrorWrite;
    }

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

    public boolean isPrimaryMysql() {
        return primary != null && primary.equalsIgnoreCase("mysql");
    }

    public boolean isPrimaryPostgres() {
        return !isPrimaryMysql();
    }
}
