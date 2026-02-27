package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.TrafficPredictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrafficPredictionRepository extends JpaRepository<TrafficPredictionEntity, Long> {
    List<TrafficPredictionEntity> findByRoadNameOrderByPredictTimeDesc(String roadName);
}
