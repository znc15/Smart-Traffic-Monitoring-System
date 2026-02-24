package com.smarttraffic.backend.config;

import com.smarttraffic.backend.model.SiteSettingsEntity;
import com.smarttraffic.backend.repository.SiteSettingsRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SiteSettingsInitializer implements CommandLineRunner {

    private final SiteSettingsRepository repo;

    public SiteSettingsInitializer(SiteSettingsRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        if (repo.findById(1L).isEmpty()) {
            repo.save(new SiteSettingsEntity());
        }
    }
}
