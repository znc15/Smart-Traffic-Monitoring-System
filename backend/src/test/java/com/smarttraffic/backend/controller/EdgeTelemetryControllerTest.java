package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.common.MessageResponse;
import com.smarttraffic.backend.dto.edge.EdgeTelemetryRequest;
import com.smarttraffic.backend.service.analytics.TelemetryIngestionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EdgeTelemetryControllerTest {

    @Test
    void ingest_shouldDelegateToServiceAndReturnAcceptedMessage() {
        TelemetryIngestionService telemetryIngestionService = mock(TelemetryIngestionService.class);
        EdgeTelemetryController controller = new EdgeTelemetryController(telemetryIngestionService);
        EdgeTelemetryRequest request = new EdgeTelemetryRequest();
        request.setNodeId("edge-01");
        request.setRoadName("主干道A");

        MessageResponse response = controller.ingest(request);

        verify(telemetryIngestionService).ingest(request);
        assertEquals("telemetry accepted", response.getMessage());
    }
}
