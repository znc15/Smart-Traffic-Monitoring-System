package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.TokenLlmEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenLlmRepository extends JpaRepository<TokenLlmEntity, Long> {
}
