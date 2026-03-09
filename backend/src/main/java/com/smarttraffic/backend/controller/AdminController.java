package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.admin.AdminUserResponse;
import com.smarttraffic.backend.dto.admin.UpdateCameraRequest;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.model.UserEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import com.smarttraffic.backend.repository.UserRepository;
import com.smarttraffic.backend.security.CurrentUser;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.config.TrafficProperties;
import com.smarttraffic.backend.service.CameraPollerService;
import com.smarttraffic.backend.service.RoadService;
import com.smarttraffic.backend.service.SystemMetricsService;
import com.smarttraffic.backend.service.TrafficService;
import com.smarttraffic.backend.service.analytics.RedisCacheService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final SystemMetricsService systemMetricsService;
    private final CameraRepository cameraRepository;
    private final UserRepository userRepository;
    private final TrafficService trafficService;
    private final TrafficProperties trafficProperties;
    private final CameraPollerService cameraPollerService;
    private final RedisCacheService redisCacheService;
    private final RoadService roadService;

    public AdminController(SystemMetricsService systemMetricsService, CameraRepository cameraRepository,
                           UserRepository userRepository, TrafficService trafficService,
                           TrafficProperties trafficProperties, CameraPollerService cameraPollerService,
                           RedisCacheService redisCacheService, RoadService roadService) {
        this.systemMetricsService = systemMetricsService;
        this.cameraRepository = cameraRepository;
        this.userRepository = userRepository;
        this.trafficService = trafficService;
        this.trafficProperties = trafficProperties;
        this.cameraPollerService = cameraPollerService;
        this.redisCacheService = redisCacheService;
        this.roadService = roadService;
    }

    @GetMapping("/resources")
    public Map<String, Object> resources() {
        SecurityUtils.requireAdmin();
        return systemMetricsService.getSystemMetrics();
    }

    @GetMapping("/nodes")
    public Map<String, Object> nodeHealth() {
        requireAdmin();
        return Map.of("nodes", cameraPollerService.getNodeHealthMap());
    }

    private void requireAdmin() {
        SecurityUtils.requireAdmin();
    }

    @GetMapping("/cameras")
    public Page<CameraEntity> listCameras(@PageableDefault(size = 20) Pageable pageable) {
        requireAdmin();
        return cameraRepository.findAll(pageable);
    }

    @PostMapping("/cameras")
    public CameraEntity createCamera(@Valid @RequestBody CameraEntity camera) {
        requireAdmin();
        camera.setId(null);
        CameraEntity saved = cameraRepository.save(camera);
        trafficService.reloadCameras(trafficProperties);
        invalidateTrafficCaches();
        return saved;
    }

    @PutMapping("/cameras/{id}")
    public CameraEntity updateCamera(@PathVariable Long id, @Valid @RequestBody UpdateCameraRequest body) {
        requireAdmin();
        CameraEntity cam = cameraRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Camera not found"));
        if (body.hasField("name")) cam.setName(body.getName());
        if (body.hasField("location")) cam.setLocation(body.getLocation());
        if (body.hasField("enabled") && body.getEnabled() != null) cam.setEnabled(body.getEnabled());
        if (body.hasField("streamUrl")) cam.setStreamUrl(body.getStreamUrl());
        if (body.hasField("roadName")) cam.setRoadName(body.getRoadName());
        if (body.hasField("latitude")) cam.setLatitude(body.getLatitude());
        if (body.hasField("longitude")) cam.setLongitude(body.getLongitude());
        CameraEntity saved = cameraRepository.save(cam);
        trafficService.reloadCameras(trafficProperties);
        invalidateTrafficCaches();
        return saved;
    }

    @DeleteMapping("/cameras/{id}")
    public Map<String, String> deleteCamera(@PathVariable Long id) {
        requireAdmin();
        if (!cameraRepository.existsById(id)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Camera not found");
        }
        cameraRepository.deleteById(id);
        trafficService.reloadCameras(trafficProperties);
        invalidateTrafficCaches();
        return Map.of("detail", "Deleted");
    }

    @GetMapping("/users")
    public Page<AdminUserResponse> listUsers(@PageableDefault(size = 20) Pageable pageable) {
        requireAdmin();
        return userRepository.findAll(pageable).map(AdminUserResponse::fromEntity);
    }

    @PutMapping("/users/{id}/role")
    public Map<String, String> updateUserRole(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireAdmin();
        CurrentUser current = SecurityUtils.requireCurrentUser();
        if (current.id().equals(id)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot modify your own account");
        }
        Integer roleId = parseRoleId(body.getOrDefault("role_id", body.get("roleId")));
        if (roleId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "role_id is required");
        }
        if (roleId != 0 && roleId != 1) {
            throw new AppException(HttpStatus.BAD_REQUEST, "role_id must be 0 or 1");
        }
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRoleId(roleId);
        userRepository.save(user);
        return Map.of("detail", "Role updated");
    }

    @PutMapping("/users/{id}/status")
    public Map<String, Object> toggleUserStatus(@PathVariable Long id) {
        requireAdmin();
        CurrentUser current = SecurityUtils.requireCurrentUser();
        if (current.id().equals(id)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot modify your own account");
        }
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return Map.of("enabled", user.isEnabled());
    }

    private Integer parseRoleId(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void invalidateTrafficCaches() {
        redisCacheService.evict("traffic:roads");
        redisCacheService.evictByPrefix("traffic:maas:");
        redisCacheService.evictByPrefix("traffic:info:");
        // 清除动态道路发现缓存，确保摄像头变更后道路列表实时更新
        roadService.evictRoadCache();
    }
}
