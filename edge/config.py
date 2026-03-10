"""
边缘节点配置
通过环境变量或 CLI 参数注入，支持模拟模式和摄像头模式
"""

import math
import os
from pathlib import Path

# ---------------------------------------------------------------------------
# 环境变量解析辅助
# ---------------------------------------------------------------------------
def _parse_float_list(name: str, default: list[float], expected_len: int) -> list[float]:
    raw = os.environ.get(name, "").strip()
    if not raw:
        return list(default)

    try:
        values = [float(part.strip()) for part in raw.split(",")]
    except ValueError:
        return list(default)

    if len(values) != expected_len:
        return list(default)

    if not all(math.isfinite(value) for value in values):
        return list(default)

    return values


# 模型存放目录（edge/models/）
MODELS_DIR = Path(__file__).parent / "models"

# 运行模式：sim = 模拟, camera = 摄像头
MODE = os.environ.get("MODE", "sim")

# RTSP 地址或本地设备编号（环境变量预设值）
CAMERA_URL = os.environ.get("CAMERA_URL", "")

# 运行时实际使用的摄像头源（由交互选择或环境变量决定）
# 在 main.py 启动时赋值
camera_source: int | str = CAMERA_URL

# 路段名称（用于显示和上报）
ROAD_NAME = os.environ.get("ROAD_NAME", "示例路段-Edge01")

# YOLOv8 模型文件名（首次运行自动下载）
MODEL_NAME = os.environ.get("MODEL", "yolov8n.pt")

# 检测置信度阈值
CONF_THRESHOLD = float(os.environ.get("CONF", "0.35"))

# OpenVINO 加速（仅 CPU 推理时有效）
USE_OPENVINO = os.environ.get("OPENVINO", "true").lower() in ("1", "true", "yes")

# 启动时是否自动打开浏览器（Docker/无头环境设为 true 禁用）
NO_BROWSER = os.environ.get("NO_BROWSER", "false").lower() in ("1", "true", "yes")

# HTTP 监听地址（支持 0.0.0.0 / 自定义 IP 绑定）
HTTP_HOST = os.environ.get("HOST", "0.0.0.0").strip() or "0.0.0.0"

# 对外访问地址（可选）；为空时自动根据 HTTP_HOST 推导
PUBLIC_HOST = os.environ.get("PUBLIC_HOST", "").strip()

# HTTP 服务端口（CLI 参数 > 环境变量 > 默认 8000）
HTTP_PORT = int(os.environ.get("PORT", "8000"))

# 推理管线优化参数
FRAME_SKIP = int(os.environ.get("FRAME_SKIP", "2"))      # 每N帧推理一次，1=每帧都推理
IMGSZ = int(os.environ.get("IMGSZ", "320"))               # YOLO 输入分辨率
QUANTIZE = os.environ.get("QUANTIZE", "none").lower()       # 量化模式: "int8", "fp16", "none"
if QUANTIZE not in ("int8", "fp16", "none"):
    QUANTIZE = "none"

# 跟踪后端配置
TRACKER_BACKEND = os.environ.get("TRACKER_BACKEND", "bytetrack").lower()
if TRACKER_BACKEND not in ("bytetrack", "simple"):
    TRACKER_BACKEND = "simple"
TRACKER_STRICT = os.environ.get("TRACKER_STRICT", "false").lower() in ("1", "true", "yes")
TRACKER_CFG = os.environ.get("TRACKER_CFG", "bytetrack.yaml")

# JPEG encoding quality for live frames (overridden by resource_manager in low/ultra_low mode)
JPEG_QUALITY = int(os.environ.get("JPEG_QUALITY", "80"))

# Max concurrent MJPEG stream clients (overridden by resource_manager in low/ultra_low mode)
MAX_MJPEG_CLIENTS = int(os.environ.get("MAX_MJPEG_CLIENTS", "5"))

# Uvicorn worker count (keep at 1 for low-spec industrial PCs)
UVICORN_WORKERS = int(os.environ.get("UVICORN_WORKERS", "1"))

# 主动上报到云端后端（可选）
EDGE_NODE_ID = os.environ.get("EDGE_NODE_ID", "edge-01")
EDGE_API_KEY = os.environ.get("EDGE_API_KEY", "").strip()
BACKEND_TELEMETRY_URL = os.environ.get("BACKEND_TELEMETRY_URL", "").strip()
TELEMETRY_INTERVAL_SEC = float(os.environ.get("TELEMETRY_INTERVAL_SEC", "3"))

# 交通分析参数（均支持热更新）
# analysis_roi: [x1, y1, x2, y2]，归一化到 0~1 的矩形 ROI
ANALYSIS_ROI = _parse_float_list("ANALYSIS_ROI", [0.05, 0.25, 0.95, 0.95], 4)
# lane_split_ratios: 左/中/右三车道分割点，归一化到 0~1
LANE_SPLIT_RATIOS = _parse_float_list("LANE_SPLIT_RATIOS", [0.33, 0.66], 2)
SPEED_METERS_PER_PIXEL = float(os.environ.get("SPEED_METERS_PER_PIXEL", "0.08"))
PARKING_STATIONARY_SECONDS = float(os.environ.get("PARKING_STATIONARY_SECONDS", "8"))
WRONG_WAY_MIN_TRACK_POINTS = int(os.environ.get("WRONG_WAY_MIN_TRACK_POINTS", "4"))


def get_access_host() -> str:
    """
    返回推荐给浏览器/日志的访问地址。
    0.0.0.0 / :: 属于绑定地址，不能直接当成访问 URL 对外展示。
    """
    if PUBLIC_HOST:
        return PUBLIC_HOST
    if HTTP_HOST in {"0.0.0.0", ""}:
        return "127.0.0.1"
    if HTTP_HOST == "::":
        return "localhost"
    return HTTP_HOST

# COCO 类别映射
CAR_CLASSES = {2, 5, 7}       # car, bus, truck → 归为"汽车"
MOTOR_CLASSES = {1, 3}        # bicycle, motorcycle → 归为"摩托/非机动车"
PERSON_CLASSES = {0}          # person


def get_model_path() -> Path:
    """
    获取当前模型的完整路径（MODELS_DIR / MODEL_NAME）
    向后兼容：如果 models/ 下找不到，回退到 edge/ 根目录
    """
    primary = MODELS_DIR / MODEL_NAME
    if primary.exists():
        return primary
    # 兼容旧部署：模型可能还在 edge/ 根目录下
    fallback = Path(__file__).parent / MODEL_NAME
    if fallback.exists():
        return fallback
    # 都不存在时返回 models/ 路径（让 YOLO 自动下载到此处）
    return primary
