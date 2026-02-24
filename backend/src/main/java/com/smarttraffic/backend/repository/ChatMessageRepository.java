package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    long countByUser_Id(Long userId);

    void deleteByUser_Id(Long userId);

    Optional<ChatMessageEntity> findByIdAndUser_Id(Long id, Long userId);

    List<ChatMessageEntity> findByUser_IdOrderByCreatedAtAsc(Long userId);

    List<ChatMessageEntity> findByUser_IdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(Long userId, Instant since);
}
