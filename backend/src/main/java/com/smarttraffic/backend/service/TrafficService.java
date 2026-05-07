package com.smarttraffic.backend.service;

import com.smarttraffic.backend.config.TrafficProperties;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashSet;
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
    private final TrafficProperties trafficProperties;

    public TrafficService(TrafficProperties trafficProperties, CameraRepository cameraRepository) {
        this.cameraRepository = cameraRepository;
        this.trafficProperties = trafficProperties;
        List<CameraEntity> cameras = cameraRepository.findByEnabledTrue();
        if (cameras.isEmpty()) {
            for (String road : trafficProperties.roadsAsList()) {
                snapshots.put(road, new Snapshot(
                        randomCount(),
                        randomCount(),
                        randomCount(),
                        randomSpeed(),
                        randomSpeed()
                ));
            }
        } else {
            for (CameraEntity cam : cameras) {
                boolean isRemote = isRemoteCamera(cam);
                snapshots.put(cameraKey(cam), new Snapshot(
                        isRemote ? 0 : randomCount(),
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

    /**
     * 获取所有路段当前快照数据，用于定时采样持久化
     */
    public Map<String, SnapshotData> getAllSnapshots() {
        Map<String, SnapshotData> result = new LinkedHashMap<>();
        long busyThreshold = 8;
        long congestedThreshold = 12;
        long speedThreshold = 40;
        for (Map.Entry<String, Snapshot> entry : snapshots.entrySet()) {
            String roadName = entry.getKey();
            Snapshot snap = entry.getValue();
            boolean online = snap.isOnline();
            String densityStatus = computeDensityStatus(snap, online, busyThreshold, congestedThreshold);
            String speedStatus = computeSpeedStatus(snap, online, speedThreshold);
            result.put(roadName, new SnapshotData(
                    snap.countCar,
                    snap.countMotor,
                    snap.countPerson,
                    snap.speedCar,
                    snap.speedMotor,
                    snap.congestionIndex,
                    densityStatus,
                    speedStatus,
                    snap.hasRemote
            ));
        }
        return result;
    }

    /** 快照数据传输对象 */
    public record SnapshotData(
            long countCar,
            long countMotor,
            long countPerson,
            long speedCar,
            long speedMotor,
            double congestionIndex,
            String densityStatus,
            String speedStatus,
            boolean hasRemote
    ) {}

    /**
     * 从数据库读取所有不重复的 roadName 作为道路列表。
     * 如果数据库中没有 roadName 数据，则 fallback 到 TrafficProperties 的配置。
     */
    public List<String> distinctRoadNames() {
        List<String> dbRoads = cameraRepository.findDistinctRoadNames();
        if (dbRoads != null && !dbRoads.isEmpty()) {
            return dbRoads.stream().sorted().toList();
        }
        return trafficProperties.roadsAsList();
    }

    public Map<String, Object> info(String roadName) {
        Snapshot snapshot = snapshots.get(roadName);
        if (snapshot == null) {
            throw new AppException(HttpStatus.NOT_FOUND, "未找到道路");
        }

        snapshot.tick();

        long busyThreshold = 8;
        long congestedThreshold = 12;
        long speedThreshold = 40;

        Map<String, Object> response = new LinkedHashMap<>();
        boolean online = snapshot.isOnline();
        response.put("online", online);
        response.put("count_car", snapshot.countCar);
        response.put("count_motor", snapshot.countMotor);
        response.put("count_person", snapshot.countPerson);
        response.put("speed_car", snapshot.speedCar);
        response.put("speed_motor", snapshot.speedMotor);
        response.put("density_status", computeDensityStatus(snapshot, online, busyThreshold, congestedThreshold));
        response.put("speed_status", computeSpeedStatus(snapshot, online, speedThreshold));
        response.put("congestion_index", snapshot.congestionIndex);
        response.put("lane_stats", snapshot.laneStats);
        response.put("events", snapshot.events);

        Map<String, Object> thresholds = new LinkedHashMap<>();
        thresholds.put("busy_threshold", busyThreshold);
        thresholds.put("congested_threshold", congestedThreshold);
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
            snapshot = new Snapshot(0, 0, 0, 0, 0, true);
            snapshots.put(roadName, snapshot);
        }
        snapshot.updateFrom(data);
        if (frameBytes != null) frameCache.put(roadName, frameBytes);
    }

    public void reloadCameras(TrafficProperties trafficProperties) {
        List<CameraEntity> cameras = cameraRepository.findByEnabledTrue();
        List<String> roadKeys = cameras.isEmpty()
                ? trafficProperties.roadsAsList()
                : cameras.stream().map(TrafficService::cameraKey).toList();
        HashSet<String> remoteKeys = cameras.stream()
                .filter(TrafficService::isRemoteCamera)
                .map(TrafficService::cameraKey)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        // Add new cameras
        for (String roadKey : roadKeys) {
            boolean isRemote = remoteKeys.contains(roadKey);
            snapshots.putIfAbsent(roadKey, new Snapshot(
                    isRemote ? 0 : randomCount(),
                    isRemote ? 0 : randomCount(),
                    isRemote ? 0 : randomCount(),
                    isRemote ? 0 : randomSpeed(),
                    isRemote ? 0 : randomSpeed(),
                    isRemote
            ));
        }
        // Remove deleted cameras
        snapshots.keySet().retainAll(new HashSet<>(roadKeys));
        frameCache.keySet().retainAll(new HashSet<>(roadKeys));
    }

    private static boolean isRemoteCamera(CameraEntity camera) {
        return (camera.getNodeUrl() != null && !camera.getNodeUrl().isBlank())
                || (camera.getStreamUrl() != null && !camera.getStreamUrl().isBlank());
    }

    private static String cameraKey(CameraEntity camera) {
        if (camera.getRoadName() != null && !camera.getRoadName().isBlank()) {
            return camera.getRoadName().trim();
        }
        return camera.getName();
    }

    private static String computeDensityStatus(Snapshot snapshot, boolean online, long busyThreshold, long congestedThreshold) {
        if (!online) {
            return "offline";
        }
        if (snapshot.densityStatusOverride != null && !snapshot.densityStatusOverride.isBlank()) {
            return snapshot.densityStatusOverride;
        }
        long total = snapshot.countCar + snapshot.countMotor + snapshot.countPerson;
        if (total > congestedThreshold) {
            return "congested";
        }
        if (total > busyThreshold) {
            return "busy";
        }
        return "clear";
    }

    private static String computeSpeedStatus(Snapshot snapshot, boolean online, long speedThreshold) {
        if (!online) {
            return "unknown";
        }
        if (snapshot.speedStatusOverride != null && !snapshot.speedStatusOverride.isBlank()) {
            return snapshot.speedStatusOverride;
        }
        if (snapshot.hasRemote && snapshot.speedCar == 0 && snapshot.speedMotor == 0) {
            return "unknown";
        }
        long averageSpeed = (snapshot.speedCar + snapshot.speedMotor) / 2;
        return averageSpeed > speedThreshold ? "fast" : "slow";
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
        private long countPerson;
        private long speedCar;
        private long speedMotor;
        private double congestionIndex;
        private Object laneStats = List.of();
        private Object events = List.of();
        private String densityStatusOverride;
        private String speedStatusOverride;

        // 远程边缘节点连接追踪
        private volatile boolean hasRemote;       // 是否曾收到过远程数据
        private volatile long lastRemoteUpdateMs; // 最后一次远程更新的时间戳

        // 超过此时间未收到远程数据则视为离线（毫秒）
        private static final long STALE_THRESHOLD_MS = 10_000;

        private Snapshot(long countCar, long countMotor, long countPerson, long speedCar, long speedMotor) {
            this(countCar, countMotor, countPerson, speedCar, speedMotor, false);
        }

        private Snapshot(long countCar, long countMotor, long countPerson, long speedCar, long speedMotor, boolean remote) {
            this.countCar = countCar;
            this.countMotor = countMotor;
            this.countPerson = countPerson;
            this.speedCar = speedCar;
            this.speedMotor = speedMotor;
            this.congestionIndex = estimateCongestionIndex(countCar, countMotor, countPerson, speedCar, speedMotor);
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
            countPerson = clamp(countPerson + ThreadLocalRandom.current().nextLong(-2, 3), 0, 40);
            speedCar = clamp(speedCar + ThreadLocalRandom.current().nextLong(-5, 6), 0, 100);
            speedMotor = clamp(speedMotor + ThreadLocalRandom.current().nextLong(-5, 6), 0, 100);
            congestionIndex = estimateCongestionIndex(countCar, countMotor, countPerson, speedCar, speedMotor);
            densityStatusOverride = null;
            speedStatusOverride = null;
            laneStats = List.of();
            events = List.of();
        }

        private synchronized void updateFrom(Map<String, Object> data) {
            if (data.containsKey("count_car")) countCar = ((Number) data.get("count_car")).longValue();
            if (data.containsKey("count_motor")) countMotor = ((Number) data.get("count_motor")).longValue();
            if (data.containsKey("count_person")) countPerson = ((Number) data.get("count_person")).longValue();
            if (data.containsKey("speed_car")) speedCar = ((Number) data.get("speed_car")).longValue();
            if (data.containsKey("speed_motor")) speedMotor = ((Number) data.get("speed_motor")).longValue();
            if (data.containsKey("congestion_index")) {
                Object value = data.get("congestion_index");
                if (value instanceof Number number) {
                    congestionIndex = number.doubleValue();
                }
            } else {
                congestionIndex = estimateCongestionIndex(countCar, countMotor, countPerson, speedCar, speedMotor);
            }
            if (data.containsKey("density_status")) {
                densityStatusOverride = String.valueOf(data.get("density_status"));
            }
            if (data.containsKey("speed_status")) {
                speedStatusOverride = String.valueOf(data.get("speed_status"));
            }
            if (data.containsKey("lane_stats")) {
                laneStats = data.get("lane_stats");
            }
            if (data.containsKey("events")) {
                events = data.get("events");
            }
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

        private static double estimateCongestionIndex(long countCar, long countMotor, long countPerson, long speedCar, long speedMotor) {
            double densityFactor = Math.min(1.0, (countCar + countMotor + countPerson) / 35.0);
            double averageSpeed = (speedCar + speedMotor) / 2.0;
            double speedPenalty = averageSpeed <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, 1.0 - (averageSpeed / 60.0)));
            return Math.round((densityFactor * 0.7 + speedPenalty * 0.3) * 1000.0) / 1000.0;
        }
    }
}
