package com.smarttraffic.backend.dto.maas;

public record MaasCongestionItemResponse(
        Long cameraId,
        String roadName,
        double lat,
        double lng,
        double congestionIndex,
        String densityStatus
) {
}
