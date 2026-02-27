package com.smarttraffic.backend.service.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.dto.edge.EdgeTelemetryRequest;
import com.smarttraffic.backend.model.TrafficEventEntity;
import com.smarttraffic.backend.model.TrafficSampleEntity;
import com.smarttraffic.backend.repository.TrafficEventRepository;
import com.smarttraffic.backend.repository.TrafficSampleRepository;
import com.smarttraffic.backend.service.TrafficService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TelemetryIngestionService {

    private final TrafficSampleRepository trafficSampleRepository;
    private final TrafficEventRepository trafficEventRepository;
    private final TrafficService trafficService;
    private final ObjectMapper objectMapper;
    private final MirrorWriteService mirrorWriteService;
    private final RedisCacheService redisCacheService;

    public TelemetryIngestionService(
            TrafficSampleRepository trafficSampleRepository,
            TrafficEventRepository trafficEventRepository,
            TrafficService trafficService,
            ObjectMapper objectMapper,
            MirrorWriteService mirrorWriteService,
            RedisCacheService redisCacheService
    ) {
        this.trafficSampleRepository = trafficSampleRepository;
        this.trafficEventRepository = trafficEventRepository;
        this.trafficService = trafficService;
        this.objectMapper = objectMapper;
        this.mirrorWriteService = mirrorWriteService;
        this.redisCacheService = redisCacheService;
    }

    @Transactional
    public void ingest(EdgeTelemetryRequest request) {
        LocalDateTime sampleTime = request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now();

        int countCar = positive(request.getCountCar());
        int countMotor = positive(request.getCountMotor());
        int countPerson = positive(request.getCountPerson());
        double speedCar = nonNegative(request.getAvgSpeedCar());
        double speedMotor = nonNegative(request.getAvgSpeedMotor());

        String densityStatus = normalizeDensityStatus(request.getDensityStatus(), countCar, countMotor, countPerson);
        String speedStatus = normalizeSpeedStatus(request.getSpeedStatus(), speedCar, speedMotor);
        double congestionIndex = request.getCongestionIndex() != null
                ? clamp01(request.getCongestionIndex())
                : estimateCongestionIndex(countCar, countMotor, countPerson, speedCar, speedMotor);

        TrafficSampleEntity sample = new TrafficSampleEntity();
        sample.setNodeId(trimToNull(request.getNodeId()));
        sample.setRoadName(request.getRoadName().trim());
        sample.setSampleTime(sampleTime);
        sample.setCountCar(countCar);
        sample.setCountMotor(countMotor);
        sample.setCountPerson(countPerson);
        sample.setAvgSpeedCar(speedCar);
        sample.setAvgSpeedMotor(speedMotor);
        sample.setDensityStatus(densityStatus);
        sample.setSpeedStatus(speedStatus);
        sample.setCongestionIndex(congestionIndex);
        sample.setLaneStatsJson(toJson(request.getLaneStats(), "[]"));
        sample.setSource("edge");
        trafficSampleRepository.save(sample);
        mirrorWriteService.mirrorTrafficSample(sample);

        List<EdgeTelemetryRequest.EventPayload> events = request.getEvents() == null ? List.of() : request.getEvents();
        for (EdgeTelemetryRequest.EventPayload item : events) {
            TrafficEventEntity event = new TrafficEventEntity();
            event.setNodeId(trimToNull(request.getNodeId()));
            event.setRoadName(request.getRoadName().trim());
            event.setEventType(item.getEventType());
            event.setLevel(item.getLevel());
            event.setStartAt(item.getStartAt());
            event.setEndAt(item.getEndAt());
            event.setPayloadJson(toJson(item, "{}"));
            trafficEventRepository.save(event);
            mirrorWriteService.mirrorTrafficEvent(event);
        }

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("count_car", countCar);
        snapshot.put("count_motor", countMotor);
        snapshot.put("count_person", countPerson);
        snapshot.put("speed_car", Math.round(speedCar));
        snapshot.put("speed_motor", Math.round(speedMotor));
        snapshot.put("density_status", densityStatus);
        snapshot.put("speed_status", speedStatus);
        snapshot.put("congestion_index", congestionIndex);
        snapshot.put("lane_stats", request.getLaneStats());
        snapshot.put("events", events);
        String normalizedRoadName = request.getRoadName().trim();
        trafficService.updateFromRemote(normalizedRoadName, snapshot, null);

        // 主动失效热点缓存，避免短时间读到旧值
        redisCacheService.evict("traffic:roads");
        redisCacheService.evict("traffic:info:" + normalizedRoadName);
        redisCacheService.evictByPrefix("traffic:pred:" + normalizedRoadName + ":");
        redisCacheService.evictByPrefix("traffic:maas:");
    }

    private static int positive(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private static double nonNegative(Double value) {
        return value == null ? 0.0 : Math.max(0.0, value);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static String normalizeDensityStatus(String input, int countCar, int countMotor, int countPerson) {
        if (input != null && !input.isBlank()) {
            String normalized = input.trim().toLowerCase();
            if (normalized.equals("clear") || normalized.equals("busy") || normalized.equals("congested") || normalized.equals("offline")) {
                return normalized;
            }
        }
        int total = countCar + countMotor + countPerson;
        if (total > 30) {
            return "congested";
        }
        if (total > 14) {
            return "busy";
        }
        return "clear";
    }

    private static String normalizeSpeedStatus(String input, double speedCar, double speedMotor) {
        if (input != null && !input.isBlank()) {
            String normalized = input.trim().toLowerCase();
            if (normalized.equals("slow") || normalized.equals("fast") || normalized.equals("unknown")) {
                return normalized;
            }
        }
        double avg = (speedCar + speedMotor) / 2.0;
        if (avg <= 0.1) {
            return "unknown";
        }
        return avg >= 40.0 ? "fast" : "slow";
    }

    private static double estimateCongestionIndex(int countCar, int countMotor, int countPerson, double speedCar, double speedMotor) {
        double densityFactor = Math.min(1.0, (countCar + countMotor + countPerson) / 35.0);
        double avgSpeed = (speedCar + speedMotor) / 2.0;
        double speedPenalty = avgSpeed <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, 1.0 - (avgSpeed / 60.0)));
        return Math.round((densityFactor * 0.7 + speedPenalty * 0.3) * 1000.0) / 1000.0;
    }

    private String toJson(Object value, String fallback) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return fallback;
        }
    }
}
