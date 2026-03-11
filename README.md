# Smart Traffic Monitoring System

基于 `Vue 3 + Spring Boot + FastAPI Edge` 的端云协同智能交通监控平台。

项目包含 4 个核心模块：
- `frontend/`：Vue 3 管理端，默认开发端口 `5174`
- `backend/`：Spring Boot API 服务，默认端口 `8000`
- `edge/`：边缘推理节点，默认端口 `8000`
- `gateway/`：Nginx 统一入口，默认对外端口 `5173`

## 仓库结构

```text
Smart-Traffic-Monitoring-System/
├── backend/              # Spring Boot 后端
├── frontend/             # Vue 3 管理端
├── edge/                 # FastAPI 边缘节点
├── gateway/              # Nginx 统一入口
├── docs/                 # 部署与项目文档
├── scripts/              # 运维 / 门禁 / 性能脚本
├── docker-compose.yml    # 单机 Compose 编排
├── .env.example          # 根编排运行时变量示例
└── README.md
```

## 快速开始

### 方式一：Docker Compose 一键启动

适合演示环境、联调环境、单机部署。

1. 复制根环境变量模板：

```bash
cp .env.example .env
```

2. 至少修改以下敏感变量：

```env
POSTGRES_PASSWORD=change-me
MYSQL_ROOT_PASSWORD=change-me
MYSQL_PASSWORD=change-me
JWT_SECRET=replace-with-random-32-char-secret
MAAS_API_KEY=replace-with-your-api-key
```

3. 启动服务：

```bash
docker compose up --build -d
docker compose ps
```

4. 访问入口：

- 网关 / 前端：`http://localhost:5173`
- 后端 API：`http://localhost:8000`

5. 验收：

```bash
curl -I http://localhost:5173/
curl -I http://localhost:5173/react/
curl -I http://localhost:8000/api/v1/site-settings
```

说明：
- `frontend` 容器本身不直接暴露宿主机端口，推荐始终通过 `gateway` 访问。
- `backend:8000` 在当前 Compose 中直接暴露，便于调试与联调；正式生产建议只保留内网访问。

### 方式二：本地分模块开发

适合调试单个模块。

1. 启动数据库和缓存：

```bash
docker compose up -d database mysql redis
```

2. 启动后端：

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

3. 启动前端：

```bash
cd frontend
pnpm install
pnpm dev
```

4. 启动边缘节点（可选）：

```bash
cd edge
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python main.py --mode sim --port 8000 --no-browser
```

5. 本地门禁：

```bash
./scripts/local-gate.sh
```

## 配置文件矩阵

不同模块的配置入口不一样，这是生产环境最容易踩坑的地方。

| 文件 | 用途 | 何时生效 |
|:-----|:-----|:---------|
| `.env` | 根 `docker compose` 运行时变量 | `docker compose up/build` 时 |
| `backend/.env.example` | 后端完整配置参考 | 本地运行 backend、或扩展 Compose 时 |
| `frontend/.env.production` | 前端 `VITE_*` 构建变量 | `pnpm build` / Docker build 时 |
| `frontend/.env.example` | 前端变量说明 | 仅模板 |
| `edge/.env.example` | Edge 节点运行时变量参考 | 本地 / Docker / systemd 启动 edge 时 |

重要说明：
- `frontend` 的 `VITE_*` 是 build-time config（构建时注入），不是 runtime config（运行时注入）。
- 如果修改了 `frontend/.env.production`，必须重新执行 `pnpm build` 或重新构建前端镜像。
- 根 `.env` 只会影响 `docker-compose.yml` 里显式引用到的变量；额外 backend 变量需要用 `docker-compose.override.yml` 或自定义 Compose 文件注入。

## 生产环境要点

### 推荐拓扑

```text
Internet
  ↓
External Nginx / Traefik / SLB (HTTPS / TLS / WAF)
  ↓
gateway:80
  ├─ /            -> frontend:80
  ├─ /api/        -> backend:8000/api/
  └─ /api/v1/ws/  -> backend:8000/api/v1/ws/
```

### 前端推荐配置

同源部署（推荐）：
- 通过 `gateway` 同时提供前端页面、API 与 WebSocket。
- `frontend/.env.production` 中把 `VITE_API_HTTP_BASE` 和 `VITE_API_WS_BASE` 留空。
- 只需设置 `VITE_AMAP_KEY`。

跨域部署：

```env
VITE_API_HTTP_BASE=https://api.example.com
VITE_API_WS_BASE=wss://api.example.com
VITE_AMAP_KEY=your_amap_key
```

### 后端推荐配置

至少设置：
- `JWT_SECRET`
- `APP_ENV=production`
- 精确的 `APP_CORS_ALLOWED_ORIGINS`
- `APP_WS_ALLOW_QUERY_TOKEN=false`
- `APP_MAAS_DEFAULT_API_KEY`

注意：
- 这些变量默认并没有全部透传到根 `docker-compose.yml` 的 `backend` 容器。
- 如果你使用根 Compose 做生产部署，请参考 [`docs/deploy/production.md`](docs/deploy/production.md) 中的 `docker-compose.override.yml` 示例。

### Edge 推荐配置

生产部署 edge 时建议：
- `MODE=camera`
- 显式设置 `CAMERA_URL`
- 设置 `EDGE_NODE_ID`
- 配置 `BACKEND_TELEMETRY_URL`
- 配置 `EDGE_API_KEY`
- 无头环境设置 `NO_BROWSER=true`

## 常用验收命令

```bash
docker compose ps
curl -I http://localhost:5173/
curl -I http://localhost:5173/react/
curl http://localhost:8000/api/v1/site-settings
bash scripts/check_mirror_consistency.sh
```

数据库灰度期间可额外执行：

```bash
./scripts/db/switch_primary.sh postgres
./scripts/db/switch_primary.sh mysql
bash scripts/check_mirror_consistency.sh --since 2026-03-11T00:00:00
```

## 默认账号与初始化

系统默认不内置账号。

- 首个注册用户会自动成为管理员。
- 如果你希望首次启动时直接创建管理员，可设置：

```env
INIT_ADMIN=true
INIT_ADMIN_USERNAME=admin
INIT_ADMIN_EMAIL=admin@example.com
INIT_ADMIN_PASSWORD=Admin@12345
```

注意：
- 仅在系统内还没有用户时生效。
- 密码需要满足复杂度要求，建议启动成功后再关闭 `INIT_ADMIN`。

## 模块文档

| 模块 | 文档 |
|:-----|:-----|
| 后端 | [`backend/README.md`](backend/README.md) |
| 前端 | [`frontend/README.md`](frontend/README.md) |
| 边缘节点 | [`edge/README.md`](edge/README.md) |
| 网关 | [`gateway/README.md`](gateway/README.md) |
| 文档中心 | [`docs/README.md`](docs/README.md) |
| 脚本工具 | [`scripts/README.md`](scripts/README.md) |

## 部署文档

| 场景 | 文档 |
|:-----|:-----|
| 本地开发 | [`docs/deploy/local.md`](docs/deploy/local.md) |
| 单机 Docker | [`docs/deploy/docker.md`](docs/deploy/docker.md) |
| 生产部署 | [`docs/deploy/production.md`](docs/deploy/production.md) |

## 开源协议

本项目基于 [MIT License](LICENSE) 开源。
