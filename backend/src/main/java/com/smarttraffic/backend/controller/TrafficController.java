package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.traffic.RoadNamesResponse;
import com.smarttraffic.backend.dto.traffic.TrafficSampleResponse;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.TrafficSampleEntity;
import com.smarttraffic.backend.repository.TrafficSampleRepository;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.service.RoadService;
import com.smarttraffic.backend.service.TrafficService;
import com.smarttraffic.backend.service.analytics.RedisCacheService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class TrafficController {

    private final TrafficService trafficService;
    private final RedisCacheService redisCacheService;
    private final RoadService roadService;
    private final TrafficSampleRepository trafficSampleRepository;

    public TrafficController(
            TrafficService trafficService,
            RedisCacheService redisCacheService,
            RoadService roadService,
            TrafficSampleRepository trafficSampleRepository
    ) {
        this.trafficService = trafficService;
        this.redisCacheService = redisCacheService;
        this.roadService = roadService;
        this.trafficSampleRepository = trafficSampleRepository;
    }

    @GetMapping("/roads_name")
    public RoadNamesResponse roadNames() {
        return new RoadNamesResponse(roadService.getActiveRoads());
    }

    @GetMapping("/info/{roadName}")
    public Map<String, Object> info(@PathVariable String roadName) {
        if (!roadService.getActiveRoads().contains(roadName)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Road not found: " + roadName);
        }
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
        if (!roadService.getActiveRoads().contains(roadName)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Road not found: " + roadName);
        }
        return ResponseEntity.ok(trafficService.frame(roadName));
    }

    @GetMapping(value = "/frames_no_auth/{roadName}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> frameNoAuth(@PathVariable String roadName) {
        if (!roadService.getActiveRoads().contains(roadName)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Road not found: " + roadName);
        }
        return ResponseEntity.ok(trafficService.frame(roadName));
    }

    @GetMapping("/traffic/samples/recent")
    public List<TrafficSampleResponse> recentSamples(
            @RequestParam(required = false) String road_name,
            @RequestParam(defaultValue = "120") int limit
    ) {
        SecurityUtils.requireCurrentUser();
        List<TrafficSampleEntity> samples = trafficSampleRepository.findRecentSamples(
                road_name, PageRequest.of(0, limit));
        // 结果按 sampleTime DESC 返回，前端需要按时间升序展示，因此反转
        List<TrafficSampleResponse> mapped = samples.stream().map(s -> new TrafficSampleResponse(
                s.getRoadName(),
                s.getSampleTime(),
                s.getCountCar(),
                s.getCountMotor(),
                s.getCountPerson(),
                s.getAvgSpeedCar() != null ? s.getAvgSpeedCar() : 0.0,
                s.getAvgSpeedMotor() != null ? s.getAvgSpeedMotor() : 0.0,
                s.getCongestionIndex() != null ? s.getCongestionIndex() : 0.0,
                s.getDensityStatus(),
                s.getSpeedStatus()
        )).collect(java.util.stream.Collectors.toList());
        java.util.Collections.reverse(mapped);
        return mapped;
    }

}
