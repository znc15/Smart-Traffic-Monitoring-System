package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.model.AlertEntity;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.service.AlertService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<AlertEntity> getAllAlerts(@RequestParam(required = false) String status) {
        SecurityUtils.requireAdmin();
        if (status != null && !status.isEmpty()) {
            return alertService.getAlertsByStatus(status);
        }
        return alertService.getAllAlerts();
    }

    @PutMapping("/{id}/status")
    public AlertEntity updateAlertStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        SecurityUtils.requireAdmin();
        String status = payload.get("status");
        if (status == null || (!status.equals("UNCONFIRMED") && !status.equals("CONFIRMED") && !status.equals("DISPOSED"))) {
            throw new IllegalArgumentException("Invalid status");
        }
        return alertService.updateAlertStatus(id, status);
    }
}
