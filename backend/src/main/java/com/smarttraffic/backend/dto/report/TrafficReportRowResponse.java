package com.smarttraffic.backend.dto.report;

import java.time.LocalDateTime;

public record TrafficReportRowResponse(
        LocalDateTime bucketAt,
        String roadName,
        Double totalFlow,
        Double avgCongestionIndex
) {
}
