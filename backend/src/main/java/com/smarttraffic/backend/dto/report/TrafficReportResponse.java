package com.smarttraffic.backend.dto.report;

import java.time.LocalDateTime;
import java.util.List;

public record TrafficReportResponse(
        String granularity,
        String roadName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        List<TrafficReportRowResponse> rows
) {
}
