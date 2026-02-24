# Edge Node - 智能交通边缘检测节点

基于 YOLOv8 + OpenVINO 的边缘检测节点，负责实时车辆检测并通过 HTTP API 上报交通数据与节点性能指标。默认启用 OpenVINO 加速 CPU 推理，首次运行自动将 PyTorch 模型导出为 OpenVINO IR 格式。后端 `CameraPollerService` 每 3 秒轮询本节点获取数据。

## 项目结构

```text
edge/
├── main.py              # 应用入口，CLI 参数解析
├── config.py            # 配置（CLI > 环境变量 > 默认值）
├── camera_discovery.py  # 摄像头自动发现与交互选择
├── detector.py          # YOLOv8 模型加载与车辆检测（OpenVINO）
├── loops.py             # 摄像头 / 模拟后台循环
├── overlay.py           # 帧信息叠加层绘制
├── routes.py            # FastAPI 路由定义
├── state.py             # 线程安全全局状态 + 性能采集
├── requirements.txt
├── environment.yml      # Conda 环境配置
└── Dockerfile
```

## 环境要求

- Python 3.10+
- pip 或 conda
- 摄像头模式需要可用的摄像头设备或 RTSP 视频流
- （可选）NVIDIA GPU + pynvml 用于 GPU 监控上报

## 部署步骤

### 1. 本地部署

**方式 A — Conda（推荐）：**

```bash
cd edge
conda env create -f environment.yml
conda activate edge-node
```

**方式 B — pip + venv：**

```bash
cd edge
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

> 首次安装会下载 ultralytics、openvino 等较大的包，请耐心等待。

**启动模拟模式（无需摄像头，用于验证部署）：**

```bash
python main.py
```

启动后访问 `http://localhost:8000/health` 确认节点运行正常。

**启动摄像头模式：**

```bash
# 交互选择 — 自动扫描本地摄像头，CLI 菜单选择
python main.py --mode camera

# 直接指定本地摄像头
python main.py --mode camera --url 0

# 指定 RTSP 流
python main.py --mode camera --url rtsp://192.168.1.100:554/stream
```

> 首次以摄像头模式启动时，OpenVINO 会自动导出模型（约 10-30 秒），后续启动跳过此步骤。

### 2. Docker 部署

```bash
# 构建镜像
docker build -t edge-node .

# 模拟模式
docker run -p 8000:8000 edge-node

# 摄像头模式（RTSP）
docker run -p 8000:8000 \
  -e MODE=camera \
  -e CAMERA_URL=rtsp://192.168.1.100:554/stream \
  -e ROAD_NAME="长安街-East" \
  edge-node

# 使用本地摄像头（需要设备映射）
docker run -p 8000:8000 \
  --device /dev/video0 \
  -e MODE=camera \
  -e CAMERA_URL=0 \
  edge-node
```

> Docker 环境下不支持交互选择，需通过环境变量指定 `CAMERA_URL`。

### 3. 接入后端

边缘节点启动后，需要在后端管理面板中添加摄像头，将 `streamUrl` 指向本节点地址：

```text
http://<边缘节点IP>:8000
```

后端 `CameraPollerService` 会自动轮询以下接口：

- `GET /api/traffic` — 获取交通数据 + 性能指标
- `GET /api/frame` — 获取最新检测帧

### 4. 多节点部署

每个边缘节点监控一个路段，通过 `--road` 和 `--port` 区分：

```bash
# 节点 1 — 长安街东段
python main.py --mode camera --url rtsp://10.0.1.11:554/stream \
  --road "长安街-East" --port 8001

# 节点 2 — 长安街西段
python main.py --mode camera --url rtsp://10.0.1.12:554/stream \
  --road "长安街-West" --port 8002
```

在后端管理面板中分别添加两个摄像头，`streamUrl` 分别指向 `:8001` 和 `:8002`。

## CLI 参数

```text
python main.py [OPTIONS]
```

