package com.smarttraffic.backend.dto.traffic;

import java.time.LocalDateTime;
import java.util.List;

public record PredictionResponse(
        String roadName,
        LocalDateTime generatedAt,
        String algorithm,
        List<PredictionPointResponse> points
) {
}
