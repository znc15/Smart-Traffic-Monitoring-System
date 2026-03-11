package com.smarttraffic.backend.service;

import com.smarttraffic.backend.repository.CameraRepository;
import com.smarttraffic.backend.service.analytics.RedisCacheService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoadServiceTest {

    @Test
    void getActiveRoads_shouldPreferRedisCacheWhenPresent() {
        CameraRepository cameraRepository = mock(CameraRepository.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        when(redisCacheService.get("roads:active", List.class)).thenReturn(Optional.of(List.of("陈兴道路", "陈富路")));

        RoadService service = new RoadService(cameraRepository, redisCacheService);

        List<String> roads = service.getActiveRoads();

        assertEquals(List.of("陈兴道路", "陈富路"), roads);
        verify(cameraRepository, never()).findDistinctRoadNameByEnabledTrue();
    }

    @Test
    void getActiveRoads_shouldQueryRepositoryAndPopulateCacheOnMiss() {
        CameraRepository cameraRepository = mock(CameraRepository.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        when(redisCacheService.get("roads:active", List.class)).thenReturn(Optional.empty());
        when(cameraRepository.findDistinctRoadNameByEnabledTrue()).thenReturn(List.of("黎利路", "阮廌路"));

        RoadService service = new RoadService(cameraRepository, redisCacheService);

        List<String> roads = service.getActiveRoads();

        assertEquals(List.of("黎利路", "阮廌路"), roads);
        verify(cameraRepository).findDistinctRoadNameByEnabledTrue();
        verify(redisCacheService).put("roads:active", List.of("黎利路", "阮廌路"), java.time.Duration.ofSeconds(30));
    }

    @Test
    void evictRoadCache_shouldDeleteActiveRoadKey() {
        CameraRepository cameraRepository = mock(CameraRepository.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RoadService service = new RoadService(cameraRepository, redisCacheService);

        service.evictRoadCache();

        verify(redisCacheService).evict("roads:active");
    }
}
