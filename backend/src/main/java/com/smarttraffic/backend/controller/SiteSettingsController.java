package com.smarttraffic.backend.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smarttraffic.backend.dto.admin.UpdateSiteSettingsRequest;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.SiteSettingsEntity;
import com.smarttraffic.backend.repository.SiteSettingsRepository;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.service.LlmService;
import com.smarttraffic.backend.service.analytics.RedisCacheService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SiteSettingsController {

    private final SiteSettingsRepository repo;
    private final LlmService llmService;
    private final RedisCacheService redisCacheService;

    public SiteSettingsController(SiteSettingsRepository repo, LlmService llmService, RedisCacheService redisCacheService) {
        this.repo = repo;
        this.llmService = llmService;
        this.redisCacheService = redisCacheService;
    }

    @GetMapping("/api/v1/site-settings")
    public Map<String, Object> get() {
        // Try cache first
        var cached = redisCacheService.getSiteSettings(Map.class);
        if (cached.isPresent()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) cached.get();
            return result;
        }

        SiteSettingsEntity entity = repo.findById(1L).orElseGet(() -> {
            SiteSettingsEntity defaults = new SiteSettingsEntity();
            defaults.setSiteName("智能交通监控系统");
            defaults.setAnnouncement("");
            return defaults;
        });
        // 不暴露敏感字段给公开接口
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", entity.getId());
        result.put("site_name", entity.getSiteName());
        result.put("announcement", entity.getAnnouncement());
        result.put("logo_url", entity.getLogoUrl());
        result.put("footer_text", entity.getFooterText());
        result.put("amap_key", entity.getAmapKey());
        result.put("amap_security_js_code", entity.getAmapSecurityJsCode());
        result.put("amap_service_host", entity.getAmapServiceHost());
        result.put("congestion_threshold", entity.getCongestionThreshold());
        result.put("ai_float_visible_pages", entity.getAiFloatVisiblePages());
        result.put("updated_at", entity.getUpdatedAt());
        redisCacheService.putSiteSettings(result);
        return result;
    }

    @GetMapping("/api/v1/admin/site-settings/full")
    public SiteSettingsEntity getFull() {
        SecurityUtils.requireAdmin();
        return repo.findById(1L).orElseGet(SiteSettingsEntity::new);
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
        if (body.hasField("llmProvider")) settings.setLlmProvider(normalizeOptionalText(body.getLlmProvider()));
        if (body.hasField("llmApiBaseUrl")) settings.setLlmApiBaseUrl(normalizeOptionalText(body.getLlmApiBaseUrl()));
        if (body.hasField("llmApiKey")) settings.setLlmApiKey(body.getLlmApiKey());
        if (body.hasField("llmModelName")) settings.setLlmModelName(normalizeOptionalText(body.getLlmModelName()));
        if (body.hasField("aiFloatVisiblePages")) settings.setAiFloatVisiblePages(body.getAiFloatVisiblePages());
        if (body.hasField("llmTitleModelName")) settings.setLlmTitleModelName(normalizeOptionalText(body.getLlmTitleModelName()));
        if (body.hasField("llmTitlePrompt")) settings.setLlmTitlePrompt(body.getLlmTitlePrompt());
        SiteSettingsEntity saved = repo.save(settings);
        redisCacheService.evictSiteSettings();
        return saved;
    }

    @PostMapping("/api/v1/admin/llm/test-connection")
    public Map<String, Object> testLlmConnection() {
        SecurityUtils.requireAdmin();
        boolean ok = llmService.testConnection();
        return Map.of("success", ok, "message", ok ? "连接成功" : "连接失败，请检查配置");
    }

    @GetMapping("/api/v1/admin/llm/models")
    public List<String> fetchModels() {
        SecurityUtils.requireAdmin();
        return llmService.fetchModels();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
