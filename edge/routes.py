"""
FastAPI 路由定义
"""

from datetime import datetime

import cv2
import numpy as np
from fastapi import APIRouter
from fastapi.responses import JSONResponse, Response

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
