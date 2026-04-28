package com.smarttraffic.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/api-docs")
public class ApiDocsController {

    @GetMapping
    public Map<String, Object> apiDocs() {
        return Map.of(
                "title", "智慧交通监控系统 API",
                "version", "1.0",
                "base_url", "/api/v1",
                "authentication", buildAuthInfo(),
                "endpoints", buildEndpoints(),
                "websocket_endpoints", buildWebSocketEndpoints()
        );
    }

    private Map<String, Object> buildAuthInfo() {
        return Map.of(
                "type", "API Key 或 Bearer Token",
                "api_key_header", "X-API-Key",
                "bearer_header", "Authorization: Bearer <token>",
                "description", "MaaS 端点使用 X-API-Key 请求头，其他受保护端点使用通过 /api/v1/auth/login 获取的 Bearer Token"
        );
    }

    private List<Map<String, Object>> buildEndpoints() {
        return List.of(
                buildEndpoint(
                        "/api/v1/api-docs",
                        "GET",
                        "获取本 API 文档",
                        "无需认证",
                        List.of(),
                        Map.of("title", "智慧交通监控系统 API", "version", "1.0"),
                        "curl https://host/api/v1/api-docs"
                ),
                buildEndpoint(
                        "/api/v1/auth/register",
                        "POST",
                        "用户注册",
                        "无需认证",
                        List.of(
                                buildParam("username", "body", "用户名", true),
                                buildParam("password", "body", "密码", true),
                                buildParam("nickname", "body", "昵称（可选）", false)
                        ),
                        Map.of("message", "注册成功"),
                        "curl -X POST -H 'Content-Type: application/json' -d '{\"username\":\"user\",\"password\":\"pass\"}' https://host/api/v1/auth/register"
                ),
                buildEndpoint(
                        "/api/v1/auth/login",
                        "POST",
                        "用户登录，返回 access_token（同时写入 Cookie）",
                        "无需认证",
                        List.of(
                                buildParam("username", "form", "用户名", true),
                                buildParam("password", "form", "密码", true)
                        ),
                        Map.of("access_token", "<jwt>", "token_type", "Bearer"),
                        "curl -X POST -d 'username=admin&password=pass' https://host/api/v1/auth/login"
                ),
                buildEndpoint(
                        "/api/v1/auth/me",
                        "GET",
                        "获取当前登录用户信息",
                        "Bearer Token",
                        List.of(),
                        Map.of("id", 1, "username", "admin", "role_id", 1),
                        "curl -H 'Authorization: Bearer <token>' https://host/api/v1/auth/me"
                ),
                buildEndpoint(
                        "/api/v1/roads_name",
                        "GET",
                        "获取所有已启用道路名称列表",
                        "无需认证",
                        List.of(),
                        Map.of("roads", List.of("road1", "road2")),
                        "curl https://host/api/v1/roads_name"
                ),
                buildEndpoint(
                        "/api/v1/info/{roadName}",
                        "GET",
                        "获取指定道路的实时交通信息",
                        "无需认证",
                        List.of(
                                buildParam("roadName", "path", "道路名称", true)
                        ),
                        Map.of("road_name", "road1", "vehicle_count", 42, "congestion_level", "moderate"),
                        "curl https://host/api/v1/info/road1"
                ),
                buildEndpoint(
                        "/api/v1/frames_no_auth/{roadName}",
                        "GET",
                        "获取指定道路摄像头最新帧图像（无需认证）",
                        "无需认证",
                        List.of(
                                buildParam("roadName", "path", "道路名称", true)
                        ),
                        Map.of("content_type", "image/jpeg"),
                        "curl https://host/api/v1/frames_no_auth/road1 --output frame.jpg"
                ),
                buildEndpoint(
                        "/api/v1/frames/{roadName}",
                        "GET",
                        "获取指定道路摄像头最新帧图像（需认证）",
                        "Bearer Token",
                        List.of(
                                buildParam("roadName", "path", "道路名称", true)
                        ),
                        Map.of("content_type", "image/jpeg"),
                        "curl -H 'Authorization: Bearer <token>' https://host/api/v1/frames/road1 --output frame.jpg"
                ),
                buildEndpoint(
                        "/api/v1/maas/congestion",
                        "GET",
                        "获取指定地理范围内的实时拥堵数据（MaaS 接口）",
                        "API Key (X-API-Key header)",
                        List.of(
                                buildParam("min_lat", "query", "最小纬度（-90 ~ 90）", true),
                                buildParam("max_lat", "query", "最大纬度（-90 ~ 90）", true),
                                buildParam("min_lng", "query", "最小经度（-180 ~ 180）", true),
                                buildParam("max_lng", "query", "最大经度（-180 ~ 180）", true)
                        ),
                        Map.of("segments", List.of()),
                        "curl -H 'X-API-Key: your-key' 'https://host/api/v1/maas/congestion?min_lat=30&max_lat=31&min_lng=120&max_lng=121'"
                ),
                buildEndpoint(
                        "/api/v1/site-settings",
                        "GET",
                        "获取站点配置信息（站点名称、公告、Logo 等）",
                        "无需认证",
                        List.of(),
                        Map.of("siteName", "智能交通监控系统", "announcement", "", "logoUrl", ""),
                        "curl https://host/api/v1/site-settings"
                ),
                buildEndpoint(
                        "/api/v1/reports/traffic/export",
                        "GET",
                        "导出交通报告（JSON 或 Excel 格式）",
                        "Bearer Token",
                        List.of(
                                buildParam("granularity", "query", "聚合粒度：hourly（默认）/ daily / weekly", false),
                                buildParam("road_name", "query", "道路名称过滤（可选）", false),
                                buildParam("start_at", "query", "开始时间，ISO 8601 格式（可选）", false),
                                buildParam("end_at", "query", "结束时间，ISO 8601 格式（可选）", false),
                                buildParam("format", "query", "导出格式：json（默认）/ xlsx", false)
                        ),
                        Map.of("records", List.of()),
                        "curl -H 'Authorization: Bearer <token>' 'https://host/api/v1/reports/traffic/export?format=json&granularity=hourly'"
                ),
                buildEndpoint(
                        "/api/v1/users/password",
                        "PUT",
                        "修改当前用户密码",
                        "Bearer Token",
                        List.of(
                                buildParam("old_password", "body", "原密码", true),
                                buildParam("new_password", "body", "新密码", true)
                        ),
                        Map.of("message", "密码修改成功！"),
                        "curl -X PUT -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' -d '{\"old_password\":\"old\",\"new_password\":\"new\"}' https://host/api/v1/users/password"
                ),
                buildEndpoint(
                        "/api/v1/users/profile",
                        "PUT",
                        "更新当前用户个人资料",
                        "Bearer Token",
                        List.of(
                                buildParam("nickname", "body", "昵称（可选）", false)
                        ),
                        Map.of("message", "资料更新成功！"),
                        "curl -X PUT -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' -d '{\"nickname\":\"新昵称\"}' https://host/api/v1/users/profile"
                ),
                buildEndpoint(
                        "/api/v1/edge/telemetry",
                        "POST",
                        "边缘节点上报遥测数据",
                        "Edge Node Headers",
                        List.of(
                                buildParam("X-Edge-Node-Id", "header", "边缘节点 ID", true),
                                buildParam("X-Edge-Key", "header", "边缘节点密钥", true),
                                buildParam("node_id", "body", "节点 ID", true),
                                buildParam("road_name", "body", "道路名称", true),
                                buildParam("vehicle_count", "body", "车辆计数", true),
                                buildParam("timestamp", "body", "时间戳（ISO 8601）", true)
                        ),
                        Map.of("message", "telemetry accepted"),
                        "curl -X POST -H 'X-Edge-Node-Id: edge-node-1' -H 'X-Edge-Key: edge-secret' -H 'Content-Type: application/json' -d '{...}' https://host/api/v1/edge/telemetry"
                ),
                buildEndpoint(
                        "/api/v1/map/overview",
                        "GET",
                        "获取 GIS 地图总览数据，包含点位、拥堵指数与快照地址",
                        "无需认证",
                        List.of(),
                        Map.of("updated_at", "2026-03-10T10:00:00", "items", List.of()),
                        "curl https://host/api/v1/map/overview"
                ),
                buildEndpoint(
                        "/api/v1/admin/events",
                        "GET",
                        "管理员查询事件日志，支持按道路、事件类型、时间范围过滤",
                        "管理员 Bearer Token",
                        List.of(
                                buildParam("road_name", "query", "道路名称过滤", false),
                                buildParam("event_type", "query", "事件类型过滤", false),
                                buildParam("start_at", "query", "开始时间，ISO 8601 格式", false),
                                buildParam("end_at", "query", "结束时间，ISO 8601 格式", false)
                        ),
                        Map.of("content", List.of(), "totalElements", 0),
                        "curl -H 'Authorization: Bearer <token>' 'https://host/api/v1/admin/events?event_type=wrong_way_suspected'"
                ),
                buildEndpoint(
                        "/api/v1/admin/nodes/{cameraId}/config",
                        "GET",
                        "管理员获取边缘节点当前配置",
                        "管理员 Bearer Token",
                        List.of(
                                buildParam("cameraId", "path", "摄像头 ID", true)
                        ),
                        Map.of("analysis_roi", List.of(), "telemetry_interval_sec", 3),
                        "curl -H 'Authorization: Bearer <token>' https://host/api/v1/admin/nodes/1/config"
                ),
                buildEndpoint(
                        "/api/v1/admin/nodes/{cameraId}/config",
                        "PUT",
                        "管理员远程下发边缘节点配置",
                        "管理员 Bearer Token",
                        List.of(
                                buildParam("cameraId", "path", "摄像头 ID", true),
                                buildParam("analysis_roi", "body", "ROI 多边形或矩形配置", false),
                                buildParam("telemetry_interval_sec", "body", "上报间隔秒数", false)
                        ),
                        Map.of("status", "ok"),
                        "curl -X PUT -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' -d '{\"telemetry_interval_sec\":2}' https://host/api/v1/admin/nodes/1/config"
                )
        );
    }