| 参数 | 说明 | 默认值 |
| ------ | ------ | -------- |
| `--mode` | 运行模式: `sim` / `camera` | `$MODE` 或 `sim` |
| `--url` | 视频源: RTSP URL / 设备编号 / 文件路径 | `$CAMERA_URL`，留空交互选择 |
| `--road` | 路段名称 | `$ROAD_NAME` 或 `示例路段-Edge01` |
| `--model` | YOLOv8 模型文件名或路径 | `$MODEL` 或 `yolov8n.pt` |
| `--conf` | 检测置信度阈值 | `$CONF` 或 `0.35` |
| `--port` | HTTP 服务端口 | `$PORT` 或 `8000` |
| `--no-openvino` | 禁用 OpenVINO，使用 PyTorch 推理 | 默认启用 OpenVINO |

参数优先级: **CLI 参数 > 环境变量 > 默认值**

### 使用示例

```bash
# 模拟模式
python main.py

# 摄像头模式 - 交互选择设备
python main.py --mode camera

# 摄像头模式 - 指定 RTSP 地址
python main.py --mode camera --url rtsp://192.168.1.100:554/stream

# 摄像头模式 - 本地摄像头设备 0
python main.py --mode camera --url 0

# 自定义端口、路段名、模型
python main.py --mode camera --url 0 --port 9000 --road "长安街-East" --model yolov8s.pt --conf 0.5

# 使用自定义训练模型（支持绝对路径或相对路径）
python main.py --mode camera --url 0 --model /path/to/best.pt
python main.py --mode camera --url 0 --model ./runs/detect/train/weights/best.pt

# 禁用 OpenVINO，使用原始 PyTorch 推理
python main.py --mode camera --url 0 --no-openvino
```

## API 接口

### GET `/api/traffic`

交通数据 + 边缘节点性能指标。后端 `CameraPollerService` 每 3 秒轮询此接口。

```json
{
  "count_car": 5,
  "count_motor": 2,
  "speed_car": 38.5,
  "speed_motor": 25.0,
  "edge_metrics": {
    "cpu_percent": 45.2,
    "memory_percent": 62.1,
    "memory_used": 4123456789,
    "memory_total": 8589934592,
    "inference_ms": 23.4,
    "fps": 18.5,
    "uptime_s": 3600,
    "model": "yolov8n.pt",
    "gpu_percent": 30.0,
    "gpu_memory_percent": 25.5
  }
}
```

> `gpu_percent` / `gpu_memory_percent` 在无 NVIDIA GPU 时为 `null`。

### GET `/api/frame`

返回最新一帧 JPEG 检测画面（带标注框和信息叠加层）。

- Content-Type: `image/jpeg`
- 未就绪时返回 1x1 灰色占位图

### GET `/api/metrics`

独立的性能指标端点，返回格式与 `edge_metrics` 字段一致。

```json
{
  "cpu_percent": 45.2,
  "memory_percent": 62.1,
  "memory_used": 4123456789,
  "memory_total": 8589934592,
  "inference_ms": 23.4,
  "fps": 18.5,
  "uptime_s": 3600,
  "model": "yolov8n.pt",
  "gpu_percent": null,
  "gpu_memory_percent": null
}
```

### GET `/health`

健康检查端点。

```json
{
  "status": "ok",
  "mode": "camera",
  "road": "示例路段-Edge01",
  "model": "yolov8n.pt",
  "timestamp": "2026-02-24T10:30:00.123456"
}
```

## 车辆分类 (COCO)

| 类别 | COCO ID | 归类 |
| ------ | --------- | ------ |
| car | 2 | 汽车 |
| bus | 5 | 汽车 |
| truck | 7 | 汽车 |
| bicycle | 1 | 摩托/非机动车 |
| motorcycle | 3 | 摩托/非机动车 |

## OpenVINO 加速

默认启用 OpenVINO 对 CPU 推理进行加速，无需 GPU 即可获得显著性能提升。

工作流程：

1. 首次启动时自动将 `.pt` 模型导出为 OpenVINO IR 格式（生成 `yolov8n_openvino_model/` 目录）
2. 后续启动直接加载 IR 模型，跳过导出步骤
3. 通过 `--no-openvino` 或 `OPENVINO=false` 可回退到 PyTorch 推理

环境变量控制: `OPENVINO=true`（默认） / `OPENVINO=false`
