# Smart Traffic Monitoring System

基于 `Vue 3 + Spring Boot + FastAPI Edge` 的端云协同智能交通监控平台。

项目包含 5 个核心模块：
- `frontend/`：Vue 3 管理端，默认开发端口 `5174`
- `backend/`：Spring Boot API 服务，默认端口 `8000`
- `edge/`：边缘推理节点，默认端口 `8000`
- `gateway/`：Nginx 统一入口，默认对外端口 `5173`

**核心功能**：
- 交通态势概览：实时交通监控、流量趋势图表（3 秒轮询持久化）
- 实时状态推送：高德地图集成，监控节点可视化
- 历史数据统计：历史数据统计与导出
- AI 智能分析：多轮对话、道路下拉选择、预设推荐问题、SSE 流式响应、停止生成、消息复制/重新生成、对话重命名/清空、自动生成对话标题（可配置独立标题模型）、**Tool Calling 动态查询道路/摄像头/历史数据**
- 节点配置管理：站点设置、用户管理（独立页面）、API 密钥（独立页面）、AI 配置（含标题模型配置）
- Redis 增强：AI 对话/消息缓存、站点配置缓存、用户信息缓存、API 限流

## AI Tool Calling 架构

AI 智能分析模块支持 LLM Tool Calling，允许 AI 在对话中动态查询交通数据：

| 工具名 | 功能 | 后端依赖 |
|---|---|---|
| `query_traffic` | 查询指定道路的实时交通数据 | `TrafficService.info(roadName)` |
| `list_cameras` | 查询摄像头列表（含经纬度、道路名称） | `CameraRepository.findByEnabledTrue()` |
| `query_history` | 查询指定道路的历史统计数据 | `TrafficSampleRepository` |
| `reverse_geocode` | 根据经纬度返回最近的摄像头/道路 | `GeocodingService` |

数据流：

```
用户提问 → LLM 判断需要工具 → tool_call 事件 → 后端执行工具 → tool_result → LLM 生成回答
```

前端通过 SSE 接收 `tool_call` / `tool_result` 事件，显示"正在调用 xxx 工具..."指示器。

根 Docker Compose 一键启动范围包含：
- `gateway`
- `frontend`
- `backend`
- `edge-node`
- `database`
- `mysql`
- `redis`

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

2. 至少修改以下敏感变量和访问地址：

```env
GATEWAY_PUBLIC_BASE=http://localhost:5173
BACKEND_PUBLIC_HTTP_BASE=http://localhost:8000
BACKEND_PUBLIC_WS_BASE=ws://localhost:8000
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
- Edge 节点：`http://localhost:9000`

5. 验收：

```bash
curl -I http://localhost:5173/
curl -I http://localhost:5173/react/
curl -I http://localhost:8000/api/v1/site-settings
curl http://localhost:9000/health
```

说明：
- `frontend` 容器本身不直接暴露宿主机端口，推荐始终通过 `gateway` 访问。
- `backend:8000` 在当前 Compose 中直接暴露，便于调试与联调；正式生产建议只保留内网访问。
- `edge-node` 容器内部仍监听 `8000`，根 Compose 默认映射到宿主机 `9000`，避免和 backend 冲突。
- 前端构建时会直接读取根 `.env` 中的 `BACKEND_PUBLIC_HTTP_BASE / BACKEND_PUBLIC_WS_BASE`。

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

## 配置文件矩阵

不同模块的配置入口不一样，这是生产环境最容易踩坑的地方。

| 文件 | 用途 | 何时生效 |
|:-----|:-----|:---------|
| `.env` | 根 `docker compose` 主配置，含前后端 IP / 地址与 edge 运行变量 | `docker compose up/build` 时 |
| `backend/.env.example` | 后端完整配置参考 | 本地运行 backend、或扩展 Compose 时 |
| `frontend/.env.production` | 前端可选覆盖文件 | 单独构建 frontend 时 |
| `frontend/.env.example` | 前端变量说明 | 仅模板 |
| `edge/.env.example` | Edge 节点运行时变量参考 | 单独部署 edge、本地 / systemd 启动 edge 时 |

