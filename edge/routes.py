"""
FastAPI 路由定义
"""

import asyncio
import logging
from datetime import datetime
from typing import AsyncGenerator

import cv2
import numpy as np
from fastapi import APIRouter
from fastapi.responses import JSONResponse, RedirectResponse, Response, StreamingResponse

import config
from state import state

logger = logging.getLogger(__name__)

router = APIRouter()

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
