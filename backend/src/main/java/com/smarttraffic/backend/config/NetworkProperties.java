package com.smarttraffic.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.network")
public class NetworkProperties {
    private String baseUrlApi = "http://localhost:8000";

    public String getBaseUrlApi() {
        return baseUrlApi;
    }

    public void setBaseUrlApi(String baseUrlApi) {
        this.baseUrlApi = baseUrlApi;
    }
}