重要说明：
- `frontend` 的 `VITE_*` 是 build-time config（构建时注入），不是 runtime config（运行时注入）。
- 主站 Docker 一键启动时，frontend 默认从根 `.env` 的 `BACKEND_PUBLIC_HTTP_BASE / BACKEND_PUBLIC_WS_BASE` 取地址。
- 如果修改了这些地址，必须重新执行 `docker compose build frontend gateway` 或重新构建前端镜像。
- `VITE_AMAP_KEY` 现在只作为部署 fallback；管理员可在“节点配置管理 → 站点设置”里覆盖高德地图 Key，刷新 `/map` 即可生效，无需重建 frontend。
- `VITE_AMAP_SECURITY_JS_CODE` / `VITE_AMAP_SERVICE_HOST` 是可选的高德安全增强配置，不进入后台站点设置。
- 如果同时配置了 `VITE_AMAP_SERVICE_HOST` 和 `VITE_AMAP_SECURITY_JS_CODE`，前者优先生效。

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

主站一键启动默认使用根 `.env` 中的公共地址变量：

```env
GATEWAY_PUBLIC_BASE=http://192.168.1.10:5173
BACKEND_PUBLIC_HTTP_BASE=http://192.168.1.11:8000
BACKEND_PUBLIC_WS_BASE=ws://192.168.1.11:8000
```

这表示：
- 浏览器访问入口是 `GATEWAY_PUBLIC_BASE`
- 前端打包进去的 API / WS 地址分别来自 `BACKEND_PUBLIC_HTTP_BASE / BACKEND_PUBLIC_WS_BASE`
- backend 会默认把 `APP_CORS_ALLOWED_ORIGINS` 对齐到 `GATEWAY_PUBLIC_BASE`
- backend 会默认把 `BASE_URL_API` 对齐到 `BACKEND_PUBLIC_HTTP_BASE`

如果你单独构建 frontend，而不是走根 Compose，才需要维护 `frontend/.env.production`。

### IP 部署示例

前后端分开 IP：

```env
GATEWAY_PUBLIC_BASE=http://192.168.1.10:5173
BACKEND_PUBLIC_HTTP_BASE=http://192.168.1.11:8000
BACKEND_PUBLIC_WS_BASE=ws://192.168.1.11:8000
```

### 后端推荐配置

至少设置：
- `JWT_SECRET`
- `APP_ENV=production`
- 精确的 `APP_CORS_ALLOWED_ORIGINS`
- `APP_WS_ALLOW_QUERY_TOKEN=false`
- `APP_MAAS_DEFAULT_API_KEY`

注意：
- 这些常见生产变量现在可以直接通过根 `.env` 透传到 `backend` 容器。
- 如果你不显式设置，Compose 会自动使用：
  - `APP_CORS_ALLOWED_ORIGINS = GATEWAY_PUBLIC_BASE`
  - `BASE_URL_API = BACKEND_PUBLIC_HTTP_BASE`
- 如需更细的环境差异化覆盖，再考虑 `docker-compose.override.yml`。

### Edge 推荐配置

生产部署 edge 时建议：
- 在根 `.env` 中设置 `EDGE_MODE=camera`
- 显式设置 `EDGE_CAMERA_URL`
- 设置 `EDGE_NODE_ID`
- 配置 `EDGE_API_KEY`
- 如需主动遥测，配置 `EDGE_BACKEND_TELEMETRY_URL=http://backend:8000/api/v1/edge/telemetry`
- 无头环境保持 `EDGE_NO_BROWSER=true`

说明：
- backend 容器访问根 Compose 内的 edge 时可使用 `http://edge-node:8000`
- 宿主机 / 浏览器访问 edge 默认使用 `http://localhost:9000`
- 主动遥测鉴权会校验后台摄像头配置中的 `edge_node_id` 和 `node_api_key`，两者需要与 `EDGE_NODE_ID / EDGE_API_KEY` 一致。

## 常用验收命令

```bash
docker compose ps
docker compose build frontend gateway backend edge-node
docker compose up -d
curl -I http://localhost:5173/
curl -I http://localhost:5173/react/
curl http://localhost:8000/api/v1/site-settings
curl http://localhost:9000/health
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

## 修改 IP 后如何重建

当你修改了根 `.env` 中的这些地址：
- `GATEWAY_PUBLIC_BASE`
- `BACKEND_PUBLIC_HTTP_BASE`
- `BACKEND_PUBLIC_WS_BASE`

请执行：

```bash
docker compose build frontend gateway backend edge-node
docker compose up -d
```

原因：
- backend 读取的是 runtime 变量
- frontend 读取的是 build-time 变量，需要重新 build 才会进入最终 `dist/`

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
