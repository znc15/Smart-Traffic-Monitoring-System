package com.smarttraffic.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_usage_logs")
public class ApiUsageLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_client_id")
    private Long apiClientId;

    @Column(name = "endpoint", nullable = false, length = 255)
    private String endpoint;

    @Column(name = "method", nullable = false, length = 10)
    private String method;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getApiClientId() {
        return apiClientId;
    }

    public void setApiClientId(Long apiClientId) {
        this.apiClientId = apiClientId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public void setRequestIp(String requestIp) {
        this.requestIp = requestIp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
