package com.smarttraffic.backend.dto.maas;

import java.time.LocalDateTime;
import java.util.List;

public record MaasCongestionResponse(
        LocalDateTime updatedAt,
        List<MaasCongestionItemResponse> data
) {
}
