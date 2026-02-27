# 性能证据包模板

## 1. OpenVINO 对比
- 环境：
- 模型：
- 开启 OpenVINO：平均 FPS / inference_ms
- 关闭 OpenVINO：平均 FPS / inference_ms
- 加速比：

## 2. 1080P 连续采样
- 采样时长：
- 平均 FPS：
- P95 inference_ms：

## 3. 云端压测（预测/查询）
- 压测工具：
- 并发：
- 样本数：
- P95 响应时间：

## 4. 产出要求
- 脚本：`scripts/perf/run_perf_bundle.sh`
- 汇总报告：`docs/reports/perf-summary-*.md`
- 原始数据：外部归档（不入仓）
