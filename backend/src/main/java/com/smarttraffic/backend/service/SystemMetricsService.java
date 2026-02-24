package com.smarttraffic.backend.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
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
        payload.put("jvm", jvmMetrics());
        payload.put("process_cpu", processCpuPercent());

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

    // JVM 运行时指标
    private Map<String, Object> jvmMetrics() {
        Map<String, Object> jvm = new LinkedHashMap<>();

        // 堆内存
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memBean.getHeapMemoryUsage();
        long heapUsed = heap.getUsed();
        long heapMax = heap.getMax();
        double heapPercent = heapMax > 0 ? Math.round(((double) heapUsed / heapMax) * 10000.0) / 100.0 : 0.0;
        jvm.put("heap_used", heapUsed);
        jvm.put("heap_max", heapMax);
        jvm.put("heap_percent", heapPercent);

        // 非堆内存
        MemoryUsage nonHeap = memBean.getNonHeapMemoryUsage();
        jvm.put("non_heap_used", nonHeap.getUsed());

        // 线程数
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        jvm.put("thread_count", threadBean.getThreadCount());

        // 运行时间
        RuntimeMXBean rtBean = ManagementFactory.getRuntimeMXBean();
        jvm.put("uptime_ms", rtBean.getUptime());

        // GC 统计
        long gcCount = 0;
        long gcTime = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long c = gcBean.getCollectionCount();
            long t = gcBean.getCollectionTime();
            if (c >= 0) gcCount += c;
            if (t >= 0) gcTime += t;
        }
        jvm.put("gc_count", gcCount);
        jvm.put("gc_time_ms", gcTime);

        return jvm;
    }

    // 进程级 CPU 使用率
    private Double processCpuPercent() {
        java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
            double load = sunBean.getProcessCpuLoad();
            if (load >= 0) {
                return Math.round(load * 10000.0) / 100.0;
            }
        }
        return 0.0;
    }
}
