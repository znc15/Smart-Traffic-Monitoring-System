package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.admin.UpdateSiteSettingsRequest;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.SiteSettingsEntity;
import com.smarttraffic.backend.repository.SiteSettingsRepository;
import com.smarttraffic.backend.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
    public SiteSettingsEntity update(@Valid @RequestBody UpdateSiteSettingsRequest body) {
        SecurityUtils.requireAdmin();

        if (body.hasField("logoUrl")) {
            String logoUrl = body.getLogoUrl();
            if (logoUrl != null && !logoUrl.isEmpty()
                    && !logoUrl.startsWith("http://") && !logoUrl.startsWith("https://")) {
                throw new AppException(HttpStatus.BAD_REQUEST,
                        "logo_url must be empty or start with http:// or https://");
            }
        }

        SiteSettingsEntity settings = repo.findById(1L).orElseGet(SiteSettingsEntity::new);
        if (body.hasField("siteName")) settings.setSiteName(body.getSiteName());
        if (body.hasField("announcement")) settings.setAnnouncement(body.getAnnouncement());
        if (body.hasField("logoUrl")) settings.setLogoUrl(body.getLogoUrl());
        if (body.hasField("footerText")) settings.setFooterText(body.getFooterText());
        if (body.hasField("amapKey")) settings.setAmapKey(normalizeOptionalText(body.getAmapKey()));
        if (body.hasField("amapSecurityJsCode")) settings.setAmapSecurityJsCode(normalizeOptionalText(body.getAmapSecurityJsCode()));
        if (body.hasField("amapServiceHost")) settings.setAmapServiceHost(normalizeOptionalText(body.getAmapServiceHost()));
        if (body.hasField("congestionThreshold")) settings.setCongestionThreshold(body.getCongestionThreshold());
        return repo.save(settings);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
