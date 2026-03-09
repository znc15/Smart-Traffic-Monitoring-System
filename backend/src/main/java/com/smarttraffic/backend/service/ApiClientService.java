package com.smarttraffic.backend.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ApiClientService {

    private final ApiClientRepository apiClientRepository;
    private final ApiUsageLogRepository apiUsageLogRepository;

    public ApiClientService(ApiClientRepository apiClientRepository,
                            ApiUsageLogRepository apiUsageLogRepository) {
        this.apiClientRepository = apiClientRepository;
        this.apiUsageLogRepository = apiUsageLogRepository;
    }

    public Page<ApiClientResponse> listClients(Pageable pageable) {
        return apiClientRepository.findAll(pageable).map(ApiClientResponse::fromEntity);
    }

    @Transactional
    public ApiClientResponse createClient(ApiClientCreateRequest request) {
        if (apiClientRepository.existsByName(request.getName())) {
            throw new AppException(HttpStatus.CONFLICT, "API Client name already exists");
        }
        ApiClientEntity entity = new ApiClientEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setAllowedEndpoints(request.getAllowedEndpoints());
        entity.setApiKey(generateApiKey());
        if (request.getRateLimit() != null) {
            entity.setRateLimit(request.getRateLimit());
        }
        return ApiClientResponse.fromEntity(apiClientRepository.save(entity));
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
            entity.setAllowedEndpoints(request.getAllowedEndpoints());
        }
        if (request.hasField("rateLimit") && request.getRateLimit() != null) {
            entity.setRateLimit(request.getRateLimit());
        }
        if (request.hasField("enabled") && request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }

        return ApiClientResponse.fromEntity(apiClientRepository.save(entity));
    }

    @Transactional
    public void deleteClient(Long id) {
        if (!apiClientRepository.existsById(id)) {
            throw new AppException(HttpStatus.NOT_FOUND, "API Client not found");
        }
        apiClientRepository.deleteById(id);
    }

    @Transactional
    public ApiClientResponse regenerateKey(Long id) {
        ApiClientEntity entity = apiClientRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "API Client not found"));
        entity.setApiKey(generateApiKey());
        return ApiClientResponse.fromEntity(apiClientRepository.save(entity));
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

    private String generateApiKey() {
        return UUID.randomUUID().toString();
    }
}

