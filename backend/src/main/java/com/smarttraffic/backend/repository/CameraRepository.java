package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.CameraEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CameraRepository extends JpaRepository<CameraEntity, Long> {
    List<CameraEntity> findByEnabledTrue();
}
