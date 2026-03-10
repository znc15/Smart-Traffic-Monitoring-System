package com.smarttraffic.backend.service;

import com.smarttraffic.backend.model.TrafficEventEntity;
import com.smarttraffic.backend.repository.TrafficEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminEventService {

    private final TrafficEventRepository trafficEventRepository;

    public AdminEventService(TrafficEventRepository trafficEventRepository) {
        this.trafficEventRepository = trafficEventRepository;
    }

    public Page<TrafficEventEntity> queryEvents(
            String roadName,
            String eventType,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Pageable pageable
    ) {
        Specification<TrafficEventEntity> specification = Specification.where(null);
        if (roadName != null && !roadName.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("roadName"), roadName.trim()));
        }
        if (eventType != null && !eventType.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("eventType"), eventType.trim()));
        }
        if (startAt != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startAt));
        }
        if (endAt != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endAt));
        }
        return trafficEventRepository.findAll(specification, pageable);
    }
}
