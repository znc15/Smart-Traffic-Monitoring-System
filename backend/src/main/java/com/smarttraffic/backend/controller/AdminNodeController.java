package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.service.EdgeNodeConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/nodes")
public class AdminNodeController {

    private final EdgeNodeConfigService edgeNodeConfigService;

    public AdminNodeController(EdgeNodeConfigService edgeNodeConfigService) {
        this.edgeNodeConfigService = edgeNodeConfigService;
    }

    @GetMapping("/{cameraId}/config")
    public Map<String, Object> getNodeConfig(@PathVariable Long cameraId) {
        SecurityUtils.requireAdmin();
        return edgeNodeConfigService.getConfig(cameraId);
    }

    @PutMapping("/{cameraId}/config")
    public Map<String, Object> updateNodeConfig(@PathVariable Long cameraId, @RequestBody Map<String, Object> payload) {
        SecurityUtils.requireAdmin();
        return edgeNodeConfigService.updateConfig(cameraId, payload);
    }
}
