package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.model.TrafficEventEntity;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.service.AdminEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/events")
public class AdminEventController {

    private final AdminEventService adminEventService;

    public AdminEventController(AdminEventService adminEventService) {
        this.adminEventService = adminEventService;
    }

    @GetMapping
    public Page<TrafficEventEntity> listEvents(
            @RequestParam(value = "road_name", required = false) String roadName,
            @RequestParam(value = "event_type", required = false) String eventType,
            @RequestParam(value = "start_at", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam(value = "end_at", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        SecurityUtils.requireAdmin();
        return adminEventService.queryEvents(roadName, eventType, startAt, endAt, pageable);
    }
}
