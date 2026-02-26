"""
FastAPI 路由定义
"""

import asyncio
import base64
import logging
import shutil
import subprocess
import threading
import time
import uuid
from datetime import datetime
from pathlib import Path
from typing import AsyncGenerator, Optional

import cv2
import numpy as np
from fastapi import APIRouter, UploadFile, File, HTTPException, Query
from fastapi.responses import FileResponse, JSONResponse, RedirectResponse, Response, StreamingResponse
from pydantic import BaseModel, Field, field_validator

import camera_discovery
import config
from detector import detect_vehicles_detailed
from state import state
from resource_manager import get_resource_level, get_resource_params
from validators import validate_probe_source, has_openvino_cache

logger = logging.getLogger(__name__)

router = APIRouter()

# edge 目录路径（用于扫描模型文件）
_EDGE_DIR = Path(__file__).parent

# ---------------------------------------------------------------------------
# 文件上传检测相关常量
# ---------------------------------------------------------------------------
# 临时文件目录
_TMP_DIR = _EDGE_DIR / "tmp"
_TMP_DIR.mkdir(exist_ok=True)

# 允许的文件类型
_IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "bmp", "webp"}
_VIDEO_EXTENSIONS = {"mp4", "avi", "mov", "mkv", "webm"}

# 文件大小限制（字节）
_MAX_IMAGE_SIZE = 10 * 1024 * 1024   # 10 MB
_MAX_VIDEO_SIZE = 100 * 1024 * 1024  # 100 MB

# 视频结果过期时间（秒）
_VIDEO_RESULT_EXPIRE_S = 30 * 60  # 30 分钟

# 视频处理：每隔 N 帧检测一次
_VIDEO_FRAME_SKIP = 3


# ---------------------------------------------------------------------------
# 配置更新请求体（所有字段可选，仅更新传入的字段）
# ---------------------------------------------------------------------------
class ConfigUpdateRequest(BaseModel):
    mode: Optional[str] = Field(None, pattern=r"^(sim|camera)$", description="运行模式: sim 或 camera")
    camera_source: Optional[str] = Field(None, description="摄像头源地址")
    model: Optional[str] = Field(None, pattern=r"^[a-zA-Z0-9_\-\.]+\.pt$", description="模型文件名（仅允许 .pt 文件，禁止路径分隔符）")
    confidence: Optional[float] = Field(None, ge=0.1, le=0.9, description="检测置信度阈值 (0.1-0.9)")
    road_name: Optional[str] = Field(None, description="路段名称")
    use_openvino: Optional[bool] = Field(None, description="是否启用 OpenVINO 加速")
    frame_skip: Optional[int] = Field(None, ge=1, le=30, description="每N帧推理一次 (1-30)")
    imgsz: Optional[int] = Field(None, ge=160, le=1280, description="YOLO 输入分辨率 (160-1280)")
    quantize: Optional[str] = Field(None, pattern=r"^(int8|fp16|none)$", description="量化模式: int8, fp16, none")
    jpeg_quality: Optional[int] = Field(None, ge=30, le=100, description="JPEG 编码质量 (30-100)")

    @field_validator('imgsz')
    @classmethod
    def imgsz_multiple_of_32(cls, v):
        if v is not None and v % 32 != 0:
            raise ValueError('imgsz must be a multiple of 32')
        return v


# ---------------------------------------------------------------------------
# 摄像头探测请求体
# ---------------------------------------------------------------------------
class ProbeRequest(BaseModel):
    url: str = Field(..., max_length=2048, description="RTSP/HTTP 视频流地址、本地设备号或 /dev/... 设备路径")

    @field_validator("url")
    @classmethod
    def validate_source(cls, value: str) -> str:
        return validate_probe_source(value)


def _current_config() -> dict:
    """读取 config 模块当前值，返回配置字典"""
    return {
        "mode": config.MODE,
        "camera_source": str(config.camera_source),
        "model": config.MODEL_NAME,
        "confidence": config.CONF_THRESHOLD,
        "road_name": config.ROAD_NAME,
        "use_openvino": config.USE_OPENVINO,
        "frame_skip": config.FRAME_SKIP,
        "imgsz": config.IMGSZ,
        "quantize": config.QUANTIZE,
        "jpeg_quality": config.JPEG_QUALITY,
    }

