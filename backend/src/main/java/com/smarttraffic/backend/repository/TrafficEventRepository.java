package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.TrafficEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TrafficEventRepository extends JpaRepository<TrafficEventEntity, Long>, JpaSpecificationExecutor<TrafficEventEntity> {
}
