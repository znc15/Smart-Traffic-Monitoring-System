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
        if (cameras.isEmpty()) {
            for (String road : trafficProperties.roadsAsList()) {
                snapshots.put(road, new Snapshot(randomCount(), randomCount(), randomSpeed(), randomSpeed()));
            }
        } else {
            for (CameraEntity cam : cameras) {
                boolean isRemote = cam.getStreamUrl() != null && !cam.getStreamUrl().isBlank();
                snapshots.put(cam.getName(), new Snapshot(
                        isRemote ? 0 : randomCount(),
                        isRemote ? 0 : randomCount(),
                        isRemote ? 0 : randomSpeed(),
                        isRemote ? 0 : randomSpeed(),
                        isRemote
                ));
            }
        }
    }

    public List<String> roadNames() {
        return snapshots.keySet().stream().sorted().toList();
    }

    public Map<String, Object> info(String roadName) {
        Snapshot snapshot = snapshots.get(roadName);
        if (snapshot == null) {
            throw new AppException(HttpStatus.NOT_FOUND, "未找到道路");
        }

        snapshot.tick();

        long countThreshold = 12;
        long speedThreshold = 40;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("online", snapshot.isOnline());
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
            throw new AppException(HttpStatus.NOT_FOUND, "未找到道路");
        }
        return frameCache.getOrDefault(roadName, SAMPLE_JPEG);
    }

    public void updateFromRemote(String roadName, Map<String, Object> data, byte[] frameBytes) {
        Snapshot snapshot = snapshots.get(roadName);
        if (snapshot == null) {
            snapshot = new Snapshot(0, 0, 0, 0, true);
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
        // 构建远程摄像头名称集合
        java.util.Set<String> remoteNames = cameras.stream()
                .filter(c -> c.getStreamUrl() != null && !c.getStreamUrl().isBlank())
                .map(CameraEntity::getName)
                .collect(java.util.stream.Collectors.toSet());
        // Add new cameras
        for (String name : names) {
            boolean isRemote = remoteNames.contains(name);
            snapshots.putIfAbsent(name, new Snapshot(
                    isRemote ? 0 : randomCount(),
                    isRemote ? 0 : randomCount(),
                    isRemote ? 0 : randomSpeed(),
                    isRemote ? 0 : randomSpeed(),
                    isRemote
            ));
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

        // 远程边缘节点连接追踪
        private volatile boolean hasRemote;       // 是否曾收到过远程数据
        private volatile long lastRemoteUpdateMs; // 最后一次远程更新的时间戳

        // 超过此时间未收到远程数据则视为离线（毫秒）
        private static final long STALE_THRESHOLD_MS = 10_000;

        private Snapshot(long countCar, long countMotor, long speedCar, long speedMotor) {
            this(countCar, countMotor, speedCar, speedMotor, false);
        }

        private Snapshot(long countCar, long countMotor, long speedCar, long speedMotor, boolean remote) {
            this.countCar = countCar;
            this.countMotor = countMotor;
            this.speedCar = speedCar;
            this.speedMotor = speedMotor;
            if (remote) {
                this.hasRemote = true;
                this.lastRemoteUpdateMs = 0; // 立即视为离线，直到首次成功轮询
            }
        }

        /** 仅对纯模拟路段随机变更数据；有真实边缘节点的路段数据冻结 */
        private synchronized void tick() {
            if (hasRemote) return;
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
            hasRemote = true;
            lastRemoteUpdateMs = System.currentTimeMillis();
        }

        /** 判断在线状态：纯模拟路段始终 true，远程路段根据最后更新时间判断 */
        private boolean isOnline() {
            if (!hasRemote) return true; // 纯模拟路段视为在线
            return (System.currentTimeMillis() - lastRemoteUpdateMs) < STALE_THRESHOLD_MS;
        }

        private static long clamp(long value, long min, long max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
