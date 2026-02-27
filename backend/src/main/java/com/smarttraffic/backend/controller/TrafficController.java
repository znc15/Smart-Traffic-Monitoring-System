package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.traffic.RoadNamesResponse;
import com.smarttraffic.backend.dto.traffic.PredictionResponse;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.service.TrafficService;
import com.smarttraffic.backend.service.analytics.RedisCacheService;
import com.smarttraffic.backend.service.analytics.TrafficPredictionService;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class TrafficController {

    private final TrafficService trafficService;
    private final TrafficPredictionService trafficPredictionService;
    private final RedisCacheService redisCacheService;

    public TrafficController(
            TrafficService trafficService,
            TrafficPredictionService trafficPredictionService,
            RedisCacheService redisCacheService
    ) {
        this.trafficService = trafficService;
        this.trafficPredictionService = trafficPredictionService;
        this.redisCacheService = redisCacheService;
    }

    @GetMapping("/roads_name")
    public RoadNamesResponse roadNames() {
        return redisCacheService.get("traffic:roads", RoadNamesResponse.class)
                .orElseGet(() -> {
                    RoadNamesResponse response = new RoadNamesResponse(trafficService.roadNames());
                    redisCacheService.put("traffic:roads", response);
                    return response;
                });
    }

    @GetMapping("/info/{roadName}")
    public Map<String, Object> info(@PathVariable String roadName) {
        String cacheKey = "traffic:info:" + roadName;
        return redisCacheService.get(cacheKey, Map.class)
                .orElseGet(() -> {
                    Map<String, Object> response = trafficService.info(roadName);
                    redisCacheService.put(cacheKey, response);
                    return response;
                });
    }

    @GetMapping(value = "/frames/{roadName}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> frameAuth(@PathVariable String roadName) {
        SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(trafficService.frame(roadName));
    }

    @GetMapping(value = "/frames_no_auth/{roadName}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> frameNoAuth(@PathVariable String roadName) {
        return ResponseEntity.ok(trafficService.frame(roadName));
    }

    @GetMapping("/traffic/predictions")
    public PredictionResponse predictions(
            @RequestParam("road_name") String roadName,
            @RequestParam(value = "horizon_minutes", defaultValue = "60") Integer horizonMinutes
    ) {
        if (roadName == null || roadName.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "road_name is required");
        }
        int horizon = horizonMinutes == null ? 60 : horizonMinutes;
        String cacheKey = "traffic:pred:" + roadName + ":" + horizon;
        return redisCacheService.get(cacheKey, PredictionResponse.class)
                .orElseGet(() -> trafficPredictionService.generatePrediction(roadName, horizon));
    }
}
