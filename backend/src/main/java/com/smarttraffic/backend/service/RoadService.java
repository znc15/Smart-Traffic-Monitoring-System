package com.smarttraffic.backend.service;

import com.smarttraffic.backend.repository.CameraRepository;
import com.smarttraffic.backend.service.analytics.RedisCacheService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 道路发现服务 —— 从已启用的摄像头中动态获取活跃道路列表，
 * 结果缓存至 Redis（TTL 30s），摄像头变更时主动清除缓存。
 */
@Service
public class RoadService {

    /** Redis 缓存键 */
    private static final String CACHE_KEY = "roads:active";

    /** 缓存有效期 30 秒 */
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private final CameraRepository cameraRepository;
    private final RedisCacheService redisCacheService;

    public RoadService(CameraRepository cameraRepository, RedisCacheService redisCacheService) {
        this.cameraRepository = cameraRepository;
        this.redisCacheService = redisCacheService;
    }

    /**
     * 获取当前所有活跃道路名称列表（已启用的摄像头中去重）。
     * 优先读取 Redis 缓存，缓存未命中则查询数据库并回写缓存。
     *
     * @return 活跃道路名称列表，不含 null 或空字符串
     */
    @SuppressWarnings("unchecked")
    public List<String> getActiveRoads() {
        // 尝试从缓存中读取
        // Jackson 对 List<String> 序列化为 JSON 数组，用 List.class 反序列化时元素均为 String
        Object cached = redisCacheService.get(CACHE_KEY, List.class).orElse(null);
        if (cached instanceof List<?> list && (list.isEmpty() || list.get(0) instanceof String)) {
            return (List<String>) list;
        }

        // 缓存 miss，查询数据库
        List<String> roads = cameraRepository.findDistinctRoadNameByEnabledTrue();

        // 写入缓存，TTL 30s
        redisCacheService.put(CACHE_KEY, roads, CACHE_TTL);

        return roads;
    }

    /**
     * 主动清除道路列表缓存，在摄像头增删改后调用。
     */
    public void evictRoadCache() {
        redisCacheService.evict(CACHE_KEY);
    }
}
