# Smart Traffic Monitoring System

智能交通监控系统（Vue 单栈 + Spring Boot + Edge 推理）。

## 当前状态

- 前端仅保留 `frontend-vue`，`/react` 已下线（返回 `404`）
- 仓库不再存放原始截图、CSV、XLSX 证据文件
- 默认支持 PostgreSQL 主库，MySQL/Redis 保留灰度能力

## 快速开始

```bash
docker compose up --build -d
docker compose ps
```

联调检查：

```bash
curl -I http://localhost:5173/
curl -I http://localhost:5173/react/
curl -I http://localhost:8000/api/v1/site-settings
```

预期：`/` 为 `200`，`/react/` 为 `404`。

## 常用命令

```bash
# 本地门禁（构建 + 测试 + 联调）
./scripts/local-gate.sh

# 清理依赖与构建缓存
./scripts/clean-project.sh

# 停止服务
docker compose down
```

## 项目结构

- `frontend-vue`：Vue3 管理端与可视化
- `backend`：认证、管理、预测、MaaS、报表导出
- `edge`：检测、追踪、车道统计、事件识别、上报
- `gateway`：统一入口网关（`5173`）
- `docs`：部署、需求追踪、答辩模板文档

## 部署文档

- 本地部署：`docs/deploy/local.md`
- Docker 部署：`docs/deploy/docker.md`
- 生产部署：`docs/deploy/production.md`

## 模块文档

- 后端：`backend/README.md`
- 边缘端：`edge/README.md`
- 前端：`frontend-vue/README.md`
