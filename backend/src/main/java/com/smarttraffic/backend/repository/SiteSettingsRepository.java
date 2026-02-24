package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.SiteSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSettingsRepository extends JpaRepository<SiteSettingsEntity, Long> {
}
