# Docker Compose 单机部署

本教程适用于根目录 `docker-compose.yml`，目标是快速拉起：

`database + mysql + redis + backend + frontend + gateway`

## 1. 准备运行时变量

```bash
cp .env.example .env
```

至少设置：

```env
POSTGRES_PASSWORD=change-me
MYSQL_ROOT_PASSWORD=change-me
MYSQL_PASSWORD=change-me
JWT_SECRET=replace-with-random-32-char-secret
MAAS_API_KEY=replace-with-your-api-key
```

说明：
- 根 `.env` 只影响 Compose 运行时变量。
- `frontend` 的 `VITE_*` 不走根 `.env`，如需生产构建变量请单独准备 `frontend/.env.production`。

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
curl "http://localhost:8000/api/v1/traffic/predictions?road_name=陈兴道路&horizon_minutes=10"
```

预期：
- `http://localhost:5173/` 返回 `200`
- `http://localhost:5173/react/` 返回 `404`

## 5. frontend 生产变量说明

如果你需要用 Compose 构建一个更接近生产的 frontend：

1. 在 `frontend/` 目录创建或更新 `.env.production`
2. 然后重新构建 frontend / gateway

示例：

```env
VITE_API_HTTP_BASE=
VITE_API_WS_BASE=
VITE_AMAP_KEY=your_amap_key
```

同源部署时推荐把 `VITE_API_HTTP_BASE` / `VITE_API_WS_BASE` 留空。

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

## 7. 停止与清理

停止：

```bash
docker compose down
```

清理卷（会删除数据库数据，危险操作）：

```bash
docker compose down -v
```
