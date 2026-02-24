package com.smarttraffic.backend.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.dto.chat.ChatResponse;
import com.smarttraffic.backend.security.CurrentUser;
import com.smarttraffic.backend.service.ChatbotService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ChatbotService chatbotService;

    public ChatWebSocketHandler(ObjectMapper objectMapper, ChatbotService chatbotService) {
        this.objectMapper = objectMapper;
        this.chatbotService = chatbotService;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        CurrentUser user = (CurrentUser) session.getAttributes().get(WebSocketAttributes.CURRENT_USER);
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String userMessage = node.path("message").asText(null);

            if (!StringUtils.hasText(userMessage)) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("message", "Bạn chưa nhập tin nhắn.");
                payload.put("image", null);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                        payload
                )));
                return;
            }

            ChatResponse response = chatbotService.reply(userMessage, user);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } catch (Exception ex) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("message", "Lỗi: " + ex.getMessage());
            payload.put("image", null);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    payload
            )));
        }
    }
}
