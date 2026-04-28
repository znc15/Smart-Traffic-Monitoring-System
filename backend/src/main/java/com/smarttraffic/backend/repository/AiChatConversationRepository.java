package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.AiChatConversationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiChatConversationRepository extends JpaRepository<AiChatConversationEntity, Long> {
    List<AiChatConversationEntity> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Page<AiChatConversationEntity> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
}
