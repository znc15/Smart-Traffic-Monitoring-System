package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.ApiClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiClientRepository extends JpaRepository<ApiClientEntity, Long> {
    Optional<ApiClientEntity> findByApiKeyAndEnabledTrue(String apiKey);

    boolean existsByName(String name);
}
