package com.smarttraffic.backend.dto.admin;

public class ApiUsageDailyStats {

    private String date;
    private Long count;

    public ApiUsageDailyStats(String date, Long count) {
        this.date = date;
        this.count = count;
    }

    public String getDate() {
        return date;
    }

    public Long getCount() {
        return count;
    }
}
