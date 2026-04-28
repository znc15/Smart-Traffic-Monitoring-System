package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.TrafficSampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrafficSampleRepository extends JpaRepository<TrafficSampleEntity, Long> {
    List<TrafficSampleEntity> findByRoadNameAndSampleTimeBetweenOrderBySampleTimeAsc(
            String roadName,
            LocalDateTime from,
            LocalDateTime to
    );

    Optional<TrafficSampleEntity> findFirstByRoadNameOrderBySampleTimeDesc(String roadName);

    @Query(value = """
            SELECT ts.* FROM traffic_samples ts
            INNER JOIN (
                SELECT road_name, MAX(sample_time) AS max_time
                FROM traffic_samples
                WHERE road_name IN (:roadNames)
                GROUP BY road_name
            ) latest ON ts.road_name = latest.road_name AND ts.sample_time = latest.max_time
            """, nativeQuery = true)
    List<TrafficSampleEntity> findLatestByRoadNames(@Param("roadNames") Collection<String> roadNames);

    void deleteByRoadName(String roadName);

    @Query("SELECT s FROM TrafficSampleEntity s WHERE " +
           "(:roadName IS NULL OR s.roadName = :roadName) " +
           "ORDER BY s.sampleTime DESC")
    List<TrafficSampleEntity> findRecentSamples(
            @Param("roadName") String roadName,
            Pageable pageable
    );
}
