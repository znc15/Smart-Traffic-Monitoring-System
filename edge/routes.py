"""
FastAPI 路由定义
"""

import asyncio
import logging
from datetime import datetime
from pathlib import Path
from typing import AsyncGenerator, Optional

import cv2
import numpy as np
from fastapi import APIRouter
from fastapi.responses import JSONResponse, RedirectResponse, Response, StreamingResponse
from pydantic import BaseModel, Field

import config
from state import state

logger = logging.getLogger(__name__)

router = APIRouter()

# edge 目录路径（用于扫描模型文件）
_EDGE_DIR = Path(__file__).parent


# ---------------------------------------------------------------------------
# 配置更新请求体（所有字段可选，仅更新传入的字段）
# ---------------------------------------------------------------------------
class ConfigUpdateRequest(BaseModel):
    mode: Optional[str] = Field(None, pattern=r"^(sim|camera)$", description="运行模式: sim 或 camera")
    camera_source: Optional[str] = Field(None, description="摄像头源地址")
    model: Optional[str] = Field(None, description="模型文件名")
    confidence: Optional[float] = Field(None, ge=0.1, le=0.9, description="检测置信度阈值 (0.1-0.9)")
    road_name: Optional[str] = Field(None, description="路段名称")
    use_openvino: Optional[bool] = Field(None, description="是否启用 OpenVINO 加速")


def _current_config() -> dict:
    """读取 config 模块当前值，返回配置字典"""
    return {
        "mode": config.MODE,
        "camera_source": str(config.camera_source),
        "model": config.MODEL_NAME,
        "confidence": config.CONF_THRESHOLD,
        "road_name": config.ROAD_NAME,
        "use_openvino": config.USE_OPENVINO,
    }

# ---------------------------------------------------------------------------
# 预计算占位 JPEG（1x1 灰色图），避免每次循环重复编码
# ---------------------------------------------------------------------------
_placeholder_img = np.full((1, 1, 3), 128, dtype=np.uint8)
_, _placeholder_buf = cv2.imencode(".jpg", _placeholder_img)
_PLACEHOLDER_JPEG: bytes = _placeholder_buf.tobytes()

# ---------------------------------------------------------------------------
# MJPEG 流并发限制
# ---------------------------------------------------------------------------
_MAX_STREAM_CLIENTS = 5
_stream_semaphore = asyncio.Semaphore(_MAX_STREAM_CLIENTS)


@router.get("/api/traffic")
def get_traffic() -> JSONResponse:
    """交通数据 + 边缘节点性能指标（后端每 3 秒轮询此接口）"""
    data = state.get_traffic()
    data["edge_metrics"] = state.get_edge_metrics()
    return JSONResponse(content=data)


@router.get("/api/frame")
def get_frame() -> Response:
    """最新 JPEG 检测帧"""
    frame = state.get_frame()
    if not frame:
        # 尚未生成帧时返回预计算的 1x1 灰色占位图
        frame = _PLACEHOLDER_JPEG
    return Response(content=frame, media_type="image/jpeg")


@router.get("/api/metrics")
def get_metrics() -> JSONResponse:
    """详细性能指标（独立端点）"""
    return JSONResponse(content=state.get_edge_metrics())


@router.get("/health")
def health_check() -> JSONResponse:
    """健康检查"""
    return JSONResponse(content={
        "status": "ok",
        "mode": config.MODE,
        "road": config.ROAD_NAME,
        "model": config.MODEL_NAME,
        "timestamp": datetime.now().isoformat(),
    })


@router.get("/")
def root_redirect() -> RedirectResponse:
    """根路由重定向到仪表盘页面"""
    return RedirectResponse(url="/static/index.html")


async def _mjpeg_generator() -> AsyncGenerator[bytes, None]:
    """逐帧生成 MJPEG 流数据，客户端断开时自动终止"""
    try:
        while True:
            frame = state.get_frame()
            # 无帧可用时使用预计算的占位 JPEG
            payload = frame if frame else _PLACEHOLDER_JPEG
            yield (
                b"--frame\r\n"
                b"Content-Type: image/jpeg\r\n\r\n"
                + payload
                + b"\r\n"
            )
            await asyncio.sleep(0.05)  # ~20 FPS
    except asyncio.CancelledError:
        # 客户端断开连接，正常退出生成器
        logger.info("MJPEG 客户端断开，生成器已终止")
    finally:
        # 释放信号量，允许新客户端连接
        _stream_semaphore.release()


@router.get("/api/stream")
async def stream_mjpeg() -> StreamingResponse | JSONResponse:
    """MJPEG 视频流端点，持续推送检测帧（最多 5 个并发客户端）"""
    # 非阻塞方式获取信号量，超出限制立即返回 503
    if not _stream_semaphore._value:
        return JSONResponse(
            status_code=503,
            content={"error": "流客户端数已达上限", "max": _MAX_STREAM_CLIENTS},
        )
    await _stream_semaphore.acquire()
    return StreamingResponse(
        _mjpeg_generator(),
        media_type="multipart/x-mixed-replace; boundary=frame",
    )


# ---------------------------------------------------------------------------
# 配置读写 API
# ---------------------------------------------------------------------------
@router.get("/api/config")
def get_config() -> JSONResponse:
    """返回当前运行配置"""
    return JSONResponse(content=_current_config())


@router.get("/api/models")
def get_models() -> JSONResponse:
    """扫描 edge 目录下可用的模型文件列表"""
    models: list[dict] = []

    # 扫描所有 .pt 文件
    for pt_file in sorted(_EDGE_DIR.glob("*.pt")):
        # 检查是否存在对应的 OpenVINO 模型目录
        # 命名约定: yolov8n.pt → yolov8n_openvino_model/
        stem = pt_file.stem  # 例如 "yolov8n"
        has_openvino = (_EDGE_DIR / f"{stem}_openvino_model").is_dir()

        try:
            size_mb = round(pt_file.stat().st_size / (1024 * 1024), 1)
        except OSError:
            size_mb = 0.0

        models.append({
            "name": pt_file.name,
            "size_mb": size_mb,
            "has_openvino": has_openvino,
        })

    return JSONResponse(content={
        "models": models,
        "current": config.MODEL_NAME,
    })


@router.put("/api/config")
def update_config(body: ConfigUpdateRequest) -> JSONResponse:
    """更新运行配置（仅更新传入的字段，未传入的保持不变）"""
    # 逐字段检查并更新 config 模块变量
    if body.mode is not None:
        config.MODE = body.mode

    if body.camera_source is not None:
        # 尝试将纯数字字符串转为 int（本地设备编号）
        try:
            config.camera_source = int(body.camera_source)
        except ValueError:
            config.camera_source = body.camera_source

    if body.model is not None:
        config.MODEL_NAME = body.model

    if body.confidence is not None:
        config.CONF_THRESHOLD = body.confidence

    if body.road_name is not None:
        config.ROAD_NAME = body.road_name

    if body.use_openvino is not None:
        config.USE_OPENVINO = body.use_openvino

    return JSONResponse(content=_current_config())
