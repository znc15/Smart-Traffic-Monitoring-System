package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.AiChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessageEntity, Long> {
    List<AiChatMessageEntity> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    void deleteByConversationId(Long conversationId);
}
