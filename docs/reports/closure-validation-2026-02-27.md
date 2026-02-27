# 未完成项收口验证报告（2026-02-27）

## 1. 全量门禁

执行：`./scripts/local-gate.sh`

结果：通过

包含：
- `frontend lint`
- `frontend build`
- `frontend-vue build`
- `backend test`
- `edge py_compile + pytest`
- `docker compose up --build`

## 2. ByteTrack/追踪链路

执行：
- `python3 -m py_compile edge/*.py`
- `pytest -q edge/tests`

结果：`11 passed`

说明：`TrackingEngine` 已统一追踪入口，支持 `TRACKER_BACKEND=bytetrack|simple`。

## 3. Vue 默认入口 + React 回滚入口

验证：
- `curl http://localhost:5173/` 返回 Vue 构建资源 `/assets/index-*.js`
- `curl http://localhost:5173/react/` 返回 React 资源 `/react/assets/index-*.js`

截图：
- `docs/defense/screenshots/vue-login.png`
- `docs/defense/screenshots/vue-dashboard.png`
- `docs/defense/screenshots/vue-analytics.png`
- `docs/defense/screenshots/vue-admin-users.png`
- `docs/defense/screenshots/vue-admin-cameras.png`
- `docs/defense/screenshots/vue-admin-settings.png`
- `docs/defense/screenshots/vue-admin-monitor.png`
- `docs/defense/screenshots/react-rollback.png`

## 4. 报表导出（JSON/XLSX）

验证流程：
1. 注册并登录测试账号
2. 调用 `GET /api/v1/reports/traffic/export?granularity=hourly&format=json`
3. 调用 `GET /api/v1/reports/traffic/export?granularity=hourly&format=xlsx`

结果：
- JSON 成功，`rows=3`
- XLSX 文件可打开（`file` 识别为 `Microsoft OOXML`）

证据文件：
- `docs/reports/raw/report_export_hourly_20260227_110502.json`
- `docs/reports/raw/report_export_hourly_20260227_110502.xlsx`

## 5. MySQL/Redis 双写灰度切换

执行：
- `./scripts/db/switch_primary.sh mysql`
- `./scripts/db/switch_primary.sh postgres`

结果：
- 两个方向均可切换并恢复 healthy
- MySQL 主库写入 `POST /api/v1/edge/telemetry` 成功（已修复 JSONB 方言问题）

增量一致性验证：
- `./scripts/check_mirror_consistency.sh --since <timestamp>`
- 结果：`traffic_samples/events/predictions` 增量计数一致，`OK`

## 6. 性能证据（当前环境）

后端预测查询延迟采样：
- 样本数：40
- P95：`137.00 ms`
- 原始数据：`docs/reports/raw/backend_predictions_latency_20260227_110444.csv`

说明：边缘端 1080P FPS 与 OpenVINO 对比需在有模型与摄像头/样例视频的实机环境补采。

## 7. MaaS 验证

调用：
- `GET /api/v1/maas/congestion?...`
- Header: `X-API-Key: dev-maas-key-change-me`

证据：
- `docs/defense/evidence/maas-congestion-20260227_110843.json`

