package com.smarttraffic.backend.service.analytics;

import com.smarttraffic.backend.model.TrafficSampleEntity;
import com.smarttraffic.backend.repository.TrafficSampleRepository;
import com.smarttraffic.backend.service.TrafficService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 定时将 TrafficService 内存快照持久化到 traffic_samples 表，
 * 为历史数据统计页面的折线图、报表查询提供数据源。
 */
@Service
public class TrafficSamplePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(TrafficSamplePersistenceService.class);

    private final TrafficService trafficService;
    private final TrafficSampleRepository trafficSampleRepository;

    public TrafficSamplePersistenceService(TrafficService trafficService,
                                           TrafficSampleRepository trafficSampleRepository) {
        this.trafficService = trafficService;
        this.trafficSampleRepository = trafficSampleRepository;
    }

    @Scheduled(fixedRate = 30_000)
    public void persistSamples() {
        try {
            Map<String, TrafficService.SnapshotData> snapshots = trafficService.getAllSnapshots();
            if (snapshots.isEmpty()) return;

            LocalDateTime now = LocalDateTime.now();
            List<TrafficSampleEntity> entities = new ArrayList<>(snapshots.size());

            for (Map.Entry<String, TrafficService.SnapshotData> entry : snapshots.entrySet()) {
                TrafficService.SnapshotData snap = entry.getValue();
                TrafficSampleEntity entity = new TrafficSampleEntity();
                entity.setRoadName(entry.getKey());
                entity.setSampleTime(now);
                entity.setCountCar(safeInt(snap.countCar()));
                entity.setCountMotor(safeInt(snap.countMotor()));
                entity.setCountPerson(safeInt(snap.countPerson()));
                entity.setAvgSpeedCar((double) snap.speedCar());
                entity.setAvgSpeedMotor((double) snap.speedMotor());
                entity.setCongestionIndex(snap.congestionIndex());
                entity.setDensityStatus(snap.densityStatus());
                entity.setSpeedStatus(snap.speedStatus());
                entity.setSource("scheduled");
                entities.add(entity);
            }

            trafficSampleRepository.saveAll(entities);
        } catch (Exception e) {
            log.warn("定时采样持久化失败: {}", e.getMessage());
        }
    }

    private static int safeInt(long value) {
        return value < 0 ? 0 : (value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value);
    }
}
