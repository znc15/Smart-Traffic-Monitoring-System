# Edge Node（智能交通边缘检测节点）

`edge` 是独立部署的边缘检测服务，负责：

- 实时车辆检测（YOLOv8）
- 输出交通统计与实时帧
- 运行时热更新配置（模型/模式/路段名等）
- 提供图片与视频上传检测接口

默认支持 OpenVINO 加速与资源分级自适应（低配机器自动降级参数）。

## 目录结构

```text
edge/
├── main.py
├── routes.py
├── detector.py
├── loops.py
├── state.py
├── config.py
├── resource_manager.py
├── validators.py
├── camera_discovery.py
├── static/
├── samples/
├── tests/
└── requirements.txt
```

## 运行要求

- Python 3.10+
- 可选摄像头或 RTSP/HTTP 视频流
- 首次运行会下载/导出模型，需联网

## 启动方式

### 1. 本地

```bash
cd edge
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python main.py
```

常用启动参数：

```bash
# 摄像头模式（交互选择）
python main.py --mode camera

# 指定本地摄像头
python main.py --mode camera --url 0

# 指定 RTSP
python main.py --mode camera --url rtsp://192.168.1.10:554/stream

# 指定端口/路段
python main.py --mode camera --port 9000 --road "长安街-East"

# 无浏览器环境
python main.py --no-browser
```

### 2. Docker

```bash
cd edge
docker build -t edge-node .

docker run -p 9000:8000 \
  -e MODE=camera \
  -e CAMERA_URL=rtsp://192.168.1.10:554/stream \
  -e ROAD_NAME="长安街-East" \
  -e NO_BROWSER=true \
  edge-node
```

## 配置优先级

`CLI 参数 > 环境变量 > 默认值`

常用环境变量：

- `MODE`：`sim` 或 `camera`
- `CAMERA_URL`：设备号 / URL / `/dev/*`
- `ROAD_NAME`
- `MODEL`：例如 `yolov8n.pt`
- `CONF`
- `PORT`
- `OPENVINO`
- `NO_BROWSER`
- `FRAME_SKIP`
- `IMGSZ`
- `QUANTIZE`：`int8 | fp16 | none`
- `JPEG_QUALITY`
- `MAX_MJPEG_CLIENTS`

## API 一览

### 交通与健康

- `GET /api/traffic`：交通数据 + `edge_metrics`
- `GET /api/frame`：最新 JPEG 帧
- `GET /api/metrics`：性能指标
- `GET /health`：健康检查
- `GET /api/resource-mode`：当前资源等级与生效参数

### 实时视频

- `GET /api/stream`：MJPEG 流
- `GET /api/video`：WebM 视频流（依赖 FFmpeg）

### 配置与模型

- `GET /api/config`
- `PUT /api/config`
- `GET /api/models`

`/api/models` 返回 `has_openvino`，同时兼容新旧 OpenVINO 缓存目录命名。

### 摄像头工具接口

- `GET /api/cameras`：扫描本地设备
- `POST /api/cameras/probe`：探测视频源连通性
- `GET /api/cameras/preview?source=...`：抓帧+检测预览

探测地址校验规则：

- 允许：设备号、`/dev/*`、`rtsp(s)://`、`http(s)://`
- 拒绝：其他格式

### 上传检测

- `POST /api/detect/image`
- `POST /api/detect/video`
- `GET /api/detect/video/result/{result_id}`

## `/api/config` 热更新说明

可更新字段（按需提交）：

- `mode`
- `camera_source`
- `model`
- `confidence`
- `road_name`
- `use_openvino`
- `frame_skip`
- `imgsz`
- `quantize`
- `jpeg_quality`

重启策略：

- `frame_skip`、`jpeg_quality`：动态生效，通常不重启
- `mode`、`model`、`use_openvino`、`quantize`、`imgsz`（OpenVINO 模式）会触发热重启

## 与后端对接

在后端管理页「摄像头管理」配置：

- `stream_url`：填边缘节点基地址，例如 `http://192.168.1.20:9000`
- `road_name`：与前端展示路段一致

后端会轮询：

- `GET /api/traffic`
- `GET /api/frame`

## 测试

```bash
cd edge
python -m py_compile *.py
pytest -q tests
```

当前测试覆盖 `validators.py` 关键行为（视频源校验、OpenVINO 缓存识别）。
