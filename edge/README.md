# Smart Traffic Edge Node

边缘节点服务，负责摄像头接入、YOLOv8 检测、目标追踪、交通统计和主动遥测上报。

## 核心能力

- YOLOv8 检测
- ByteTrack / SimpleTracker 追踪
- OpenVINO 加速
- MJPEG 实时流
- 运行时配置热更新
- 向 backend 主动上报遥测
- `sim` / `camera` 两种模式

## 安装

```bash
cd edge
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

测试环境：

```bash
pip install -r requirements-dev.txt
```

## 启动

默认端口是 `8000`。

### 仿真模式

```bash
python main.py --mode sim --port 8000 --no-browser
```

### 摄像头模式

```bash
python main.py --mode camera --url 0 --road "人民路" --port 8000 --no-browser
python main.py --mode camera --url "rtsp://admin:pass@192.168.1.100/stream" --road "人民路" --port 8000 --no-browser
```

### 生产 / 无头环境注意

- `MODE=camera` 时必须显式设置 `--url` 或 `CAMERA_URL`
- 否则程序会进入交互式摄像头选择，不适合 Docker / systemd / 无头环境
- 建议同时设置 `NO_BROWSER=true`

### Docker 部署

在 `edge/` 目录中：

```bash
docker compose up -d
```

说明：
- `edge` 不在主站根 `docker-compose.yml` 的一键启动范围内
- 主站和 edge 仍然分开部署

## 常用接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/health` | 健康检查 |
| `GET` | `/api/traffic` | 实时交通数据 |
| `GET` | `/api/frame` | 当前帧 |
| `GET` | `/api/stream` | MJPEG 视频流 |
| `GET` | `/api/metrics` | FPS / 推理耗时 / 资源指标 |
| `GET` | `/api/config` | 当前配置 |
| `PUT` | `/api/config` | 更新配置 |
| `POST` | `/api/cameras/probe` | 测试视频源 |
| `POST` | `/api/detect/image` | 图片检测 |
| `POST` | `/api/detect/video` | 视频检测 |
| `GET` | `/api/detect/video/result/{id}` | 下载检测结果 |

示例：

```bash
curl http://localhost:8000/health
curl http://localhost:8000/api/metrics
curl -X PUT http://localhost:8000/api/config \
  -H "X-Edge-Key: edge-secret" \
  -H "Content-Type: application/json" \
  -d '{"confidence": 0.4}'
```

## 鉴权

当配置了 `EDGE_API_KEY` 后，以下接口会要求 `X-Edge-Key`：

- `/api/traffic`
- `/api/frame`
- `/api/stream`
- `/api/metrics`
- `/api/config`
- `/api/cameras/probe`
- `/api/detect/image`
- `/api/detect/video`
- `/api/detect/video/result/{id}`

说明：
- `X-Edge-Node-Id` 可选；若配置了 `EDGE_NODE_ID`，服务端会一起校验
- 对 `<img>` / `<video>` / 下载链接这类无法自定义 header 的场景，可用 `edge_key` / `edge_node_id` query 参数兜底

## 配置参考

完整模板见 [`edge/.env.example`](.env.example)。

### 基础运行

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `MODE` | `sim` / `camera` | `sim` |
| `CAMERA_URL` | 摄像头地址或设备索引 | 空 |
| `ROAD_NAME` | 路段名称 | `示例路段-Edge01` |
| `MODEL` | 模型文件名 | `yolov8n.pt` |
| `CONF` | 检测阈值 | `0.35` |
| `OPENVINO` | 是否启用 OpenVINO | `true` |
| `NO_BROWSER` | 是否禁用自动打开浏览器 | `false` |
| `HOST` | HTTP 监听地址 | `0.0.0.0` |
| `PUBLIC_HOST` | 对外展示地址 | 空 |
| `PORT` | HTTP 端口 | `8000` |

### 性能与资源

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `FRAME_SKIP` | 每 N 帧推理一次 | `2` |
| `IMGSZ` | YOLO 输入尺寸 | `320` |
| `QUANTIZE` | `int8` / `fp16` / `none` | `none` |
| `JPEG_QUALITY` | JPEG 质量 | `80` |
| `MAX_MJPEG_CLIENTS` | 最大并发 MJPEG 客户端数 | `5` |
| `UVICORN_WORKERS` | worker 数量 | `1` |

### 追踪与交通分析

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `TRACKER_BACKEND` | `bytetrack` / `simple` | `bytetrack` |
| `TRACKER_STRICT` | 严格模式 | `false` |
| `TRACKER_CFG` | tracker 配置文件 | `bytetrack.yaml` |
| `ANALYSIS_ROI` | 归一化 ROI | `0.05,0.25,0.95,0.95` |
| `LANE_SPLIT_RATIOS` | 车道分割比例 | `0.33,0.66` |
| `SPEED_METERS_PER_PIXEL` | 速度标定系数 | `0.08` |
| `PARKING_STATIONARY_SECONDS` | 违停判定秒数 | `8` |
| `WRONG_WAY_MIN_TRACK_POINTS` | 逆行最少轨迹点数 | `4` |

### 遥测与接入

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `EDGE_NODE_ID` | 节点 ID | `edge-01` |
| `EDGE_API_KEY` | 节点密钥 | 空 |
| `BACKEND_TELEMETRY_URL` | backend 遥测上报地址 | 空 |
| `TELEMETRY_INTERVAL_SEC` | 上报间隔 | `3` |

说明：
- 只有设置了 `BACKEND_TELEMETRY_URL`，主动上报线程才会启动
- 上报时会自动带上 `X-Edge-Node-Id` 和 `X-Edge-Key`

## CLI 参数

`main.py` 额外支持以下启动参数：

- `--host`
- `--public-host`
- `--imgsz`
- `--frame-skip`
- `--quantize`
- `--tracker-backend`
- `--tracker-strict`
- `--tracker-cfg`
- `--no-openvino`
- `--no-browser`

## 热更新与热重启

通过 `PUT /api/config` 更新配置时：

可直接热更新：
- `frame_skip`
- `jpeg_quality`
- `telemetry_interval_sec`
- ROI / 车道 / 速度 / 违停 / 逆行相关参数

会触发检测循环重启：
- `mode`
- `camera_source`
- `model`
- `use_openvino`
- `quantize`
- tracker 相关配置

## 生产配置建议

最小推荐配置：

```env
MODE=camera
CAMERA_URL=rtsp://user:pass@camera/stream
ROAD_NAME=人民路
EDGE_NODE_ID=edge-01
EDGE_API_KEY=replace-me
BACKEND_TELEMETRY_URL=http://192.168.1.11:8000/api/v1/edge/telemetry
NO_BROWSER=true
```

弱 CPU 工业机可参考 `edge/docker-compose.yml` 的配置：

```env
IMGSZ=160
FRAME_SKIP=4
QUANTIZE=int8
OPENVINO=true
MAX_MJPEG_CLIENTS=1
UVICORN_WORKERS=1
```

模型目录：
- 默认使用 `edge/models/`
- Docker Compose 会把它挂载到容器内 `/app/models`

## 测试

```bash
python3 -m pip install -r requirements-dev.txt
pytest -q tests
```

如果只跑配置 / 鉴权 / 状态机测试，不需要完整安装 `torch` / `ultralytics` / `openvino`。
