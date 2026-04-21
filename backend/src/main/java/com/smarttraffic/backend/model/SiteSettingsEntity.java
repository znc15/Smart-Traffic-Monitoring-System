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
    public Double getCongestionThreshold() { return congestionThreshold; }
    public void setCongestionThreshold(Double congestionThreshold) { this.congestionThreshold = congestionThreshold; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
