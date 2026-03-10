package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.service.MapOverviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/map")
public class MapController {

    private final MapOverviewService mapOverviewService;

    public MapController(MapOverviewService mapOverviewService) {
        this.mapOverviewService = mapOverviewService;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return mapOverviewService.overview();
    }
}
