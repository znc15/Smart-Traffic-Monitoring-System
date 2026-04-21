package com.smarttraffic.backend.service.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.dto.edge.EdgeTelemetryRequest;
import com.smarttraffic.backend.model.TrafficEventEntity;
import com.smarttraffic.backend.model.TrafficSampleEntity;
import com.smarttraffic.backend.repository.TrafficEventRepository;
import com.smarttraffic.backend.repository.TrafficSampleRepository;
import com.smarttraffic.backend.service.TrafficService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelemetryIngestionServiceTest {

    @Test
    void ingest_shouldNormalizePayloadPersistEntitiesAndEvictCaches() {
        TrafficSampleRepository sampleRepository = mock(TrafficSampleRepository.class);
        TrafficEventRepository eventRepository = mock(TrafficEventRepository.class);
        TrafficService trafficService = mock(TrafficService.class);
        MirrorWriteService mirrorWriteService = mock(MirrorWriteService.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);

        when(sampleRepository.save(any(TrafficSampleEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(TrafficEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TelemetryIngestionService service = new TelemetryIngestionService(
                sampleRepository,
                eventRepository,
                trafficService,
                new ObjectMapper(),
                mirrorWriteService,
                redisCacheService,
                null // AlertService
        );

        LocalDateTime now = LocalDateTime.of(2026, 3, 11, 10, 30, 0);
        EdgeTelemetryRequest request = new EdgeTelemetryRequest();
        request.setNodeId(" edge-01 ");
        request.setRoadName(" 主干道A ");
        request.setTimestamp(now);
        request.setCountCar(-3);
        request.setCountMotor(20);
        request.setCountPerson(20);
        request.setAvgSpeedCar(-1.0);
        request.setAvgSpeedMotor(50.0);
        request.setDensityStatus("INVALID");
        request.setSpeedStatus("");

        EdgeTelemetryRequest.LaneStat laneStat = new EdgeTelemetryRequest.LaneStat();
        laneStat.setLaneId("L1");
        laneStat.setTurnType("straight");
        laneStat.setCount(8);
        request.setLaneStats(List.of(laneStat));

        EdgeTelemetryRequest.EventPayload event = new EdgeTelemetryRequest.EventPayload();
        event.setEventType("wrong_way_suspected");
        event.setLevel("high");
        event.setStartAt(now.minusMinutes(1));
        request.setEvents(List.of(event));

        service.ingest(request);

        ArgumentCaptor<TrafficSampleEntity> sampleCaptor = ArgumentCaptor.forClass(TrafficSampleEntity.class);
        verify(sampleRepository).save(sampleCaptor.capture());
        TrafficSampleEntity savedSample = sampleCaptor.getValue();
        assertEquals("edge-01", savedSample.getNodeId());
        assertEquals("主干道A", savedSample.getRoadName());
        assertEquals(now, savedSample.getSampleTime());
        assertEquals(0, savedSample.getCountCar());
        assertEquals(20, savedSample.getCountMotor());
        assertEquals(20, savedSample.getCountPerson());
        assertEquals(0.0, savedSample.getAvgSpeedCar());
        assertEquals(50.0, savedSample.getAvgSpeedMotor());
        assertEquals("congested", savedSample.getDensityStatus());
        assertEquals("slow", savedSample.getSpeedStatus());
        assertEquals("edge", savedSample.getSource());
        assertTrue(savedSample.getCongestionIndex() > 0.0);

        ArgumentCaptor<TrafficEventEntity> eventCaptor = ArgumentCaptor.forClass(TrafficEventEntity.class);
        verify(eventRepository).save(eventCaptor.capture());
        TrafficEventEntity savedEvent = eventCaptor.getValue();
        assertEquals("主干道A", savedEvent.getRoadName());
        assertEquals("wrong_way_suspected", savedEvent.getEventType());
        assertEquals("high", savedEvent.getLevel());

        ArgumentCaptor<Map<String, Object>> snapshotCaptor = ArgumentCaptor.forClass(Map.class);
        verify(trafficService).updateFromRemote(eq("主干道A"), snapshotCaptor.capture(), isNull());
        Map<String, Object> snapshot = snapshotCaptor.getValue();
        assertEquals(0, snapshot.get("count_car"));
        assertEquals(20, snapshot.get("count_motor"));
        assertEquals(20, snapshot.get("count_person"));
        assertEquals("congested", snapshot.get("density_status"));
        assertEquals("slow", snapshot.get("speed_status"));

        verify(mirrorWriteService).mirrorTrafficSample(any(TrafficSampleEntity.class));
        verify(mirrorWriteService).mirrorTrafficEvent(any(TrafficEventEntity.class));
        verify(redisCacheService).evict("traffic:roads");
        verify(redisCacheService).evict("traffic:info:主干道A");
        verify(redisCacheService).evictByPrefix("traffic:pred:主干道A:");
        verify(redisCacheService).evictByPrefix("traffic:maas:");
    }

    @Test
    void ingest_shouldSkipEventPersistenceWhenPayloadHasNoEvents() {
        TrafficSampleRepository sampleRepository = mock(TrafficSampleRepository.class);
        TrafficEventRepository eventRepository = mock(TrafficEventRepository.class);
        TrafficService trafficService = mock(TrafficService.class);
        MirrorWriteService mirrorWriteService = mock(MirrorWriteService.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);

        when(sampleRepository.save(any(TrafficSampleEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TelemetryIngestionService service = new TelemetryIngestionService(
                sampleRepository,
                eventRepository,
                trafficService,
                new ObjectMapper(),
                mirrorWriteService,
                redisCacheService,
                null // AlertService
        );

        EdgeTelemetryRequest request = new EdgeTelemetryRequest();
        request.setNodeId("edge-02");
        request.setRoadName("次干道B");
        request.setCountCar(3);

        service.ingest(request);

        verify(sampleRepository).save(any(TrafficSampleEntity.class));
        verify(eventRepository, never()).save(any(TrafficEventEntity.class));
        verify(mirrorWriteService, never()).mirrorTrafficEvent(any(TrafficEventEntity.class));
        verify(trafficService).updateFromRemote(eq("次干道B"), anyMap(), isNull());
    }
}
