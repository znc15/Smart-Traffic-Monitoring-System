package com.smarttraffic.backend.service;

import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 基于摄像头经纬度的反向地理编码服务
 */
@Service
public class GeocodingService {

    private final CameraRepository cameraRepository;

    public GeocodingService(CameraRepository cameraRepository) {
        this.cameraRepository = cameraRepository;
    }

    /**
     * 查找距离给定经纬度最近的摄像头
     */
    public CameraEntity findNearestCamera(double lat, double lng) {
        List<CameraEntity> cameras = cameraRepository.findByEnabledTrue().stream()
                .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                .toList();
        if (cameras.isEmpty()) return null;
        return cameras.stream()
                .min(Comparator.comparingDouble(c -> haversineKm(lat, lng, c.getLatitude(), c.getLongitude())))
                .orElse(null);
    }

    /**
     * 查找距离给定经纬度最近的道路名称
     */
    public String findNearestRoad(double lat, double lng) {
        CameraEntity nearest = findNearestCamera(lat, lng);
        if (nearest == null) return null;
        return nearest.getRoadName() != null ? nearest.getRoadName() : nearest.getName();
    }

    /**
     * 查找距离给定经纬度最近的 N 个摄像头
     */
    public List<Map<String, Object>> findNearbyCameras(double lat, double lng, int limit) {
        List<CameraEntity> cameras = cameraRepository.findByEnabledTrue().stream()
                .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                .toList();
        return cameras.stream()
                .sorted(Comparator.comparingDouble(c -> haversineKm(lat, lng, c.getLatitude(), c.getLongitude())))
                .limit(limit)
                .map(c -> {
                    Map<String, Object> item = new java.util.LinkedHashMap<>();
                    item.put("name", c.getName());
                    item.put("road_name", c.getRoadName());
                    item.put("latitude", c.getLatitude());
                    item.put("longitude", c.getLongitude());
                    item.put("location", c.getLocation());
                    item.put("distance_km", Math.round(haversineKm(lat, lng, c.getLatitude(), c.getLongitude()) * 100.0) / 100.0);
                    return item;
                })
                .toList();
    }

    /**
     * Haversine 公式计算两点间距离（公里）
     */
    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}