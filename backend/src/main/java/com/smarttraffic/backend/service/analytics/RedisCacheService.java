package com.smarttraffic.backend.service.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.config.DbRuntimeProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Service
public class RedisCacheService {

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
            // ignore cache serialization failures
        } catch (Exception ignored) {
            // ignore redis connectivity failures
        }
    }

    public void evict(String key) {
        if (!dbRuntimeProperties.isRedisCacheEnabled()) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
            // ignore redis connectivity failures
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
            // ignore redis connectivity failures
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
}
