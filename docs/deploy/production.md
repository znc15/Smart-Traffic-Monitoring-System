# 生产部署指南

这份文档聚焦两件事：
- 让当前仓库的主站 Docker 一键启动方式讲清楚
- 把前后端 IP / 地址统一收口到根 `.env`

## 1. 推荐拓扑

```text
Internet
  ↓
External Nginx / Traefik / SLB
  - HTTPS / TLS termination
  - WAF / 限流 / 访问控制
  ↓
gateway:80
  ├─ /            -> frontend:80
  ├─ /api/        -> backend:8000/api/
  └─ /api/v1/ws/  -> backend:8000/api/v1/ws/

backend -> postgres / mysql / redis
edge    -> backend (/api/v1/edge/telemetry)
```

说明：
- 根 `docker-compose.yml` 目前把 `backend:8000` 暴露到宿主机，便于演示和联调。
- 正式生产建议把 backend 只留在内网，或者至少限制为 `127.0.0.1` / 私网访问。
- 根 Compose 的一键启动范围只包含主站，不包含 edge。

## 2. 配置入口总览

| 文件 | 用途 |
|------|------|
| 根 `.env` | 主站 Docker 一键启动主配置，含前后端 IP / 地址 |
| `frontend/.env.production` | frontend 可选覆盖文件，单独构建时使用 |
| `docker-compose.override.yml` | 高级覆盖手段，不是常见生产变量必需项 |
| `backend/.env.example` | backend 完整配置参考 |
| `edge/.env.example` | edge 运行时配置参考 |

## 3. 先准备根 `.env`

```bash
cp .env.example .env
```

至少填写：

```env
GATEWAY_PUBLIC_BASE=http://localhost:5173
BACKEND_PUBLIC_HTTP_BASE=http://localhost:8000
BACKEND_PUBLIC_WS_BASE=ws://localhost:8000
POSTGRES_PASSWORD=replace-me
MYSQL_ROOT_PASSWORD=replace-me
MYSQL_PASSWORD=replace-me
JWT_SECRET=replace-with-random-32-char-secret
MAAS_API_KEY=replace-with-your-maas-api-key
```

可选但常用：

```env
APP_ENV=production
APP_WS_ALLOW_QUERY_TOKEN=false
APP_MAAS_DEFAULT_CLIENT_NAME=default-dev-client
TRAFFIC_ROADS=陈兴道路,陈富路,阮惠路,黎利路,阮廌路
INIT_ADMIN=false
APP_DB_PRIMARY=postgres
APP_DB_MIRROR_WRITE=false
APP_DB_MIRROR_MYSQL_ENABLED=false
APP_DB_MIRROR_POSTGRES_ENABLED=false
APP_REDIS_CACHE_ENABLED=true
APP_CACHE_TTL_SECONDS=10
```

## 4. 配置前后端 IP

前后端分开 IP 的推荐写法：

```env
GATEWAY_PUBLIC_BASE=http://192.168.1.10:5173
BACKEND_PUBLIC_HTTP_BASE=http://192.168.1.11:8000
BACKEND_PUBLIC_WS_BASE=ws://192.168.1.11:8000
```

说明：
- 浏览器入口使用 `GATEWAY_PUBLIC_BASE`
- frontend 在 Docker build 时会自动把 `BACKEND_PUBLIC_HTTP_BASE / BACKEND_PUBLIC_WS_BASE` 写进最终构建产物
- backend 会默认把 `APP_CORS_ALLOWED_ORIGINS` 对齐到 `GATEWAY_PUBLIC_BASE`
- backend 会默认把 `BASE_URL_API` 对齐到 `BACKEND_PUBLIC_HTTP_BASE`

如果你单独构建 frontend，而不是通过根 Compose，才需要手动维护 `frontend/.env.production`。

## 5. backend 生产变量

现在这些常见变量已经可以直接通过根 `.env` 透传：

