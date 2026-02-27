# 生产部署教程（Production）

## 1. 目标架构

推荐拓扑：

1. 外层 Nginx/Traefik（TLS 终止、WAF、限流）
2. 内层 `gateway`（`/` -> Vue 单栈）
3. `backend`（仅内网暴露）
4. `postgres` 主库（默认）
5. `mysql` 镜像库（灰度阶段）
6. `redis` 缓存

## 2. 生产前准备

- 设置强 `JWT_SECRET`（至少 32 位随机串）
- 设置精确 `APP_CORS_ALLOWED_ORIGINS`
- 关闭 `APP_WS_ALLOW_QUERY_TOKEN`（除非兼容旧客户端）
- 配置 `APP_MAAS_DEFAULT_API_KEY` 并定期轮换

## 3. 部署步骤

### 3.1 拉取代码与镜像构建

```bash
git pull
docker compose build
```

### 3.2 启动

```bash
docker compose up -d
```

### 3.3 验收

```bash
docker compose ps
curl -I http://127.0.0.1:5173/
curl -I http://127.0.0.1:5173/react/
curl -I http://127.0.0.1:8000/api/v1/site-settings
```

验收预期：

- `/` 返回 `200`
- `/react/` 返回 `404`

## 4. 数据库灰度策略

- 阶段 A：`postgres` 主库，`mysql` 镜像双写，观察 24h
- 阶段 B：切 `mysql` 主库，`postgres` 镜像双写，观察 24h
- 阶段 C：稳定后关闭镜像写

执行命令：

```bash
./scripts/db/switch_primary.sh postgres
./scripts/db/switch_primary.sh mysql
```

一致性检查：

```bash
bash scripts/check_mirror_consistency.sh
```

## 5. 监控与告警

至少采集：

- 后端：接口错误率、P95/P99、JVM、连接池
- Edge：FPS、inference_ms、CPU/内存、上报成功率
- DB：慢查询、连接数
- Redis：命中率、内存使用、连接数

## 6. 备份与恢复

### PostgreSQL 备份

```bash
docker exec database pg_dump -U postgres transportation_system > backup_$(date +%F).sql
```

### PostgreSQL 恢复

```bash
cat backup_YYYY-MM-DD.sql | docker exec -i database psql -U postgres -d transportation_system
```

## 7. 回滚

- 前端回滚：回退到上一版 `gateway` 与 `frontend` 镜像 tag
- 数据库回滚：执行 `./scripts/db/switch_primary.sh postgres`
- 应用回滚：回退镜像 tag 后 `docker compose up -d`
