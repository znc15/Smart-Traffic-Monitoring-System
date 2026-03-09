package com.smarttraffic.backend.dto.admin;

import com.smarttraffic.backend.model.ApiClientEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class ApiClientResponse {

    private Long id;
    private String name;
    private String apiKey;
    private String description;
    private List<String> allowedEndpoints;
    private Integer rateLimit;
    private boolean enabled;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;

    public static ApiClientResponse fromEntity(ApiClientEntity entity, List<String> allowedEndpoints) {
        ApiClientResponse response = new ApiClientResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setApiKey(entity.getApiKey());
        response.setDescription(entity.getDescription());
        response.setAllowedEndpoints(allowedEndpoints != null ? allowedEndpoints : Collections.emptyList());
        response.setRateLimit(entity.getRateLimit());
        response.setEnabled(entity.isEnabled());
        response.setLastUsedAt(entity.getLastUsedAt());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getAllowedEndpoints() {
        return allowedEndpoints;
    }

    public void setAllowedEndpoints(List<String> allowedEndpoints) {
        this.allowedEndpoints = allowedEndpoints;
    }

    public Integer getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(Integer rateLimit) {
        this.rateLimit = rateLimit;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
