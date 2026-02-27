# 智能交通监控系统（Smart Traffic Monitoring System）

本项目是一个三端协同的智能交通监控系统：

- `edge`：边缘节点，基于 YOLOv8 + OpenVINO 做实时车辆检测与画面输出
- `backend`：Spring Boot 聚合服务，负责鉴权、管理端 API、WebSocket 推送
- `frontend`：React 管理与监控界面，Docker 生产镜像使用 Nginx 托管静态资源

## 技术栈

- 边缘端：Python 3.10+, FastAPI, OpenCV, Ultralytics YOLOv8
- 后端：Spring Boot 3.4, Spring Security, Spring Data JPA, Flyway, PostgreSQL
- 前端：React + TypeScript + Vite + pnpm
- 交付：Docker Compose, GitHub Actions CI

## 项目结构

```text
Smart-Traffic-Monitoring-System
├── backend/                  # Spring Boot 后端
├── edge/                     # 边缘节点
├── frontend/                 # React 前端
├── docs/                     # 需求矩阵、部署文档、报告
├── scripts/                  # 本地门禁脚本
├── docker-compose.yml
└── README.md
```

关键文档入口：

- `docs/requirements/traceability.md`
- `docs/reports/gap-baseline.md`
- `docs/deploy/local.md`
- `docs/deploy/docker.md`
- `docs/deploy/production.md`
- `backend/README.md`

## 快速启动（Docker）

### 1. 一键构建并启动

```bash
docker compose up --build
```

启动后默认地址：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8000`
- 数据库：`localhost:5433`

### 2. 停止

```bash
docker compose down
```

### 3. 网络不稳定时（Maven 镜像可配置）

后端构建已支持镜像仓库与重试，默认使用阿里云公共仓库。可按需覆盖：

```bash
MAVEN_MIRROR_URL=https://repo.maven.apache.org/maven2 docker compose build backend
```

可用构建参数（`docker-compose.yml` 中已透传）：

- `MAVEN_BASE_IMAGE`
- `JRE_BASE_IMAGE`
- `MAVEN_MIRROR_URL`
- `MAVEN_MIRROR_ID`
- `NODE_BASE_IMAGE`
- `NGINX_BASE_IMAGE`

## 详细部署教程

本章节按“先可用、再收敛安全”的顺序，给出一套可执行的部署流程。

### 方案 A：单机 Docker Compose（推荐）

适用于 1 台服务器同时部署 `frontend + backend + postgres`。

#### 第 1 步：准备服务器

1. 安装 Docker Engine 与 Docker Compose 插件。
2. 确保防火墙放行端口：`5173`、`8000`（`5433` 仅内网需要时开放）。
3. 拉取代码并进入项目目录：

```bash
git clone <your-repo-url>
cd Smart-Traffic-Monitoring-System
```

#### 第 2 步：配置后端生产参数

编辑 `docker-compose.yml` 的 `backend.environment`，至少配置以下项：

- `APP_ENV=production`
- `JWT_SECRET=<至少 32 位强随机字符串>`
- `APP_CORS_ALLOWED_ORIGINS=<你的前端域名，多个用逗号分隔>`
- `APP_WS_ALLOW_QUERY_TOKEN=false`（如需兼容旧前端可临时设为 `true`）

如果你需要首次自动初始化管理员，再额外配置：

- `INIT_ADMIN=true`
- `INIT_ADMIN_USERNAME=<管理员用户名>`
- `INIT_ADMIN_EMAIL=<管理员邮箱>`
- `INIT_ADMIN_PASSWORD=<强密码，至少 8 位且含大小写/数字/特殊字符>`

#### 第 3 步：构建并启动

```bash
docker compose up -d --build
```

查看启动日志：

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

#### 第 4 步：部署验收

1. 检查容器状态：

```bash
docker compose ps
```

2. 检查后端公开接口：

```bash
curl http://localhost:8000/api/v1/site-settings
curl http://localhost:8000/api/v1/roads_name
```

3. 检查前端页面：

访问 `http://<服务器IP>:5173`，确认页面可打开。

#### 第 5 步：首次上线后的安全收敛

如果第 2 步开启了管理员初始化，首次登录完成后请把 `INIT_ADMIN` 改回 `false`，然后重启后端：

```bash
docker compose up -d backend
```

### 方案 B：中心服务 + 多边缘节点

适用于后端集中部署、多个路口各自部署 `edge` 节点。

1. 在中心服务器按“方案 A”部署 `frontend + backend + postgres`。
2. 在每个路口设备单独部署 `edge`（见 [edge/README.md](edge/README.md) 的生产部署章节）。
3. 在管理端「摄像头管理」为每个摄像头配置：
   - `stream_url`：`http://<edge-ip>:<edge-port>`
   - `road_name`：对应路段名称
