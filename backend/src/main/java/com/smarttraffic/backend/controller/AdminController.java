package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.admin.AdminUserResponse;
import com.smarttraffic.backend.dto.admin.UpdateCameraRequest;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.model.UserEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import com.smarttraffic.backend.repository.TrafficSampleRepository;
import com.smarttraffic.backend.repository.TrafficEventRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;
    private final TrafficSampleRepository trafficSampleRepository;
    private final TrafficEventRepository trafficEventRepository;

    public AdminController(SystemMetricsService systemMetricsService, CameraRepository cameraRepository,
                           UserRepository userRepository, TrafficService trafficService,
                           TrafficProperties trafficProperties, CameraPollerService cameraPollerService,
                           RedisCacheService redisCacheService, RoadService roadService,
                           PasswordEncoder passwordEncoder,
                           TrafficSampleRepository trafficSampleRepository,
                           TrafficEventRepository trafficEventRepository) {
        this.systemMetricsService = systemMetricsService;
        this.cameraRepository = cameraRepository;
        this.userRepository = userRepository;
        this.trafficService = trafficService;
        this.trafficProperties = trafficProperties;
        this.cameraPollerService = cameraPollerService;
        this.redisCacheService = redisCacheService;
        this.roadService = roadService;
        this.passwordEncoder = passwordEncoder;
        this.trafficSampleRepository = trafficSampleRepository;
        this.trafficEventRepository = trafficEventRepository;
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
        camera.setRoadName(normalizeRoadName(camera.getRoadName(), camera.getName()));
        camera.setEdgeNodeId(trimToNull(camera.getEdgeNodeId()));
        camera.setNodeApiKey(trimToNull(camera.getNodeApiKey()));
        camera.setNodeUrl(trimToNull(camera.getNodeUrl()));
        camera.setStreamUrl(trimToNull(camera.getStreamUrl()));
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
        if (body.hasField("streamUrl")) cam.setStreamUrl(trimToNull(body.getStreamUrl()));
        if (body.hasField("nodeUrl")) cam.setNodeUrl(trimToNull(body.getNodeUrl()));
        if (body.hasField("roadName")) cam.setRoadName(normalizeRoadName(body.getRoadName(), cam.getName()));
        if (body.hasField("edgeNodeId")) cam.setEdgeNodeId(trimToNull(body.getEdgeNodeId()));
        if (body.hasField("nodeApiKey")) cam.setNodeApiKey(trimToNull(body.getNodeApiKey()));
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
        CameraEntity cam = cameraRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Camera not found"));
        String roadName = cam.getRoadName();
        cameraRepository.deleteById(id);
        // 级联删除该路段关联的采样和事件数据
        if (roadName != null && !roadName.isBlank()) {
            trafficSampleRepository.deleteByRoadName(roadName);
            trafficEventRepository.deleteByRoadName(roadName);
        }
        trafficService.reloadCameras(trafficProperties);
        invalidateTrafficCaches();
        return Map.of("detail", "Deleted");
    }

    @GetMapping("/users")
    public Page<AdminUserResponse> listUsers(@PageableDefault(size = 20) Pageable pageable) {
        requireAdmin();
        return userRepository.findAll(pageable).map(AdminUserResponse::fromEntity);
    }

    @PutMapping("/users/{id}")
    public AdminUserResponse updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireAdmin();
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        if (body.containsKey("username")) {
            user.setUsername((String) body.get("username"));
        }
        if (body.containsKey("email")) {
            user.setEmail((String) body.get("email"));
        }
        if (body.containsKey("phoneNumber") || body.containsKey("phone_number")) {
            String phone = (String) body.getOrDefault("phoneNumber", body.get("phone_number"));
            user.setPhoneNumber(phone);
        }
        if (body.containsKey("password") && body.get("password") != null) {
            String rawPassword = (String) body.get("password");
            if (!rawPassword.trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(rawPassword));
            }
        }
        if (body.containsKey("is_superuser") || body.containsKey("role_id") || body.containsKey("roleId")) {
            CurrentUser current = SecurityUtils.requireCurrentUser();
            if (current.id().equals(id)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Cannot modify your own role");
            }
            user.setRoleId(resolveRoleId(body));
        }
        UserEntity saved = userRepository.save(user);
        redisCacheService.evictUserInfo(id);
        return AdminUserResponse.fromEntity(saved);
    }

    @DeleteMapping("/users/{id}")
    public Map<String, String> deleteUser(@PathVariable Long id) {
        requireAdmin();
        CurrentUser current = SecurityUtils.requireCurrentUser();
        if (current.id().equals(id)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot delete your own account");
        }
        if (!userRepository.existsById(id)) {
            throw new AppException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
        redisCacheService.evictUserInfo(id);
        return Map.of("detail", "Deleted");
    }

    @PutMapping("/users/{id}/role")
    public Map<String, String> updateUserRole(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireAdmin();
        CurrentUser current = SecurityUtils.requireCurrentUser();
        if (current.id().equals(id)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot modify your own account");
        }
        Integer roleId = resolveRoleId(body);
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRoleId(roleId);
        userRepository.save(user);
        redisCacheService.evictUserInfo(id);
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
        redisCacheService.evictUserInfo(id);
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

    private Integer resolveRoleId(Map<String, Object> body) {
        if (body.containsKey("is_superuser")) {
            return Boolean.TRUE.equals(body.get("is_superuser"))
                    ? CurrentUser.ADMIN_ROLE_ID
                    : CurrentUser.USER_ROLE_ID;
        }

        Integer roleId = parseRoleId(body.getOrDefault("role_id", body.get("roleId")));
        if (roleId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "role_id is required");
        }
        if (roleId != CurrentUser.ADMIN_ROLE_ID && roleId != CurrentUser.USER_ROLE_ID) {
            throw new AppException(HttpStatus.BAD_REQUEST, "role_id must be 0 or 1");
        }
        return roleId;
    }

    private void invalidateTrafficCaches() {
        redisCacheService.evict("traffic:roads");
        redisCacheService.evictByPrefix("traffic:maas:");
        redisCacheService.evictByPrefix("traffic:info:");
        // 清除动态道路发现缓存，确保摄像头变更后道路列表实时更新
        roadService.evictRoadCache();
    }

    private static String normalizeRoadName(String roadName, String fallbackName) {
        String normalized = trimToNull(roadName);
        if (normalized != null) {
            return normalized;
        }
        return trimToNull(fallbackName);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
