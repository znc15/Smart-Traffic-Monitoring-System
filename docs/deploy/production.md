# 生产部署教程（Production）

## 1. 架构建议

- Nginx/Traefik 作为外层网关（TLS 终止 + 反向代理）
- `frontend` 静态资源服务
- `backend` 仅内网暴露给网关
- `postgres` 仅内网访问
- 边缘节点通过专线或 VPN 上报 `telemetry`

## 2. 安全基线

1. 使用强 `JWT_SECRET`。
2. 生产环境禁用 `APP_WS_ALLOW_QUERY_TOKEN`。
3. 收敛 `APP_CORS_ALLOWED_ORIGINS` 到明确域名。
4. 更换 `APP_MAAS_DEFAULT_API_KEY`，并定期轮换。
5. 开启容器日志采集与审计。

## 3. 上线步骤

1. 预发布环境完成回归。
2. 执行 `docker compose up --build -d`。
3. 验证健康检查与关键接口。
4. 开启监控与告警。

## 4. 监控指标

- 后端：接口错误率、P95/P99、JVM/线程、DB 连接池
- 边缘：FPS、inference_ms、CPU/内存、上报成功率
- 数据：样本写入量、预测任务成功率、MaaS 调用量

## 5. 备份与恢复

### 备份

```bash
docker exec database pg_dump -U postgres transportation_system > backup_$(date +%F).sql
```

### 恢复

```bash
cat backup_YYYY-MM-DD.sql | docker exec -i database psql -U postgres -d transportation_system
```

## 6. 回滚策略

- 每个里程碑独立发布版本号。
- 回滚按镜像 tag + DB 备份点执行。
- 新增接口默认灰度发布，保留旧接口一个迭代。