    private List<Map<String, Object>> buildWebSocketEndpoints() {
        return List.of(
                Map.of(
                        "path", "/api/v1/ws/info/{roadName}",
                        "description", "实时交通信息 WebSocket，服务端定期推送道路交通数据",
                        "authentication", "Bearer Token（Cookie access_token 或 query 参数 token）",
                        "path_params", List.of(buildParam("roadName", "path", "道路名称", true))
                ),
                Map.of(
                        "path", "/api/v1/ws/frames/{roadName}",
                        "description", "实时视频帧 WebSocket，服务端定期推送摄像头 JPEG 帧（二进制）",
                        "authentication", "Bearer Token（Cookie access_token 或 query 参数 token）",
                        "path_params", List.of(buildParam("roadName", "path", "道路名称", true))
                ),
                Map.of(
                        "path", "/api/v1/admin/ws/resources",
                        "description", "管理员系统资源监控 WebSocket，推送 CPU/内存/连接数等指标",
                        "authentication", "管理员 Bearer Token（Cookie access_token 或 query 参数 token）",
                        "path_params", List.of()
                )
        );
    }

    private Map<String, Object> buildEndpoint(
            String path,
            String method,
            String description,
            String authentication,
            List<Map<String, Object>> parameters,
            Object responseExample,
            String curlExample
    ) {
        return Map.of(
                "path", path,
                "method", method,
                "description", description,
                "authentication", authentication,
                "parameters", parameters,
                "response_example", responseExample,
                "curl_example", curlExample
        );
    }

    private Map<String, Object> buildParam(String name, String type, String description, boolean required) {
        return Map.of(
                "name", name,
                "type", type,
                "description", description,
                "required", required
        );
    }
}
