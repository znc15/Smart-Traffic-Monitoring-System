"""
FastAPI 路由定义
"""

import asyncio
from datetime import datetime
from typing import AsyncGenerator

import cv2
import numpy as np
from fastapi import APIRouter
from fastapi.responses import JSONResponse, RedirectResponse, Response, StreamingResponse

import config
from state import state

router = APIRouter()


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
        # 尚未生成帧时返回 1x1 灰色占位图
        placeholder = np.full((1, 1, 3), 128, dtype=np.uint8)
        _, jpeg = cv2.imencode(".jpg", placeholder)
        frame = jpeg.tobytes()
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
    """逐帧生成 MJPEG 流数据"""
    while True:
        frame = state.get_frame()
        if frame:
            yield (
                b"--frame\r\n"
                b"Content-Type: image/jpeg\r\n\r\n"
                + frame
                + b"\r\n"
            )
        else:
            # 无帧可用时生成 1x1 灰色占位图
            placeholder = np.full((1, 1, 3), 128, dtype=np.uint8)
            _, jpeg = cv2.imencode(".jpg", placeholder)
            yield (
                b"--frame\r\n"
                b"Content-Type: image/jpeg\r\n\r\n"
                + jpeg.tobytes()
                + b"\r\n"
            )
        await asyncio.sleep(0.05)  # ~20 FPS


@router.get("/api/stream")
async def stream_mjpeg() -> StreamingResponse:
    """MJPEG 视频流端点，持续推送检测帧"""
    return StreamingResponse(
        _mjpeg_generator(),
        media_type="multipart/x-mixed-replace; boundary=frame",
    )
