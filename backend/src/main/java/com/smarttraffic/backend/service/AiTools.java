package com.smarttraffic.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.model.TrafficSampleEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import com.smarttraffic.backend.repository.TrafficSampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AI 工具执行器，处理 LLM tool call 请求
 */
@Service
public class AiTools {

    private static final Logger log = LoggerFactory.getLogger(AiTools.class);
    private static final int MAX_HISTORY_HOURS = 24;

    private final TrafficService trafficService;
    private final CameraRepository cameraRepository;
    private final TrafficSampleRepository trafficSampleRepository;
    private final GeocodingService geocodingService;
    private final ObjectMapper objectMapper;

    public AiTools(TrafficService trafficService,
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
     * 执行工具调用
     */
    public String execute(String toolName, JsonNode args) {
        try {
            return switch (toolName) {
                case "query_traffic" -> executeQueryTraffic(args);
                case "list_cameras" -> executeListCameras(args);
                case "query_history" -> executeQueryHistory(args);
                case "reverse_geocode" -> executeReverseGeocode(args);
                default -> "{\"error\": \"未知工具: " + toolName + "\"}";
            };
        } catch (Exception e) {
            log.warn("工具 {} 执行失败: {}", toolName, e.getMessage());
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String executeQueryTraffic(JsonNode args) throws Exception {
        String roadName = args.path("road_name").asText("");
        if (roadName.isBlank()) {
            return "{\"error\": \"road_name 参数不能为空\"}";
        }
        Map<String, Object> info = trafficService.info(roadName);
        return objectMapper.writeValueAsString(info);
    }

    private String executeListCameras(JsonNode args) throws Exception {
        String roadNameFilter = args.has("road_name") && !args.path("road_name").isNull()
                ? args.path("road_name").asText("") : null;

        List<CameraEntity> cameras = cameraRepository.findByEnabledTrue();
        List<Map<String, Object>> result = new ArrayList<>();
        for (CameraEntity cam : cameras) {
            if (roadNameFilter != null && !roadNameFilter.isBlank()) {
                if (cam.getRoadName() == null || !cam.getRoadName().contains(roadNameFilter)) {
                    continue;
                }
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", cam.getName());
            item.put("road_name", cam.getRoadName());
            item.put("location", cam.getLocation());
            item.put("latitude", cam.getLatitude());
            item.put("longitude", cam.getLongitude());
            item.put("enabled", cam.isEnabled());
            result.add(item);
        }
        return objectMapper.writeValueAsString(Map.of("cameras", result));
    }

    private String executeQueryHistory(JsonNode args) throws Exception {
        String roadName = args.path("road_name").asText("");
        if (roadName.isBlank()) {
            return "{\"error\": \"road_name 参数不能为空\"}";
        }

        int hours = args.has("hours") ? args.path("hours").asInt(1) : 1;
        hours = Math.max(1, Math.min(hours, MAX_HISTORY_HOURS));

        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(hours);

        List<TrafficSampleEntity> samples =
                trafficSampleRepository.findByRoadNameAndSampleTimeBetweenOrderBySampleTimeAsc(roadName, from, to);

        List<Map<String, Object>> result = new ArrayList<>();
        for (TrafficSampleEntity s : samples) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("time", s.getSampleTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            item.put("count_car", s.getCountCar());
            item.put("count_motor", s.getCountMotor());
            item.put("count_person", s.getCountPerson());
            item.put("avg_speed_car", s.getAvgSpeedCar());
            item.put("avg_speed_motor", s.getAvgSpeedMotor());
            item.put("congestion_index", s.getCongestionIndex());
            item.put("density_status", s.getDensityStatus());
            item.put("speed_status", s.getSpeedStatus());
            result.add(item);
        }
        return objectMapper.writeValueAsString(Map.of("road", roadName, "samples", result));
    }

    private String executeReverseGeocode(JsonNode args) throws Exception {
        if (!args.has("latitude") || !args.has("longitude")) {
            return "{\"error\": \"latitude 和 longitude 参数不能为空\"}";
        }
        double lat = args.path("latitude").asDouble();
        double lng = args.path("longitude").asDouble();
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return "{\"error\": \"经纬度超出有效范围\"}";
        }

        List<Map<String, Object>> nearby = geocodingService.findNearbyCameras(lat, lng, 5);
        String nearestRoad = geocodingService.findNearestRoad(lat, lng);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nearest_road", nearestRoad);
        result.put("nearby_cameras", nearby);
        return objectMapper.writeValueAsString(result);
    }

    /**
     * 构建工具定义列表（OpenAI 格式，Claude 可复用）
     */
    public static List<Map<String, Object>> buildToolDefinitions() {
        return List.of(
                Map.of("type", "function",
                        "function", Map.of(
                                "name", "query_traffic",
                                "description", "查询指定道路的实时交通数据，包括车流量、车速、拥堵指数等",
                                "parameters", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "road_name", Map.of("type", "string", "description", "道路名称")
                                        ),
                                        "required", List.of("road_name")
                                )
                        )),
                Map.of("type", "function",
                        "function", Map.of(
                                "name", "list_cameras",
                                "description", "查询监控摄像头列表，包含经纬度和道路名称。可按道路名称筛选",
                                "parameters", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "road_name", Map.of("type", "string", "description", "可选：按道路名称筛选")
                                        ),
                                        "required", List.of()
                                )
                        )),
                Map.of("type", "function",
                        "function", Map.of(
                                "name", "query_history",
                                "description", "查询指定道路的历史交通统计数据（最多 24 小时）",
                                "parameters", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "road_name", Map.of("type", "string", "description", "道路名称"),
                                                "hours", Map.of("type", "integer", "description", "查询的小时数（1-24，默认 1）")
                                        ),
                                        "required", List.of("road_name")
                                )
                        )),
                Map.of("type", "function",
                        "function", Map.of(
                                "name", "reverse_geocode",
                                "description", "根据经纬度查找最近的摄像头和道路。当用户提及位置或坐标时使用",
                                "parameters", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "latitude", Map.of("type", "number", "description", "纬度 (-90 ~ 90)"),
                                                "longitude", Map.of("type", "number", "description", "经度 (-180 ~ 180)")
                                        ),
                                        "required", List.of("latitude", "longitude")
                                )
                        ))
        );
    }
}