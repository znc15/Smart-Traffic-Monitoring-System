package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.admin.AdminUserResponse;
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
        if (body.containsKey("stream_url") || body.containsKey("streamUrl")) {
            Object streamUrl = body.getOrDefault("stream_url", body.get("streamUrl"));
            cam.setStreamUrl(streamUrl != null ? String.valueOf(streamUrl) : null);
        }
        if (body.containsKey("road_name") || body.containsKey("roadName")) {
            Object roadName = body.getOrDefault("road_name", body.get("roadName"));
            cam.setRoadName(roadName != null ? String.valueOf(roadName) : null);
        }
        if (body.containsKey("latitude")) {
            cam.setLatitude(parseDouble(body.get("latitude")));
        }
        if (body.containsKey("longitude")) {
            cam.setLongitude(parseDouble(body.get("longitude")));
        }
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
    public List<AdminUserResponse> listUsers() {
        requireAdmin();
        return userRepository.findAll().stream()
                .map(AdminUserResponse::fromEntity)
                .toList();
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

    private Double parseDouble(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw instanceof String text) {
            String normalized = text.trim();
            if (normalized.isEmpty()) {
                return null;
            }
            try {
                return Double.parseDouble(normalized);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
