# Smart Traffic Backend

> Spring Boot 后端服务 —— 负责鉴权、交通数据汇聚、智能预测、MaaS 开放接口与报表导出。

---

## 技术栈

| 组件 | 版本 / 说明 |
|------|-------------|
| **Spring Boot** | 3.4.2 (Java 17) |
| **Spring Security + JWT** | JJWT 0.12.6，Bearer Token 鉴权 |
| **Spring Data JPA** | ORM 持久层 |
| **Flyway** | 数据库版本迁移（支持 PostgreSQL & MySQL） |
| **Spring Data Redis** | 缓存层 |
| **WebSocket** | 实时数据推送 |
| **Apache POI** | XLSX 报表导出 |
| **PostgreSQL** | 16，主库 |
| **MySQL** | 8.4，可选镜像双写 |
| **Redis** | 7，缓存 |

---

## 目录结构

```
backend/
├── src/main/java/com/smarttraffic/backend/
│   ├── config/          # 安全、JWT、数据库、WebSocket、CORS、Redis 配置
│   ├── controller/      # REST 控制器（Auth、Traffic、Report、MaaS、Admin、Edge 等）
│   ├── dto/             # 数据传输对象
│   ├── exception/       # 全局异常处理
│   ├── model/           # JPA 实体（User、TrafficData 等）
│   ├── repository/      # 数据访问层
│   ├── security/        # JWT Token 提供者、API Key 认证过滤器
│   ├── service/         # 业务逻辑（含 analytics 子包）
│   └── websocket/       # WebSocket 处理器
├── src/main/resources/
│   ├── application.yml
│   ├── db/migration/         # PostgreSQL Flyway 迁移脚本
│   └── db/migration-mysql/   # MySQL Flyway 迁移脚本
├── pom.xml
├── Dockerfile
└── README.md
```

---

## API 参考

### 认证

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| `POST` | `/api/v1/auth/register` | 用户注册 | 无 |
| `POST` | `/api/v1/auth/login` | 用户登录，返回 JWT | 无 |
| `GET` | `/api/v1/auth/me` | 获取当前登录用户信息 | Bearer Token |

### 边缘设备

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| `POST` | `/api/v1/edge/telemetry` | 边缘节点遥测数据上报 | Bearer Token |

### 交通数据

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| `GET` | `/api/v1/traffic/predictions` | 获取交通流量预测 | Bearer Token |

### MaaS 开放接口

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| `GET` | `/api/v1/maas/congestion` | 查询拥堵状况 | `X-API-Key` |

### 报表

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| `GET` | `/api/v1/reports/traffic/export` | 导出交通报表（JSON / XLSX） | Bearer Token |

> 报表接口支持 `granularity` 和 `format` 查询参数。

---

## 配置参考

### 安全配置

| 环境变量 | 说明 | 示例 |
|----------|------|------|
| `JWT_SECRET` | JWT 签名密钥 | `my-super-secret-key` |
| `APP_CORS_ALLOWED_ORIGINS` | 允许的跨域来源 | `http://localhost:5173` |
| `APP_WS_ALLOW_QUERY_TOKEN` | 是否允许 WebSocket 通过 query 传递 Token | `true` |
| `INIT_ADMIN_USERNAME` | 初始管理员用户名 | `admin` |
| `INIT_ADMIN_PASSWORD` | 初始管理员密码 | `admin123` |

### 数据库配置

| 环境变量 | 说明 | 示例 |
|----------|------|------|
| `SPRING_DATASOURCE_URL` | 主数据源 JDBC URL | `jdbc:postgresql://localhost:5432/traffic` |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | `postgres` |
| `SPRING_FLYWAY_LOCATIONS` | Flyway 迁移脚本路径 | `classpath:db/migration` |
| `APP_DB_PRIMARY` | 主库类型 | `postgres` \| `mysql` |
| `APP_DB_MIRROR_WRITE` | 是否启用镜像双写 | `true` \| `false` |

### Redis 配置

| 环境变量 | 说明 | 示例 |
|----------|------|------|
| `SPRING_REDIS_HOST` | Redis 主机地址 | `localhost` |
| `SPRING_REDIS_PORT` | Redis 端口 | `6379` |
| `APP_REDIS_CACHE_ENABLED` | 是否启用 Redis 缓存 | `true` |
| `APP_CACHE_TTL_SECONDS` | 缓存 TTL（秒） | `300` |

---

## 本地开发

### 前置条件

- Java 17+
- Maven 3.8+
- PostgreSQL 16（或 MySQL 8.4）
- Redis 7

### 启动

```bash
cd backend
mvn -B spring-boot:run
```

服务默认监听 `8000` 端口。

### 构建

```bash
cd backend
mvn -B clean package -DskipTests
```

### Docker

```bash
cd backend
docker build -t smart-traffic-backend .
docker run -p 8000:8000 smart-traffic-backend
```

---

## 测试

```bash
cd backend
mvn -B test
```

---

## 双写灰度

后端支持 PostgreSQL + MySQL 双写灰度切换，用于数据库迁移过渡期。

### 切换主库

```bash
# 切换主库为 PostgreSQL
./scripts/db/switch_primary.sh postgres

# 切换主库为 MySQL
./scripts/db/switch_primary.sh mysql
```

### 一致性校验

```bash
bash scripts/check_mirror_consistency.sh
```

> 该脚本会对比两个数据库的核心表数据，输出差异报告。

---

## 报表导出示例

```bash
# 导出 XLSX 格式的小时粒度报表
curl -L -H "Authorization: Bearer <token>" \
  "http://localhost:8000/api/v1/reports/traffic/export?granularity=hourly&format=xlsx" \
  -o traffic_report.xlsx

# 导出 JSON 格式的日粒度报表
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8000/api/v1/reports/traffic/export?granularity=daily&format=json"
```

---

## 常见问题

| 问题 | 解决方案 |
|------|----------|
| `Unsupported Database: PostgreSQL 16.x` | 确认 `pom.xml` 中包含 `flyway-database-postgresql` 依赖 |
| 启动后接口返回 401 | 检查 `JWT_SECRET` 是否配置、Token 是否过期、请求头是否携带 `Authorization: Bearer <token>` |
| Redis 连接失败 | 确认 Redis 服务已启动，检查 `SPRING_REDIS_HOST` 和 `SPRING_REDIS_PORT` 配置 |
| Flyway 迁移失败 | 检查 `SPRING_FLYWAY_LOCATIONS` 是否指向正确的迁移目录，确保数据库已创建 |
| WebSocket 连接断开 | 确认 `APP_WS_ALLOW_QUERY_TOKEN` 设置正确，检查 CORS 和代理配置 |
