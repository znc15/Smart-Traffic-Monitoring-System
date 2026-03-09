package com.smarttraffic.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_clients")
public class ApiClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "api_key", nullable = false, unique = true, length = 255)
    private String apiKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "allowed_endpoints", columnDefinition = "TEXT")
    private String allowedEndpoints;

    @Column(name = "rate_limit", nullable = false)
    private Integer rateLimit = 1000;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAllowedEndpoints() {
        return allowedEndpoints;
    }

    public void setAllowedEndpoints(String allowedEndpoints) {
        this.allowedEndpoints = allowedEndpoints;
    }

    public Integer getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(Integer rateLimit) {
        this.rateLimit = rateLimit;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
