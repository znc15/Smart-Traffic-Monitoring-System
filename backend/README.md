# Smart Traffic Backend

Spring Boot 后端服务，负责鉴权、交通数据汇聚、预测、MaaS 开放接口、报表导出与边缘节点接入。

## 技术栈

| 组件 | 版本 / 说明 |
|------|-------------|
| Spring Boot | 3.4.2 |
| Java | 17 |
| Spring Security + JWT | JJWT 0.13.0 |
| Spring Data JPA | ORM 持久层 |
| Flyway | PostgreSQL / MySQL 迁移 |
| Spring Data Redis | 缓存 |
| WebSocket | 实时推送 |
| Apache POI | XLSX 导出 |

## 本地启动

如果数据库与缓存来自仓库根 `docker compose up -d database mysql redis`，
建议使用下面这组环境变量启动 backend：

```bash
cd backend
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/transportation_system \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=odoo \
SPRING_REDIS_HOST=localhost \
SPRING_REDIS_PORT=6380 \
JWT_SECRET=change-this-dev-secret-change-this-dev-secret \
mvn -B spring-boot:run
```

验证：

```bash
curl http://localhost:8000/api/v1/site-settings
```

说明：
- 默认监听端口是 `8000`
- `application.yml` 中的默认数据库名是 `traffic_db` / 密码 `password`
- 如果你直接沿用根 Compose 起的依赖，必须显式覆盖为 `transportation_system` / `odoo`

## 构建与测试

```bash
cd backend
mvn -B test
mvn -B clean package -DskipTests
```

Docker 构建：

```bash
cd backend
docker build -t smart-traffic-backend .
docker run -p 8000:8000 smart-traffic-backend
```

## 关键接口与鉴权

### 登录与注册

| 方法 | 路径 | 鉴权 |
|------|------|------|
| `POST` | `/api/v1/auth/register` | 无 |
| `POST` | `/api/v1/auth/login` | 无 |
| `GET` | `/api/v1/auth/me` | Bearer Token / Cookie |

注意：
- `POST /api/v1/auth/login` 当前接受 `application/x-www-form-urlencoded`
- 请求字段名是 `username`
- 但后端目前按“邮箱”匹配登录，因此接入方请传用户邮箱到 `username`

### 公开接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v1/api-docs` | 机器可读 API 文档 |
| `GET` | `/api/v1/site-settings` | 站点配置 |
| `GET` | `/api/v1/roads_name` | 道路列表 |
| `GET` | `/api/v1/frames_no_auth/{roadName}` | 无鉴权帧图 |
| `GET` | `/api/v1/traffic/predictions` | 交通预测 |

### 专用鉴权接口

| 方法 | 路径 | 鉴权方式 |
|------|------|----------|
| `POST` | `/api/v1/edge/telemetry` | `X-Edge-Node-Id` + `X-Edge-Key` |
| `GET` | `/api/v1/maas/**` | `X-API-Key` |
| `GET` | `/api/v1/reports/traffic/export` | Bearer Token / Cookie |

说明：
- `edge/telemetry` 不是 Bearer Token
- `maas` 路由在 Spring Security 中是公开路由，但会由专用 API Key filter 校验

## 配置参考

完整变量见 [`backend/.env.example`](.env.example)。

### 基础运行

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `SERVER_PORT` | HTTP 端口 | `8000` |
| `APP_ENV` | 运行环境 | `development` |
| `BASE_URL_API` | 对外 API 基址 | `http://localhost:8000` |

### 鉴权与安全

| 变量 | 说明 |
|------|------|
| `JWT_SECRET` | JWT 密钥，生产环境必须替换 |
| `ACCESS_TOKEN_EXPIRE_DAYS` | Access token 有效期 |
| `APP_CORS_ALLOWED_ORIGINS` | 允许的跨域来源 |
| `APP_WS_ALLOW_QUERY_TOKEN` | 是否允许 WS 使用 query token |

生产建议：
- `APP_ENV=production`
- 精确设置 `APP_CORS_ALLOWED_ORIGINS`
- 除非兼容旧客户端，否则 `APP_WS_ALLOW_QUERY_TOKEN=false`

### 数据库与缓存

