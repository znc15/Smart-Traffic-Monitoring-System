# 性能证据包汇总

- 采样时间: `20260310_210000`
- edge `openvino_on` 平均 FPS: 31.40
- edge `openvino_on` 平均 inference_ms: 24.80
- edge `openvino_off` 平均 FPS: 14.20
- edge `openvino_off` 平均 inference_ms: 55.60
- OpenVINO 推理加速比: 2.24x
- backend 预测接口平均延迟: 46.20 ms
- backend 预测接口 P95 延迟: 71.00 ms
- backend 原始延迟数据: `docs/reports/raw/backend_latency_20260310_210000.csv`
- edge 原始指标: `docs/reports/raw/edge_metrics_openvino_on_20260310_210000.csv`
- edge 原始指标: `docs/reports/raw/edge_metrics_openvino_off_20260310_210000.csv`

## 准确率记录建议

- 记录测试集来源、样本数量、标注方法。
- 分别记录 `car / motor / person` 的 Precision、Recall、mAP50。
- 对 OpenVINO on/off 使用同一视频源重复采样，避免数据漂移。
