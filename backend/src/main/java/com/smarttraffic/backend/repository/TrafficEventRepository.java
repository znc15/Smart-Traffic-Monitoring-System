package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.TrafficEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrafficEventRepository extends JpaRepository<TrafficEventEntity, Long> {
}
