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

    private static final int MAX_TEXT_LENGTH = 1000;

    @PutMapping("/api/v1/admin/site-settings")
    public SiteSettingsEntity update(@RequestBody Map<String, String> body) {
        var user = SecurityUtils.requireCurrentUser();
        if (!user.isAdmin()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Admin only");
        }

        // Validate logoUrl: must be empty or start with http:// / https://
        if (body.containsKey("logoUrl")) {
            String logoUrl = body.get("logoUrl");
            if (logoUrl != null && !logoUrl.isEmpty()
                    && !logoUrl.startsWith("http://") && !logoUrl.startsWith("https://")) {
                throw new AppException(HttpStatus.BAD_REQUEST,
                        "logoUrl must be empty or start with http:// or https://");
            }
        }

        // Validate text field lengths
        if (body.containsKey("footerText") && body.get("footerText") != null
                && body.get("footerText").length() > MAX_TEXT_LENGTH) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "footerText must not exceed " + MAX_TEXT_LENGTH + " characters");
        }
        if (body.containsKey("announcement") && body.get("announcement") != null
                && body.get("announcement").length() > MAX_TEXT_LENGTH) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "announcement must not exceed " + MAX_TEXT_LENGTH + " characters");
        }

        SiteSettingsEntity settings = repo.findById(1L).orElseGet(SiteSettingsEntity::new);
        if (body.containsKey("siteName")) settings.setSiteName(body.get("siteName"));
        if (body.containsKey("announcement")) settings.setAnnouncement(body.get("announcement"));
        if (body.containsKey("logoUrl")) settings.setLogoUrl(body.get("logoUrl"));
        if (body.containsKey("footerText")) settings.setFooterText(body.get("footerText"));
        return repo.save(settings);
    }
}
