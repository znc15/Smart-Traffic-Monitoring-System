# 未完成项收口整改计划（执行状态版）

更新时间：2026-02-27

## 总体结论

四个未完成项已完成代码与流程闭环，当前进入“实机补采与答辩打包”阶段：

1. ByteTrack 正式接入：已完成并默认启用。
2. Vue 全量对齐 + 默认入口切换：已完成，`/` 为 Vue，`/react/` 为 React 回滚。
3. 报表 XLSX 导出 + 证据包框架 + 答辩目录：已完成首版（脚本、模板、目录）。
4. MySQL/Redis 第二阶段切换：已完成双写灰度切换脚本与实切验证（postgres<->mysql 双向可切）。

验收证据：`docs/reports/closure-validation-2026-02-27.md`

## 里程碑状态

### M1：ByteTrack 正式接入

- [x] 追踪抽象层 `edge/tracking_engine.py`
- [x] 默认后端 `TRACKER_BACKEND=bytetrack`
- [x] 降级开关 `TRACKER_BACKEND=simple`
- [x] `TRACKER_STRICT`、`TRACKER_CFG` 配置支持
- [x] 三条 loop 统一追踪入口
- [x] `edge/tests/test_tracking_engine.py`

### M2：Vue 全量功能 + 默认入口切换

- [x] 登录、首页监控、分析页、管理员页全链路可用
- [x] `snake_case` 统一读取 + `camelCase` 兼容读取
- [x] 集中 store，避免重复 WebSocket
- [x] 网关路由：`/` -> Vue，`/react/` -> React
- [x] React 构建 base 修正为 `/react/`（含路由 basename）

### M3：报表导出 + 证据包 + 答辩材料

- [x] 后端导出接口（JSON/XLSX）
- [x] Apache POI 生成 XLSX
- [x] Vue 分析页下载入口（JSON/XLSX）
- [x] 性能采集脚本 `scripts/perf/run_perf_bundle.sh`
- [x] 答辩目录 `docs/defense/*`
- [x] 实测数据填充（当前环境已完成门禁、导出、切主与截图；1080P/OpenVINO 实机数据按目标硬件补采）

### M4：MySQL/Redis 双写灰度与切主

- [x] PostgreSQL/MySQL 双迁移目录
- [x] Redis 缓存接入（道路、路况、预测、MaaS）
- [x] 双写服务 `MirrorWriteService`
- [x] 切换脚本 `scripts/db/switch_primary.sh`
- [x] 一致性脚本 `scripts/check_mirror_consistency.sh`
- [x] 实切验证：`postgres -> mysql -> postgres` 可用

## 门禁结果

- [x] `pnpm -C frontend lint && pnpm -C frontend build`
- [x] `pnpm -C frontend-vue build`
- [x] `mvn -B -f backend/pom.xml test`
- [x] `python3 -m py_compile edge/*.py && pytest -q edge/tests`
- [x] `docker compose up --build -d`

## 备注

- 镜像库 Flyway 失败已调整为“只告警不阻塞主服务启动”，满足灰度稳定性要求。
- 主库 Flyway 失败仍保持阻塞（防止脏启动）。
