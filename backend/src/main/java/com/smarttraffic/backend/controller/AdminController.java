package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.security.CurrentUser;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.service.SystemMetricsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final SystemMetricsService systemMetricsService;

    public AdminController(SystemMetricsService systemMetricsService) {
        this.systemMetricsService = systemMetricsService;
    }

    @GetMapping("/resources")
    public Map<String, Object> resources() {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        if (!user.isAdmin()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Chỉ admin mới được phép truy cập tài nguyên hệ thống.");
        }
        return systemMetricsService.getSystemMetrics();
    }
}
