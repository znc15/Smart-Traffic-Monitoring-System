package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.admin.ApiClientCreateRequest;
import com.smarttraffic.backend.dto.admin.ApiClientResponse;
import com.smarttraffic.backend.dto.admin.ApiClientUpdateRequest;
import com.smarttraffic.backend.dto.admin.ApiUsageResponse;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.service.ApiClientService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/api-clients")
public class ApiClientController {

    private final ApiClientService apiClientService;

    public ApiClientController(ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    @GetMapping
    public Page<ApiClientResponse> listClients(@PageableDefault(size = 20) Pageable pageable) {
        requireAdmin();
        return apiClientService.listClients(pageable);
    }

    @PostMapping
    public ApiClientResponse createClient(@Valid @RequestBody ApiClientCreateRequest request) {
        requireAdmin();
        return apiClientService.createClient(request);
    }

    @PutMapping("/{id}")
    public ApiClientResponse updateClient(@PathVariable Long id,
                                          @Valid @RequestBody ApiClientUpdateRequest request) {
        requireAdmin();
        return apiClientService.updateClient(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteClient(@PathVariable Long id) {
        requireAdmin();
        apiClientService.deleteClient(id);
        return Map.of("detail", "Deleted");
    }

    @PostMapping("/{id}/regenerate")
    public ApiClientResponse regenerateKey(@PathVariable Long id) {
        requireAdmin();
        return apiClientService.regenerateKey(id);
    }

    @GetMapping("/{id}/usage")
    public ApiUsageResponse getUsageStats(@PathVariable Long id,
                                          @RequestParam(defaultValue = "30") int days) {
        requireAdmin();
        int clampedDays = Math.max(1, Math.min(days, 365));
        return apiClientService.getUsageStats(id, clampedDays);
    }

    private void requireAdmin() {
        SecurityUtils.requireAdmin();
    }
}

