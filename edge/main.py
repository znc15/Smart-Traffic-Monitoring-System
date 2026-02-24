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
import webbrowser
from contextlib import asynccontextmanager

import psutil
from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles

import config
from routes import router
from loops import camera_loop, sim_loop
from camera_discovery import interactive_select


# ---------------------------------------------------------------------------
# 自动打开浏览器（后台线程，延迟等待服务器就绪）
# ---------------------------------------------------------------------------
def _open_browser(port: int, timeout: float = 10.0) -> None:
    """在后台线程中轮询 /health 端点，服务器就绪后打开浏览器"""
    import time
    import urllib.request
    import urllib.error

    url = f"http://localhost:{port}"
    health_url = f"{url}/health"
    deadline = time.monotonic() + timeout
    interval = 0.3  # 轮询间隔（秒）

    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen(health_url, timeout=1):
                # 服务器已就绪
                break
        except (urllib.error.URLError, OSError):
            time.sleep(interval)
    else:
        # 超时仍未就绪，仍尝试打开（可能只是 /health 响应慢）
        print(f"[WARN] 等待服务器就绪超时 ({timeout}s)，仍尝试打开浏览器")

    try:
        webbrowser.open(url)
        print(f"[INFO] 已自动打开浏览器: {url}")
    except Exception:
        # Docker / 无头环境中 webbrowser 可能不可用，静默忽略
        print("[WARN] 无法自动打开浏览器，请手动访问仪表盘")


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

    # 打印仪表盘访问地址
    print(f"[INFO] Dashboard: http://localhost:{config.HTTP_PORT}")

    # 自动打开浏览器（除非被禁用）
    if not config.NO_BROWSER:
        bt = threading.Thread(target=_open_browser, args=(config.HTTP_PORT,), daemon=True)
        bt.start()

    yield


# ---------------------------------------------------------------------------
# FastAPI 应用
# ---------------------------------------------------------------------------
app = FastAPI(title="Edge Node - YOLOv8 智能交通检测", lifespan=lifespan)
app.include_router(router)

# 挂载静态文件目录（仪表盘 HTML 等）
_static_dir = os.path.join(os.path.dirname(__file__), "static")
os.makedirs(_static_dir, exist_ok=True)
app.mount("/static", StaticFiles(directory=_static_dir), name="static")


# ---------------------------------------------------------------------------
# 命令行参数解析
# ---------------------------------------------------------------------------
def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="智能交通监控 - 边缘检测节点",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    p.add_argument(
        "--mode", choices=["sim", "camera"], default=None,
        help="运行模式: sim=模拟, camera=摄像头 (默认读取 $MODE 或 sim)",
    )
    p.add_argument(
        "--url", default=None,
        help="视频源地址: RTSP URL / 本地设备编号 / 文件路径 (留空则交互选择)",
    )
    p.add_argument(
        "--road", default=None,
        help="路段名称，用于显示和上报 (默认读取 $ROAD_NAME)",
    )
    p.add_argument(
        "--model", default=None,
        help="YOLOv8 模型文件名 (默认读取 $MODEL 或 yolov8n.pt)",
    )
    p.add_argument(
        "--conf", type=float, default=None,
        help="检测置信度阈值 (默认读取 $CONF 或 0.35)",
    )
    p.add_argument(
        "--port", type=int, default=None,
        help="HTTP 服务端口 (默认读取 $PORT 或 8000)",
    )
    p.add_argument(
        "--no-openvino", action="store_true", default=False,
        help="禁用 OpenVINO 加速，使用原始 PyTorch 推理",
    )
    p.add_argument(
        "--no-browser", action="store_true", default=False,
        help="禁用启动时自动打开浏览器（Docker/无头环境使用）",
    )
    return p.parse_args()


# ---------------------------------------------------------------------------
# 将 CLI 参数写入 config（CLI > 环境变量 > 默认值）
# ---------------------------------------------------------------------------
def apply_args(args: argparse.Namespace) -> int:
    """应用命令行参数到 config 模块，返回端口号"""
    if args.mode is not None:
        config.MODE = args.mode
    if args.road is not None:
        config.ROAD_NAME = args.road
    if args.model is not None:
        config.MODEL_NAME = args.model
    if args.conf is not None:
        config.CONF_THRESHOLD = args.conf
    if args.no_openvino:
        config.USE_OPENVINO = False
    if args.no_browser:
        config.NO_BROWSER = True

    port = args.port or config.HTTP_PORT

    # 将端口写入 config，供 lifespan 和其他模块读取
    config.HTTP_PORT = port

    # 摄像头源决策
    if config.MODE == "camera":
        if args.url is not None:
            # CLI 显式指定了地址
            try:
                config.camera_source = int(args.url)
            except ValueError:
                config.camera_source = args.url
        elif config.CAMERA_URL:
            # 环境变量指定了地址
            try:
                config.camera_source = int(config.CAMERA_URL)
            except ValueError:
                config.camera_source = config.CAMERA_URL
        else:
            # 都没指定，交互选择
            result = interactive_select()
            if result == "sim":
                config.MODE = "sim"
                print("[INFO] 已切换到模拟模式")
            else:
                config.camera_source = result

    return port


# ---------------------------------------------------------------------------
# 入口
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    import uvicorn

    args = parse_args()
    port = apply_args(args)
    uvicorn.run("main:app", host="0.0.0.0", port=port)
