package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.CameraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CameraRepository extends JpaRepository<CameraEntity, Long> {
    List<CameraEntity> findByEnabledTrue();

    /** 从数据库读取所有不重复的 roadName（排除 null 和空字符串） */
    @Query("SELECT DISTINCT c.roadName FROM CameraEntity c WHERE c.roadName IS NOT NULL AND c.roadName <> ''")
    List<String> findDistinctRoadNames();

    /** 从已启用的摄像头中查询不重复的道路名称 */
    @Query("SELECT DISTINCT c.roadName FROM CameraEntity c WHERE c.enabled = true AND c.roadName IS NOT NULL AND c.roadName <> ''")
    List<String> findDistinctRoadNameByEnabledTrue();

    @Query("""
            SELECT c
            FROM CameraEntity c
            WHERE c.enabled = true
              AND c.latitude IS NOT NULL
              AND c.longitude IS NOT NULL
              AND c.latitude BETWEEN :minLat AND :maxLat
              AND c.longitude BETWEEN :minLng AND :maxLng
            """)
    List<CameraEntity> findInBoundingBox(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng
    );
}
