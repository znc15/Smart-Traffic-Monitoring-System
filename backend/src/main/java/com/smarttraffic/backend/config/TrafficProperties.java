package com.smarttraffic.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app.traffic")
public class TrafficProperties {
    private String roads = "Đường Trần Hưng Đạo,Đường Trần Phú,Đường Nguyễn Huệ,Đường Lê Lợi,Đường Nguyễn Trãi";

    public String getRoads() {
        return roads;
    }

    public void setRoads(String roads) {
        this.roads = roads;
    }

    public List<String> roadsAsList() {
        return Arrays.stream(roads.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
