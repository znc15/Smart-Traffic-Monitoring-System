# 差距基线报告（整改前后对照）

更新时间：2026-02-26

## 1. 构建与运行

- 前端 Docker 构建问题：已统一使用 `npm install -g pnpm`，规避 Corepack 签名波动。
- 后端 Flyway 与 PostgreSQL 16 兼容：已引入并使用 `flyway-database-postgresql`，容器可正常启动。
- Compose 启动顺序与健康检查：已为数据库/后端/前端补充健康检查与依赖条件。

## 2. 功能差距

- 补齐项：
  - 新增 `POST /api/v1/edge/telemetry` 主动上报入口。
  - 新增 `GET /api/v1/traffic/predictions` 预测接口（7天历史 -> 未来60分钟默认）。
  - 新增 `GET /api/v1/maas/congestion` + `X-API-Key` 鉴权。
  - 新增摄像头经纬度字段和 bbox 查询能力。
  - 新增历史样本/事件/预测数据表与统计视图。

- 未完全闭合项：
  - 前端 Vue + ECharts 全量替换尚未完成（本次先保证后端契约和数据闭环）。
  - Excel 报表导出接口未落地。
  - 边缘异常识别仍是规则版，需后续升级为更强鲁棒模型。

## 3. 数据契约

- 统一输出 `snake_case`。
- 兼容策略：前端仍可双读 `camelCase + snake_case` 一迭代。

## 4. 测试现状

- 已通过：
  - `frontend`: lint + build
  - `backend`: `mvn -B test`
  - `edge`: `py_compile` + `pytest`
- 待补：
  - 预测准确性对比报告。
  - OpenVINO 优化前后对比与压测报告归档。

## 5. 下一批建议

1. 完成 `frontend-vue` 迁移并切换主入口。
2. 增加 `Excel` 导出接口与前端下载入口。
3. 补齐性能证据包并固化到 `docs/reports/`。
