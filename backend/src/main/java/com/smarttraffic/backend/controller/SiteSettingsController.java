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

        String logoUrl = firstValue(body, "logo_url", "logoUrl");
        String footerText = firstValue(body, "footer_text", "footerText");
        String announcement = body.get("announcement");
        String siteName = firstValue(body, "site_name", "siteName");

        // Validate logoUrl/logo_url: must be empty or start with http:// / https://
        if (containsAny(body, "logo_url", "logoUrl")) {
            if (logoUrl != null && !logoUrl.isEmpty()
                    && !logoUrl.startsWith("http://") && !logoUrl.startsWith("https://")) {
                throw new AppException(HttpStatus.BAD_REQUEST,
                        "logo_url must be empty or start with http:// or https://");
            }
        }

        // Validate text field lengths
        if (containsAny(body, "footer_text", "footerText") && footerText != null
                && footerText.length() > MAX_TEXT_LENGTH) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "footer_text must not exceed " + MAX_TEXT_LENGTH + " characters");
        }
        if (body.containsKey("announcement") && announcement != null
                && announcement.length() > MAX_TEXT_LENGTH) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "announcement must not exceed " + MAX_TEXT_LENGTH + " characters");
        }

        SiteSettingsEntity settings = repo.findById(1L).orElseGet(SiteSettingsEntity::new);
        if (containsAny(body, "site_name", "siteName")) settings.setSiteName(siteName);
        if (body.containsKey("announcement")) settings.setAnnouncement(announcement);
        if (containsAny(body, "logo_url", "logoUrl")) settings.setLogoUrl(logoUrl);
        if (containsAny(body, "footer_text", "footerText")) settings.setFooterText(footerText);
        return repo.save(settings);
    }

    private static boolean containsAny(Map<String, String> body, String... keys) {
        for (String key : keys) {
            if (body.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    private static String firstValue(Map<String, String> body, String primary, String fallback) {
        if (body.containsKey(primary)) {
            return body.get(primary);
        }
        return body.get(fallback);
    }
}
