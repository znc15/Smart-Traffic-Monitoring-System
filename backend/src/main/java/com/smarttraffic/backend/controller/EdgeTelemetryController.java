package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.edge.EdgeTelemetryRequest;
import com.smarttraffic.backend.dto.common.MessageResponse;
import com.smarttraffic.backend.service.analytics.TelemetryIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/edge")
public class EdgeTelemetryController {

    private final TelemetryIngestionService telemetryIngestionService;

    public EdgeTelemetryController(TelemetryIngestionService telemetryIngestionService) {
        this.telemetryIngestionService = telemetryIngestionService;
    }

    @PostMapping("/telemetry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse ingest(@Valid @RequestBody EdgeTelemetryRequest request) {
        telemetryIngestionService.ingest(request);
        return new MessageResponse("telemetry accepted");
    }
}
