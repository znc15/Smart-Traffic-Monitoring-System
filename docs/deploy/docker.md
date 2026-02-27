# Docker Compose 部署教程

本教程用于单机完整部署：`gateway + frontend-vue + frontend-react + backend + postgres + mysql + redis`。

## 1. 构建并启动

```bash
docker compose up --build -d
```

## 2. 健康检查

```bash
docker compose ps
```

期望：

- `gateway`、`frontend-vue`、`frontend-react`、`backend`、`database`、`mysql`、`redis` 全部 `healthy`。

## 3. 路由验证

```bash
curl -I http://localhost:5173/
curl -I http://localhost:5173/react/
```

页面验证：

- `http://localhost:5173/` 为 Vue
- `http://localhost:5173/react/` 为 React

## 4. 接口验证

```bash
curl http://localhost:8000/api/v1/roads_name
curl "http://localhost:8000/api/v1/traffic/predictions?road_name=陈兴道路&horizon_minutes=10"
```

## 5. MySQL/Redis 灰度切换

### 5.1 PostgreSQL 主库 + MySQL 镜像双写

```bash
./scripts/db/switch_primary.sh postgres
```

### 5.2 MySQL 主库 + PostgreSQL 镜像双写

```bash
./scripts/db/switch_primary.sh mysql
```

### 5.3 一致性校验

```bash
bash scripts/check_mirror_consistency.sh
```

按灰度窗口做增量校验（推荐）：

```bash
bash scripts/check_mirror_consistency.sh --since 2026-02-27T03:00:00
```

## 6. 回滚

若切换后异常，可快速回到 PostgreSQL 主库：

```bash
./scripts/db/switch_primary.sh postgres
```

## 7. 停止与清理

```bash
docker compose down
```

清理卷（会删除数据库数据，谨慎执行）：

```bash
docker compose down -v
```
