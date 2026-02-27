# 需求追踪矩阵（任务书 + 开题报告）

更新时间：2026-02-27

状态定义：`未开始` / `进行中` / `完成`

| 编号 | 任务书/开题要求 | 实现位置 | 验证方式 | 状态 |
|---|---|---|---|---|
| R1 | 端云协同总体架构 | `edge/` + `backend/` + `frontend-vue/` | 三端联调与 compose 启动日志 | 完成 |
| R2 | OpenVINO 推理加速 | `edge/detector.py` | FPS/推理时间日志对比 | 完成 |
| R3 | 多类别检测（车/非机动车/行人） | `edge/detector.py` | `edge/tests` + `tracked_objects.class` | 完成 |
| R4 | 目标追踪去重（ByteTrack/同类能力） | `edge/tracking_engine.py` + `edge/loops.py` | `edge/tests/test_tracking_engine.py` + `tracked_objects.track_id` | 完成 |
| R5 | ROI/车道级统计（左转/直行/右转） | `edge/traffic_enrichment.py` | `lane_stats` 字段回归 | 完成 |
| R6 | 异常事件（拥堵/违停/逆行） | `edge/traffic_enrichment.py` | `events` 字段与事件入库 | 完成 |
| R7 | 云端高并发接收与清洗 | `EdgeTelemetryController.java` + `TelemetryIngestionService.java` | `POST /api/v1/edge/telemetry` 契约测试 | 完成 |
| R8 | 历史7天预测未来1小时 | `TrafficPredictionService.java` | `GET /api/v1/traffic/predictions` 返回序列校验 | 完成 |
| R9 | MaaS 接口（API Key + bbox） | `MaasController.java` + `ApiKeyAuthenticationFilter.java` | `GET /api/v1/maas/congestion` 鉴权回归 | 完成 |
| R10 | GIS 摄像头地理坐标 | `CameraEntity.java` + `V5__camera_geo_and_api_clients.sql` | bbox 查询命中验证 | 完成 |
| R11 | 可视化报表（时/日/周/月） | `V6__reporting_views.sql` + `frontend-vue` 分析页 | 四粒度查询与展示验证 | 完成 |
| R12 | XLSX 导出 | `ReportController.java` + `TrafficReportExportService.java` | 导出接口下载与可打开性验证 | 完成 |
| R13 | 安全基线（CORS/JWT/APIKey） | `SecurityConfig.java` + `TokenExtractionService.java` | 鉴权与跨域回归 | 完成 |
| R14 | CI 门禁 | `.github/workflows/ci.yml` | PR 流水线结果 | 完成 |
| R15 | 部署文档与运维说明 | `README.md` + `docs/deploy/*` | 新环境部署复现 | 完成 |

## 备注

1. 本仓库已清空原始截图与原始 CSV/XLSX 证据，保留文字化追踪与复测流程。
2. 实机性能证据按 `docs/reports/perf-evidence-template.md` 模板补采并外部归档。
