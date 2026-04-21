package com.smarttraffic.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.model.TrafficEventEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import com.smarttraffic.backend.repository.TrafficEventRepository;
import com.smarttraffic.backend.service.analytics.MirrorWriteService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CameraPollerServiceTest {

    @Test
    void poll_shouldUpdateTrafficSnapshotAndMarkNodeOnline() {
        CameraRepository cameraRepository = mock(CameraRepository.class);
        TrafficService trafficService = mock(TrafficService.class);
        TrafficEventRepository trafficEventRepository = mock(TrafficEventRepository.class);
        MirrorWriteService mirrorWriteService = mock(MirrorWriteService.class);
        CameraEntity camera = enabledCamera("主干道A", "edge-01", "node-secret", "http://edge-node");
        when(cameraRepository.findByEnabledTrue()).thenReturn(List.of(camera));

        CameraPollerService service = newService(
                cameraRepository,
                trafficService,
                trafficEventRepository,
                mirrorWriteService,
                successRestClient()
        );

        service.poll();

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<byte[]> frameCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(trafficService).updateFromRemote(eq("主干道A"), dataCaptor.capture(), frameCaptor.capture());

        assertEquals(3, dataCaptor.getValue().get("count_car"));
        assertNotNull(dataCaptor.getValue().get("edge_metrics"));
        assertEquals("frame-bytes", new String(frameCaptor.getValue(), StandardCharsets.UTF_8));

        Map<String, Object> roadHealth = service.getNodeHealthMap().get("主干道A");
        assertTrue((Boolean) roadHealth.get("online"));
        assertEquals("online", roadHealth.get("health_status"));
        assertNull(roadHealth.get("status_reason_code"));
        assertEquals("edge-01", roadHealth.get("edge_node_id"));
        assertEquals("http://edge-node", roadHealth.get("node_url"));
        assertEquals(0, roadHealth.get("consecutive_failures"));
        verify(trafficEventRepository, never()).save(any(TrafficEventEntity.class));
    }

    @Test
    void poll_shouldClassifyAuthFailureAsOffline() {
        CameraRepository cameraRepository = mock(CameraRepository.class);
        TrafficService trafficService = mock(TrafficService.class);
        TrafficEventRepository trafficEventRepository = mock(TrafficEventRepository.class);
        MirrorWriteService mirrorWriteService = mock(MirrorWriteService.class);
        CameraEntity camera = enabledCamera("次干道B", "edge-02", "node-secret", "http://edge-node");
        when(cameraRepository.findByEnabledTrue()).thenReturn(List.of(camera));

        CameraPollerService service = newService(
                cameraRepository,
                trafficService,
                trafficEventRepository,
                mirrorWriteService,
                trafficFailureRestClient(
                        HttpClientErrorException.create(
                                HttpStatus.UNAUTHORIZED,
                                "Unauthorized",
                                HttpHeaders.EMPTY,
                                new byte[0],
                                StandardCharsets.UTF_8
                        )
                )
        );

        service.poll();

        Map<String, Object> offlineHealth = service.getNodeHealthMap().get("次干道B");
        assertFalse((Boolean) offlineHealth.get("online"));
        assertEquals("offline", offlineHealth.get("health_status"));
        assertEquals("auth_failed", offlineHealth.get("status_reason_code"));
        assertEquals("traffic", offlineHealth.get("last_error_stage"));
        assertEquals("节点鉴权失败，请检查 edge key / node id", offlineHealth.get("status_reason_message"));
        assertEquals(1, offlineHealth.get("error_count"));
        verify(trafficEventRepository, never()).save(any(TrafficEventEntity.class));
    }

    @Test
    void poll_shouldClassifyTimeoutAsOffline() {
        CameraRepository cameraRepository = mock(CameraRepository.class);
        TrafficService trafficService = mock(TrafficService.class);
        TrafficEventRepository trafficEventRepository = mock(TrafficEventRepository.class);
        MirrorWriteService mirrorWriteService = mock(MirrorWriteService.class);
        CameraEntity camera = enabledCamera("超时路段", "edge-03", "node-secret", "http://edge-node");
        when(cameraRepository.findByEnabledTrue()).thenReturn(List.of(camera));

        CameraPollerService service = newService(
                cameraRepository,
                trafficService,
                trafficEventRepository,
                mirrorWriteService,
                trafficFailureRestClient(new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out")))
        );

        service.poll();

        Map<String, Object> health = service.getNodeHealthMap().get("超时路段");
        assertEquals("offline", health.get("health_status"));
        assertEquals("timeout", health.get("status_reason_code"));
        assertEquals("节点请求超时", health.get("status_reason_message"));
        verify(trafficEventRepository, never()).save(any(TrafficEventEntity.class));
    }

    @Test
    void poll_shouldMarkFrameFailureAsDegradedAndKeepNodeOnline() {
        CameraRepository cameraRepository = mock(CameraRepository.class);
        TrafficService trafficService = mock(TrafficService.class);
        TrafficEventRepository trafficEventRepository = mock(TrafficEventRepository.class);
        MirrorWriteService mirrorWriteService = mock(MirrorWriteService.class);
        CameraEntity camera = enabledCamera("主干道C", "edge-04", "node-secret", "http://edge-node");
        when(cameraRepository.findByEnabledTrue()).thenReturn(List.of(camera));

        CameraPollerService service = newService(
                cameraRepository,
                trafficService,
                trafficEventRepository,
                mirrorWriteService,
                degradedRestClient()
        );

        service.poll();

        Map<String, Object> health = service.getNodeHealthMap().get("主干道C");
        assertTrue((Boolean) health.get("online"));
        assertEquals("degraded", health.get("health_status"));
        assertEquals("frame_fetch_failed", health.get("status_reason_code"));
        assertEquals("frame", health.get("last_error_stage"));
        assertEquals("视频帧拉取失败", health.get("status_reason_message"));
        assertNotNull(health.get("last_success_time"));
        verify(trafficService).updateFromRemote(eq("主干道C"), any(Map.class), eq(null));
        verify(trafficEventRepository, never()).save(any(TrafficEventEntity.class));
    }

    @Test
    void poll_shouldRecordHealthStatusTransitionsFromOfflineToDegradedToOnline() throws Exception {
        CameraRepository cameraRepository = mock(CameraRepository.class);
        TrafficService trafficService = mock(TrafficService.class);
        TrafficEventRepository trafficEventRepository = mock(TrafficEventRepository.class);
        MirrorWriteService mirrorWriteService = mock(MirrorWriteService.class);
        CameraEntity camera = enabledCamera("次干道D", "edge-05", "node-secret", "http://edge-node");
        when(cameraRepository.findByEnabledTrue()).thenReturn(List.of(camera));
        when(trafficEventRepository.save(any(TrafficEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CameraPollerService service = newService(
                cameraRepository,
                trafficService,
                trafficEventRepository,
                mirrorWriteService,
                offlineThenDegradedThenOnlineRestClient()
        );

        service.poll(); // initial offline - do not emit event yet
        Map<String, Object> offlineHealth = service.getNodeHealthMap().get("次干道D");
        assertEquals("offline", offlineHealth.get("health_status"));

        service.poll(); // offline -> degraded
        Map<String, Object> degradedHealth = service.getNodeHealthMap().get("次干道D");
        assertEquals("degraded", degradedHealth.get("health_status"));
        assertEquals("frame_fetch_failed", degradedHealth.get("status_reason_code"));

        service.poll(); // degraded -> online
        Map<String, Object> recoveredHealth = service.getNodeHealthMap().get("次干道D");
        assertEquals("online", recoveredHealth.get("health_status"));
        assertNull(recoveredHealth.get("status_reason_code"));
        assertNull(recoveredHealth.get("status_reason_message"));
        assertNull(recoveredHealth.get("last_error_stage"));
        assertNull(recoveredHealth.get("last_error"));
        assertEquals(0, recoveredHealth.get("consecutive_failures"));
        assertNotNull(recoveredHealth.get("last_success_time"));

        ArgumentCaptor<TrafficEventEntity> eventCaptor = ArgumentCaptor.forClass(TrafficEventEntity.class);
        verify(trafficEventRepository, times(2)).save(eventCaptor.capture());
        verify(mirrorWriteService, times(2)).mirrorTrafficEvent(any(TrafficEventEntity.class));

        List<TrafficEventEntity> savedEvents = eventCaptor.getAllValues();
        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> firstPayload = mapper.readValue(savedEvents.get(0).getPayloadJson(), Map.class);
        Map<?, ?> secondPayload = mapper.readValue(savedEvents.get(1).getPayloadJson(), Map.class);

        assertEquals("node_health_status_changed", savedEvents.get(0).getEventType());
        assertEquals("warning", savedEvents.get(0).getLevel());
        assertEquals("offline", firstPayload.get("from_health_status"));
        assertEquals("degraded", firstPayload.get("to_health_status"));
        assertEquals("frame_fetch_failed", firstPayload.get("reason_code"));

        assertEquals("node_health_status_changed", savedEvents.get(1).getEventType());
        assertEquals("info", savedEvents.get(1).getLevel());
        assertEquals("degraded", secondPayload.get("from_health_status"));
        assertEquals("online", secondPayload.get("to_health_status"));
        assertEquals(null, secondPayload.get("reason_code"));
    }

    private static CameraEntity enabledCamera(String roadName, String nodeId, String nodeApiKey, String nodeUrl) {
        CameraEntity camera = new CameraEntity();
        camera.setId(1L);
        camera.setName(roadName + "-cam");
        camera.setRoadName(roadName);
        camera.setEdgeNodeId(nodeId);
        camera.setNodeApiKey(nodeApiKey);
        camera.setNodeUrl(nodeUrl);
        camera.setEnabled(true);
        return camera;
    }

    private static CameraPollerService newService(
            CameraRepository cameraRepository,
            TrafficService trafficService,
            TrafficEventRepository trafficEventRepository,
            MirrorWriteService mirrorWriteService,
            RestClient restClient
    ) {
        CameraPollerService service = new CameraPollerService(
                cameraRepository,
                trafficService,
                trafficEventRepository,
                new ObjectMapper(),
                mirrorWriteService,
                null // AlertService
        );
        ReflectionTestUtils.setField(service, "restClient", restClient);
        return service;
    }

    private static RestClient successRestClient() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec trafficUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec trafficHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec trafficResponseSpec = mock(RestClient.ResponseSpec.class);
        RestClient.RequestHeadersUriSpec frameUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec frameHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec frameResponseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(trafficUriSpec, frameUriSpec);

        when(trafficUriSpec.uri(anyString())).thenReturn(trafficHeadersSpec);
        when(trafficHeadersSpec.headers(any())).thenAnswer(invocation -> trafficHeadersSpec);
        when(trafficHeadersSpec.retrieve()).thenReturn(trafficResponseSpec);
        when(trafficResponseSpec.body(Map.class)).thenReturn(successTrafficBody());

        when(frameUriSpec.uri(anyString())).thenReturn(frameHeadersSpec);
        when(frameHeadersSpec.headers(any())).thenAnswer(invocation -> frameHeadersSpec);
        when(frameHeadersSpec.retrieve()).thenReturn(frameResponseSpec);
        when(frameResponseSpec.body(byte[].class)).thenReturn("frame-bytes".getBytes(StandardCharsets.UTF_8));

        return restClient;
    }

    private static RestClient degradedRestClient() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec trafficUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec trafficHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec trafficResponseSpec = mock(RestClient.ResponseSpec.class);
        RestClient.RequestHeadersUriSpec frameUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec frameHeadersSpec = mock(RestClient.RequestHeadersSpec.class);

        when(restClient.get()).thenReturn(trafficUriSpec, frameUriSpec);
        when(trafficUriSpec.uri(anyString())).thenReturn(trafficHeadersSpec);
        when(trafficHeadersSpec.headers(any())).thenAnswer(invocation -> trafficHeadersSpec);
        when(trafficHeadersSpec.retrieve()).thenReturn(trafficResponseSpec);
        when(trafficResponseSpec.body(Map.class)).thenReturn(successTrafficBody());

        when(frameUriSpec.uri(anyString())).thenReturn(frameHeadersSpec);
        when(frameHeadersSpec.headers(any())).thenAnswer(invocation -> frameHeadersSpec);
        when(frameHeadersSpec.retrieve()).thenThrow(new RuntimeException("frame stream unavailable"));

        return restClient;
    }

    private static RestClient trafficFailureRestClient(Exception ex) {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec trafficUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec trafficHeadersSpec = mock(RestClient.RequestHeadersSpec.class);

        when(restClient.get()).thenReturn(trafficUriSpec);
        when(trafficUriSpec.uri(anyString())).thenReturn(trafficHeadersSpec);
        when(trafficHeadersSpec.headers(any())).thenAnswer(invocation -> trafficHeadersSpec);
        when(trafficHeadersSpec.retrieve()).thenThrow(ex);

        return restClient;
    }

    private static RestClient offlineThenDegradedThenOnlineRestClient() {
        RestClient restClient = mock(RestClient.class);

        RestClient.RequestHeadersUriSpec firstTrafficUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec firstTrafficHeadersSpec = mock(RestClient.RequestHeadersSpec.class);

        RestClient.RequestHeadersUriSpec secondTrafficUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec secondTrafficHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec secondTrafficResponseSpec = mock(RestClient.ResponseSpec.class);
        RestClient.RequestHeadersUriSpec secondFrameUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec secondFrameHeadersSpec = mock(RestClient.RequestHeadersSpec.class);

        RestClient.RequestHeadersUriSpec thirdTrafficUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec thirdTrafficHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec thirdTrafficResponseSpec = mock(RestClient.ResponseSpec.class);
        RestClient.RequestHeadersUriSpec thirdFrameUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec thirdFrameHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec thirdFrameResponseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(
                firstTrafficUriSpec,
                secondTrafficUriSpec,
                secondFrameUriSpec,
                thirdTrafficUriSpec,
                thirdFrameUriSpec
        );

        when(firstTrafficUriSpec.uri(anyString())).thenReturn(firstTrafficHeadersSpec);
        when(firstTrafficHeadersSpec.headers(any())).thenAnswer(invocation -> firstTrafficHeadersSpec);
        when(firstTrafficHeadersSpec.retrieve()).thenThrow(
                new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out"))
        );

        when(secondTrafficUriSpec.uri(anyString())).thenReturn(secondTrafficHeadersSpec);
        when(secondTrafficHeadersSpec.headers(any())).thenAnswer(invocation -> secondTrafficHeadersSpec);
        when(secondTrafficHeadersSpec.retrieve()).thenReturn(secondTrafficResponseSpec);
        when(secondTrafficResponseSpec.body(Map.class)).thenReturn(successTrafficBody());
        when(secondFrameUriSpec.uri(anyString())).thenReturn(secondFrameHeadersSpec);
        when(secondFrameHeadersSpec.headers(any())).thenAnswer(invocation -> secondFrameHeadersSpec);
        when(secondFrameHeadersSpec.retrieve()).thenThrow(new RuntimeException("frame stream unavailable"));

        when(thirdTrafficUriSpec.uri(anyString())).thenReturn(thirdTrafficHeadersSpec);
        when(thirdTrafficHeadersSpec.headers(any())).thenAnswer(invocation -> thirdTrafficHeadersSpec);
        when(thirdTrafficHeadersSpec.retrieve()).thenReturn(thirdTrafficResponseSpec);
        when(thirdTrafficResponseSpec.body(Map.class)).thenReturn(successTrafficBody());
        when(thirdFrameUriSpec.uri(anyString())).thenReturn(thirdFrameHeadersSpec);
        when(thirdFrameHeadersSpec.headers(any())).thenAnswer(invocation -> thirdFrameHeadersSpec);
        when(thirdFrameHeadersSpec.retrieve()).thenReturn(thirdFrameResponseSpec);
        when(thirdFrameResponseSpec.body(byte[].class)).thenReturn("frame-bytes".getBytes(StandardCharsets.UTF_8));

        return restClient;
    }

    private static Map<String, Object> successTrafficBody() {
        return Map.of(
                "count_car", 3,
                "count_motor", 1,
                "count_person", 0,
                "edge_metrics", Map.of("fps", 24.0, "inference_ms", 11.5, "cpu_percent", 35.0)
        );
    }
}
