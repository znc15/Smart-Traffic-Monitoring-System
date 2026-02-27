# Smart Traffic Monitoring System

智能交通监控系统，当前采用 Vue 单栈前端。

## 当前入口

- `http://localhost:5173/`：Vue 控制台
- `http://localhost:8000/`：后端 API
- `localhost:5433`：PostgreSQL
- `localhost:3307`：MySQL
- `localhost:6380`：Redis

## 架构概览

- `edge`：YOLOv8 + OpenVINO + ByteTrack（默认）实时检测与追踪
- `backend`：Spring Boot（鉴权、管理、预测、MaaS、报表导出、双写灰度）
- `frontend-vue`：Vue3 + ECharts，唯一前端入口
- `gateway`：Nginx 路由层，统一 `5173` 端口

## 一键启动

```bash
docker compose up --build -d
```

检查状态：

```bash
docker compose ps
```

期望：`backend`、`frontend-vue`、`gateway`、`database`、`mysql`、`redis` 均为 `healthy`。

## 本地门禁

```bash
./scripts/local-gate.sh
```

顺序：

1. `frontend-vue`：`pnpm build`
2. `backend`：`mvn -B test`
3. `edge`：`python3 -m py_compile edge/*.py && pytest -q edge/tests`
4. `docker compose up --build -d`

## 报表导出接口

```text
GET /api/v1/reports/traffic/export
  ?granularity=hourly|daily|weekly|monthly
  &road_name=<optional>
  &start_at=<ISO8601 optional>
  &end_at=<ISO8601 optional>
  &format=json|xlsx
```

认证：`Authorization: Bearer <token>` 或登录 Cookie。

## MySQL/Redis 双写灰度切换

```bash
./scripts/db/switch_primary.sh postgres
./scripts/db/switch_primary.sh mysql
```

一致性校验：

```bash
bash scripts/check_mirror_consistency.sh
bash scripts/check_mirror_consistency.sh --all
bash scripts/check_mirror_consistency.sh --since 2026-02-27T03:00:00
```

## 详细部署教程

- 本地部署：`docs/deploy/local.md`
- Docker 部署：`docs/deploy/docker.md`
- 生产部署：`docs/deploy/production.md`

## 子模块 README

- 后端：`backend/README.md`
- 边缘端：`edge/README.md`
- Vue 前端：`frontend-vue/README.md`
