# Docker Compose 单机部署

本教程适用于根目录 `docker-compose.yml`，目标是快速拉起：

`database + mysql + redis + backend + edge-node + frontend + gateway`

## 1. 准备运行时变量

```bash
cp .env.example .env
```

至少设置：

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

说明：
- 根 `.env` 现在同时影响：
  - backend 运行时变量
  - frontend 构建时的 API / WS 地址
  - edge-node 运行时变量
- 根 Docker 一键启动范围包含 `gateway/frontend/backend/edge-node/database/mysql/redis`
- `edge` 如需部署到独立边缘设备，仍可使用 `edge/docker-compose.yml`

## 2. 构建并启动

```bash
docker compose up --build -d
docker compose ps
```

## 3. 服务与端口

| 服务 | 对外端口 | 说明 |
|------|----------|------|
| `gateway` | `5173` | 推荐统一入口 |
| `backend` | `8000` | 当前 Compose 为了联调直接暴露 |
| `edge-node` | `9000` | 容器内仍监听 `8000`，宿主机默认映射到 `9000` |
| `database` | `5433` | PostgreSQL |
| `mysql` | `3307` | MySQL |
| `redis` | `6380` | Redis |
| `frontend` | 不直接暴露 | 由 `gateway` 反向代理 |

## 4. 路由与接口验证

```bash
curl -I http://localhost:5173/
curl -I http://localhost:5173/react/
curl http://localhost:8000/api/v1/site-settings
curl http://localhost:8000/api/v1/roads_name
curl http://localhost:9000/health
```

预期：
- `http://localhost:5173/` 返回 `200`
- `http://localhost:5173/react/` 返回 `404`

## 5. frontend 生产变量说明

如果你需要前后端分开 IP：

```env
GATEWAY_PUBLIC_BASE=http://192.168.1.10:5173
BACKEND_PUBLIC_HTTP_BASE=http://192.168.1.11:8000
BACKEND_PUBLIC_WS_BASE=ws://192.168.1.11:8000
```

改完地址后执行：

```bash
docker compose build frontend gateway backend edge-node
docker compose up -d
```

说明：
- frontend 会在 Docker build 时把 `BACKEND_PUBLIC_HTTP_BASE / BACKEND_PUBLIC_WS_BASE` 写进最终产物
- backend 会默认把：
  - `APP_CORS_ALLOWED_ORIGINS` 对齐到 `GATEWAY_PUBLIC_BASE`
  - `BASE_URL_API` 对齐到 `BACKEND_PUBLIC_HTTP_BASE`
- 如需单独构建 frontend，才考虑 `frontend/.env.production`

## 6. 数据库灰度切换

PostgreSQL 主库：

```bash
./scripts/db/switch_primary.sh postgres
```

MySQL 主库：

```bash
./scripts/db/switch_primary.sh mysql
```

一致性校验：

```bash
bash scripts/check_mirror_consistency.sh
bash scripts/check_mirror_consistency.sh --since 2026-03-11T00:00:00
bash scripts/check_mirror_consistency.sh --all
```

说明：
- 当前自动化脚本支持“切主库 + 保持双写”
- 如果要结束灰度并关闭镜像写，请参考 [`production.md`](production.md) 中的手动步骤

## 7. AI 助手配置（可选）

AI 助手支持 OpenAI 兼容 API 和 Claude API，通过后台管理界面配置：

1. 登录后进入「系统管理 → AI 配置」
2. 选择 LLM 提供商（`openai` / `claude`）
3. 填写 API Base URL、API Key、模型名称
4. 点击「保存」后可「测试连接」

说明：
- AI 助手的配置存储在 `site_settings` 表，不需要重启服务
- 对话历史存储在 `ai_chat_conversations` / `ai_chat_messages` 表
- SSE 流式响应默认超时 120 秒

## 8. 停止与清理

停止：

```bash
docker compose down
```

清理卷（会删除数据库数据，危险操作）：

```bash
docker compose down -v
```
