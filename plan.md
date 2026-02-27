# 任务书与开题报告一致性整改计划（执行状态版）

更新时间：2026-02-26

## 总体状态

- 当前结论：`M0-M1` 已完成，`M2-M4` 已形成可运行闭环，`M5-M6` 完成首批落地（Vue 骨架 + CI + 部署文档）。
- 本地门禁：已通过（`./scripts/local-gate.sh`）。
- 一键部署：已通过（`docker compose up --build -d`，三服务健康）。

## 里程碑状态

### M0：基线与追踪矩阵

- [x] `docs/requirements/traceability.md`
- [x] `docs/reports/gap-baseline.md`
- [x] 本地门禁脚本 `scripts/local-gate.sh`

### M1：构建与运行阻塞清零

- [x] 前端 Docker 改为直接安装 pnpm，移除 Corepack 签名依赖
- [x] Flyway + PostgreSQL 16 兼容（含 `flyway-database-postgresql`）
- [x] `docker-compose.yml` 健康检查与启动依赖修复

### M2：边缘端能力补齐

- [x] 检测类别补充 `person`
- [x] 新增轻量追踪器 `edge/simple_tracker.py`（输出 `track_id`）
- [x] 新增 `lane_stats` 与 `events` 规则增强
- [x] 新增主动上报线程 `edge/telemetry_reporter.py`
- [ ] ByteTrack 正式接入（当前为同类能力替代实现）

### M3：云端汇聚与预测

- [x] `POST /api/v1/edge/telemetry`
- [x] 历史样本/事件/预测入库（Flyway V2-V4）
- [x] `GET /api/v1/traffic/predictions`（7天历史 -> 未来N分钟）
- [x] 预测结果持久化

### M4：MaaS 与 GIS

- [x] 摄像头经纬度字段（Flyway V5 + CameraEntity）
- [x] `GET /api/v1/maas/congestion`（bbox）
- [x] `X-API-Key` 认证过滤器与默认开发 client
- [ ] 地图页面点位与快照弹窗（需前端页面联动完成）

### M5：前端栈一致性整改（Vue + ECharts）

- [x] 新建 `frontend-vue` 工程并可构建
- [x] ECharts 图表示例落地
- [ ] 全量功能对齐（登录态、管理员功能细节、导出）
- [ ] 默认入口切换与网关灰度

### M6：测试、CI、部署与答辩材料

- [x] CI 工作流（frontend/backend/edge）
- [x] README 与部署教程（本地/Docker/生产）首版
- [ ] 性能证据包（mAP、OpenVINO 对比、1080P FPS、压测）
- [ ] 答辩证据集（截图、脚本、指标总表）

## 已落地接口

- `POST /api/v1/edge/telemetry`
- `GET /api/v1/traffic/predictions?road_name=...&horizon_minutes=...`
- `GET /api/v1/maas/congestion?min_lat=...&max_lat=...&min_lng=...&max_lng=...` + `X-API-Key`

## 数据迁移

- [x] `V2__traffic_samples.sql`
- [x] `V3__traffic_events.sql`
- [x] `V4__traffic_predictions.sql`
- [x] `V5__camera_geo_and_api_clients.sql`
- [x] `V6__reporting_views.sql`

## 下一阶段（建议 5-7 天）

1. 完成 `frontend-vue` 与旧前端的功能对齐和灰度切换方案。
2. 补齐报表导出（JSON + XLSX）与前端下载入口。
3. 固化性能基准脚本并输出 `docs/reports/perf-*.md`。
4. 形成答辩材料目录（演示脚本、关键截图、指标表、风险闭环）。
