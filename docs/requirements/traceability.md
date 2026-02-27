# 需求追踪矩阵（任务书 + 开题报告）

更新时间：2026-02-27

状态定义：`未开始` / `进行中` / `完成`

| 编号 | 任务书/开题要求 | 实现位置 | 测试/证据 | 状态 |
|---|---|---|---|---|
| R1 | 端云协同总体架构 | `edge/` + `backend/` + `frontend/` | 三端联调截图与 compose 启动日志 | 完成 |
| R2 | OpenVINO 推理加速 | `edge/detector.py` | edge 页面 FPS/推理时间显示 | 完成 |
| R3 | 多类别检测（车/非机动车/行人） | `edge/detector.py` | `edge/tests` + `tracked_objects.class` 输出 | 完成 |
| R4 | 目标追踪去重（ByteTrack/同类能力） | `edge/tracking_engine.py` + `edge/loops.py` | `edge/tests/test_tracking_engine.py` + `tracked_objects.track_id` | 完成 |
| R5 | ROI/车道级统计（左转/直行/右转） | `edge/traffic_enrichment.py` | `lane_stats` 字段 + 回归验证 | 完成 |
| R6 | 异常事件（拥堵/违停/逆行） | `edge/traffic_enrichment.py` | `events` 字段 + `traffic_events` 入库 | 完成 |
| R7 | 云端高并发接收与清洗 | `backend/controller/EdgeTelemetryController.java` + `TelemetryIngestionService.java` | `POST /api/v1/edge/telemetry` | 完成 |
| R8 | 历史7天预测未来1小时 | `TrafficPredictionService.java` | `GET /api/v1/traffic/predictions` | 完成 |
| R9 | 标准化 MaaS 接口（API Key+bbox） | `MaasController.java` + `ApiKeyAuthenticationFilter.java` | `GET /api/v1/maas/congestion` | 完成 |
| R10 | GIS 摄像头地理坐标 | `CameraEntity.java` + `V5__camera_geo_and_api_clients.sql` | bbox 查询命中验证 | 完成 |
| R11 | 可视化报表（时/日/周/月） | `V6__reporting_views.sql` | SQL 查询结果截图 | 完成 |
| R12 | Excel 导出 | `ReportController.java` + `TrafficReportExportService.java` | `docs/reports/raw/report_export_hourly_*.xlsx` | 完成 |
| R13 | 安全基线（CORS/JWT/APIKey） | `SecurityConfig.java` + `TokenExtractionService.java` + `ApiKeyAuthenticationFilter.java` | 鉴权回归用例 | 完成 |
| R14 | CI 门禁 | `.github/workflows/ci.yml` | PR 必须通过流水线 | 完成 |
| R15 | 部署文档与运维说明 | `README.md` + `docs/deploy/*` | 新环境部署复现实验 | 完成 |

## 备注

1. 门禁与联调结果见：`docs/reports/closure-validation-2026-02-27.md`。
2. 边缘端 OpenVINO/FPS 实机压测结果需按目标硬件环境补采。
