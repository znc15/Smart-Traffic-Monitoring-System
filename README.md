# 智能交通监控系统（Smart Transportation Monitoring System）

本项目是一个智能交通监控系统：

- 前端实时展示道路车流数据、视频帧与系统资源使用情况
- 后端已迁移为 **Spring Boot** 架构，并保持与前端兼容的 `/api/v1` 接口契约

## 一、技术架构

- **后端（当前）**：Spring Boot 3 + Spring Security + Spring WebSocket + Spring Data JPA
- **前端**：React + Vite
- **数据库**：PostgreSQL 16
- **接口基础路径**：`http://localhost:8000/api/v1`

> 说明：项目后端目录已统一为 `backend/`（Spring Boot）。

## 二、项目结构

```text
Smart-Traffic-Monitoring-System
├── backend/                   # 当前主后端（Spring Boot）
├── frontend/
├── docker-compose.yml
└── README.md
```

## 三、环境要求

- Java 17+
- Maven 3.9+
- Node.js 18+
- Docker / Docker Compose（可选）

## 四、使用 Docker 启动（推荐）

```bash
docker compose up --build
```

启动后服务地址：

- 后端 API：`http://localhost:8000`
- 前端：`http://localhost:5173`
- PostgreSQL：`localhost:5433`

停止服务：

```bash
docker compose down
```

## 五、本地启动（不使用 Docker）

### 1) 启动后端

```bash
cd backend
mvn clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

### 2) 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

## 六、后端环境变量

`backend/src/main/resources/application.yml` 支持以下变量：

- `SERVER_PORT`（默认：`8000`）
- `SPRING_DATASOURCE_URL`（默认：`jdbc:postgresql://localhost:5433/traffic_db`）
- `SPRING_DATASOURCE_USERNAME`（默认：`postgres`）
- `SPRING_DATASOURCE_PASSWORD`（默认：`password`）
- `JWT_SECRET`
- `ACCESS_TOKEN_EXPIRE_DAYS`（默认：`7`）
- `BASE_URL_API`（默认：`http://localhost:8000`）
- `TRAFFIC_ROADS`（逗号分隔的道路名称列表）

## 七、API 接口总览（`/api/v1`）

### 1) 认证接口（Auth）

- `POST /auth/register`（JSON：`username,email,phone_number,password`）
- `POST /auth/login`（`form-urlencoded`：`username=<邮箱>&password=...`）
- `GET /auth/me`（需要 JWT）

### 2) 用户接口（User）

- `PUT /users/profile`（需要 JWT）
- `PUT /users/password`（需要 JWT）

### 3) 交通接口（Traffic）

- `GET /roads_name`
- `GET /info/{road_name}`
- `GET /frames/{road_name}`（需要 JWT）
- `GET /frames_no_auth/{road_name}`
- `WS /ws/info/{road_name}`（需要 JWT）
- `WS /ws/frames/{road_name}`（需要 JWT）

### 4) 聊天接口（Chat）

- `POST /chat`（需要 JWT）
- `POST /chat_no_auth`
- `WS /ws/chat`（需要 JWT）

### 5) 聊天历史（Chat History）

- `POST /chat/messages`（需要 JWT）
- `GET /chat/messages`（需要 JWT）
- `DELETE /chat/messages`（需要 JWT）
- `DELETE /chat/messages/{message_id}`（需要 JWT）
- `GET /chat/messages/count`（需要 JWT）

### 6) 管理接口（Admin）

- `GET /admin/resources`（需要 JWT + 管理员角色）
- `WS /admin/ws/resources`（需要 JWT + 管理员角色）
- `GET /admin/cameras`（需要 JWT + 管理员角色）
- `POST /admin/cameras`（需要 JWT + 管理员角色）
- `PUT /admin/cameras/{id}`（需要 JWT + 管理员角色）
- `DELETE /admin/cameras/{id}`（需要 JWT + 管理员角色）
- `GET /admin/users`（需要 JWT + 管理员角色）
- `PUT /admin/users/{id}/role`（需要 JWT + 管理员角色）
- `PUT /admin/users/{id}/status`（需要 JWT + 管理员角色）

### 7) 站点设置（Site Settings）

- `GET /site-settings`（公开）
- `PUT /admin/site-settings`（需要 JWT + 管理员角色）

## 八、鉴权说明

- REST 请求：`Authorization: Bearer <token>`
- WebSocket：可通过 `Authorization` 头、`access_token` Cookie 或 `?token=` 传递
- 登录成功后，后端也会写入 `httpOnly` 的 `access_token` Cookie

## 九、快速调用示例

```bash
# 登录（form-urlencoded）
curl -X POST "http://localhost:8000/api/v1/auth/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user@example.com&password=your_password"
```

## 十、摄像头端 API 规范（Camera-Side API）

后端每 3 秒轮询每个已配置 `stream_url` 的摄像头。摄像头端需实现以下两个 HTTP 接口：

### 1) 获取交通数据

```
GET {stream_url}/api/traffic
Content-Type: application/json
```

返回 JSON：

```json
{
  "count_car": 8,
  "count_motor": 5,
  "speed_car": 45,
  "speed_motor": 35
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `count_car` | number | 当前画面中汽车数量 |
| `count_motor` | number | 当前画面中摩托车/电动车数量 |
| `speed_car` | number | 汽车平均速度（km/h） |
| `speed_motor` | number | 摩托车平均速度（km/h） |

### 2) 获取视频帧

```
GET {stream_url}/api/frame
Content-Type: image/jpeg
```

返回当前摄像头画面的 JPEG 图片（二进制）。

### 3) 配置说明

- 在管理面板「摄像头管理」中设置每个摄像头的「接入地址」（如 `http://192.168.1.100:8080`）
- 留空则使用模拟数据（随机生成）
- 超时设置：连接 2 秒，读取 2 秒
- 轮询间隔：3 秒
