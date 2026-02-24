package com.smarttraffic.backend.dto.common;

public class CountResponse {
    private long count;

    public CountResponse() {
    }

    public CountResponse(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
