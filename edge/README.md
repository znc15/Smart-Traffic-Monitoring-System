# Edge Node

边缘节点负责实时检测、追踪、统计、事件识别和视频输出。

## 能力

- YOLOv8 检测（机动车/非机动车/行人）
- ByteTrack 追踪（默认）
- `SimpleTracker` 降级链路
- OpenVINO 推理加速
- 热更新配置（模型/路段/追踪后端）
- 主动上报到后端 `/api/v1/edge/telemetry`

## 安装

```bash
cd edge
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## 启动

```bash
python main.py --mode sim --port 9000 --no-browser
```

摄像头模式示例：

```bash
python main.py --mode camera --url 0 --road "陈兴道路" --port 9000 --no-browser
```

## ByteTrack 配置

默认使用 ByteTrack：

- `TRACKER_BACKEND=bytetrack`
- `TRACKER_STRICT=false`
- `TRACKER_CFG=bytetrack.yaml`

可降级为 simple：

```bash
TRACKER_BACKEND=simple python main.py --mode camera --url 0
```

严格模式（ByteTrack 失败直接报错，不回退）：

```bash
TRACKER_BACKEND=bytetrack TRACKER_STRICT=true python main.py --mode camera --url 0
```

## 关键环境变量

- `MODE`：`sim|camera`
- `CAMERA_URL`
- `ROAD_NAME`
- `MODEL`
- `CONF`
- `OPENVINO`
- `PORT`
- `NO_BROWSER`
- `TRACKER_BACKEND`
- `TRACKER_STRICT`
- `TRACKER_CFG`
- `EDGE_NODE_ID`
- `BACKEND_TELEMETRY_URL`
- `TELEMETRY_INTERVAL_SEC`

## API

- `GET /health`
- `GET /api/traffic`
- `GET /api/frame`
- `GET /api/stream`
- `GET /api/metrics`
- `GET /api/config`
- `PUT /api/config`
- `GET /api/models`

`/api/config` 已支持 tracker 相关字段热更新：

- `tracker_backend`
- `tracker_strict`
- `tracker_cfg`

## 测试

```bash
python3 -m py_compile edge/*.py
pytest -q edge/tests
```

