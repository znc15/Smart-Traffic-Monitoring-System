package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.traffic.PredictionResponse;
import com.smarttraffic.backend.dto.traffic.RoadNamesResponse;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.service.RoadService;
import com.smarttraffic.backend.service.TrafficService;
import com.smarttraffic.backend.service.analytics.RedisCacheService;
import com.smarttraffic.backend.service.analytics.TrafficPredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrafficControllerTest {

    @Test
    void roadNames_shouldReturnActiveRoadsFromRoadService() {
        TrafficService trafficService = mock(TrafficService.class);
        TrafficPredictionService predictionService = mock(TrafficPredictionService.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RoadService roadService = mock(RoadService.class);
        when(roadService.getActiveRoads()).thenReturn(List.of("主干道A", "次干道B"));

        TrafficController controller = new TrafficController(
                trafficService,
                predictionService,
                redisCacheService,
                roadService
        );

        RoadNamesResponse response = controller.roadNames();

        assertEquals(List.of("主干道A", "次干道B"), response.getRoadNames());
    }

    @Test
    void info_shouldRejectUnknownRoad() {
        TrafficService trafficService = mock(TrafficService.class);
        TrafficPredictionService predictionService = mock(TrafficPredictionService.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RoadService roadService = mock(RoadService.class);
        when(roadService.getActiveRoads()).thenReturn(List.of("主干道A"));

        TrafficController controller = new TrafficController(
                trafficService,
                predictionService,
                redisCacheService,
                roadService
        );

        AppException ex = assertThrows(AppException.class, () -> controller.info("未知道路"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void info_shouldUseTrafficServiceAndPopulateCacheOnMiss() {
        TrafficService trafficService = mock(TrafficService.class);
        TrafficPredictionService predictionService = mock(TrafficPredictionService.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RoadService roadService = mock(RoadService.class);
        when(roadService.getActiveRoads()).thenReturn(List.of("主干道A"));
        when(redisCacheService.get("traffic:info:主干道A", Map.class)).thenReturn(Optional.empty());
        Map<String, Object> snapshot = Map.of("count_car", 8, "density_status", "busy");
        when(trafficService.info("主干道A")).thenReturn(snapshot);

        TrafficController controller = new TrafficController(
                trafficService,
                predictionService,
                redisCacheService,
                roadService
        );

        Map<String, Object> response = controller.info("主干道A");

        assertSame(snapshot, response);
        verify(redisCacheService).put("traffic:info:主干道A", snapshot);
    }

    @Test
    void predictions_shouldReturnCachedValueWhenPresent() {
        TrafficService trafficService = mock(TrafficService.class);
        TrafficPredictionService predictionService = mock(TrafficPredictionService.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RoadService roadService = mock(RoadService.class);
        PredictionResponse cached = new PredictionResponse("主干道A", LocalDateTime.now(), "cached", List.of());
        when(redisCacheService.get("traffic:pred:主干道A:30", PredictionResponse.class)).thenReturn(Optional.of(cached));

        TrafficController controller = new TrafficController(
                trafficService,
                predictionService,
                redisCacheService,
                roadService
        );

        PredictionResponse response = controller.predictions("主干道A", 30);

        assertSame(cached, response);
        verify(predictionService, never()).generatePrediction(anyString(), anyInt());
        verify(redisCacheService).get("traffic:pred:主干道A:30", PredictionResponse.class);
    }

    @Test
    void predictions_shouldInvokePredictionServiceOnCacheMiss() {
        TrafficService trafficService = mock(TrafficService.class);
        TrafficPredictionService predictionService = mock(TrafficPredictionService.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RoadService roadService = mock(RoadService.class);
        when(redisCacheService.get("traffic:pred:主干道A:15", PredictionResponse.class)).thenReturn(Optional.empty());
        PredictionResponse generated = new PredictionResponse("主干道A", LocalDateTime.now(), "algo", List.of());
        when(predictionService.generatePrediction("主干道A", 15)).thenReturn(generated);

        TrafficController controller = new TrafficController(
                trafficService,
                predictionService,
                redisCacheService,
                roadService
        );

        PredictionResponse response = controller.predictions("主干道A", 15);

        assertSame(generated, response);
        verify(predictionService).generatePrediction("主干道A", 15);
        verify(redisCacheService).get("traffic:pred:主干道A:15", PredictionResponse.class);
    }
}
