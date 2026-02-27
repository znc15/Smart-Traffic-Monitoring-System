package com.smarttraffic.backend.dto.edge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EdgeTelemetryRequest {

    @NotBlank(message = "node_id is required")
    private String nodeId;

    @NotBlank(message = "road_name is required")
    private String roadName;

    private LocalDateTime timestamp;

    @Min(value = 0, message = "count_car must be >= 0")
    private Integer countCar = 0;

    @Min(value = 0, message = "count_motor must be >= 0")
    private Integer countMotor = 0;

    @Min(value = 0, message = "count_person must be >= 0")
    private Integer countPerson = 0;

    @Min(value = 0, message = "avg_speed_car must be >= 0")
    private Double avgSpeedCar = 0.0;

    @Min(value = 0, message = "avg_speed_motor must be >= 0")
    private Double avgSpeedMotor = 0.0;

    private String densityStatus;
    private String speedStatus;

    @Min(value = 0, message = "congestion_index must be >= 0")
    @Max(value = 1, message = "congestion_index must be <= 1")
    private Double congestionIndex;

    @Valid
    private List<LaneStat> laneStats = new ArrayList<>();

    @Valid
    private List<EventPayload> events = new ArrayList<>();

    private EdgeMetrics edgeMetrics;

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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    public List<LaneStat> getLaneStats() {
        return laneStats;
    }

    public void setLaneStats(List<LaneStat> laneStats) {
        this.laneStats = laneStats;
    }

    public List<EventPayload> getEvents() {
        return events;
    }

    public void setEvents(List<EventPayload> events) {
        this.events = events;
    }

    public EdgeMetrics getEdgeMetrics() {
        return edgeMetrics;
    }

    public void setEdgeMetrics(EdgeMetrics edgeMetrics) {
        this.edgeMetrics = edgeMetrics;
    }

    public static class LaneStat {
        @NotBlank(message = "lane_id is required")
        private String laneId;

        @NotBlank(message = "turn_type is required")
        private String turnType;

        @Min(value = 0, message = "count must be >= 0")
        private Integer count = 0;

        public String getLaneId() {
            return laneId;
        }

        public void setLaneId(String laneId) {
            this.laneId = laneId;
        }

        public String getTurnType() {
            return turnType;
        }

        public void setTurnType(String turnType) {
            this.turnType = turnType;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }

    public static class EventPayload {
        @NotBlank(message = "event_type is required")
        private String eventType;

        private String level;
        private LocalDateTime startAt;
        private LocalDateTime endAt;

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public LocalDateTime getStartAt() {
            return startAt;
        }

        public void setStartAt(LocalDateTime startAt) {
            this.startAt = startAt;
        }

        public LocalDateTime getEndAt() {
            return endAt;
        }

        public void setEndAt(LocalDateTime endAt) {
            this.endAt = endAt;
        }
    }

    public static class EdgeMetrics {
        private Double fps;
        private Double inferenceMs;
        private Double cpuPercent;

        public Double getFps() {
            return fps;
        }

        public void setFps(Double fps) {
            this.fps = fps;
        }

        public Double getInferenceMs() {
            return inferenceMs;
        }

        public void setInferenceMs(Double inferenceMs) {
            this.inferenceMs = inferenceMs;
        }

        public Double getCpuPercent() {
            return cpuPercent;
        }

        public void setCpuPercent(Double cpuPercent) {
            this.cpuPercent = cpuPercent;
        }
    }
}
