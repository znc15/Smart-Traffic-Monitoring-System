# Smart Traffic Gateway

> Nginx 反向代理网关 —— 统一入口，负责请求路由、WebSocket 透传与服务转发。

---

## 概述

Gateway 是整个 Smart Traffic Monitoring System 的统一流量入口，基于 **Nginx 1.27 (Alpine)** 构建。所有外部请求经由网关分发到对应的后端服务，同时处理请求头透传、WebSocket 升级等。

| 项目 | 说明 |
|------|------|
| **基础镜像** | `nginx:1.27-alpine` |
| **对外端口** | `5173` → 容器内 `80` |
| **主要职责** | 反向代理、路由分发、请求头透传 |

---

## 架构说明

```
客户端 (Browser)
    │
    ▼  :5173
┌──────────┐
│  Gateway  │  Nginx 反向代理
│  (Nginx)  │
└────┬─────┘
     │
     ├── /           → frontend:80    (Vue 前端)
     ├── /react      → 404            (已下线)
     └── /react/     → 404            (已下线)
```

- 所有根路径请求转发到 `frontend` 服务
- `/react` 相关路径显式返回 404（React 版本已下线）
- 透传 `X-Real-IP`、`X-Forwarded-For`、`X-Forwarded-Proto` 头

---

## 文件说明

```
gateway/
├── nginx.conf      # Nginx 主配置文件
├── Dockerfile      # Docker 构建文件
└── .dockerignore   # 构建忽略规则
```

| 文件 | 说明 |
|------|------|
| `nginx.conf` | 定义 upstream、路由规则和代理配置 |
| `Dockerfile` | 基于 `nginx:1.27-alpine`，拷入配置文件 |
| `.dockerignore` | 排除构建无关文件 |

---

## 配置说明

### nginx.conf

```nginx
upstream frontend {
    server frontend:80;
}

server {
    listen 80;
    server_name _;

    # React 版本已下线，显式返回 404
    location = /react { return 404; }
    location /react/  { return 404; }

    # 所有请求转发到前端服务
    location / {
        proxy_pass http://frontend;
        proxy_http_version 1.1;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 关键配置项

| 配置 | 值 | 说明 |
|------|----|------|
| `listen` | `80` | 容器内监听端口 |
| `server_name` | `_` | 匹配所有域名 |
| `proxy_http_version` | `1.1` | 支持 WebSocket 升级所需 |
| `X-Real-IP` | `$remote_addr` | 透传客户端真实 IP |
| `X-Forwarded-For` | `$proxy_add_x_forwarded_for` | 透传代理链 IP |
| `X-Forwarded-Proto` | `$scheme` | 透传原始协议（http/https） |

---

## 路由规则

| 路径 | 目标 | 状态码 | 说明 |
|------|------|--------|------|
| `/` | `frontend:80` | 200 | Vue 前端应用 |
| `/react` | — | 404 | React 版本已下线 |
| `/react/*` | — | 404 | React 子路径全部拦截 |

---

## Docker 构建与运行

### 构建镜像

```bash
cd gateway
docker build -t smart-traffic-gateway .
```

### 独立运行

```bash
docker run -d \
  --name gateway \
  -p 5173:80 \
  smart-traffic-gateway
```

### 通过 Docker Compose 启动

在项目根目录中，Gateway 作为服务编排的一部分自动启动：

```bash
docker compose up -d gateway
```

> 默认映射 `5173:80`，浏览器访问 `http://localhost:5173` 即可进入系统。

---

## 常见问题

| 问题 | 解决方案 |
|------|----------|
| 502 Bad Gateway | 确认 `frontend` 服务已启动且网络互通（Docker 网络中服务名解析正常） |
| WebSocket 连接失败 | 确认 `proxy_http_version` 设置为 `1.1`，并配置了 `Upgrade` 和 `Connection` 头 |
| `/react` 路径未返回 404 | 检查 `nginx.conf` 中 `location = /react` 是否存在精确匹配规则 |
