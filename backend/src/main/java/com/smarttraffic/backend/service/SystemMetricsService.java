package com.smarttraffic.backend.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SystemMetricsService {

    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("cpu_percent", cpuPercent());
        payload.put("memory", memoryMetrics());
        payload.put("disk", diskMetrics());
        payload.put("gpu", null);

        return payload;
    }

    private Double cpuPercent() {
        java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
            double cpuLoad = sunBean.getSystemCpuLoad();
            if (cpuLoad >= 0) {
                return Math.round(cpuLoad * 10000.0) / 100.0;
            }
        }
        double loadAverage = osBean.getSystemLoadAverage();
        return loadAverage >= 0 ? Math.round(loadAverage * 100.0) / 100.0 : 0.0;
    }

    private Map<String, Object> memoryMetrics() {
        java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (!(osBean instanceof com.sun.management.OperatingSystemMXBean sunBean)) {
            return null;
        }

        long total = sunBean.getTotalPhysicalMemorySize();
        long free = sunBean.getFreePhysicalMemorySize();
        long used = total - free;
        double percent = total == 0 ? 0.0 : ((double) used / total) * 100.0;

        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("total", total);
        memory.put("available", free);
        memory.put("percent", Math.round(percent * 100.0) / 100.0);
        memory.put("used", used);
        memory.put("free", free);
        return memory;
    }

    private Map<String, Object> diskMetrics() {
        File root = new File("/");
        long total = root.getTotalSpace();
        long free = root.getUsableSpace();
        long used = total - free;
        double percent = total == 0 ? 0.0 : ((double) used / total) * 100.0;

        Map<String, Object> disk = new LinkedHashMap<>();
        disk.put("total", total);
        disk.put("used", used);
        disk.put("free", free);
        disk.put("percent", Math.round(percent * 100.0) / 100.0);
        return disk;
    }
}
