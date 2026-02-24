"""
Smart Traffic Monitoring System - Edge Node (YOLOv8)
边缘节点：使用 YOLOv8 进行车辆检测，上报交通数据与节点性能指标

Usage:
  python main.py                                    # 模拟模式（默认）
  python main.py --mode camera                      # 摄像头模式，交互选择设备
  python main.py --mode camera --url rtsp://...     # 指定 RTSP 地址
  python main.py --mode camera --url 0              # 本地摄像头设备 0
  python main.py --port 9000 --road "XX路段"        # 自定义端口和路段名
"""

import os
import argparse
import threading
from contextlib import asynccontextmanager

import psutil
from fastapi import FastAPI

import config
from routes import router
from loops import camera_loop, sim_loop
from camera_discovery import interactive_select


# ---------------------------------------------------------------------------
# 应用生命周期
# ---------------------------------------------------------------------------
@asynccontextmanager
async def lifespan(application: FastAPI):
    """启动后台检测线程"""
    # 预初始化 psutil CPU 采样（首次调用返回 0，需要预热）
    psutil.cpu_percent(interval=None)

    if config.MODE == "camera":
        print(f"[INFO] 摄像头模式，视频源: {config.camera_source}，模型: {config.MODEL_NAME}")
        t = threading.Thread(target=camera_loop, daemon=True)
    else:
        print(f"[INFO] 模拟模式，路段: {config.ROAD_NAME}")
        t = threading.Thread(target=sim_loop, daemon=True)
    t.start()
    yield


# ---------------------------------------------------------------------------
# FastAPI 应用
# ---------------------------------------------------------------------------
app = FastAPI(title="Edge Node - YOLOv8 智能交通检测", lifespan=lifespan)
app.include_router(router)


# ---------------------------------------------------------------------------
# 入口
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", "8000"))

    # 如果是摄像头模式且未指定视频源，启动交互选择
    if config.MODE == "camera" and not config.CAMERA_URL:
        result = interactive_select()
        if result == "sim":
            config.MODE = "sim"
            print("[INFO] 已切换到模拟模式")
        else:
            config.camera_source = result
            print(f"[INFO] 视频源已设置: {result}")
    elif config.MODE == "camera" and config.CAMERA_URL:
        # 环境变量指定了地址，尝试转为整数（本地设备编号）
        try:
            config.camera_source = int(config.CAMERA_URL)
        except ValueError:
            config.camera_source = config.CAMERA_URL

    uvicorn.run("main:app", host="0.0.0.0", port=port)