# ---------------------------------------------------------------------------
# 预计算占位 JPEG（1x1 灰色图），避免每次循环重复编码
# ---------------------------------------------------------------------------
_placeholder_img = np.full((1, 1, 3), 128, dtype=np.uint8)
_, _placeholder_buf = cv2.imencode(".jpg", _placeholder_img)
_PLACEHOLDER_JPEG: bytes = _placeholder_buf.tobytes()

# ---------------------------------------------------------------------------
# MJPEG stream concurrency control (lock + counter, reads config dynamically)
# ---------------------------------------------------------------------------
_stream_lock = asyncio.Lock()
_stream_count = 0


@router.get("/api/traffic", response_model=None)
def get_traffic() -> JSONResponse:
    """交通数据 + 边缘节点性能指标（后端每 3 秒轮询此接口）"""
    data = state.get_traffic()
    data["edge_metrics"] = state.get_edge_metrics()
    return JSONResponse(content=data)


@router.get("/api/frame", response_model=None)
def get_frame() -> Response:
    """最新 JPEG 检测帧"""
    frame = state.get_frame()
    if not frame:
        # 尚未生成帧时返回预计算的 1x1 灰色占位图
        frame = _PLACEHOLDER_JPEG
    return Response(content=frame, media_type="image/jpeg")


@router.get("/api/metrics", response_model=None)
def get_metrics() -> JSONResponse:
    """详细性能指标（独立端点）"""
    return JSONResponse(content=state.get_edge_metrics())


@router.get("/health", response_model=None)
def health_check() -> JSONResponse:
    """健康检查"""
    return JSONResponse(content={
        "status": "ok",
        "mode": config.MODE,
        "road": config.ROAD_NAME,
        "model": config.MODEL_NAME,
        "timestamp": datetime.now().isoformat(),
    })


@router.get("/api/resource-mode", response_model=None)
def get_resource_mode() -> JSONResponse:
    """Return current resource level and active resource-aware parameters."""
    return JSONResponse(content=get_resource_params())


@router.get("/", response_model=None)
def root_redirect() -> RedirectResponse:
    """根路由重定向到仪表盘页面"""
    return RedirectResponse(url="/static/index.html")


async def _mjpeg_generator() -> AsyncGenerator[bytes, None]:
    """逐帧生成 MJPEG 流数据，客户端断开时自动终止"""
    global _stream_count
    try:
        while True:
            frame = state.get_frame()
            # Fall back to placeholder when no frame is available yet
            payload = frame if frame else _PLACEHOLDER_JPEG
            yield (
                b"--frame\r\n"
                b"Content-Type: image/jpeg\r\n\r\n"
                + payload
                + b"\r\n"
            )
            await asyncio.sleep(0.05)  # ~20 FPS
    except asyncio.CancelledError:
        logger.info("MJPEG 客户端断开，生成器已终止")
    finally:
        async with _stream_lock:
            _stream_count -= 1


@router.get("/api/stream", response_model=None)
async def stream_mjpeg() -> StreamingResponse | JSONResponse:
    """MJPEG 视频流端点，持续推送检测帧（并发数由 resource_manager 动态控制）"""
    global _stream_count
    async with _stream_lock:
        if _stream_count >= config.MAX_MJPEG_CLIENTS:
            return JSONResponse(
                status_code=503,
                content={"error": "流客户端数已达上限", "max": config.MAX_MJPEG_CLIENTS},
            )
        _stream_count += 1
    return StreamingResponse(
        _mjpeg_generator(),
        media_type="multipart/x-mixed-replace; boundary=frame",
    )


# ---------------------------------------------------------------------------
# WebM 视频流（FFmpeg 编码）
# ---------------------------------------------------------------------------
def _ffmpeg_available() -> bool:
    """检查 FFmpeg 是否可用"""
    return shutil.which("ffmpeg") is not None


