package com.smarttraffic.backend.dto.admin;

import java.util.List;

public class ApiUsageResponse {

    private Long totalCalls;
    private List<ApiUsageDailyStats> dailyStats;
    private List<ApiUsageEndpointStats> endpointStats;

    public ApiUsageResponse(Long totalCalls, List<ApiUsageDailyStats> dailyStats, List<ApiUsageEndpointStats> endpointStats) {
        this.totalCalls = totalCalls;
        this.dailyStats = dailyStats;
        this.endpointStats = endpointStats;
    }

    public Long getTotalCalls() {
        return totalCalls;
    }

    public List<ApiUsageDailyStats> getDailyStats() {
        return dailyStats;
    }

    public List<ApiUsageEndpointStats> getEndpointStats() {
        return endpointStats;
    }
}
