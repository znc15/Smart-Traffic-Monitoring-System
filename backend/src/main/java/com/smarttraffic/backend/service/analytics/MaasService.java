package com.smarttraffic.backend.service.analytics;

import com.smarttraffic.backend.dto.maas.MaasCongestionItemResponse;
import com.smarttraffic.backend.dto.maas.MaasCongestionResponse;
import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.model.TrafficSampleEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import com.smarttraffic.backend.repository.TrafficSampleRepository;
import com.smarttraffic.backend.service.TrafficService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MaasService {

    private final CameraRepository cameraRepository;
    private final TrafficSampleRepository trafficSampleRepository;
    private final TrafficService trafficService;

    public MaasService(
            CameraRepository cameraRepository,
            TrafficSampleRepository trafficSampleRepository,
            TrafficService trafficService
    ) {
        this.cameraRepository = cameraRepository;
        this.trafficSampleRepository = trafficSampleRepository;
        this.trafficService = trafficService;
    }

    @Transactional(readOnly = true)
    public MaasCongestionResponse queryCongestion(double minLat, double maxLat, double minLng, double maxLng) {
        List<CameraEntity> cameras = cameraRepository.findInBoundingBox(
                minLat,
                maxLat,
                minLng,
                maxLng
        );

        List<CameraEntity> validCameras = cameras.stream()
                .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                .toList();

        Set<String> roadNames = new LinkedHashSet<>();
        for (CameraEntity camera : validCameras) {
            roadNames.add(firstNonBlank(camera.getRoadName(), camera.getName()));
        }

        Map<String, TrafficSampleEntity> latestByRoad = roadNames.isEmpty()
                ? Map.of()
                : trafficSampleRepository.findLatestByRoadNames(roadNames).stream()
                        .collect(Collectors.toMap(TrafficSampleEntity::getRoadName, Function.identity(), (a, b) -> a));

        List<MaasCongestionItemResponse> data = new ArrayList<>();
        LocalDateTime updatedAt = LocalDateTime.now();

        for (CameraEntity camera : validCameras) {
            String roadName = firstNonBlank(camera.getRoadName(), camera.getName());
            TrafficSampleEntity latest = latestByRoad.get(roadName);

            double congestionIndex;
            String densityStatus;
            if (latest != null) {
                congestionIndex = latest.getCongestionIndex() == null ? 0.0 : latest.getCongestionIndex();
                densityStatus = latest.getDensityStatus() == null ? "unknown" : latest.getDensityStatus();
                if (latest.getSampleTime() != null && latest.getSampleTime().isAfter(updatedAt)) {
                    updatedAt = latest.getSampleTime();
                }
            } else {
                var snapshot = trafficService.info(roadName);
                congestionIndex = toDouble(snapshot.get("congestion_index"));
                densityStatus = String.valueOf(snapshot.getOrDefault("density_status", "unknown"));
            }

            data.add(new MaasCongestionItemResponse(
                    camera.getId(),
                    roadName,
                    camera.getLatitude(),
                    camera.getLongitude(),
                    round3(congestionIndex),
                    densityStatus
            ));
        }

        return new MaasCongestionResponse(updatedAt, data);
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback == null ? "unknown" : fallback;
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
