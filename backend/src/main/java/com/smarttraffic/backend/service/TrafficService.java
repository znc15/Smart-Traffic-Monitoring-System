package com.smarttraffic.backend.service;

import com.smarttraffic.backend.config.TrafficProperties;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TrafficService {

    private static final byte[] SAMPLE_JPEG = Base64.getDecoder().decode(
            "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFAABAAAAAAAAAAAAAAAAAAAAB//EABQQAQAAAAAAAAAAAAAAAAAAAAD/xAAUAQEAAAAAAAAAAAAAAAAAAAAA/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAwDAQACEQMRAD8AAAH/2Q=="
    );

    private final ConcurrentMap<String, Snapshot> snapshots = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, byte[]> frameCache = new ConcurrentHashMap<>();
    private final CameraRepository cameraRepository;

    public TrafficService(TrafficProperties trafficProperties, CameraRepository cameraRepository) {
        this.cameraRepository = cameraRepository;
        List<CameraEntity> cameras = cameraRepository.findByEnabledTrue();
        List<String> names = cameras.isEmpty()
                ? trafficProperties.roadsAsList()
                : cameras.stream().map(CameraEntity::getName).toList();
        for (String road : names) {
            snapshots.put(road, new Snapshot(randomCount(), randomCount(), randomSpeed(), randomSpeed()));
        }
    }

    public List<String> roadNames() {
        return snapshots.keySet().stream().sorted().toList();
    }

    public Map<String, Object> info(String roadName) {
        Snapshot snapshot = snapshots.get(roadName);
        if (snapshot == null) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đường");
        }

        snapshot.tick();

        long countThreshold = 12;
        long speedThreshold = 40;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count_car", snapshot.countCar);
        response.put("count_motor", snapshot.countMotor);
        response.put("speed_car", snapshot.speedCar);
        response.put("speed_motor", snapshot.speedMotor);
        response.put("density_status", (snapshot.countCar + snapshot.countMotor) > countThreshold ? "high" : "normal");
        response.put("speed_status", (snapshot.speedCar + snapshot.speedMotor) / 2 > speedThreshold ? "fast" : "slow");

        Map<String, Object> thresholds = new LinkedHashMap<>();
        thresholds.put("count_threshold", countThreshold);
        thresholds.put("speed_threshold", speedThreshold);
        response.put("thresholds", thresholds);

        return response;
    }

    public byte[] frame(String roadName) {
        if (!snapshots.containsKey(roadName)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đường");
        }
        return frameCache.getOrDefault(roadName, SAMPLE_JPEG);
    }

    public void updateFromRemote(String roadName, Map<String, Object> data, byte[] frameBytes) {
        Snapshot snapshot = snapshots.get(roadName);
        if (snapshot == null) {
            snapshot = new Snapshot(0, 0, 0, 0);
            snapshots.put(roadName, snapshot);
        }
        snapshot.updateFrom(data);
        if (frameBytes != null) frameCache.put(roadName, frameBytes);
    }

    public void reloadCameras(TrafficProperties trafficProperties) {
        List<CameraEntity> cameras = cameraRepository.findByEnabledTrue();
        List<String> names = cameras.isEmpty()
                ? trafficProperties.roadsAsList()
                : cameras.stream().map(CameraEntity::getName).toList();
        // Add new cameras
        for (String name : names) {
            snapshots.putIfAbsent(name, new Snapshot(randomCount(), randomCount(), randomSpeed(), randomSpeed()));
        }
        // Remove deleted cameras
        snapshots.keySet().retainAll(new java.util.HashSet<>(names));
        frameCache.keySet().retainAll(new java.util.HashSet<>(names));
    }

    private static long randomCount() {
        return ThreadLocalRandom.current().nextLong(0, 18);
    }

    private static long randomSpeed() {
        return ThreadLocalRandom.current().nextLong(10, 70);
    }

    private static class Snapshot {
        private long countCar;
        private long countMotor;
        private long speedCar;
        private long speedMotor;

        private Snapshot(long countCar, long countMotor, long speedCar, long speedMotor) {
            this.countCar = countCar;
            this.countMotor = countMotor;
            this.speedCar = speedCar;
            this.speedMotor = speedMotor;
        }

        private synchronized void tick() {
            countCar = clamp(countCar + ThreadLocalRandom.current().nextLong(-2, 3), 0, 40);
            countMotor = clamp(countMotor + ThreadLocalRandom.current().nextLong(-2, 3), 0, 40);
            speedCar = clamp(speedCar + ThreadLocalRandom.current().nextLong(-5, 6), 0, 100);
            speedMotor = clamp(speedMotor + ThreadLocalRandom.current().nextLong(-5, 6), 0, 100);
        }

        private synchronized void updateFrom(Map<String, Object> data) {
            if (data.containsKey("count_car")) countCar = ((Number) data.get("count_car")).longValue();
            if (data.containsKey("count_motor")) countMotor = ((Number) data.get("count_motor")).longValue();
            if (data.containsKey("speed_car")) speedCar = ((Number) data.get("speed_car")).longValue();
            if (data.containsKey("speed_motor")) speedMotor = ((Number) data.get("speed_motor")).longValue();
        }

        private static long clamp(long value, long min, long max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
