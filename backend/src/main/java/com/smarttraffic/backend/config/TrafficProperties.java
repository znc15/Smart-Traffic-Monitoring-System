package com.smarttraffic.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app.traffic")
public class TrafficProperties {
    private String roads = "陈兴道路,陈富路,阮惠路,黎利路,阮廌路";

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
