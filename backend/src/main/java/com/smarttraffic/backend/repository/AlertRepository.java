package com.smarttraffic.backend.repository;

import com.smarttraffic.backend.model.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, Long> {
    List<AlertEntity> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT a FROM AlertEntity a WHERE a.status = :status ORDER BY a.createdAt DESC")
    List<AlertEntity> findByStatusOrderByCreatedAtDesc(String status);
    
    @Query("SELECT COUNT(a) FROM AlertEntity a WHERE a.status = :status")
    long countByStatus(String status);

    @Query("SELECT a FROM AlertEntity a WHERE a.type = :type AND a.roadName = :roadName AND a.status != 'DISPOSED' ORDER BY a.createdAt DESC LIMIT 1")
    Optional<AlertEntity> findTopByTypeAndRoadNameAndStatusNotOrderByCreatedAtDesc(String type, String roadName, String status);
}
