# Backend Service (Spring Boot)

后端负责认证、管理、交通汇聚、预测、MaaS 对外接口、报表导出，以及 MySQL/PostgreSQL 双写灰度能力。

## 运行前提

- Java 17+
- Maven 3.9+
- PostgreSQL（主库默认）
- MySQL（灰度镜像/切主）
- Redis（热点缓存）

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

### 认证

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`

### 交通与预测

- `POST /api/v1/edge/telemetry`
- `GET /api/v1/traffic/predictions`

### MaaS

- `GET /api/v1/maas/congestion`（需 `X-API-Key`）

### 报表导出

- `GET /api/v1/reports/traffic/export?granularity=...&format=json|xlsx`

## 报表导出示例

```bash
# JSON
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8000/api/v1/reports/traffic/export?granularity=hourly&format=json"

# XLSX
curl -L -H "Authorization: Bearer <token>" \
  "http://localhost:8000/api/v1/reports/traffic/export?granularity=hourly&format=xlsx" \
  -o traffic_report.xlsx
```

## 配置项（重点）

### 安全与鉴权

- `APP_ENV`
- `JWT_SECRET`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_WS_ALLOW_QUERY_TOKEN`
- `INIT_ADMIN`
- `INIT_ADMIN_USERNAME`
- `INIT_ADMIN_EMAIL`
- `INIT_ADMIN_PASSWORD`

### 主库与灰度

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_FLYWAY_LOCATIONS`
- `APP_DB_PRIMARY`（`postgres|mysql`）
- `APP_DB_MIRROR_WRITE`（`true|false`）
- `APP_DB_MIRROR_MYSQL_ENABLED`
- `APP_DB_MIRROR_MYSQL_URL`
- `APP_DB_MIRROR_MYSQL_USERNAME`
- `APP_DB_MIRROR_MYSQL_PASSWORD`
- `APP_DB_MIRROR_POSTGRES_ENABLED`
- `APP_DB_MIRROR_POSTGRES_URL`
- `APP_DB_MIRROR_POSTGRES_USERNAME`
- `APP_DB_MIRROR_POSTGRES_PASSWORD`

### Redis 缓存

- `SPRING_REDIS_HOST`
- `SPRING_REDIS_PORT`
- `APP_REDIS_CACHE_ENABLED`
- `APP_CACHE_TTL_SECONDS`

## Flyway 迁移目录

- PostgreSQL：`src/main/resources/db/migration/`
- MySQL：`src/main/resources/db/migration-mysql/`

## 主库切换与一致性校验

项目根目录执行：

```bash
# PostgreSQL 主库 + MySQL 镜像双写
./scripts/db/switch_primary.sh postgres

# MySQL 主库 + PostgreSQL 镜像双写
./scripts/db/switch_primary.sh mysql

# 一致性校验（双写表）
bash scripts/check_mirror_consistency.sh
```

说明：镜像库迁移失败仅记录警告，不阻塞主服务启动；主库迁移失败会阻塞启动。

## 常见问题

1. `Unsupported Database: PostgreSQL 16.x`

- 确认 `pom.xml` 已包含 `flyway-database-postgresql`。

2. MySQL 迁移失败后无法再次启动

- 运行切换脚本前，脚本会自动清理 MySQL `flyway_schema_history` 中 `success=0` 记录。

3. 返回 `Internal server error` 难定位

- `ApiExceptionHandler` 已输出 fallback 异常日志，先看 `docker compose logs backend`。