| 变量 | 说明 |
|------|------|
| `SPRING_DATASOURCE_URL` | 主库 JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | 主库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | 主库密码 |
| `SPRING_FLYWAY_LOCATIONS` | Flyway 迁移目录 |
| `SPRING_REDIS_HOST` | Redis 主机 |
| `SPRING_REDIS_PORT` | Redis 端口 |
| `APP_REDIS_CACHE_ENABLED` | 是否启用缓存 |
| `APP_CACHE_TTL_SECONDS` | 缓存 TTL |

### 初始化与种子数据

| 变量 | 说明 |
|------|------|
| `INIT_ADMIN` | 是否启用初始化管理员 |
| `INIT_ADMIN_USERNAME` | 管理员用户名 |
| `INIT_ADMIN_EMAIL` | 管理员邮箱 |
| `INIT_ADMIN_PASSWORD` | 管理员密码 |
| `TRAFFIC_ROADS` | 首次初始化道路列表 |
| `APP_MAAS_DEFAULT_CLIENT_NAME` | 默认 MaaS client 名称 |
| `APP_MAAS_DEFAULT_API_KEY` | 默认 MaaS API Key |

初始化管理员说明：
- 只在系统里还没有任何用户时生效
- 需要同时提供用户名、邮箱、密码
- 密码必须满足复杂度要求，建议首启完成后关闭

### 双库灰度

| 变量 | 说明 |
|------|------|
| `APP_DB_PRIMARY` | 主库类型：`postgres` / `mysql` |
| `APP_DB_MIRROR_WRITE` | 是否启用镜像双写 |
| `APP_DB_MIRROR_MYSQL_ENABLED` | 是否启用 MySQL 镜像 |
| `APP_DB_MIRROR_MYSQL_URL` | MySQL 镜像库 URL |
| `APP_DB_MIRROR_MYSQL_USERNAME` | MySQL 镜像库用户名 |
| `APP_DB_MIRROR_MYSQL_PASSWORD` | MySQL 镜像库密码 |
| `APP_DB_MIRROR_POSTGRES_ENABLED` | 是否启用 PostgreSQL 镜像 |
| `APP_DB_MIRROR_POSTGRES_URL` | PostgreSQL 镜像库 URL |
| `APP_DB_MIRROR_POSTGRES_USERNAME` | PostgreSQL 镜像库用户名 |
| `APP_DB_MIRROR_POSTGRES_PASSWORD` | PostgreSQL 镜像库密码 |

## 生产环境说明

如果你通过仓库根 `docker-compose.yml` 部署 backend：
- `JWT_SECRET` 和数据库密码可直接走根 `.env`
- `APP_ENV`、`APP_CORS_ALLOWED_ORIGINS`、`BASE_URL_API`、`INIT_ADMIN_*`、`TRAFFIC_ROADS` 现在也可以直接走根 `.env`

IP 部署示例：

```env
GATEWAY_PUBLIC_BASE=http://192.168.1.10:5173
BACKEND_PUBLIC_HTTP_BASE=http://192.168.1.11:8000
```

默认情况下，根 Compose 会自动使用：
- `APP_CORS_ALLOWED_ORIGINS = GATEWAY_PUBLIC_BASE`
- `BASE_URL_API = BACKEND_PUBLIC_HTTP_BASE`

健康检查可直接使用：

```bash
curl http://localhost:8000/api/v1/site-settings
```

## 灰度切换

切换主库：

```bash
./scripts/db/switch_primary.sh postgres
./scripts/db/switch_primary.sh mysql
```

一致性校验：

```bash
bash scripts/check_mirror_consistency.sh
bash scripts/check_mirror_consistency.sh --all
```

## 常见问题

| 问题 | 解决方案 |
|------|----------|
| backend 启动后连不上数据库 | 检查是否使用了 Compose 对应的数据库名 `transportation_system` 与密码 `odoo` |
| Redis 连接失败 | 本地使用根 Compose 时端口应为 `6380`，不是 `6379` |
| 登录成功但 Cookie 不安全 | 生产环境请设置 `APP_ENV=production` |
| edge 遥测上报失败 | 检查 `X-Edge-Node-Id` 与 `X-Edge-Key`，不是 Bearer Token |
