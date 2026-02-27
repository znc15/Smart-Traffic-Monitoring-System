package com.smarttraffic.backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;

@Entity
@Table(name = "traffic_samples")
public class TrafficSampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", length = 128)
    private String nodeId;

    @Column(name = "road_name", nullable = false, length = 255)
    private String roadName;

    @Column(name = "sample_time", nullable = false)
    private LocalDateTime sampleTime;

    @Column(name = "count_car", nullable = false)
    private Integer countCar = 0;

    @Column(name = "count_motor", nullable = false)
    private Integer countMotor = 0;

    @Column(name = "count_person", nullable = false)
    private Integer countPerson = 0;

    @Column(name = "avg_speed_car", nullable = false)
    private Double avgSpeedCar = 0.0;

    @Column(name = "avg_speed_motor", nullable = false)
    private Double avgSpeedMotor = 0.0;

    @Column(name = "density_status", length = 32)
    private String densityStatus;

    @Column(name = "speed_status", length = 32)
    private String speedStatus;

    @Column(name = "congestion_index", nullable = false)
    private Double congestionIndex = 0.0;

    @Column(name = "lane_stats_json", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String laneStatsJson;

    @Column(name = "source", nullable = false, length = 32)
    private String source = "edge";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (sampleTime == null) {
            sampleTime = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getRoadName() {
        return roadName;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public LocalDateTime getSampleTime() {
        return sampleTime;
    }

    public void setSampleTime(LocalDateTime sampleTime) {
        this.sampleTime = sampleTime;
    }

    public Integer getCountCar() {
        return countCar;
    }

    public void setCountCar(Integer countCar) {
        this.countCar = countCar;
    }

    public Integer getCountMotor() {
        return countMotor;
    }

    public void setCountMotor(Integer countMotor) {
        this.countMotor = countMotor;
    }

    public Integer getCountPerson() {
        return countPerson;
    }

    public void setCountPerson(Integer countPerson) {
        this.countPerson = countPerson;
    }

    public Double getAvgSpeedCar() {
        return avgSpeedCar;
    }

    public void setAvgSpeedCar(Double avgSpeedCar) {
        this.avgSpeedCar = avgSpeedCar;
    }

    public Double getAvgSpeedMotor() {
        return avgSpeedMotor;
    }

    public void setAvgSpeedMotor(Double avgSpeedMotor) {
        this.avgSpeedMotor = avgSpeedMotor;
    }

    public String getDensityStatus() {
        return densityStatus;
    }

    public void setDensityStatus(String densityStatus) {
        this.densityStatus = densityStatus;
    }

    public String getSpeedStatus() {
        return speedStatus;
    }

    public void setSpeedStatus(String speedStatus) {
        this.speedStatus = speedStatus;
    }

    public Double getCongestionIndex() {
        return congestionIndex;
    }

    public void setCongestionIndex(Double congestionIndex) {
        this.congestionIndex = congestionIndex;
    }

    public String getLaneStatsJson() {
        return laneStatsJson;
    }

    public void setLaneStatsJson(String laneStatsJson) {
        this.laneStatsJson = laneStatsJson;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
