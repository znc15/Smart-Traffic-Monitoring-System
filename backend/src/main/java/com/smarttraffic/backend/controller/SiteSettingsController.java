package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.SiteSettingsEntity;
import com.smarttraffic.backend.repository.SiteSettingsRepository;
import com.smarttraffic.backend.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class SiteSettingsController {

    private final SiteSettingsRepository repo;

    public SiteSettingsController(SiteSettingsRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/api/v1/site-settings")
    public SiteSettingsEntity get() {
        return repo.findById(1L).orElseGet(() -> {
            SiteSettingsEntity defaults = new SiteSettingsEntity();
            defaults.setSiteName("智能交通监控系统");
            defaults.setAnnouncement("");
            return defaults;
        });
    }

    @PutMapping("/api/v1/admin/site-settings")
    public SiteSettingsEntity update(@RequestBody Map<String, String> body) {
        var user = SecurityUtils.requireCurrentUser();
        if (!user.isAdmin()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Admin only");
        }
        SiteSettingsEntity settings = repo.findById(1L).orElseGet(SiteSettingsEntity::new);
        if (body.containsKey("siteName")) settings.setSiteName(body.get("siteName"));
        if (body.containsKey("announcement")) settings.setAnnouncement(body.get("announcement"));
        return repo.save(settings);
    }
}
