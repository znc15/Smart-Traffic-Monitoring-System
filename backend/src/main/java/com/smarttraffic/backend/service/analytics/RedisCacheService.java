package com.smarttraffic.backend.service.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.config.DbRuntimeProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RedisCacheService {

    private static final String PREFIX_AI_CONVERSATIONS = "ai:conversations:";
    private static final String PREFIX_AI_MESSAGES = "ai:messages:";
    private static final String KEY_SITE_SETTINGS = "config:site_settings";
    private static final String PREFIX_USER_INFO = "user:info:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DbRuntimeProperties dbRuntimeProperties;

    public RedisCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            DbRuntimeProperties dbRuntimeProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.dbRuntimeProperties = dbRuntimeProperties;
    }

    // ─── Generic Cache Operations ─────────────────────────────────

    public <T> Optional<T> get(String key, Class<T> clazz) {
        if (!dbRuntimeProperties.isRedisCacheEnabled()) {
            return Optional.empty();
        }
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, clazz));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public <T> Optional<T> get(String key, TypeReference<T> typeRef) {
        if (!dbRuntimeProperties.isRedisCacheEnabled()) {
            return Optional.empty();
        }
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, typeRef));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public void put(String key, Object value) {
        put(key, value, Duration.ofSeconds(Math.max(1, dbRuntimeProperties.getCacheTtlSeconds())));
    }

    public void put(String key, Object value, Duration ttl) {
        if (!dbRuntimeProperties.isRedisCacheEnabled()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, payload, ttl);
        } catch (JsonProcessingException ignored) {
        } catch (Exception ignored) {
        }
    }

    public void evict(String key) {
        if (!dbRuntimeProperties.isRedisCacheEnabled()) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
        }
    }

    public void evictByPrefix(String prefix) {
        if (!dbRuntimeProperties.isRedisCacheEnabled()) {
            return;
        }
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }
            redisTemplate.delete(keys);
        } catch (Exception ignored) {
        }
    }

    public Set<String> keys(String prefix) {
        if (!dbRuntimeProperties.isRedisCacheEnabled()) {
            return Collections.emptySet();
        }
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            return keys == null ? Collections.emptySet() : keys;
        } catch (Exception ex) {
            return Collections.emptySet();
        }
    }

    // ─── AI Conversations Cache ───────────────────────────────────

    public <T> Optional<T> getAiConversations(Long userId, Class<T> clazz) {
        return get(PREFIX_AI_CONVERSATIONS + userId, clazz);
    }

    public void putAiConversations(Long userId, Object value) {
        put(PREFIX_AI_CONVERSATIONS + userId, value, Duration.ofSeconds(60));
    }

    public void evictAiConversations(Long userId) {
        evict(PREFIX_AI_CONVERSATIONS + userId);
    }

    // ─── AI Messages Cache ────────────────────────────────────────

    public <T> Optional<T> getAiMessages(Long conversationId, Class<T> clazz) {
        return get(PREFIX_AI_MESSAGES + conversationId, clazz);
    }

    public void putAiMessages(Long conversationId, Object value) {
        put(PREFIX_AI_MESSAGES + conversationId, value, Duration.ofSeconds(120));
    }

    public void evictAiMessages(Long conversationId) {
        evict(PREFIX_AI_MESSAGES + conversationId);
    }

    // ─── Site Settings Cache ──────────────────────────────────────

    public <T> Optional<T> getSiteSettings(Class<T> clazz) {
        return get(KEY_SITE_SETTINGS, clazz);
    }

    public void putSiteSettings(Object value) {
        put(KEY_SITE_SETTINGS, value, Duration.ofSeconds(300));
    }

    public void evictSiteSettings() {
        evict(KEY_SITE_SETTINGS);
    }

    // ─── User Info Cache ──────────────────────────────────────────

    public <T> Optional<T> getUserInfo(Long userId, Class<T> clazz) {
        return get(PREFIX_USER_INFO + userId, clazz);
    }

    public void putUserInfo(Long userId, Object value) {
        put(PREFIX_USER_INFO + userId, value, Duration.ofSeconds(180));
    }

    public void evictUserInfo(Long userId) {
        evict(PREFIX_USER_INFO + userId);
    }
}