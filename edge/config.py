"""
边缘节点配置
通过环境变量或 CLI 参数注入，支持模拟模式和摄像头模式
"""

import os
from pathlib import Path

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

# HTTP 服务端口（CLI 参数 > 环境变量 > 默认 8000）
HTTP_PORT = int(os.environ.get("PORT", "8000"))

# 推理管线优化参数
FRAME_SKIP = int(os.environ.get("FRAME_SKIP", "2"))      # 每N帧推理一次，1=每帧都推理
IMGSZ = int(os.environ.get("IMGSZ", "320"))               # YOLO 输入分辨率
QUANTIZE = os.environ.get("QUANTIZE", "none")              # 量化模式: "int8", "fp16", "none"

# JPEG encoding quality for live frames (overridden by resource_manager in low mode)
JPEG_QUALITY = int(os.environ.get("JPEG_QUALITY", "80"))

# Max concurrent MJPEG stream clients (overridden by resource_manager in low mode)
MAX_MJPEG_CLIENTS = int(os.environ.get("MAX_MJPEG_CLIENTS", "5"))

# COCO 类别映射
CAR_CLASSES = {2, 5, 7}       # car, bus, truck → 归为"汽车"
MOTOR_CLASSES = {1, 3}        # bicycle, motorcycle → 归为"摩托/非机动车"


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