async def _video_generator() -> AsyncGenerator[bytes, None]:
    """使用 FFmpeg 将 JPEG 帧编码为 WebM (VP8) 视频流"""
    # 先获取一帧确定分辨率
    frame_data = state.get_frame()
    if not frame_data:
        await asyncio.sleep(1)
        frame_data = state.get_frame()
    if not frame_data:
        return

    # 解码获取分辨率
    nparr = np.frombuffer(frame_data, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    if img is None:
        return
    h, w = img.shape[:2]

    # 启动 FFmpeg: 接收 raw BGR 帧，输出 WebM (VP8)
    cmd = [
        "ffmpeg",
        "-f", "rawvideo",
        "-pixel_format", "bgr24",
        "-video_size", f"{w}x{h}",
        "-framerate", "20",
        "-i", "pipe:0",
        "-c:v", "libvpx",
        "-b:v", "1M",
        "-deadline", "realtime",
        "-cpu-used", "8",
        "-f", "webm",
        "-cluster_size_limit", "65536",
        "-cluster_time_limit", "100",
        "pipe:1",
    ]

    proc = subprocess.Popen(
        cmd,
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
    )

    stop = threading.Event()

    def _writer():
        """写入线程：持续将帧写入 FFmpeg stdin"""
        while not stop.is_set():
            jpeg = state.get_frame()
            if not jpeg:
                stop.wait(0.05)
                continue
            arr = np.frombuffer(jpeg, np.uint8)
            frame = cv2.imdecode(arr, cv2.IMREAD_COLOR)
            if frame is None:
                stop.wait(0.05)
                continue
            # 确保分辨率一致
            if frame.shape[1] != w or frame.shape[0] != h:
                frame = cv2.resize(frame, (w, h))
            try:
                proc.stdin.write(frame.tobytes())
                proc.stdin.flush()
            except (BrokenPipeError, OSError):
                break
            stop.wait(0.05)  # ~20fps

    writer_thread = threading.Thread(target=_writer, daemon=True)
    writer_thread.start()

    try:
        # 读取 FFmpeg 输出并 yield
        loop = asyncio.get_running_loop()
        while True:
            chunk = await loop.run_in_executor(
                None, proc.stdout.read, 4096
            )
            if not chunk:
                break
            yield chunk
    except (asyncio.CancelledError, GeneratorExit):
        pass
    finally:
        stop.set()
        try:
            proc.stdin.close()
        except OSError:
            pass
        proc.terminate()
        try:
            proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            proc.kill()


@router.get("/api/video", response_model=None)
async def video_stream() -> StreamingResponse | JSONResponse:
    """真正的视频流（WebM/VP8 编码），需要 FFmpeg"""
    if not _ffmpeg_available():
        return JSONResponse(
            status_code=503,
            content={"detail": "FFmpeg 未安装，无法提供视频流。请使用 /api/stream (MJPEG)"},
        )
    return StreamingResponse(
        _video_generator(),
        media_type="video/webm",
    )


# ---------------------------------------------------------------------------
# 配置读写 API
# ---------------------------------------------------------------------------
@router.get("/api/config", response_model=None)
def get_config() -> JSONResponse:
    """返回当前运行配置"""
    return JSONResponse(content=_current_config())


@router.get("/api/models", response_model=None)
def get_models() -> JSONResponse:
    """扫描 models/ 目录下可用的模型文件列表"""
    models: list[dict] = []

    # 确保 models 目录存在
    models_dir = config.MODELS_DIR
    models_dir.mkdir(exist_ok=True)

    # 扫描 models/ 目录下所有 .pt 文件
    for pt_file in sorted(models_dir.glob("*.pt")):
        stem = pt_file.stem  # 例如 "yolov8n"
        has_openvino = has_openvino_cache(models_dir, stem)

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
@router.put("/api/config", response_model=None)
def update_config(body: ConfigUpdateRequest) -> JSONResponse:
    """更新运行配置（仅更新传入的字段，未传入的保持不变），并触发检测循环热重启"""
    # Snapshot current values before mutation, used to detect what changed
    old_model = config.MODEL_NAME
    old_openvino = config.USE_OPENVINO
    old_imgsz = config.IMGSZ
    old_quantize = config.QUANTIZE
    old_mode = config.MODE
    old_road_name = config.ROAD_NAME

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
        # 验证模型文件实际存在于 models/ 目录下，防止路径穿越
        model_path = config.MODELS_DIR / body.model
        if not model_path.is_file():
            raise HTTPException(
                status_code=400,
                detail=f"模型文件不存在: {body.model}",
            )
        config.MODEL_NAME = body.model
        logger.info("模型切换请求: %s -> %s", old_model, config.MODEL_NAME)

    if body.confidence is not None:
        config.CONF_THRESHOLD = body.confidence

    if body.road_name is not None:
        config.ROAD_NAME = body.road_name
        if old_road_name != config.ROAD_NAME:
            logger.info("路段名称已更新: %s -> %s", old_road_name, config.ROAD_NAME)

    if body.use_openvino is not None:
        config.USE_OPENVINO = body.use_openvino

    if body.frame_skip is not None:
        config.FRAME_SKIP = body.frame_skip

    if body.imgsz is not None:
        config.IMGSZ = body.imgsz

    if body.jpeg_quality is not None:
        config.JPEG_QUALITY = body.jpeg_quality

    if body.quantize is not None:
        config.QUANTIZE = body.quantize

    # Determine whether a full restart is needed.
    # frame_skip, jpeg_quality are read dynamically in the loop — no restart required.
    # model, openvino, quantize, and imgsz (when OpenVINO is active) need a restart
    # because OpenVINO IR models have a fixed input size baked in at export time.
    imgsz_changed = body.imgsz is not None and body.imgsz != old_imgsz
    model_changed = (
        config.MODEL_NAME != old_model
        or config.USE_OPENVINO != old_openvino
        or (body.quantize is not None and config.QUANTIZE != old_quantize)
        or (imgsz_changed and config.USE_OPENVINO)
    )
    needs_restart = model_changed or body.mode is not None

    restarted = False
    if needs_restart and state.restart_callback is not None:
        try:
            state.restart_callback(model_changed=model_changed)
            restarted = True
            logger.info(
                "检测循环热重启完成: mode=%s->%s, model_changed=%s, openvino=%s, quantize=%s, imgsz=%s",
                old_mode,
                config.MODE,
                model_changed,
                config.USE_OPENVINO,
                config.QUANTIZE,
                config.IMGSZ,
            )
        except Exception as e:
            logger.error(f"重启检测循环失败: {e}")

    result = _current_config()
    result["restarted"] = restarted
    return JSONResponse(content=result)


# ---------------------------------------------------------------------------
# 摄像头发现与探测 API
# ---------------------------------------------------------------------------
@router.get("/api/cameras", response_model=None)
async def get_cameras() -> JSONResponse:
    """扫描本地摄像头设备，返回可用设备列表"""
    loop = asyncio.get_running_loop()
    cameras = await loop.run_in_executor(None, camera_discovery.scan_local_cameras)
    return JSONResponse(content={"cameras": cameras})


@router.post("/api/cameras/probe", response_model=None)
async def probe_camera(body: ProbeRequest) -> JSONResponse:
    """测试 RTSP/视频流地址的连通性"""
    loop = asyncio.get_running_loop()
    result = await loop.run_in_executor(None, camera_discovery.probe_rtsp, body.url)
    if result is not None:
        return JSONResponse(content={
            "reachable": True,
            "width": result["width"],
            "height": result["height"],
            "message": "连接成功",
        })
    return JSONResponse(content={
        "reachable": False,
        "message": "无法连接到指定地址",
    })


# ---------------------------------------------------------------------------
# 摄像头预览测试 API
# ---------------------------------------------------------------------------
def _capture_and_detect(source: str) -> dict | None:
    """同步函数：打开摄像头，抓帧，检测，返回结果"""
    # 尝试解析为整数（设备索引）
    try:
        src = int(source)
    except ValueError:
        src = source

    cap = cv2.VideoCapture(src)
    try:
        if not cap.isOpened():
            return None
        # 丢弃前 3 帧，让编解码器稳定
        for _ in range(3):
            cap.read()
        ret, frame = cap.read()
        if not ret or frame is None or frame.size == 0:
            return None
    finally:
        cap.release()

    annotated, count_car, count_motor, inference_ms, details = detect_vehicles_detailed(frame)
    success, jpeg = cv2.imencode(".jpg", annotated, [cv2.IMWRITE_JPEG_QUALITY, 85])
    if not success:
        return None
    return {
        "jpeg": jpeg.tobytes(),
        "count_car": count_car,
        "count_motor": count_motor,
        "inference_ms": inference_ms,
    }


@router.get("/api/cameras/preview", response_model=None)
async def preview_camera(source: str = Query(..., description="摄像头源（设备索引或 URL）")) -> Response:
    """抓取一帧并运行检测，用于测试摄像头是否正常"""
    loop = asyncio.get_running_loop()
    try:
        result = await asyncio.wait_for(
            loop.run_in_executor(None, _capture_and_detect, source),
            timeout=10.0,
        )
    except asyncio.TimeoutError:
        return JSONResponse(status_code=408, content={"detail": "抓帧超时（10秒）"})

    if result is None:
        return JSONResponse(status_code=400, content={"detail": "无法打开摄像头或抓取帧失败"})

    return Response(
        content=result["jpeg"],
        media_type="image/jpeg",
        headers={
            "X-Count-Car": str(result["count_car"]),
            "X-Count-Motor": str(result["count_motor"]),
            "X-Inference-Ms": str(round(result["inference_ms"], 1)),
        },
    )


# ---------------------------------------------------------------------------
# 文件上传检测：辅助函数
# ---------------------------------------------------------------------------
def _cleanup_expired_results() -> None:
    """删除 tmp 目录中超过过期时间的临时文件（包括 input_* 和 result_*.mp4）"""
    now = time.time()
    for f in _TMP_DIR.iterdir():
        if not f.is_file():
            continue
        # 清理范围：result_*.mp4 输出文件 + input_* 残留输入文件
        if f.suffix == ".mp4" or f.name.startswith("input_"):
            try:
                age = now - f.stat().st_mtime
                if age > _VIDEO_RESULT_EXPIRE_S:
                    f.unlink()
                    logger.info("已清理过期临时文件: %s", f.name)
            except OSError:
                pass


def _get_file_extension(filename: str) -> str:
    """提取文件扩展名（小写，不含点号）"""
    return filename.rsplit(".", 1)[-1].lower() if "." in filename else ""


# ---------------------------------------------------------------------------
# POST /api/detect/image - 图片上传检测
# ---------------------------------------------------------------------------
@router.post("/api/detect/image", response_model=None)
async def detect_image(file: UploadFile = File(...)) -> JSONResponse:
    """
    接收上传图片，使用 YOLOv8 检测车辆，
    返回 base64 标注图片和检测结果 JSON
    """
    # 验证文件类型
    ext = _get_file_extension(file.filename or "")
    if ext not in _IMAGE_EXTENSIONS:
        raise HTTPException(
            status_code=400,
            detail=f"不支持的图片格式: .{ext}，允许: {', '.join(sorted(_IMAGE_EXTENSIONS))}",
        )

    # 读取文件内容并检查大小
    content = await file.read()
    if len(content) > _MAX_IMAGE_SIZE:
        raise HTTPException(
            status_code=400,
            detail=f"图片文件过大: {len(content) / 1024 / 1024:.1f}MB，上限 {_MAX_IMAGE_SIZE // 1024 // 1024}MB",
        )

    # 解码图片
    np_arr = np.frombuffer(content, np.uint8)
    frame = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    if frame is None:
        raise HTTPException(status_code=400, detail="无法解码图片，文件可能已损坏")

    # 在线程池中异步调用检测，避免阻塞事件循环
    loop = asyncio.get_running_loop()
    annotated, car_count, motor_count, inference_ms, objects_list = (
        await loop.run_in_executor(None, detect_vehicles_detailed, frame)
    )

    # 将标注图片编码为 JPEG base64
    _, buf = cv2.imencode(".jpg", annotated, [cv2.IMWRITE_JPEG_QUALITY, 85])
    b64_str = base64.b64encode(buf.tobytes()).decode("ascii")

    return JSONResponse(content={
        "success": True,
        "annotated_image": f"data:image/jpeg;base64,{b64_str}",
        "detections": {
            "count_car": car_count,
            "count_motor": motor_count,
            "inference_ms": round(inference_ms, 1),
            "objects": objects_list,
        },
    })


# ---------------------------------------------------------------------------
# POST /api/detect/video - 视频上传检测
# ---------------------------------------------------------------------------
def _process_video(input_path: Path, output_path: Path) -> dict:
    """
    逐帧检测视频并写入标注后的输出视频（同步，在后台线程中调用）
    返回摘要字典
    """
    cap = cv2.VideoCapture(str(input_path))
    if not cap.isOpened():
        raise RuntimeError("无法打开视频文件")

    # 读取视频属性
    fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

    # 初始化视频写入器（mp4v 编码）
    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    writer = cv2.VideoWriter(str(output_path), fourcc, fps, (width, height))

    processed = 0
    sum_car = 0
    sum_motor = 0
    sum_ms = 0.0
    frame_idx = 0
    last_annotated = None

    try:
        while True:
            ret, frame = cap.read()
            if not ret:
                break

            if frame_idx % _VIDEO_FRAME_SKIP == 0:
                # 执行检测
                annotated, car_c, motor_c, ms, _ = detect_vehicles_detailed(frame)
                last_annotated = annotated
                processed += 1
                sum_car += car_c
                sum_motor += motor_c
                sum_ms += ms
            else:
                # 非检测帧：复用上一次标注结果或原始帧
                annotated = last_annotated if last_annotated is not None else frame

            writer.write(annotated)
            frame_idx += 1
    finally:
        cap.release()
        writer.release()
        # 清理输入临时文件
        try:
            input_path.unlink()
        except OSError:
            pass

    duration_s = total_frames / fps if fps > 0 else 0.0

    return {
        "total_frames": total_frames,
        "processed_frames": processed,
        "avg_car_count": round(sum_car / max(processed, 1), 1),
        "avg_motor_count": round(sum_motor / max(processed, 1), 1),
        "avg_inference_ms": round(sum_ms / max(processed, 1), 1),
        "duration_s": round(duration_s, 1),
    }


@router.post("/api/detect/video", response_model=None)
async def detect_video(file: UploadFile = File(...)) -> JSONResponse:
    """
    接收上传视频，逐帧检测车辆，生成标注视频并返回下载链接和摘要
    """
    # 验证文件类型
    ext = _get_file_extension(file.filename or "")
    if ext not in _VIDEO_EXTENSIONS:
        raise HTTPException(
            status_code=400,
            detail=f"不支持的视频格式: .{ext}，允许: {', '.join(sorted(_VIDEO_EXTENSIONS))}",
        )

    # 读取文件内容并检查大小
    content = await file.read()
    if len(content) > _MAX_VIDEO_SIZE:
        raise HTTPException(
            status_code=400,
            detail=f"视频文件过大: {len(content) / 1024 / 1024:.1f}MB，上限 {_MAX_VIDEO_SIZE // 1024 // 1024}MB",
        )

    # 清理过期的历史结果
    _cleanup_expired_results()

    # 生成唯一 ID，保存上传文件到临时目录
    result_id = uuid.uuid4().hex[:12]
    input_path = _TMP_DIR / f"input_{result_id}.{ext}"
    output_path = _TMP_DIR / f"result_{result_id}.mp4"

    input_path.write_bytes(content)

    # 在线程池中异步处理视频，避免阻塞事件循环
    try:
        loop = asyncio.get_running_loop()
        summary = await loop.run_in_executor(None, _process_video, input_path, output_path)
    except RuntimeError as e:
        # 清理残留文件
        for p in (input_path, output_path):
            try:
                p.unlink()
            except OSError:
                pass
        raise HTTPException(status_code=400, detail=str(e))

    return JSONResponse(content={
        "success": True,
        "video_url": f"/api/detect/video/result/{result_id}",
        "summary": summary,
    })


# ---------------------------------------------------------------------------
# GET /api/detect/video/result/{result_id} - 下载标注后的视频
# ---------------------------------------------------------------------------
@router.get("/api/detect/video/result/{result_id}", response_model=None)
async def get_video_result(result_id: str) -> FileResponse:
    """返回标注后的视频文件供下载"""
    # 校验 result_id 格式，防止路径遍历
    if not result_id.isalnum() or len(result_id) != 12:
        raise HTTPException(status_code=400, detail="无效的结果 ID")

    result_path = _TMP_DIR / f"result_{result_id}.mp4"
    if not result_path.exists():
        raise HTTPException(status_code=404, detail="视频结果不存在或已过期")

    return FileResponse(
        path=str(result_path),
        media_type="video/mp4",
        filename=f"detected_{result_id}.mp4",
    )
