package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.traffic.RoadNamesResponse;
import com.smarttraffic.backend.dto.traffic.PredictionResponse;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.service.TrafficService;
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

    public TrafficController(TrafficService trafficService, TrafficPredictionService trafficPredictionService) {
        this.trafficService = trafficService;
        this.trafficPredictionService = trafficPredictionService;
    }

    @GetMapping("/roads_name")
    public RoadNamesResponse roadNames() {
        return new RoadNamesResponse(trafficService.roadNames());
    }

    @GetMapping("/info/{roadName}")
    public Map<String, Object> info(@PathVariable String roadName) {
        return trafficService.info(roadName);
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
        return trafficPredictionService.generatePrediction(roadName, horizonMinutes == null ? 60 : horizonMinutes);
    }
}