4. 确保中心后端可访问所有边缘节点端口。

### 运维与升级

#### 日常升级

```bash
git pull
docker compose up -d --build
```

#### 数据备份（PostgreSQL）

```bash
docker exec database pg_dump -U postgres transportation_system > backup_$(date +%F).sql
```

#### 数据恢复（PostgreSQL）

```bash
cat backup_2026-02-26.sql | docker exec -i database psql -U postgres -d transportation_system
```

### 常见部署问题

1. Maven 依赖下载慢或失败：使用镜像参数

```bash
MAVEN_MIRROR_URL=https://repo.maven.apache.org/maven2 docker compose build backend
```

2. 浏览器报 CORS：检查 `APP_CORS_ALLOWED_ORIGINS` 是否包含当前前端地址。
3. WebSocket 认证失败：先确认 Cookie/Header 传递是否正常，再检查 `APP_WS_ALLOW_QUERY_TOKEN` 开关。

## 本地开发启动

### 1. Backend

```bash
cd backend
mvn clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

### 2. Frontend

```bash
cd frontend
pnpm install
pnpm dev
```

### 3. Edge（独立运行）

```bash
cd edge
pip install -r requirements.txt
python main.py --port 9000
```

建议把边缘节点运行在与后端不同端口（例如 `9000`），避免本机端口冲突。

详细说明见：

- [edge/README.md](edge/README.md)
- [edge/samples/README.md](edge/samples/README.md)

## 后端关键配置

配置来源：`backend/src/main/resources/application.yml`，也可参考 `backend/.env.example`。

核心环境变量：

- `APP_ENV`：`development` / `production`
- `JWT_SECRET`：至少 32 字符，非开发环境禁止默认值
- `APP_CORS_ALLOWED_ORIGINS`：CORS 白名单，默认 `http://localhost:5173`
- `APP_WS_ALLOW_QUERY_TOKEN`：WebSocket 是否允许 `?token=` 兼容模式
- `INIT_ADMIN`：是否启用初始化管理员（默认关闭）
- `INIT_ADMIN_USERNAME`
- `INIT_ADMIN_EMAIL`
- `INIT_ADMIN_PASSWORD`

### 初始化管理员策略

默认不再创建弱口令管理员。仅在 `INIT_ADMIN=true` 且提供完整 `INIT_ADMIN_*` 时初始化管理员，并校验密码复杂度。

## 鉴权与安全约定

- HTTP：仅支持
  - `Authorization: Bearer <token>`
  - 或 `access_token` HttpOnly Cookie
- WebSocket：支持 Header/Cookie；`?token=` 仅在 `APP_WS_ALLOW_QUERY_TOKEN=true` 时兼容
- 登录接口会写入 `HttpOnly` Cookie，`sameSite=Lax`，`secure` 按环境自动收紧

## API 基础路径与字段规范

- 后端 API 前缀：`/api/v1`
- 响应字段命名：`snake_case`（已统一）

交通状态字段：

- `density_status`: `clear | busy | congested | offline`
- `speed_status`: `slow | fast | unknown`

## 主要接口一览

### 认证

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`（`application/x-www-form-urlencoded`）
- `GET /api/v1/auth/me`

### 交通

- `GET /api/v1/roads_name`
- `GET /api/v1/info/{roadName}`
- `GET /api/v1/frames/{roadName}`（需鉴权）
- `GET /api/v1/frames_no_auth/{roadName}`
- `WS /api/v1/ws/info/{roadName}`
- `WS /api/v1/ws/frames/{roadName}`

### 管理

- `GET /api/v1/admin/resources`
- `GET /api/v1/admin/nodes`
- `GET /api/v1/admin/cameras`
- `POST /api/v1/admin/cameras`
- `PUT /api/v1/admin/cameras/{id}`
- `DELETE /api/v1/admin/cameras/{id}`
- `GET /api/v1/admin/users`
- `PUT /api/v1/admin/users/{id}/role`
- `PUT /api/v1/admin/users/{id}/status`
- `PUT /api/v1/admin/site-settings`
- `WS /api/v1/admin/ws/resources`

## 质量门禁（本地）

按以下顺序执行：

```bash
./scripts/local-gate.sh
```

## CI

仓库已提供 GitHub Actions：`.github/workflows/ci.yml`，自动执行：

- 前端：`pnpm lint` + `pnpm build`
- 后端：`mvn test`
- 边缘端：`py_compile` + `pytest edge/tests`
