package com.smarttraffic.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.model.TrafficSampleEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import com.smarttraffic.backend.repository.TrafficSampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI Tool 执行器，处理 LLM 的 tool call 请求
 */
@Service
public class AiToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiToolExecutor.class);

    private final TrafficService trafficService;
    private final CameraRepository cameraRepository;
    private final TrafficSampleRepository trafficSampleRepository;
    private final GeocodingService geocodingService;
    private final ObjectMapper objectMapper;

    public AiToolExecutor(TrafficService trafficService,
                          CameraRepository cameraRepository,
                          TrafficSampleRepository trafficSampleRepository,
                          GeocodingService geocodingService,
                          ObjectMapper objectMapper) {
        this.trafficService = trafficService;
        this.cameraRepository = cameraRepository;
        this.trafficSampleRepository = trafficSampleRepository;
        this.geocodingService = geocodingService;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行 tool call 并返回结果
     * @param toolName 工具名称
     * @param argumentsJson JSON 格式的参数
     * @return 执行结果（Map 格式，会被序列化为 JSON）
     */
    public Map<String, Object> execute(String toolName, String argumentsJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> args = objectMapper.readValue(argumentsJson, Map.class);
            return switch (toolName) {
                case "query_traffic" -> executeQueryTraffic(args);
                case "list_cameras" -> executeListCameras(args);
                case "query_history" -> executeQueryHistory(args);
                case "reverse_geocode" -> executeReverseGeocode(args);
                default -> errorResult("未知工具: " + toolName);
            };
        } catch (Exception e) {
            log.error("Tool 执行失败: {} - {}", toolName, e.getMessage());
            return errorResult("工具执行失败: " + e.getMessage());
        }
    }

    /**
     * 查询指定道路的实时交通数据
     */
    private Map<String, Object> executeQueryTraffic(Map<String, Object> args) {
        String roadName = (String) args.get("road_name");
        if (roadName == null || roadName.isBlank()) {
            return errorResult("缺少参数: road_name");
        }

        try {
            Map<String, Object> info = trafficService.info(roadName);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("road_name", roadName);
            result.put("data", info);
            return result;
        } catch (Exception e) {
            return errorResult("道路 '" + roadName + "' 不在监控范围内。可用道路: " + 
                    String.join("、", trafficService.roadNames()));
        }
    }

    /**
     * 查询摄像头列表
     */
    private Map<String, Object> executeListCameras(Map<String, Object> args) {
        List<CameraEntity> cameras = cameraRepository.findByEnabledTrue();
        
        List<Map<String, Object>> cameraList = new ArrayList<>();
        for (CameraEntity cam : cameras) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", cam.getName());
            item.put("road_name", cam.getRoadName());
            item.put("location", cam.getLocation());
            item.put("latitude", cam.getLatitude());
            item.put("longitude", cam.getLongitude());
            item.put("enabled", cam.isEnabled());
            cameraList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("count", cameraList.size());
        result.put("cameras", cameraList);
        return result;
    }

    /**
     * 查询指定道路的历史统计数据
     */
    private Map<String, Object> executeQueryHistory(Map<String, Object> args) {
        String roadName = (String) args.get("road_name");
        if (roadName == null || roadName.isBlank()) {
            return errorResult("缺少参数: road_name");
        }

        // 默认查询最近 24 小时
        int hours = args.containsKey("hours") ? 
                ((Number) args.get("hours")).intValue() : 24;
        hours = Math.min(hours, 168); // 最多 7 天

        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(hours);

        List<TrafficSampleEntity> samples = trafficSampleRepository
                .findByRoadNameAndSampleTimeBetweenOrderBySampleTimeAsc(roadName, from, to);

        if (samples.isEmpty()) {
            return errorResult("道路 '" + roadName + "' 没有历史数据");
        }

        List<Map<String, Object>> historyList = new ArrayList<>();
        for (TrafficSampleEntity sample : samples) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("time", sample.getSampleTime().toString());
            item.put("count_car", sample.getCountCar());
            item.put("count_motor", sample.getCountMotor());
            item.put("count_person", sample.getCountPerson());
            item.put("avg_speed_car", sample.getAvgSpeedCar());
            item.put("avg_speed_motor", sample.getAvgSpeedMotor());
            item.put("congestion_index", sample.getCongestionIndex());
            item.put("density_status", sample.getDensityStatus());
            item.put("speed_status", sample.getSpeedStatus());
            historyList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("road_name", roadName);
        result.put("from", from.toString());
        result.put("to", to.toString());
        result.put("sample_count", samples.size());
        result.put("history", historyList);
        return result;
    }

    /**
     * 反向地理编码：根据经纬度返回最近的摄像头/道路
     */
    private Map<String, Object> executeReverseGeocode(Map<String, Object> args) {
        Object latObj = args.get("latitude");
        Object lngObj = args.get("longitude");

        if (latObj == null || lngObj == null) {
            return errorResult("缺少参数: latitude 或 longitude");
        }

        double lat = ((Number) latObj).doubleValue();
        double lng = ((Number) lngObj).doubleValue();

        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return errorResult("无效的经纬度值");
        }

        CameraEntity nearest = geocodingService.findNearestCamera(lat, lng);
        if (nearest == null) {
            return errorResult("没有找到启用的摄像头");
        }

        String nearestRoad = geocodingService.findNearestRoad(lat, lng);
        List<Map<String, Object>> nearbyCameras = geocodingService.findNearbyCameras(lat, lng, 5);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("query_location", Map.of("latitude", lat, "longitude", lng));
        result.put("nearest_road", nearestRoad);
        result.put("nearest_camera", Map.of(
                "name", nearest.getName(),
                "road_name", nearest.getRoadName(),
                "location", nearest.getLocation(),
                "latitude", nearest.getLatitude(),
                "longitude", nearest.getLongitude()
        ));
        result.put("nearby_cameras", nearbyCameras);
        return result;
    }

    private Map<String, Object> errorResult(String message) {
        return Map.of("success", false, "error", message);
    }
}