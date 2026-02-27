# Smart Traffic Monitoring System

智能交通监控系统（Vue 单栈 + Spring Boot + Edge 推理）。

## 当前状态

- 前端仅保留 `frontend`，`/react` 已下线（返回 `404`）
- 仓库不再存放原始截图、CSV、XLSX 证据文件
- 默认支持 PostgreSQL 主库，MySQL/Redis 保留灰度能力

## 快速开始

```bash
docker compose up --build -d
docker compose ps
```

联调检查：

```bash
curl -I http://localhost:5173/
curl -I http://localhost:5173/react/
curl -I http://localhost:8000/api/v1/site-settings
```

预期：`/` 为 `200`，`/react/` 为 `404`。

## 默认账号密码（开发）

- 当前安全默认：**不内置默认账号密码**（`INIT_ADMIN=false`）。
- 若要在本地快速体验，可开启初始化管理员并使用以下开发默认值：

```bash
INIT_ADMIN=true
INIT_ADMIN_USERNAME=admin
INIT_ADMIN_EMAIL=admin@example.com
INIT_ADMIN_PASSWORD=Admin@12345
```

- 登录账号：`admin`
- 登录密码：`Admin@12345`

说明：仅用于本地开发演示，生产环境请务必更换并关闭初始化开关。

## 常用命令

```bash
# 本地门禁（构建 + 测试 + 联调）
./scripts/local-gate.sh

# 清理依赖与构建缓存
./scripts/clean-project.sh

# 停止服务
docker compose down
```

## 服务器部署配置（必改）

部署到服务器前，请至少完成下面两类配置。

### 1. 后端与容器环境变量（项目根目录 `.env`）

在项目根目录创建 `.env`（不要提交到仓库）：

```bash
# 数据库（按实际环境填写）
SPRING_DATASOURCE_URL=jdbc:postgresql://database:5432/transportation_system
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<strong-db-password>

# 安全
JWT_SECRET=<at-least-32-char-random-secret>
APP_CORS_ALLOWED_ORIGINS=https://your-domain.com,https://www.your-domain.com
APP_WS_ALLOW_QUERY_TOKEN=false

# 可选：首次初始化管理员（初始化完成后建议改回 false）
INIT_ADMIN=false
INIT_ADMIN_USERNAME=
INIT_ADMIN_EMAIL=
INIT_ADMIN_PASSWORD=

# 可选：MaaS 默认客户端
APP_MAAS_DEFAULT_CLIENT_NAME=default-prod-client
APP_MAAS_DEFAULT_API_KEY=<strong-api-key>
```

要点：
- `APP_CORS_ALLOWED_ORIGINS` 必须填写你的实际域名，不能保留 `localhost/127.0.0.1`。
- `JWT_SECRET` 必须是强随机字符串，不能使用默认值。
- 生产环境建议 `APP_WS_ALLOW_QUERY_TOKEN=false`。

### 2. 前端接口地址（`frontend/.env.production`）

前端是编译期注入接口地址，部署前需在 `frontend` 目录创建：

```bash
VITE_API_HTTP_BASE=https://your-domain.com
VITE_API_WS_BASE=wss://your-domain.com
```

如果你使用反向代理并把后端也挂在同域名下，这里同样填写该域名即可。

### 3. 启动与校验

```bash
docker compose up --build -d --remove-orphans
docker compose ps
curl -I https://your-domain.com/
curl -I https://your-domain.com/react/
```

预期：
- `/` 返回 `200`
- `/react/` 返回 `404`

## 项目结构

- `frontend`：Vue3 管理端与可视化
- `backend`：认证、管理、预测、MaaS、报表导出
- `edge`：检测、追踪、车道统计、事件识别、上报
- `gateway`：统一入口网关（`5173`）
- `docs`：部署、需求追踪、答辩模板文档

## 部署文档

- 本地部署：`docs/deploy/local.md`
- Docker 部署：`docs/deploy/docker.md`
- 生产部署：`docs/deploy/production.md`

## 模块文档

- 后端：`backend/README.md`
- 边缘端：`edge/README.md`
- 前端：`frontend/README.md`
