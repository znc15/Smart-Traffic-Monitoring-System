package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.TrafficSampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TrafficSampleRepository extends JpaRepository<TrafficSampleEntity, Long> {
    List<TrafficSampleEntity> findByRoadNameAndSampleTimeBetweenOrderBySampleTimeAsc(
            String roadName,
            LocalDateTime from,
            LocalDateTime to
    );

    Optional<TrafficSampleEntity> findFirstByRoadNameOrderBySampleTimeDesc(String roadName);
}
