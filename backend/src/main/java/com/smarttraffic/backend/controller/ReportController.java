package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.report.TrafficReportResponse;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.service.analytics.TrafficReportExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final TrafficReportExportService trafficReportExportService;

    public ReportController(TrafficReportExportService trafficReportExportService) {
        this.trafficReportExportService = trafficReportExportService;
    }

    @GetMapping("/traffic/export")
    public ResponseEntity<?> exportTrafficReport(
            @RequestParam(value = "granularity", defaultValue = "hourly") String granularity,
            @RequestParam(value = "road_name", required = false) String roadName,
            @RequestParam(value = "start_at", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam(value = "end_at", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
            @RequestParam(value = "format", defaultValue = "json") String format
    ) {
        String normalizedFormat = format == null ? "json" : format.trim().toLowerCase(Locale.ROOT);
        if (!normalizedFormat.equals("json") && !normalizedFormat.equals("xlsx")) {
            throw new AppException(HttpStatus.BAD_REQUEST, "format must be json or xlsx");
        }

        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "end_at must be greater than or equal to start_at");
        }

        if (normalizedFormat.equals("json")) {
            TrafficReportResponse payload = trafficReportExportService.queryReport(granularity, roadName, startAt, endAt);
            return ResponseEntity.ok(payload);
        }

        byte[] bytes = trafficReportExportService.exportXlsx(granularity, roadName, startAt, endAt);
        String fileName = TrafficReportExportService.buildExportFileName(granularity);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(bytes);
    }
}
