package com.smarttraffic.backend.service.analytics;

import com.smarttraffic.backend.config.DbRuntimeProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisRateLimitService {

    private static final String PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;
    private final DbRuntimeProperties dbRuntimeProperties;

    public RedisRateLimitService(StringRedisTemplate redisTemplate, DbRuntimeProperties dbRuntimeProperties) {
        this.redisTemplate = redisTemplate;
        this.dbRuntimeProperties = dbRuntimeProperties;
    }

    /**
     * Check if a request is allowed under the rate limit.
     * Uses Redis INCR + EXPIRE for a fixed-window counter.
     *
     * @param apiKey      the API key identifier
     * @param windowSeconds the time window in seconds
     * @param maxRequests the maximum number of requests allowed in the window
     * @return true if the request is allowed, false if rate-limited
     */
    public boolean isAllowed(String apiKey, int windowSeconds, int maxRequests) {
        if (!dbRuntimeProperties.isRedisCacheEnabled()) {
            return true;
        }
        try {
            String key = PREFIX + apiKey + ":" + windowSeconds;
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                return true;
            }
            // Set TTL only on first increment
            if (count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            return count <= maxRequests;
        } catch (Exception ex) {
            // On Redis failure, allow the request through
            return true;
        }
    }

    /**
     * Reset the rate limit counter for a given API key and window.
     */
    public void reset(String apiKey, int windowSeconds) {
        if (!dbRuntimeProperties.isRedisCacheEnabled()) {
            return;
        }
        try {
            redisTemplate.delete(PREFIX + apiKey + ":" + windowSeconds);
        } catch (Exception ignored) {
        }
    }
}