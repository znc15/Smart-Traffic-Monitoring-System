package com.smarttraffic.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.dto.admin.ApiClientCreateRequest;
import com.smarttraffic.backend.dto.admin.ApiClientResponse;
import com.smarttraffic.backend.dto.admin.ApiClientUpdateRequest;
import com.smarttraffic.backend.dto.admin.ApiUsageDailyStats;
import com.smarttraffic.backend.dto.admin.ApiUsageEndpointStats;
import com.smarttraffic.backend.dto.admin.ApiUsageResponse;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.ApiClientEntity;
import com.smarttraffic.backend.repository.ApiClientRepository;
import com.smarttraffic.backend.repository.ApiUsageLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
public class ApiClientService {

    private static final Logger log = LoggerFactory.getLogger(ApiClientService.class);

    private final ApiClientRepository apiClientRepository;
    private final ApiUsageLogRepository apiUsageLogRepository;
    private final ObjectMapper objectMapper;

    public ApiClientService(ApiClientRepository apiClientRepository,
                            ApiUsageLogRepository apiUsageLogRepository,
                            ObjectMapper objectMapper) {
        this.apiClientRepository = apiClientRepository;
        this.apiUsageLogRepository = apiUsageLogRepository;
        this.objectMapper = objectMapper;
    }

    public Page<ApiClientResponse> listClients(Pageable pageable) {
        return apiClientRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public ApiClientResponse createClient(ApiClientCreateRequest request) {
        if (apiClientRepository.existsByName(request.getName())) {
            throw new AppException(HttpStatus.CONFLICT, "API Client name already exists");
        }
        ApiClientEntity entity = new ApiClientEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setAllowedEndpoints(serializeEndpoints(request.getAllowedEndpoints()));
        entity.setApiKey(generateApiKey());
        if (request.getRateLimit() != null) {
            entity.setRateLimit(request.getRateLimit());
        }
        return toResponse(apiClientRepository.save(entity));
    }

    @Transactional
    public ApiClientResponse updateClient(Long id, ApiClientUpdateRequest request) {
        ApiClientEntity entity = apiClientRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "API Client not found"));

        if (request.hasField("name") && request.getName() != null) {
            if (!request.getName().equals(entity.getName()) && apiClientRepository.existsByName(request.getName())) {
                throw new AppException(HttpStatus.CONFLICT, "API Client name already exists");
            }
            entity.setName(request.getName());
        }
        if (request.hasField("description")) {
            entity.setDescription(request.getDescription());
        }
        if (request.hasField("allowedEndpoints")) {
            entity.setAllowedEndpoints(serializeEndpoints(request.getAllowedEndpoints()));
        }
        if (request.hasField("rateLimit") && request.getRateLimit() != null) {
            entity.setRateLimit(request.getRateLimit());
        }
        if (request.hasField("enabled") && request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }

        return toResponse(apiClientRepository.save(entity));
    }

    @Transactional
    public void deleteClient(Long id) {
        if (!apiClientRepository.existsById(id)) {
            throw new AppException(HttpStatus.NOT_FOUND, "API Client not found");
        }
        apiUsageLogRepository.deleteByApiClientId(id);
        apiClientRepository.deleteById(id);
    }

    @Transactional
    public ApiClientResponse regenerateKey(Long id) {
        ApiClientEntity entity = apiClientRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "API Client not found"));
        entity.setApiKey(generateApiKey());
        return toResponse(apiClientRepository.save(entity));
    }

    public ApiUsageResponse getUsageStats(Long id, int days) {
        if (!apiClientRepository.existsById(id)) {
            throw new AppException(HttpStatus.NOT_FOUND, "API Client not found");
        }

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();

        long totalCalls = apiUsageLogRepository.countByApiClientIdAndCreatedAtBetween(id, start, end);

        List<ApiUsageDailyStats> dailyStats = apiUsageLogRepository
                .countByClientIdGroupByDay(id, start, end)
                .stream()
                .map(row -> new ApiUsageDailyStats(row[0].toString(), ((Number) row[1]).longValue()))
                .toList();

        List<ApiUsageEndpointStats> endpointStats = apiUsageLogRepository
                .countByClientIdGroupByEndpoint(id, start, end)
                .stream()
                .map(row -> new ApiUsageEndpointStats((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        return new ApiUsageResponse(totalCalls, dailyStats, endpointStats);
    }

    private ApiClientResponse toResponse(ApiClientEntity entity) {
        return ApiClientResponse.fromEntity(entity, deserializeEndpoints(entity.getAllowedEndpoints()));
    }

    private String serializeEndpoints(List<String> endpoints) {
        if (endpoints == null || endpoints.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(endpoints);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize allowed endpoints: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private List<String> deserializeEndpoints(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize allowed endpoints '{}': {}", json, ex.getMessage());
            return Collections.emptyList();
        }
    }

    private String generateApiKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return "stm_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

