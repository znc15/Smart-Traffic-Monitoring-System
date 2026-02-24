# Edge Node - 智能交通边缘检测节点

基于 YOLOv8 的边缘检测节点，负责实时车辆检测并通过 HTTP API 上报交通数据与节点性能指标。后端 `CameraPollerService` 每 3 秒轮询本节点获取数据。

## 项目结构

```
edge/
├── main.py              # 应用入口，CLI 参数解析
├── config.py            # 配置（CLI > 环境变量 > 默认值）
├── camera_discovery.py  # 摄像头自动发现与交互选择
├── detector.py          # YOLOv8 模型加载与车辆检测
├── loops.py             # 摄像头 / 模拟后台循环
├── overlay.py           # 帧信息叠加层绘制
├── routes.py            # FastAPI 路由定义
├── state.py             # 线程安全全局状态 + 性能采集
├── requirements.txt
└── Dockerfile
```

## 快速开始

```bash
pip install -r requirements.txt
python main.py                # 默认模拟模式
```

## CLI 参数

```
python main.py [OPTIONS]
```

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `--mode` | 运行模式: `sim` / `camera` | `$MODE` 或 `sim` |
| `--url` | 视频源: RTSP URL / 设备编号 / 文件路径 | `$CAMERA_URL`，留空交互选择 |
| `--road` | 路段名称 | `$ROAD_NAME` 或 `示例路段-Edge01` |
| `--model` | YOLOv8 模型文件名 | `$MODEL` 或 `yolov8n.pt` |
| `--conf` | 检测置信度阈值 | `$CONF` 或 `0.35` |
| `--port` | HTTP 服务端口 | `$PORT` 或 `8000` |

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
|------|---------|------|
| car | 2 | 汽车 |
| bus | 5 | 汽车 |
| truck | 7 | 汽车 |
| bicycle | 1 | 摩托/非机动车 |
| motorcycle | 3 | 摩托/非机动车 |

## Docker 部署

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
