package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.chat.ChatRequest;
import com.smarttraffic.backend.dto.chat.ChatResponse;
import com.smarttraffic.backend.security.CurrentUser;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        return chatbotService.reply(request.getMessage(), user);
    }

    @PostMapping("/chat_no_auth")
    public ChatResponse chatNoAuth(@Valid @RequestBody ChatRequest request) {
        return chatbotService.reply(request.getMessage(), null);
    }
}
