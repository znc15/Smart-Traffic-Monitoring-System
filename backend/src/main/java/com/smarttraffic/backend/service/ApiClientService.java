package com.smarttraffic.backend.service;

import com.smarttraffic.backend.dto.admin.ApiClientCreateRequest;
import com.smarttraffic.backend.dto.admin.ApiClientResponse;
import com.smarttraffic.backend.dto.admin.ApiClientUpdateRequest;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.ApiClientEntity;
import com.smarttraffic.backend.repository.ApiClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ApiClientService {

    private final ApiClientRepository apiClientRepository;

    public ApiClientService(ApiClientRepository apiClientRepository) {
        this.apiClientRepository = apiClientRepository;
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

    private String generateApiKey() {
        return UUID.randomUUID().toString();
    }
}
