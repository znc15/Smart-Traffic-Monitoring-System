# Docker Compose 部署教程

## 1. 配置环境变量

编辑 `docker-compose.yml` 的 `backend.environment`：

- `APP_ENV=production`
- `JWT_SECRET=<强随机串>`
- `APP_CORS_ALLOWED_ORIGINS=<前端域名>`
- `APP_MAAS_DEFAULT_API_KEY=<替换默认值>`

## 2. 构建并启动

```bash
docker compose up --build -d
```

## 3. 检查状态

```bash
docker compose ps
docker compose logs -f backend
```

## 4. 接口验证

```bash
curl http://localhost:8000/api/v1/roads_name
curl "http://localhost:8000/api/v1/traffic/predictions?road_name=陈兴道路&horizon_minutes=10"
curl -H 'X-API-Key: dev-maas-key-change-me' \
  "http://localhost:8000/api/v1/maas/congestion?min_lat=0&max_lat=90&min_lng=0&max_lng=180"
```

## 5. 停止与清理

```bash
docker compose down
```

带卷清理（危险操作，仅在确认无数据保留需求时执行）：

```bash
docker compose down -v
```