- `APP_ENV`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_WS_ALLOW_QUERY_TOKEN`
- `BASE_URL_API`
- `TRAFFIC_ROADS`
- `INIT_ADMIN`
- `INIT_ADMIN_USERNAME`
- `INIT_ADMIN_EMAIL`
- `INIT_ADMIN_PASSWORD`
- `APP_MAAS_DEFAULT_CLIENT_NAME`

注意：
- `APP_ENV=production` 很重要，登录 Cookie 才会带 `Secure`
- `INIT_ADMIN_*` 只在系统里还没有任何用户时生效
- 初始化完成后，建议把 `INIT_ADMIN` 关闭
- 如果你需要极少数环境专属覆盖，再使用 `docker-compose.override.yml`

## 6. 构建与启动

```bash
docker compose build frontend gateway backend
docker compose up -d
docker compose ps
```

如果只修改了前后端 IP / 地址，也仍然建议至少重建 frontend、gateway、backend：

```bash
docker compose build frontend gateway backend
docker compose up -d
```

## 7. 上线验收

```bash
curl -I http://127.0.0.1:5173/
curl -I http://127.0.0.1:5173/react/
curl http://127.0.0.1:8000/api/v1/site-settings
curl http://127.0.0.1:8000/api/v1/roads_name
docker compose ps
```

预期：
- `/` 返回 `200`
- `/react/` 返回 `404`
- `backend`、`frontend`、`gateway`、`database`、`mysql`、`redis` 都处于 `healthy`

说明：
- `frontend` / `gateway` 的 healthcheck 只检查 Nginx 进程存在，不代表 upstream 一定可用
- 验收时仍然要实际跑 `curl`

## 8. 数据库灰度发布

当前仓库已自动化的只有两步：

阶段 A：PostgreSQL 主库，MySQL 镜像双写

```bash
./scripts/db/switch_primary.sh postgres
```

阶段 B：MySQL 主库，PostgreSQL 镜像双写

```bash
./scripts/db/switch_primary.sh mysql
```

一致性检查：

```bash
bash scripts/check_mirror_consistency.sh
bash scripts/check_mirror_consistency.sh --since 2026-03-11T00:00:00
bash scripts/check_mirror_consistency.sh --all
```

阶段 C：结束灰度并关闭镜像写

当前仓库没有单独脚本，需要手动把 backend 环境变量改回：

```yaml
services:
  backend:
    environment:
      APP_DB_MIRROR_WRITE: "false"
      APP_DB_MIRROR_MYSQL_ENABLED: "false"
      APP_DB_MIRROR_POSTGRES_ENABLED: "false"
```

然后执行：

```bash
docker compose up -d backend
```

## 9. Edge 生产配置建议

edge 不在主站一键启动范围内，通常独立部署在边缘设备上。

推荐最小配置：

```env
MODE=camera
CAMERA_URL=rtsp://user:pass@camera/stream
ROAD_NAME=人民路
EDGE_NODE_ID=edge-01
EDGE_API_KEY=replace-me
BACKEND_TELEMETRY_URL=http://192.168.1.11:8000/api/v1/edge/telemetry
NO_BROWSER=true
```

弱 CPU 工业机可考虑：

```env
IMGSZ=160
FRAME_SKIP=4
QUANTIZE=int8
OPENVINO=true
MAX_MJPEG_CLIENTS=1
UVICORN_WORKERS=1
```

## 10. 备份与恢复

PostgreSQL 备份：

```bash
docker exec database pg_dump -U postgres transportation_system > backup_$(date +%F).sql
```

PostgreSQL 恢复：

```bash
cat backup_YYYY-MM-DD.sql | docker exec -i database psql -U postgres -d transportation_system
```

## 11. 回滚策略

应用回滚：
- 当前仓库默认是本地 `build:`，不是固定镜像 tag 发布流
- 回滚时建议切回上一个 Git commit / tag，再重新 `docker compose build && docker compose up -d`

数据库回滚：

```bash
./scripts/db/switch_primary.sh postgres
```

外层入口回滚：
- 如果用了外层 Nginx / Traefik / SLB，请在外层反向代理做切流
