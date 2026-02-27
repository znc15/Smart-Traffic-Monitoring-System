# Smart Traffic Monitoring System

智能交通监控系统，包含边缘检测（`edge`）、云端服务（`backend`）、Vue 默认前端与 React 回滚前端（`frontend-vue` / `frontend`）。

## 当前默认入口

- `http://localhost:5173/`：Vue 控制台（默认）
- `http://localhost:5173/react/`：React 回滚入口（保留一个迭代）
- `http://localhost:8000/`：后端 API
- `localhost:5433`：PostgreSQL
- `localhost:3307`：MySQL
- `localhost:6380`：Redis

## 架构概览

- `edge`：YOLOv8 + OpenVINO + ByteTrack（默认）实时检测与追踪。
- `backend`：Spring Boot（鉴权、管理、预测、MaaS、报表导出、双写灰度）。
- `frontend-vue`：Vue3 + ECharts，默认入口。
- `frontend`：React 历史版本，通过 `/react/` 路由保留回滚能力。
- `gateway`：Nginx 路由层，统一 `5173` 端口。

## 一键启动

```bash
docker compose up --build -d
```

检查状态：

```bash
docker compose ps
```

期望：`backend`、`frontend-vue`、`frontend-react`、`gateway`、`database`、`mysql`、`redis` 全部 `healthy`。

停止：

```bash
docker compose down
```

## 本地门禁命令

```bash
./scripts/local-gate.sh
```

等价顺序：

1. `frontend`：`pnpm lint && pnpm build`
2. `frontend-vue`：`pnpm build`
3. `backend`：`mvn -B test`
4. `edge`：`python3 -m py_compile edge/*.py && pytest -q edge/tests`
5. `docker compose up --build -d`

## 收口功能清单（已落地）

- ByteTrack 默认接入，支持 `TRACKER_BACKEND=simple` 降级。
- Vue 前端全量页面可用（登录、监控、分析、管理员四子模块）。
- 报表导出：`JSON + XLSX`。
- 双写灰度脚本：`scripts/db/switch_primary.sh`（`postgres|mysql`）。

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

默认 `docker compose` 仍以 PostgreSQL 为主库。切换通过脚本执行：

```bash
# PostgreSQL 主库 + MySQL 镜像双写
./scripts/db/switch_primary.sh postgres

# MySQL 主库 + PostgreSQL 镜像双写
./scripts/db/switch_primary.sh mysql
```

一致性校验（默认仅核对双写表）：

```bash
bash scripts/check_mirror_consistency.sh

# 扩展核对所有核心表
bash scripts/check_mirror_consistency.sh --all

# 按灰度窗口做增量一致性校验（推荐）
bash scripts/check_mirror_consistency.sh --since 2026-02-27T03:00:00
```

## 性能证据与答辩材料

- 性能采集脚本：`scripts/perf/run_perf_bundle.sh`
- 证据模板：`docs/reports/perf-evidence-template.md`
- 答辩索引：`docs/defense/index.md`
- 收口验证报告：`docs/reports/closure-validation-2026-02-27.md`

## 详细部署教程

- 本地部署：`docs/deploy/local.md`
- Docker 部署：`docs/deploy/docker.md`
- 生产部署：`docs/deploy/production.md`

## 子模块 README

- 后端：`backend/README.md`
- 边缘端：`edge/README.md`
- Vue 前端：`frontend-vue/README.md`
