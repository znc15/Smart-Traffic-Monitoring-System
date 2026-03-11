# Smart Traffic Monitoring — Edge Node

> 边缘计算节点，负责视频流接入、车辆检测与追踪、交通数据采集及主动上报。

![Python](https://img.shields.io/badge/Python-3.10+-3776AB?logo=python&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-0.115-009688?logo=fastapi&logoColor=white)
![YOLOv8](https://img.shields.io/badge/YOLOv8-Ultralytics_≥8.2-FF6F00?logo=yolo&logoColor=white)
![OpenVINO](https://img.shields.io/badge/OpenVINO-≥2024.0-0068B5?logo=intel&logoColor=white)
![OpenCV](https://img.shields.io/badge/OpenCV-4.10_headless-5C3EE8?logo=opencv&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 目录

- [核心能力](#核心能力)
- [技术栈](#技术栈)
- [架构](#架构)
- [目录结构](#目录结构)
- [API 参考](#api-参考)
- [环境变量参考](#环境变量参考)
- [安装](#安装)
- [启动](#启动)
- [追踪后端切换](#追踪后端切换)
- [测试](#测试)

---

## 核心能力

- **YOLOv8 目标检测** — 支持多种模型（nano → xlarge），自动下载
- **OpenVINO 推理加速** — Intel 平台 CPU/iGPU 推理加速，一键启用
- **ByteTrack 多目标追踪** — 高精度在线追踪，支持降级到 SimpleTracker
- **MJPEG 实时视频流** — 浏览器直接预览，叠加检测框与统计信息
- **配置热更新** — 运行时修改检测参数，无需重启服务
- **主动遥测上报** — 定时向后端推送交通统计与节点健康数据
- **仿真模式** — 无需摄像头即可开发调试

---

## 技术栈

| 类别 | 技术 | 版本 / 说明 |
|:-----|:-----|:------------|
| Web 框架 | FastAPI | 0.115 |
| 目标检测 | Ultralytics YOLOv8 | ≥ 8.2 |
| 多目标追踪 | ByteTrack (lapx) | lapx ≥ 0.5.11 |
| 降级追踪 | SimpleTracker | 内置，无额外依赖 |
| 推理加速 | OpenVINO | ≥ 2024.0（可选） |
| 视觉处理 | OpenCV (headless) | 4.10 |
| 深度学习 | PyTorch (CPU) | — |

---

## 架构

```mermaid
graph LR
    A[摄像头 / 仿真源] -->|帧采集| B(主循环 loops.py)
    B -->|每帧| C[YOLOv8 检测 detector.py]
    C -->|检测结果| D[ByteTrack 追踪 tracking_engine.py]
    D -->|追踪数据| E[交通数据增强 traffic_enrichment.py]
    E -->|统计结果| F[状态存储 state.py]
    F -->|API 响应| G[FastAPI 路由 routes.py]
    F -->|定时上报| H[遥测上报 telemetry_reporter.py]
    G -->|HTTP/MJPEG| I[前端 / 监控大屏]
    H -->|POST| J[后端服务]
```

**数据流**：帧采集 → 检测 → 追踪 → 数据增强 → 状态汇聚 → API 响应 + 遥测上报

---

## 目录结构

```
edge/
├── main.py                  # FastAPI 入口、启动参数解析、检测循环
├── detector.py              # YOLOv8 检测封装、OpenVINO 加速逻辑
├── tracking_engine.py       # ByteTrack 追踪引擎
├── simple_tracker.py        # 降级追踪器（IoU 匹配）
├── telemetry_reporter.py    # 主动上报后端（交通统计 + 健康数据）
├── routes.py                # FastAPI API 路由定义
├── config.py                # 配置管理（环境变量 + 热更新）
├── state.py                 # 全局运行时状态
├── overlay.py               # 视频帧叠加层（检测框、标签、统计）
├── camera_discovery.py      # 摄像头发现与枚举
├── resource_manager.py      # 资源生命周期管理
├── loops.py                 # 主检测循环
├── traffic_enrichment.py    # 交通数据增强（车流量、密度、速度估算）
├── validators.py            # 输入参数验证
├── requirements.txt         # Python 依赖
├── Dockerfile               # 容器镜像
├── docker-compose.yml       # 独立 Edge 编排
├── models/                  # 模型文件目录（自动下载或手动放置）
├── samples/                 # 样本数据（仿真模式使用）
├── static/                  # 静态资源
└── tests/                   # 测试用例
```

---

## API 参考

| 方法 | 路径 | 说明 |
|:-----|:-----|:-----|
| `GET` | `/health` | 健康检查，返回节点状态 |
| `GET` | `/api/traffic` | 实时交通数据（车流量、车辆列表、统计摘要） |
| `GET` | `/api/frame` | 获取当前检测帧（JPEG） |
| `GET` | `/api/stream` | MJPEG 实时视频流（浏览器可直接用 `<img>` 展示） |
| `GET` | `/api/metrics` | 性能指标（FPS、推理耗时、内存占用） |
| `GET` | `/api/config` | 获取当前运行配置 |
| `PUT` | `/api/config` | 更新配置（热更新，无需重启） |
| `GET` | `/api/models` | 列出可用模型文件 |
| `POST` | `/api/cameras/probe` | 测试 RTSP/视频流地址连通性（需要 Edge Key） |
| `POST` | `/api/detect/image` | 上传图片检测（需要 Edge Key） |
| `POST` | `/api/detect/video` | 上传视频检测（需要 Edge Key） |
| `GET` | `/api/detect/video/result/{id}` | 下载标注视频（需要 Edge Key） |

### 鉴权范围

当 `EDGE_API_KEY` 已配置时，以下接口会统一要求携带 `X-Edge-Key`：

- `/api/traffic`
- `/api/frame`
- `/api/stream`
- `/api/video`
- `/api/metrics`
- `/api/config`
- `/api/cameras/probe`
- `/api/detect/image`
- `/api/detect/video`
- `/api/detect/video/result/{id}`

说明：
- `X-Edge-Node-Id` 可选；当节点配置了 `EDGE_NODE_ID` 时会一起校验。
- `edge_key` / `edge_node_id` query 参数仅作为 `<img>`、`<video>`、下载链接等无法自定义 header 场景的兜底。

**示例请求**：

```bash
# 健康检查
curl http://localhost:9000/health

# 获取实时交通数据
curl http://localhost:9000/api/traffic

# 热更新检测置信度
curl -X PUT http://localhost:9000/api/config \
  -H "X-Edge-Key: edge-secret" \
  -H "Content-Type: application/json" \
  -d '{"confidence": 0.4}'

# 浏览器打开视频流
open "http://localhost:9000/api/stream?edge_key=edge-secret"
```

---

## 环境变量参考

| 变量 | 说明 | 默认值 |
|:-----|:-----|:-------|
| `MODE` | 运行模式：`sim`（仿真）/ `camera`（摄像头） | `sim` |
| `CAMERA_URL` | 摄像头地址（RTSP URL 或设备索引） | `0` |
| `ROAD_NAME` | 路段名称（用于上报与显示） | — |
| `MODEL` | 模型文件路径 | `yolov8n.pt` |
| `CONF` | 检测置信度阈值 | `0.25` |
| `OPENVINO` | 启用 OpenVINO 加速（`true` / `false`） | `false` |
| `TRACKER_BACKEND` | 追踪后端：`bytetrack` / `simple` | `bytetrack` |
| `TRACKER_STRICT` | 严格模式（缺少依赖时报错而非降级） | `false` |
| `TRACKER_CFG` | ByteTrack 自定义配置文件路径 | — |
| `EDGE_NODE_ID` | 节点唯一标识（多节点部署时必填） | — |
| `BACKEND_TELEMETRY_URL` | 后端遥测上报地址 | — |
| `TELEMETRY_INTERVAL_SEC` | 遥测上报间隔（秒） | — |

---

## 安装

**前置条件**：Python ≥ 3.10

```bash
cd edge

# 创建虚拟环境
python3 -m venv .venv
source .venv/bin/activate    # Windows: .venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 测试依赖（推荐本地开发使用）
pip install -r requirements-dev.txt
```

> 如需 OpenVINO 加速，确保已安装 `openvino >= 2024.0`，并设置 `OPENVINO=true`。

---

## 启动

### 仿真模式（无需摄像头）

```bash
python main.py --mode sim --port 9000 --no-browser
```

### 摄像头模式

```bash
# 本地摄像头（设备索引 0）
python main.py --mode camera --url 0 --road "陈兴道路" --port 9000 --no-browser

# RTSP 流
python main.py --mode camera --url "rtsp://admin:pass@192.168.1.100/stream" --road "人民路" --port 9000 --no-browser
```

### Docker 部署

```bash
docker compose up -d
```

---

## 追踪后端切换

Edge 支持两种多目标追踪后端，可通过环境变量动态切换：

| 后端 | 特点 | 适用场景 |
|:-----|:-----|:---------|
| `bytetrack` | 高精度，基于 Kalman 滤波 + 匈牙利匹配 | 生产环境推荐 |
| `simple` | 轻量级 IoU 匹配，零额外依赖 | 资源受限设备 / 快速验证 |

```bash
# ByteTrack（默认）
TRACKER_BACKEND=bytetrack python main.py --mode camera --url 0

# SimpleTracker 降级模式
TRACKER_BACKEND=simple python main.py --mode camera --url 0

# ByteTrack 严格模式（缺少 lapx 时直接报错，而非静默降级）
TRACKER_BACKEND=bytetrack TRACKER_STRICT=true python main.py --mode camera --url 0
```

---

## 测试

```bash
# 安装测试依赖
python3 -m pip install -r requirements-dev.txt

# 运行 Edge 测试
pytest -q tests/
```

如果只需要跑接口 / 状态机相关测试，`requirements-dev.txt` 已刻意避免引入 `torch` / `ultralytics` / `openvino`；
上传检测等接口在缺少完整推理依赖时会返回 `503`，但不会阻塞鉴权与配置测试。

## 热更新说明

- `frame_skip`、`jpeg_quality`、`telemetry_interval_sec`、交通规则参数会动态生效，不强制重启。
- `mode`、`camera_source`、模型/推理后端相关参数会触发检测循环热重启。
- 若旧检测线程在 5 秒内未退出，本次更新会返回失败并回滚配置，不会强行启动第二个检测线程。
