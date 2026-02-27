package com.smarttraffic.backend.dto.traffic;

import java.time.LocalDateTime;

public record PredictionPointResponse(
        LocalDateTime ts,
        double predictedFlow,
        double confidenceLow,
        double confidenceHigh
) {
}
