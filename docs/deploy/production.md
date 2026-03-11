# 生产部署指南

这份文档聚焦两件事：
- 让当前仓库的配置方式讲清楚
- 区分“演示 Compose”与“正式生产拓扑”

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

## 2. 配置入口总览

| 文件 | 用途 |
|------|------|
| 根 `.env` | Compose 运行时变量，数据库密码、JWT、MaaS key 等 |
| `frontend/.env.production` | frontend 构建时的 `VITE_*` 变量 |
| `docker-compose.override.yml` | 给 backend 注入额外生产变量 |
| `backend/.env.example` | backend 完整配置参考 |
| `edge/.env.example` | edge 运行时配置参考 |

## 3. 先准备根 `.env`

```bash
cp .env.example .env
```

至少填写：

```env
POSTGRES_PASSWORD=replace-me
MYSQL_ROOT_PASSWORD=replace-me
MYSQL_PASSWORD=replace-me
JWT_SECRET=replace-with-random-32-char-secret
MAAS_API_KEY=replace-with-your-maas-api-key
```

可选但常用：

```env
APP_DB_PRIMARY=postgres
APP_DB_MIRROR_WRITE=false
APP_DB_MIRROR_MYSQL_ENABLED=false
APP_DB_MIRROR_POSTGRES_ENABLED=false
APP_REDIS_CACHE_ENABLED=true
APP_CACHE_TTL_SECONDS=10
```

## 4. 准备 frontend 构建变量

`frontend` 的 `VITE_*` 是 build-time config。

推荐同源部署：

```bash
cat > frontend/.env.production <<'EOF'
VITE_API_HTTP_BASE=
VITE_API_WS_BASE=
VITE_AMAP_KEY=your_amap_key
EOF
```

说明：
- 同源部署时把 `VITE_API_HTTP_BASE` / `VITE_API_WS_BASE` 留空即可
- 浏览器会自动走当前域名下的 `/api/...`
- 如果你修改了这个文件，必须重新构建 frontend 镜像

跨域部署时：

```env
VITE_API_HTTP_BASE=https://api.example.com
VITE_API_WS_BASE=wss://api.example.com
VITE_AMAP_KEY=your_amap_key
```

## 5. 给 backend 注入额外生产变量

根 `docker-compose.yml` 目前没有把所有 backend 生产变量都显式透传。
如果你需要配置 `APP_ENV`、`APP_CORS_ALLOWED_ORIGINS`、初始化管理员等，请创建
`docker-compose.override.yml`：

```yaml
services:
  backend:
    environment:
      APP_ENV: production
      APP_CORS_ALLOWED_ORIGINS: https://traffic.example.com
      APP_WS_ALLOW_QUERY_TOKEN: "false"
      BASE_URL_API: https://traffic.example.com
      TRAFFIC_ROADS: 陈兴道路,陈富路,阮惠路,黎利路,阮廌路
      INIT_ADMIN: "true"
      INIT_ADMIN_USERNAME: admin
      INIT_ADMIN_EMAIL: admin@example.com
      INIT_ADMIN_PASSWORD: Admin@12345
```

注意：
- `APP_ENV=production` 很重要，登录 Cookie 才会带 `Secure`
- `INIT_ADMIN_*` 只在系统里还没有任何用户时生效
- 初始化完成后，建议把 `INIT_ADMIN` 关闭

## 6. 构建与启动

```bash
docker compose build
docker compose up -d
docker compose ps
```

如果只重建前端入口：

```bash
docker compose build frontend gateway
docker compose up -d frontend gateway
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

edge 通常独立部署在边缘设备上，推荐最小配置：

```env
MODE=camera
CAMERA_URL=rtsp://user:pass@camera/stream
ROAD_NAME=人民路
EDGE_NODE_ID=edge-01
EDGE_API_KEY=replace-me
BACKEND_TELEMETRY_URL=https://traffic.example.com/api/v1/edge/telemetry
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
