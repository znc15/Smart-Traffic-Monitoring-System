package com.smarttraffic.backend.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class WebSocketSchedulerConfig {

    private static final int POOL_SIZE = 5;

    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService webSocketScheduler() {
        return Executors.newScheduledThreadPool(POOL_SIZE, r -> {
            Thread t = new Thread(r, "ws-scheduler");
            t.setDaemon(true);
            return t;
        });
    }
}
