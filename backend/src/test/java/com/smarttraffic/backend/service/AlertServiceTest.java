package com.smarttraffic.backend.service;

import com.smarttraffic.backend.model.AlertEntity;
import com.smarttraffic.backend.repository.AlertRepository;
import com.smarttraffic.backend.repository.SiteSettingsRepository;
import com.smarttraffic.backend.websocket.AlertWebSocketHandler;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertServiceTest {

    @Test
    void createAlert_shouldBroadcastWhenNewAlertCreated() {
        AlertRepository alertRepository = mock(AlertRepository.class);
        SiteSettingsRepository siteSettingsRepository = mock(SiteSettingsRepository.class);
        AlertWebSocketHandler alertWebSocketHandler = mock(AlertWebSocketHandler.class);
        AlertService alertService = new AlertService(alertRepository, siteSettingsRepository, alertWebSocketHandler);

        when(alertRepository.findTopByTypeAndRoadNameAndStatusNotOrderByCreatedAtDesc(
                eq("CONGESTION"),
                eq("Road A"),
                eq("DISPOSED")
        )).thenReturn(Optional.empty());

        AlertEntity saved = new AlertEntity();
        saved.setId(10L);
        saved.setType("CONGESTION");
        saved.setLevel("WARNING");
        saved.setRoadName("Road A");
        saved.setNodeId("node-1");
        saved.setMessage("msg");
        when(alertRepository.save(any(AlertEntity.class))).thenReturn(saved);

        AlertEntity created = alertService.createAlert("CONGESTION", "WARNING", "Road A", "node-1", "msg");

        assertSame(saved, created);
        verify(alertRepository).save(any(AlertEntity.class));
        verify(alertWebSocketHandler).broadcast(saved);
    }

    @Test
    void createAlert_shouldNotBroadcastWhenExistingAlertPresent() {
        AlertRepository alertRepository = mock(AlertRepository.class);
        SiteSettingsRepository siteSettingsRepository = mock(SiteSettingsRepository.class);
        AlertWebSocketHandler alertWebSocketHandler = mock(AlertWebSocketHandler.class);
        AlertService alertService = new AlertService(alertRepository, siteSettingsRepository, alertWebSocketHandler);

        AlertEntity existing = new AlertEntity();
        existing.setId(99L);
        when(alertRepository.findTopByTypeAndRoadNameAndStatusNotOrderByCreatedAtDesc(
                eq("CONGESTION"),
                eq("Road A"),
                eq("DISPOSED")
        )).thenReturn(Optional.of(existing));

        AlertEntity result = alertService.createAlert("CONGESTION", "WARNING", "Road A", "node-1", "msg");

        assertSame(existing, result);
        verify(alertRepository, never()).save(any(AlertEntity.class));
        verify(alertWebSocketHandler, never()).broadcast(any(AlertEntity.class));
    }
}

