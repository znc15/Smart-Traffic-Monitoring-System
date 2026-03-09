package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.ApiUsageLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ApiUsageLogRepository extends JpaRepository<ApiUsageLogEntity, Long> {

    void deleteByApiClientId(Long apiClientId);

    List<ApiUsageLogEntity> findByApiClientIdAndCreatedAtBetween(
            Long clientId,
            LocalDateTime start,
            LocalDateTime end
    );

    long countByApiClientIdAndCreatedAtBetween(
            Long clientId,
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("""
            SELECT CAST(a.createdAt AS date) AS day, COUNT(a) AS total
            FROM ApiUsageLogEntity a
            WHERE a.apiClientId = :clientId
              AND a.createdAt BETWEEN :start AND :end
            GROUP BY CAST(a.createdAt AS date)
            ORDER BY CAST(a.createdAt AS date)
            """)
    List<Object[]> countByClientIdGroupByDay(
            @Param("clientId") Long clientId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            SELECT a.endpoint, COUNT(a) AS total
            FROM ApiUsageLogEntity a
            WHERE a.apiClientId = :clientId
              AND a.createdAt BETWEEN :start AND :end
            GROUP BY a.endpoint
            ORDER BY total DESC
            """)
    List<Object[]> countByClientIdGroupByEndpoint(
            @Param("clientId") Long clientId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            SELECT a.endpoint, COUNT(a) AS total, AVG(a.responseTimeMs) AS avgResponseMs
            FROM ApiUsageLogEntity a
            WHERE a.createdAt BETWEEN :start AND :end
            GROUP BY a.endpoint
            ORDER BY total DESC
            """)
    List<Object[]> countByEndpointGrouped(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}

