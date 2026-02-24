package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.chat.ChatMessageCreateRequest;
import com.smarttraffic.backend.dto.chat.ChatMessageListItem;
import com.smarttraffic.backend.dto.chat.ChatMessageResponse;
import com.smarttraffic.backend.dto.common.CountResponse;
import com.smarttraffic.backend.security.CurrentUser;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.service.ChatHistoryService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    public ChatHistoryController(ChatHistoryService chatHistoryService) {
        this.chatHistoryService = chatHistoryService;
    }

    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponse> saveMessage(@Valid @RequestBody ChatMessageCreateRequest request) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        ChatMessageResponse response = chatHistoryService.saveMessage(user.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/messages")
    public List<ChatMessageListItem> getMessages(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since
    ) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        int boundedLimit = Math.max(1, Math.min(1000, limit));
        int boundedOffset = Math.max(0, offset);
        return chatHistoryService.getMessages(user.id(), boundedLimit, boundedOffset, since);
    }

    @DeleteMapping("/messages")
    public ResponseEntity<Void> clearMessages() {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        chatHistoryService.clearMessages(user.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        chatHistoryService.deleteMessage(user.id(), messageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/messages/count")
    public CountResponse countMessages() {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        return new CountResponse(chatHistoryService.countMessages(user.id()));
    }
}
