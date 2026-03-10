package com.smarttraffic.backend.service;

import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.model.TrafficSampleEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import com.smarttraffic.backend.repository.TrafficSampleRepository;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MapOverviewService {

    private final CameraRepository cameraRepository;
    private final TrafficSampleRepository trafficSampleRepository;
    private final TrafficService trafficService;

    public MapOverviewService(
            CameraRepository cameraRepository,
            TrafficSampleRepository trafficSampleRepository,
            TrafficService trafficService
    ) {
        this.cameraRepository = cameraRepository;
        this.trafficSampleRepository = trafficSampleRepository;
        this.trafficService = trafficService;
    }

    public Map<String, Object> overview() {
        List<CameraEntity> cameras = cameraRepository.findByEnabledTrue().stream()
                .filter(camera -> camera.getLatitude() != null && camera.getLongitude() != null)
                .toList();

        List<String> roadNames = cameras.stream()
                .map(MapOverviewService::roadKey)
                .distinct()
                .toList();
        Map<String, TrafficSampleEntity> latestByRoad = latestSamples(roadNames);

        List<Map<String, Object>> items = cameras.stream()
                .map(camera -> toItem(camera, latestByRoad.get(roadKey(camera))))
                .toList();

        return Map.of(
                "updated_at", LocalDateTime.now(),
                "items", items
        );
    }

    private Map<String, TrafficSampleEntity> latestSamples(Collection<String> roadNames) {
        if (roadNames.isEmpty()) {
            return Map.of();
        }
        return trafficSampleRepository.findLatestByRoadNames(roadNames).stream()
                .collect(Collectors.toMap(TrafficSampleEntity::getRoadName, Function.identity(), (left, right) -> left));
    }

    private Map<String, Object> toItem(CameraEntity camera, TrafficSampleEntity sample) {
        String roadName = roadKey(camera);
        Map<String, Object> snapshot = trafficService.info(roadName);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("camera_id", camera.getId());
        item.put("name", camera.getName());
        item.put("road_name", roadName);
        item.put("edge_node_id", camera.getEdgeNodeId());
        item.put("latitude", camera.getLatitude());
        item.put("longitude", camera.getLongitude());
        item.put("online", snapshot.getOrDefault("online", false));
        item.put("density_status", snapshot.getOrDefault("density_status", sample == null ? "unknown" : sample.getDensityStatus()));
        item.put("congestion_index", snapshot.getOrDefault("congestion_index", sample == null ? 0.0 : sample.getCongestionIndex()));
        item.put("count_car", snapshot.getOrDefault("count_car", sample == null ? 0 : sample.getCountCar()));
        item.put("count_motor", snapshot.getOrDefault("count_motor", sample == null ? 0 : sample.getCountMotor()));
        item.put("count_person", snapshot.getOrDefault("count_person", sample == null ? 0 : sample.getCountPerson()));
        item.put("speed_car", snapshot.getOrDefault("speed_car", sample == null ? 0.0 : sample.getAvgSpeedCar()));
        item.put("speed_motor", snapshot.getOrDefault("speed_motor", sample == null ? 0.0 : sample.getAvgSpeedMotor()));
        item.put("snapshot_url", buildFrameUrl(roadName));
        item.put("updated_at", sample != null ? sample.getSampleTime() : null);
        return item;
    }

    private String buildFrameUrl(String roadName) {
        String encodedRoad = URLEncoder.encode(roadName, StandardCharsets.UTF_8);
        return "/api/v1/frames_no_auth/" + encodedRoad;
    }

    private static String roadKey(CameraEntity camera) {
        if (camera.getRoadName() != null && !camera.getRoadName().isBlank()) {
            return camera.getRoadName().trim();
        }
        return camera.getName();
    }
}
