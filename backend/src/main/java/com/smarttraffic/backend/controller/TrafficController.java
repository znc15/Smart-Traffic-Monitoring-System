package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.traffic.RoadNamesResponse;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.service.TrafficService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class TrafficController {

    private final TrafficService trafficService;

    public TrafficController(TrafficService trafficService) {
        this.trafficService = trafficService;
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
}
