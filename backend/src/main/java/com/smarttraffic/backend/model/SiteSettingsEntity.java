package com.smarttraffic.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "site_settings")
public class SiteSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_name", nullable = false)
    private String siteName = "智能交通监控系统";

    @Column(name = "announcement", columnDefinition = "TEXT")
    private String announcement = "";

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "footer_text", columnDefinition = "TEXT")
    private String footerText;

    @Column(name = "amap_key")
    private String amapKey;

    @Column(name = "amap_security_js_code")
    private String amapSecurityJsCode;

    @Column(name = "amap_service_host")
    private String amapServiceHost;

    @Column(name = "llm_provider", length = 32)
    private String llmProvider;

    @Column(name = "llm_api_base_url", length = 512)
    private String llmApiBaseUrl;

    @Column(name = "llm_api_key", length = 512)
    private String llmApiKey;

    @Column(name = "llm_model_name", length = 128)
    private String llmModelName;

    @Column(name = "ai_float_visible_pages", columnDefinition = "TEXT")
    private String aiFloatVisiblePages;

    @Column(name = "llm_title_model_name", length = 255)
    private String llmTitleModelName;

    @Column(name = "llm_title_prompt", columnDefinition = "TEXT")
    private String llmTitlePrompt;

    @Column(name = "congestion_threshold")
    private Double congestionThreshold = 0.8;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onSave() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public String getAnnouncement() { return announcement; }
    public void setAnnouncement(String announcement) { this.announcement = announcement; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getFooterText() { return footerText; }
    public void setFooterText(String footerText) { this.footerText = footerText; }
    public String getAmapKey() { return amapKey; }
    public void setAmapKey(String amapKey) { this.amapKey = amapKey; }
    public String getAmapSecurityJsCode() { return amapSecurityJsCode; }
    public void setAmapSecurityJsCode(String amapSecurityJsCode) { this.amapSecurityJsCode = amapSecurityJsCode; }
    public String getAmapServiceHost() { return amapServiceHost; }
    public void setAmapServiceHost(String amapServiceHost) { this.amapServiceHost = amapServiceHost; }
    public String getLlmProvider() { return llmProvider; }
    public void setLlmProvider(String llmProvider) { this.llmProvider = llmProvider; }
    public String getLlmApiBaseUrl() { return llmApiBaseUrl; }
    public void setLlmApiBaseUrl(String llmApiBaseUrl) { this.llmApiBaseUrl = llmApiBaseUrl; }
    public String getLlmApiKey() { return llmApiKey; }
    public void setLlmApiKey(String llmApiKey) { this.llmApiKey = llmApiKey; }
    public String getLlmModelName() { return llmModelName; }
    public void setLlmModelName(String llmModelName) { this.llmModelName = llmModelName; }
    public String getAiFloatVisiblePages() { return aiFloatVisiblePages; }
    public void setAiFloatVisiblePages(String aiFloatVisiblePages) { this.aiFloatVisiblePages = aiFloatVisiblePages; }
    public String getLlmTitleModelName() { return llmTitleModelName; }
    public void setLlmTitleModelName(String llmTitleModelName) { this.llmTitleModelName = llmTitleModelName; }
    public String getLlmTitlePrompt() { return llmTitlePrompt; }
    public void setLlmTitlePrompt(String llmTitlePrompt) { this.llmTitlePrompt = llmTitlePrompt; }
    public Double getCongestionThreshold() { return congestionThreshold; }
    public void setCongestionThreshold(Double congestionThreshold) { this.congestionThreshold = congestionThreshold; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
