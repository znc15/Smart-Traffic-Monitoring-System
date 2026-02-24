package com.smarttraffic.backend.dto.traffic;

import java.util.List;

public class RoadNamesResponse {
    private List<String> roadNames;

    public RoadNamesResponse(List<String> roadNames) {
        this.roadNames = roadNames;
    }

    public List<String> getRoadNames() {
        return roadNames;
    }

    public void setRoadNames(List<String> roadNames) {
        this.roadNames = roadNames;
    }
}
