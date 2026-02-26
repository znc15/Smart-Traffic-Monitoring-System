package com.smarttraffic.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppRuntimeProperties {
    private String env = "development";

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public boolean isDevelopment() {
        return env == null || env.equalsIgnoreCase("development");
    }
}

