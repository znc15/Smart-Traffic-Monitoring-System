package com.smarttraffic.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

public class ApiClientUpdateRequest {

    @Size(max = 100)
    private String name;

    private String description;

    private String allowedEndpoints;

    @Min(1)
    private Integer rateLimit;

    private Boolean enabled;

    @JsonIgnore
    private final Set<String> setFields = new HashSet<>();

    @JsonIgnore
    public boolean hasField(String field) {
        return setFields.contains(field);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        setFields.add("name");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        setFields.add("description");
    }

    public String getAllowedEndpoints() {
        return allowedEndpoints;
    }

    public void setAllowedEndpoints(String allowedEndpoints) {
        this.allowedEndpoints = allowedEndpoints;
        setFields.add("allowedEndpoints");
    }

    public Integer getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(Integer rateLimit) {
        this.rateLimit = rateLimit;
        setFields.add("rateLimit");
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
        setFields.add("enabled");
    }
}
