package com.smarttraffic.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "traffic_predictions")
public class TrafficPredictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "road_name", nullable = false, length = 255)
    private String roadName;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "predict_time", nullable = false)
    private LocalDateTime predictTime;

    @Column(name = "predicted_flow", nullable = false)
    private Double predictedFlow;

    @Column(name = "confidence_low", nullable = false)
    private Double confidenceLow;

    @Column(name = "confidence_high", nullable = false)
    private Double confidenceHigh;

    @Column(name = "algorithm", nullable = false, length = 64)
    private String algorithm;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getRoadName() {
        return roadName;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getPredictTime() {
        return predictTime;
    }

    public void setPredictTime(LocalDateTime predictTime) {
        this.predictTime = predictTime;
    }

    public Double getPredictedFlow() {
        return predictedFlow;
    }

    public void setPredictedFlow(Double predictedFlow) {
        this.predictedFlow = predictedFlow;
    }

    public Double getConfidenceLow() {
        return confidenceLow;
    }

    public void setConfidenceLow(Double confidenceLow) {
        this.confidenceLow = confidenceLow;
    }

    public Double getConfidenceHigh() {
        return confidenceHigh;
    }

    public void setConfidenceHigh(Double confidenceHigh) {
        this.confidenceHigh = confidenceHigh;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
