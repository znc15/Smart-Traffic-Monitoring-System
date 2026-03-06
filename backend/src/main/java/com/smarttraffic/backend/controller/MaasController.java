package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.maas.MaasCongestionResponse;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.service.analytics.RedisCacheService;
import com.smarttraffic.backend.service.analytics.MaasService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/maas")
public class MaasController {

    private final MaasService maasService;
    private final RedisCacheService redisCacheService;

    public MaasController(MaasService maasService, RedisCacheService redisCacheService) {
        this.maasService = maasService;
        this.redisCacheService = redisCacheService;
    }

    @GetMapping("/congestion")
    public MaasCongestionResponse congestion(
            @RequestParam("min_lat")
            @DecimalMin(value = "-90", message = "纬度不能小于-90")
            @DecimalMax(value = "90", message = "纬度不能大于90")
            double minLat,

            @RequestParam("max_lat")
            @DecimalMin(value = "-90", message = "纬度不能小于-90")
            @DecimalMax(value = "90", message = "纬度不能大于90")
            double maxLat,

            @RequestParam("min_lng")
            @DecimalMin(value = "-180", message = "经度不能小于-180")
            @DecimalMax(value = "180", message = "经度不能大于180")
            double minLng,

            @RequestParam("max_lng")
            @DecimalMin(value = "-180", message = "经度不能小于-180")
            @DecimalMax(value = "180", message = "经度不能大于180")
            double maxLng
    ) {
        if (minLat > maxLat || minLng > maxLng) {
            throw new AppException(HttpStatus.BAD_REQUEST, "invalid bbox range");
        }
        String cacheKey = String.format("traffic:maas:%s:%s:%s:%s", minLat, maxLat, minLng, maxLng);
        return redisCacheService.get(cacheKey, MaasCongestionResponse.class)
                .orElseGet(() -> {
                    MaasCongestionResponse response = maasService.queryCongestion(minLat, maxLat, minLng, maxLng);
                    redisCacheService.put(cacheKey, response);
                    return response;
                });
    }
}
