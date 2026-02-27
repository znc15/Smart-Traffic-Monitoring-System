# Edge Node

边缘节点负责实时检测、追踪、车道统计、事件识别和云端上报。

## 核心能力

- YOLOv8 检测（机动车/非机动车/行人）
- ByteTrack 追踪（默认）
- `SimpleTracker` 降级模式
- OpenVINO 推理加速
- 配置热更新（模型、路段、追踪后端）
- 主动上报 `POST /api/v1/edge/telemetry`

## 安装

```bash
cd edge
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## 启动

```bash
# 仿真模式
python main.py --mode sim --port 9000 --no-browser

# 摄像头模式
python main.py --mode camera --url 0 --road "陈兴道路" --port 9000 --no-browser
```

## 追踪后端切换

```bash
# 默认 ByteTrack
TRACKER_BACKEND=bytetrack python main.py --mode camera --url 0

# 降级 SimpleTracker
TRACKER_BACKEND=simple python main.py --mode camera --url 0

# 严格模式（ByteTrack 失败直接报错）
TRACKER_BACKEND=bytetrack TRACKER_STRICT=true python main.py --mode camera --url 0
```

## 关键环境变量

- `MODE`、`CAMERA_URL`、`ROAD_NAME`
- `MODEL`、`CONF`、`OPENVINO`
- `TRACKER_BACKEND`、`TRACKER_STRICT`、`TRACKER_CFG`
- `EDGE_NODE_ID`、`BACKEND_TELEMETRY_URL`、`TELEMETRY_INTERVAL_SEC`

## API

- `GET /health`
- `GET /api/traffic`
- `GET /api/frame`
- `GET /api/stream`
- `GET /api/metrics`
- `GET /api/config`
- `PUT /api/config`
- `GET /api/models`

## 测试

```bash
python3 -m py_compile edge/*.py
pytest -q edge/tests
```
