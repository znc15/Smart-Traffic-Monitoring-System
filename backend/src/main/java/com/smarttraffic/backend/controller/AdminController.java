package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.model.UserEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import com.smarttraffic.backend.repository.UserRepository;
import com.smarttraffic.backend.security.CurrentUser;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.service.SystemMetricsService;
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

    public AdminController(SystemMetricsService systemMetricsService, CameraRepository cameraRepository, UserRepository userRepository) {
        this.systemMetricsService = systemMetricsService;
        this.cameraRepository = cameraRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/resources")
    public Map<String, Object> resources() {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        if (!user.isAdmin()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Chỉ admin mới được phép truy cập tài nguyên hệ thống.");
        }
        return systemMetricsService.getSystemMetrics();
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
        return cameraRepository.save(camera);
    }

    @PutMapping("/cameras/{id}")
    public CameraEntity updateCamera(@PathVariable Long id, @RequestBody CameraEntity body) {
        requireAdmin();
        CameraEntity cam = cameraRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Camera not found"));
        if (body.getName() != null) cam.setName(body.getName());
        if (body.getLocation() != null) cam.setLocation(body.getLocation());
        cam.setEnabled(body.isEnabled());
        return cameraRepository.save(cam);
    }

    @DeleteMapping("/cameras/{id}")
    public Map<String, String> deleteCamera(@PathVariable Long id) {
        requireAdmin();
        if (!cameraRepository.existsById(id)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Camera not found");
        }
        cameraRepository.deleteById(id);
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
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRoleId(body.get("roleId"));
        userRepository.save(user);
        return Map.of("detail", "Role updated");
    }

    @PutMapping("/users/{id}/status")
    public Map<String, Object> toggleUserStatus(@PathVariable Long id) {
        requireAdmin();
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return Map.of("enabled", user.isEnabled());
    }
}
