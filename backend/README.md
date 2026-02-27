# Backend Service (Spring Boot)

后端负责鉴权、管理、交通数据汇聚、预测、MaaS 接口和报表导出。

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

## 关键接口

- 认证：`POST /api/v1/auth/login`、`GET /api/v1/auth/me`
- 边缘上报：`POST /api/v1/edge/telemetry`
- 交通预测：`GET /api/v1/traffic/predictions`
- MaaS：`GET /api/v1/maas/congestion`（`X-API-Key`）
- 报表导出：`GET /api/v1/reports/traffic/export?granularity=...&format=json|xlsx`

## 报表导出示例

```bash
curl -L -H "Authorization: Bearer <token>" \
  "http://localhost:8000/api/v1/reports/traffic/export?granularity=hourly&format=xlsx" \
  -o traffic_report.xlsx
```

## 关键配置

### 安全

- `JWT_SECRET`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_WS_ALLOW_QUERY_TOKEN`
- `INIT_ADMIN` / `INIT_ADMIN_*`

### 数据库

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_FLYWAY_LOCATIONS`
- `APP_DB_PRIMARY`（`postgres|mysql`）
- `APP_DB_MIRROR_WRITE`（`true|false`）

### Redis

- `SPRING_REDIS_HOST`
- `SPRING_REDIS_PORT`
- `APP_REDIS_CACHE_ENABLED`
- `APP_CACHE_TTL_SECONDS`

## 双写灰度切换

在项目根目录执行：

```bash
./scripts/db/switch_primary.sh postgres
./scripts/db/switch_primary.sh mysql
bash scripts/check_mirror_consistency.sh
```

## 常见问题

1. `Unsupported Database: PostgreSQL 16.x`
- 确认 `pom.xml` 包含 `flyway-database-postgresql`。

2. 启动后接口 401
- 检查 `JWT_SECRET`、token 是否过期、浏览器是否携带 Cookie。
