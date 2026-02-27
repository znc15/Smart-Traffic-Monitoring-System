package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.maas.MaasCongestionResponse;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.service.analytics.MaasService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/maas")
public class MaasController {

    private final MaasService maasService;

    public MaasController(MaasService maasService) {
        this.maasService = maasService;
    }

    @GetMapping("/congestion")
    public MaasCongestionResponse congestion(
            @RequestParam("min_lat") double minLat,
            @RequestParam("max_lat") double maxLat,
            @RequestParam("min_lng") double minLng,
            @RequestParam("max_lng") double maxLng
    ) {
        if (minLat > maxLat || minLng > maxLng) {
            throw new AppException(HttpStatus.BAD_REQUEST, "invalid bbox range");
        }
        return maasService.queryCongestion(minLat, maxLat, minLng, maxLng);
    }
}
