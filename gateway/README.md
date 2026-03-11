# Smart Traffic Gateway

Nginx 统一入口，负责前端入口、API 反向代理和 WebSocket 透传。

## 当前职责

| 能力 | 说明 |
|------|------|
| 前端入口 | `/` -> `frontend:80` |
| API 反向代理 | `/api/` -> `backend:8000/api/` |
| WebSocket 透传 | `/api/v1/ws/` -> `backend:8000/api/v1/ws/` |
| 历史路径封禁 | `/react`、`/react/` 返回 `404` |

## 路由规则

| 路径 | 上游 | 说明 |
|------|------|------|
| `/` | `frontend:80` | Vue 前端 |
| `/api/` | `backend:8000/api/` | REST API |
| `/api/v1/ws/` | `backend:8000/api/v1/ws/` | WebSocket |
| `/react` | 无 | 显式返回 `404` |
| `/react/` | 无 | 显式返回 `404` |

`gateway/nginx.conf` 同时会透传：
- `Host`
- `X-Real-IP`
- `X-Forwarded-For`
- `X-Forwarded-Proto`

## 运行方式

推荐只通过仓库根 `docker compose` 使用：

```bash
docker compose up -d gateway
```

访问：
- `http://localhost:5173`

### 为什么不推荐单独 `docker run`

当前 `nginx.conf` 依赖以下上游名称：
- `frontend`
- `backend`

如果你只单独启动 `gateway` 容器，这两个 upstream 名称默认不会存在，
Nginx 会直接返回 502。

只有在以下条件满足时，才适合独立运行：
- `gateway`、`frontend`、`backend` 在同一个 Docker network
- 该 network 内可以解析到 `frontend` / `backend` 这两个服务名

## Docker 构建

```bash
cd gateway
docker build -t smart-traffic-gateway .
```

## 生产环境建议

- 当前 gateway 不负责 TLS
- 正式生产建议在它前面再放一层外部 Nginx / Traefik / SLB 做 HTTPS termination
- backend 在根 Compose 中直接暴露 `8000` 只是为了演示和联调，生产建议不要直接对公网开放

## 健康检查说明

根 Compose 中 `gateway` 的 healthcheck 只是检查 Nginx 主进程是否存在。

这不代表：
- upstream 一定可达
- frontend / backend 一定可用

上线验收仍建议执行：

```bash
curl -I http://localhost:5173/
curl -I http://localhost:5173/react/
curl http://localhost:8000/api/v1/site-settings
```

## 常见问题

| 问题 | 解决方案 |
|------|----------|
| 502 Bad Gateway | 检查 `frontend` / `backend` 是否在同一网络且服务名可解析 |
| WebSocket 失败 | 检查 `/api/v1/ws/` 代理是否命中，确认浏览器连接的是 gateway 域名 |
| `/react` 未返回 404 | 检查 `nginx.conf` 中精确匹配规则是否还在 |
