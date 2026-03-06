package com.smarttraffic.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

public class UpdateCameraRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String location;

    private Boolean enabled;

    @JsonAlias("stream_url")
    @Size(max = 1024)
    private String streamUrl;

    @JsonAlias("road_name")
    @Size(max = 255)
    private String roadName;

    @DecimalMin(value = "-90", message = "纬度范围为 -90 到 90")
    @DecimalMax(value = "90", message = "纬度范围为 -90 到 90")
    private Double latitude;

    @DecimalMin(value = "-180", message = "经度范围为 -180 到 180")
    @DecimalMax(value = "180", message = "经度范围为 -180 到 180")
    private Double longitude;

    @JsonIgnore
    private final Set<String> setFields = new HashSet<>();

    @JsonIgnore
    public boolean hasField(String field) {
        return setFields.contains(field);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; setFields.add("name"); }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; setFields.add("location"); }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; setFields.add("enabled"); }

    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; setFields.add("streamUrl"); }

    public String getRoadName() { return roadName; }
    public void setRoadName(String roadName) { this.roadName = roadName; setFields.add("roadName"); }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; setFields.add("latitude"); }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; setFields.add("longitude"); }
}
