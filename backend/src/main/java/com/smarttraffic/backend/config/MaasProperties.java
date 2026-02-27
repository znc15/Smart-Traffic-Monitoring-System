package com.smarttraffic.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.maas")
public class MaasProperties {

    private String defaultClientName = "default-dev-client";
    private String defaultApiKey = "dev-maas-key-change-me";

    public String getDefaultClientName() {
        return defaultClientName;
    }

    public void setDefaultClientName(String defaultClientName) {
        this.defaultClientName = defaultClientName;
    }

    public String getDefaultApiKey() {
        return defaultApiKey;
    }

    public void setDefaultApiKey(String defaultApiKey) {
        this.defaultApiKey = defaultApiKey;
    }
}
