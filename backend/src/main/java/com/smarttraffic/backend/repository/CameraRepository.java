package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.CameraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CameraRepository extends JpaRepository<CameraEntity, Long> {
    List<CameraEntity> findByEnabledTrue();

    /** 从数据库读取所有不重复的 roadName（排除 null 和空字符串） */
    @Query("SELECT DISTINCT c.roadName FROM CameraEntity c WHERE c.roadName IS NOT NULL AND c.roadName <> ''")
    List<String> findDistinctRoadNames();
}
