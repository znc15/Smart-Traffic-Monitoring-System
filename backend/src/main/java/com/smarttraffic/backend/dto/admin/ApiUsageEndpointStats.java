package com.smarttraffic.backend.dto.admin;

public class ApiUsageEndpointStats {

    private String endpoint;
    private Long count;

    public ApiUsageEndpointStats(String endpoint, Long count) {
        this.endpoint = endpoint;
        this.count = count;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Long getCount() {
        return count;
    }
}
