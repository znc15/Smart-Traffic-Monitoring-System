package com.smarttraffic.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cameras")
public class CameraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String location;

    @Column(name = "stream_url")
    private String streamUrl;

    @Column(name = "node_url", length = 512)
    private String nodeUrl;

    @Column(name = "edge_node_id", length = 128)
    private String edgeNodeId;

    @Column(name = "node_api_key", length = 255)
    private String nodeApiKey;

    @Column(name = "road_name")
    private String roadName;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    private boolean enabled = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }
    public String getNodeUrl() { return nodeUrl; }
    public void setNodeUrl(String nodeUrl) { this.nodeUrl = nodeUrl; }
    public String getEdgeNodeId() { return edgeNodeId; }
    public void setEdgeNodeId(String edgeNodeId) { this.edgeNodeId = edgeNodeId; }
    public String getNodeApiKey() { return nodeApiKey; }
    public void setNodeApiKey(String nodeApiKey) { this.nodeApiKey = nodeApiKey; }
    public String getRoadName() { return roadName; }
    public void setRoadName(String roadName) { this.roadName = roadName; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
