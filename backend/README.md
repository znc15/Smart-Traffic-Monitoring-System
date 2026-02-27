# Backend（Spring Boot 云端服务）

`backend` 提供认证、管理、交通聚合、预测与 MaaS 对外接口。

## 功能模块

- 用户认证与权限（JWT + Cookie）
- 管理端接口（用户/摄像头/系统监控）
- 边缘主动上报接收：`POST /api/v1/edge/telemetry`
- 交通预测：`GET /api/v1/traffic/predictions`
- MaaS 查询（API Key）：`GET /api/v1/maas/congestion`

## 环境变量

核心配置（可参考 `.env.example`）：

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_WS_ALLOW_QUERY_TOKEN`
- `APP_MAAS_DEFAULT_CLIENT_NAME`
- `APP_MAAS_DEFAULT_API_KEY`

## 本地运行

```bash
cd backend
mvn -B spring-boot:run
```

## 测试

```bash
cd backend
mvn -B test
```

## 数据迁移（Flyway）

迁移文件位于 `src/main/resources/db/migration/`：

- `V1__init.sql`
- `V2__traffic_samples.sql`
- `V3__traffic_events.sql`
- `V4__traffic_predictions.sql`
- `V5__camera_geo_and_api_clients.sql`
- `V6__reporting_views.sql`

## 关键接口示例

### 1) 边缘上报

```bash
curl -X POST http://localhost:8000/api/v1/edge/telemetry \
  -H 'Content-Type: application/json' \
  -d '{
    "node_id":"edge-01",
    "road_name":"陈兴道路",
    "count_car":10,
    "count_motor":5,
    "count_person":2,
    "avg_speed_car":30.5,
    "avg_speed_motor":25.0
  }'
```

### 2) 预测

```bash
curl "http://localhost:8000/api/v1/traffic/predictions?road_name=陈兴道路&horizon_minutes=60"
```

### 3) MaaS

```bash
curl -H 'X-API-Key: dev-maas-key-change-me' \
  "http://localhost:8000/api/v1/maas/congestion?min_lat=0&max_lat=90&min_lng=0&max_lng=180"
```
