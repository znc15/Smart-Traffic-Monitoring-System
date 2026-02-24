package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.model.UserEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import com.smarttraffic.backend.repository.UserRepository;
import com.smarttraffic.backend.security.CurrentUser;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.config.TrafficProperties;
import com.smarttraffic.backend.service.CameraPollerService;
import com.smarttraffic.backend.service.SystemMetricsService;
import com.smarttraffic.backend.service.TrafficService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    public AdminController(SystemMetricsService systemMetricsService, CameraRepository cameraRepository,
                           UserRepository userRepository, TrafficService trafficService,
                           TrafficProperties trafficProperties, CameraPollerService cameraPollerService) {
        this.systemMetricsService = systemMetricsService;
        this.cameraRepository = cameraRepository;
        this.userRepository = userRepository;
        this.trafficService = trafficService;
        this.trafficProperties = trafficProperties;
        this.cameraPollerService = cameraPollerService;
    }

    @GetMapping("/resources")
    public Map<String, Object> resources() {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        if (!user.isAdmin()) {
            throw new AppException(HttpStatus.FORBIDDEN, "仅管理员可访问系统资源");
        }
        return systemMetricsService.getSystemMetrics();
    }

    @GetMapping("/nodes")
    public Map<String, Object> nodeHealth() {
        requireAdmin();
        return Map.of("nodes", cameraPollerService.getNodeHealthMap());
    }

    private void requireAdmin() {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        if (!user.isAdmin()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    @GetMapping("/cameras")
    public List<CameraEntity> listCameras() {
        requireAdmin();
        return cameraRepository.findAll();
    }

    @PostMapping("/cameras")
    public CameraEntity createCamera(@RequestBody CameraEntity camera) {
        requireAdmin();
        camera.setId(null);
        CameraEntity saved = cameraRepository.save(camera);
        trafficService.reloadCameras(trafficProperties);
        return saved;
    }

    @PutMapping("/cameras/{id}")
    public CameraEntity updateCamera(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireAdmin();
        CameraEntity cam = cameraRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Camera not found"));
        if (body.containsKey("name")) cam.setName((String) body.get("name"));
        if (body.containsKey("location")) cam.setLocation((String) body.get("location"));
        if (body.containsKey("enabled")) cam.setEnabled((Boolean) body.get("enabled"));
        if (body.containsKey("stream_url")) cam.setStreamUrl((String) body.get("stream_url"));
        CameraEntity saved = cameraRepository.save(cam);
        trafficService.reloadCameras(trafficProperties);
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
        return Map.of("detail", "Deleted");
    }

    @GetMapping("/users")
    public List<Map<String, Object>> listUsers() {
        requireAdmin();
        return userRepository.findAll().stream().map(u -> Map.<String, Object>of(
                "id", u.getId(), "username", u.getUsername(), "email", u.getEmail(),
                "phoneNumber", u.getPhoneNumber(), "roleId", u.getRoleId(),
                "enabled", u.isEnabled(), "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
        )).toList();
    }

    @PutMapping("/users/{id}/role")
    public Map<String, String> updateUserRole(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        requireAdmin();
        CurrentUser current = SecurityUtils.requireCurrentUser();
        if (current.id().equals(id)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot modify your own account");
        }
        Integer roleId = body.get("roleId");
        if (roleId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "roleId is required");
        }
        if (roleId != 0 && roleId != 1) {
            throw new AppException(HttpStatus.BAD_REQUEST, "roleId must be 0 or 1");
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
}
