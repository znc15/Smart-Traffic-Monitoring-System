package com.smarttraffic.backend.dto.traffic;

import java.time.LocalDateTime;

public record TrafficSampleResponse(
        String road_name,
        LocalDateTime sample_time,
        int count_car,
        int count_motor,
        int count_person,
        double avg_speed_car,
        double avg_speed_motor,
        double congestion_index,
        String density_status,
        String speed_status
) {}
