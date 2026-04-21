package com.smarttraffic.backend.service;

import com.smarttraffic.backend.model.AlertEntity;
import com.smarttraffic.backend.model.SiteSettingsEntity;
import com.smarttraffic.backend.repository.AlertRepository;
import com.smarttraffic.backend.repository.SiteSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final SiteSettingsRepository siteSettingsRepository;

    public AlertService(AlertRepository alertRepository, SiteSettingsRepository siteSettingsRepository) {
        this.alertRepository = alertRepository;
        this.siteSettingsRepository = siteSettingsRepository;
    }

    public List<AlertEntity> getAllAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<AlertEntity> getAlertsByStatus(String status) {
        return alertRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public AlertEntity createAlert(String type, String level, String roadName, String nodeId, String message) {
        // Only create if there's no undisposed alert of the same type for the same road
        return alertRepository.findTopByTypeAndRoadNameAndStatusNotOrderByCreatedAtDesc(type, roadName, "DISPOSED")
                .orElseGet(() -> {
                    AlertEntity alert = new AlertEntity();
                    alert.setType(type);
                    alert.setLevel(level);
                    alert.setRoadName(roadName);
                    alert.setNodeId(nodeId);
                    alert.setMessage(message);
                    return alertRepository.save(alert);
                });
    }

    @Transactional
    public void checkAndCreateCongestionAlert(String roadName, String nodeId, double congestionIndex) {
        Double threshold = siteSettingsRepository.findById(1L)
                .map(SiteSettingsEntity::getCongestionThreshold)
                .orElse(0.8);

        if (congestionIndex >= threshold) {
            createAlert(
                    "CONGESTION",
                    "WARNING",
                    roadName,
                    nodeId,
                    String.format("道路 %s 拥堵指数达到 %.2f，超过阈值 %.2f", roadName, congestionIndex, threshold)
            );
        }
    }

    @Transactional
    public void createHeartbeatTimeoutAlert(String roadName, String nodeId) {
        createAlert(
                "HEARTBEAT_TIMEOUT",
                "CRITICAL",
                roadName,
                nodeId,
                String.format("边缘节点 %s (%s) 心跳超时", nodeId != null ? nodeId : "未知", roadName)
        );
    }

    @Transactional
    public AlertEntity updateAlertStatus(Long id, String status) {
        AlertEntity alert = alertRepository.findById(id).orElseThrow(() -> new RuntimeException("Alert not found"));
        alert.setStatus(status);
        return alertRepository.save(alert);
    }
}
