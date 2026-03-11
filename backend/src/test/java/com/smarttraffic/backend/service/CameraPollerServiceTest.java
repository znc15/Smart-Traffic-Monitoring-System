package com.smarttraffic.backend.service;

import com.smarttraffic.backend.model.CameraEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CameraPollerServiceTest {

    @Test
    void poll_shouldUpdateTrafficSnapshotAndMarkNodeOnline() {
        CameraRepository cameraRepository = mock(CameraRepository.class);
        TrafficService trafficService = mock(TrafficService.class);
        CameraEntity camera = enabledCamera("主干道A", "edge-01", "node-secret", "http://edge-node");
        when(cameraRepository.findByEnabledTrue()).thenReturn(List.of(camera));

        CameraPollerService service = new CameraPollerService(cameraRepository, trafficService);
        ReflectionTestUtils.setField(service, "restClient", successRestClient());

        service.poll();

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<byte[]> frameCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(trafficService).updateFromRemote(eq("主干道A"), dataCaptor.capture(), frameCaptor.capture());

        assertEquals(3, dataCaptor.getValue().get("count_car"));
        assertNotNull(dataCaptor.getValue().get("edge_metrics"));
        assertEquals("frame-bytes", new String(frameCaptor.getValue(), StandardCharsets.UTF_8));

        Map<String, Map<String, Object>> healthMap = service.getNodeHealthMap();
        Map<String, Object> roadHealth = healthMap.get("主干道A");
        assertTrue((Boolean) roadHealth.get("online"));
        assertEquals("online", roadHealth.get("health_status"));
        assertEquals(null, roadHealth.get("status_reason_code"));
        assertEquals("edge-01", roadHealth.get("edge_node_id"));
        assertEquals("http://edge-node", roadHealth.get("node_url"));
        assertEquals(0, roadHealth.get("consecutive_failures"));
    }

    @Test
    void poll_shouldClassifyAuthFailureAsOffline() {
        CameraRepository cameraRepository = mock(CameraRepository.class);
        TrafficService trafficService = mock(TrafficService.class);
        CameraEntity camera = enabledCamera("次干道B", "edge-02", "node-secret", "http://edge-node");
        when(cameraRepository.findByEnabledTrue()).thenReturn(List.of(camera));

        CameraPollerService service = new CameraPollerService(cameraRepository, trafficService);
        ReflectionTestUtils.setField(service, "restClient", trafficFailureRestClient(
                HttpClientErrorException.create(
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                )
        ));

        service.poll();

        Map<String, Object> offlineHealth = service.getNodeHealthMap().get("次干道B");
        assertFalse((Boolean) offlineHealth.get("online"));
        assertEquals("offline", offlineHealth.get("health_status"));
        assertEquals("auth_failed", offlineHealth.get("status_reason_code"));
        assertEquals("traffic", offlineHealth.get("last_error_stage"));
        assertEquals("节点鉴权失败，请检查 edge key / node id", offlineHealth.get("status_reason_message"));
        assertEquals(1, offlineHealth.get("error_count"));
    }

    @Test
    void poll_shouldClassifyTimeoutAsOffline() {
        CameraRepository cameraRepository = mock(CameraRepository.class);
        TrafficService trafficService = mock(TrafficService.class);
        CameraEntity camera = enabledCamera("超时路段", "edge-03", "node-secret", "http://edge-node");
        when(cameraRepository.findByEnabledTrue()).thenReturn(List.of(camera));

        CameraPollerService service = new CameraPollerService(cameraRepository, trafficService);
        ReflectionTestUtils.setField(
                service,
                "restClient",
                trafficFailureRestClient(new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out")))
        );

        service.poll();

        Map<String, Object> health = service.getNodeHealthMap().get("超时路段");
        assertEquals("offline", health.get("health_status"));
        assertEquals("timeout", health.get("status_reason_code"));
        assertEquals("节点请求超时", health.get("status_reason_message"));
    }

    @Test
    void poll_shouldMarkFrameFailureAsDegradedAndKeepNodeOnline() {
        CameraRepository cameraRepository = mock(CameraRepository.class);
        TrafficService trafficService = mock(TrafficService.class);
        CameraEntity camera = enabledCamera("主干道C", "edge-04", "node-secret", "http://edge-node");
        when(cameraRepository.findByEnabledTrue()).thenReturn(List.of(camera));

        CameraPollerService service = new CameraPollerService(cameraRepository, trafficService);
        ReflectionTestUtils.setField(service, "restClient", degradedRestClient());

        service.poll();

        Map<String, Object> health = service.getNodeHealthMap().get("主干道C");
        assertTrue((Boolean) health.get("online"));
        assertEquals("degraded", health.get("health_status"));
        assertEquals("frame_fetch_failed", health.get("status_reason_code"));
        assertEquals("frame", health.get("last_error_stage"));
        assertEquals("视频帧拉取失败", health.get("status_reason_message"));
        assertNotNull(health.get("last_success_time"));
        verify(trafficService).updateFromRemote(eq("主干道C"), any(Map.class), eq(null));
    }

    @Test
    void poll_shouldClearReasonFieldsWhenNodeRecovers() {
        CameraRepository cameraRepository = mock(CameraRepository.class);
        TrafficService trafficService = mock(TrafficService.class);
        CameraEntity camera = enabledCamera("次干道D", "edge-05", "node-secret", "http://edge-node");
        when(cameraRepository.findByEnabledTrue()).thenReturn(List.of(camera));

        CameraPollerService service = new CameraPollerService(cameraRepository, trafficService);
        ReflectionTestUtils.setField(service, "restClient", failingThenSuccessRestClient());

        service.poll();
        Map<String, Object> degradedHealth = service.getNodeHealthMap().get("次干道D");
        assertEquals("degraded", degradedHealth.get("health_status"));
        assertEquals("frame_fetch_failed", degradedHealth.get("status_reason_code"));

        service.poll();

        Map<String, Object> recoveredHealth = service.getNodeHealthMap().get("次干道D");
        assertEquals("online", recoveredHealth.get("health_status"));
        assertEquals(null, recoveredHealth.get("status_reason_code"));
        assertEquals(null, recoveredHealth.get("status_reason_message"));
        assertEquals(null, recoveredHealth.get("last_error_stage"));
        assertEquals(null, recoveredHealth.get("last_error"));
        assertEquals(0, recoveredHealth.get("consecutive_failures"));
        assertNotNull(recoveredHealth.get("last_success_time"));
        verify(trafficService, times(2)).updateFromRemote(eq("次干道D"), any(Map.class), any());
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
        when(trafficHeadersSpec.headers(any())).thenAnswer(invocation -> {
            invocation.<java.util.function.Consumer<HttpHeaders>>getArgument(0).accept(new HttpHeaders());
            return trafficHeadersSpec;
        });
        when(trafficHeadersSpec.retrieve()).thenReturn(trafficResponseSpec);
        when(trafficResponseSpec.body(Map.class)).thenReturn(Map.of(
                "count_car", 3,
                "count_motor", 1,
                "count_person", 0,
                "edge_metrics", Map.of("fps", 24.0, "inference_ms", 11.5, "cpu_percent", 35.0)
        ));

        when(frameUriSpec.uri(anyString())).thenReturn(frameHeadersSpec);
        when(frameHeadersSpec.headers(any())).thenAnswer(invocation -> {
            invocation.<java.util.function.Consumer<HttpHeaders>>getArgument(0).accept(new HttpHeaders());
            return frameHeadersSpec;
        });
        when(frameHeadersSpec.retrieve()).thenReturn(frameResponseSpec);
        when(frameResponseSpec.body(byte[].class)).thenReturn("frame-bytes".getBytes(StandardCharsets.UTF_8));

        return restClient;
    }

    private static RestClient failingThenSuccessRestClient() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec firstTrafficUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec firstTrafficHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec firstTrafficResponseSpec = mock(RestClient.ResponseSpec.class);
        RestClient.RequestHeadersUriSpec firstFrameUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec firstFrameHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.RequestHeadersUriSpec secondTrafficUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec secondTrafficHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec secondTrafficResponseSpec = mock(RestClient.ResponseSpec.class);
        RestClient.RequestHeadersUriSpec secondFrameUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec secondFrameHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec secondFrameResponseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(
                firstTrafficUriSpec,
                firstFrameUriSpec,
                secondTrafficUriSpec,
                secondFrameUriSpec
        );
        when(firstTrafficUriSpec.uri(anyString())).thenReturn(firstTrafficHeadersSpec);
        when(firstTrafficHeadersSpec.headers(any())).thenAnswer(invocation -> firstTrafficHeadersSpec);
        when(firstTrafficHeadersSpec.retrieve()).thenReturn(firstTrafficResponseSpec);
        when(firstTrafficResponseSpec.body(Map.class)).thenReturn(Map.of(
                "count_car", 3,
                "count_motor", 1,
                "count_person", 0,
                "edge_metrics", Map.of("fps", 24.0, "inference_ms", 11.5, "cpu_percent", 35.0)
        ));
        when(firstFrameUriSpec.uri(anyString())).thenReturn(firstFrameHeadersSpec);
        when(firstFrameHeadersSpec.headers(any())).thenAnswer(invocation -> firstFrameHeadersSpec);
        when(firstFrameHeadersSpec.retrieve()).thenThrow(new RuntimeException("frame stream unavailable"));

        when(secondTrafficUriSpec.uri(anyString())).thenReturn(secondTrafficHeadersSpec);
        when(secondTrafficHeadersSpec.headers(any())).thenAnswer(invocation -> secondTrafficHeadersSpec);
        when(secondTrafficHeadersSpec.retrieve()).thenReturn(secondTrafficResponseSpec);
        when(secondTrafficResponseSpec.body(Map.class)).thenReturn(Map.of(
                "count_car", 3,
                "count_motor", 1,
                "count_person", 0,
                "edge_metrics", Map.of("fps", 24.0, "inference_ms", 11.5, "cpu_percent", 35.0)
        ));
        when(secondFrameUriSpec.uri(anyString())).thenReturn(secondFrameHeadersSpec);
        when(secondFrameHeadersSpec.headers(any())).thenAnswer(invocation -> secondFrameHeadersSpec);
        when(secondFrameHeadersSpec.retrieve()).thenReturn(secondFrameResponseSpec);
        when(secondFrameResponseSpec.body(byte[].class)).thenReturn("frame-bytes".getBytes(StandardCharsets.UTF_8));

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
        when(trafficResponseSpec.body(Map.class)).thenReturn(Map.of(
                "count_car", 3,
                "count_motor", 1,
                "count_person", 0,
                "edge_metrics", Map.of("fps", 24.0, "inference_ms", 11.5, "cpu_percent", 35.0)
        ));
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
}
