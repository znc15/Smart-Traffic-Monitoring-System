package com.smarttraffic.backend.service;

import com.smarttraffic.backend.dto.chat.ChatMessageCreateRequest;
import com.smarttraffic.backend.dto.chat.ChatMessageListItem;
import com.smarttraffic.backend.dto.chat.ChatMessageResponse;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.ChatMessageEntity;
import com.smarttraffic.backend.model.UserEntity;
import com.smarttraffic.backend.repository.ChatMessageRepository;
import com.smarttraffic.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ChatHistoryService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatHistoryService(ChatMessageRepository chatMessageRepository, UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChatMessageResponse saveMessage(Long userId, ChatMessageCreateRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setUser(user);
        entity.setMessage(request.getMessage());
        entity.setIsUser(Boolean.TRUE.equals(request.getIsUser()));
        entity.setImages(request.getImages());
        entity.setExtraData(request.getExtraData());
        entity.setCreatedAt(Instant.now());

        ChatMessageEntity saved = chatMessageRepository.save(entity);
        return toMessageResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageListItem> getMessages(Long userId, int limit, int offset, Instant since) {
        List<ChatMessageEntity> all = since == null
                ? chatMessageRepository.findByUser_IdOrderByCreatedAtAsc(userId)
                : chatMessageRepository.findByUser_IdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(userId, since);

        return all.stream()
                .skip(offset)
                .limit(limit)
                .map(this::toListItem)
                .toList();
    }

    @Transactional
    public void clearMessages(Long userId) {
        chatMessageRepository.deleteByUser_Id(userId);
    }

    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        ChatMessageEntity entity = chatMessageRepository.findByIdAndUser_Id(messageId, userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Message not found"));
        chatMessageRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public long countMessages(Long userId) {
        return chatMessageRepository.countByUser_Id(userId);
    }

    private ChatMessageResponse toMessageResponse(ChatMessageEntity entity) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(entity.getId());
        response.setUserId(entity.getUser().getId());
        response.setMessage(entity.getMessage());
        response.setIsUser(entity.getIsUser());
        response.setImages(entity.getImages());
        response.setExtraData(entity.getExtraData());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    private ChatMessageListItem toListItem(ChatMessageEntity entity) {
        ChatMessageListItem item = new ChatMessageListItem();
        item.setId(String.valueOf(entity.getId()));
        item.setText(entity.getMessage());
        item.setUser(entity.getIsUser());
        item.setTime(TIME_FORMATTER.format(entity.getCreatedAt()));
        item.setImage(entity.getImages());
        item.setCreatedAt(entity.getCreatedAt().toString());
        return item;
    }
}
