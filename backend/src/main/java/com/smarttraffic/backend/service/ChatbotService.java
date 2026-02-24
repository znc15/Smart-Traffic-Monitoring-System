package com.smarttraffic.backend.service;

import com.smarttraffic.backend.config.NetworkProperties;
import com.smarttraffic.backend.dto.chat.ChatResponse;
import com.smarttraffic.backend.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotService {

    private final TrafficService trafficService;
    private final NetworkProperties networkProperties;

    public ChatbotService(TrafficService trafficService, NetworkProperties networkProperties) {
        this.trafficService = trafficService;
        this.networkProperties = networkProperties;
    }

    public ChatResponse reply(String message, CurrentUser currentUser) {
        List<String> roads = trafficService.roadNames();
        if (roads.isEmpty()) {
            return new ChatResponse("Hiện chưa có dữ liệu đường để phân tích.", Collections.emptyList());
        }

        String road = roads.get(0);
        Map<String, Object> info = trafficService.info(road);
        String userPrefix = currentUser == null ? "Khách" : currentUser.username();

        String text = String.format(
                "%s: Đã nhận câu hỏi \"%s\". %s hiện có ô tô: %s, xe máy: %s, tốc độ ô tô: %s km/h, tốc độ xe máy: %s km/h.",
                userPrefix,
                message,
                road,
                info.get("count_car"),
                info.get("count_motor"),
                info.get("speed_car"),
                info.get("speed_motor")
        );

        String encodedRoad = UriUtils.encodePathSegment(road, StandardCharsets.UTF_8);
        String imageUrl = networkProperties.getBaseUrlApi() + "/api/v1/frames_no_auth/" + encodedRoad;
        return new ChatResponse(text, List.of(imageUrl));
    }
}
