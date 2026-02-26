"""
YOLOv8 车辆检测模块
支持 OpenVINO 加速：首次运行自动将 .pt 导出为 OpenVINO IR，后续直接加载
"""

import threading
import time
import random
from pathlib import Path

import cv2
import numpy as np
from ultralytics import YOLO

import config

# ---------------------------------------------------------------------------
# Color constants for detection overlays
# ---------------------------------------------------------------------------
_COLOR_CAR = (255, 120, 40)     # blue-orange for cars
_COLOR_MOTOR = (40, 220, 120)   # green for motorcycles

# ---------------------------------------------------------------------------
# 模型加载（懒加载单例，支持 OpenVINO，线程安全）
# ---------------------------------------------------------------------------
_model: YOLO | None = None
_model_lock = threading.Lock()


def reset_model() -> None:
    """重置模型缓存，强制下次调用时重新加载（用于模型热切换）"""
    global _model
    with _model_lock:
        _model = None
    print("[INFO] 模型缓存已清除，下次检测时将重新加载")


def _get_openvino_path() -> Path:
    """Return the OpenVINO IR model directory path.

    The directory name encodes both quantization mode and input size so that
    exports with different settings do not collide.  Format:
      {stem}[_int8]_{imgsz}_openvino_model/

    Examples:
      yolov8n_320_openvino_model/
      yolov8n_int8_320_openvino_model/
      yolov8n_640_openvino_model/
    """
    model_path = config.get_model_path()
    # Only INT8 quantization adds a prefix before the size tag; FP16 does not.
    quant_tag = "_int8" if config.QUANTIZE == "int8" else ""
    return model_path.parent / f"{model_path.stem}{quant_tag}_{config.IMGSZ}_openvino_model"


def _load_model() -> YOLO:
    """
    加载 YOLOv8 模型（双重检查锁定，线程安全）
    - USE_OPENVINO=True: 自动导出并加载 OpenVINO IR 格式（CPU 加速）
    - USE_OPENVINO=False: 直接加载 PyTorch 模型
    """
    global _model
    # 第一次检查（无锁，快速路径）
    if _model is not None:
        return _model

    with _model_lock:
        # 第二次检查（持锁，防止并发重复加载）
        if _model is not None:
            return _model

        # 获取模型完整路径（优先 models/ 目录，兼容旧部署回退到 edge/ 根目录）
        model_path = config.get_model_path()

        if config.USE_OPENVINO:
            ov_dir = _get_openvino_path()
            if not ov_dir.exists():
                print(f"[INFO] 首次运行，导出 OpenVINO 模型: {model_path} → {ov_dir}")
                pt_model = YOLO(str(model_path))
                export_kwargs: dict = {
                    "format": "openvino",
                    "imgsz": config.IMGSZ,
                }
                if config.QUANTIZE == "fp16":
                    export_kwargs["half"] = True
                    print("[INFO] 启用 FP16 半精度量化导出")
                elif config.QUANTIZE == "int8":
                    export_kwargs["int8"] = True
                    print("[INFO] 启用 INT8 量化导出")
                print(f"[INFO] 导出参数: imgsz={config.IMGSZ}, quantize={config.QUANTIZE}")
                pt_model.export(**export_kwargs)
                print(f"[INFO] OpenVINO 导出完成: {ov_dir}")

            print(f"[INFO] 加载 OpenVINO 模型: {ov_dir}")
            _model = YOLO(ov_dir)
            print("[INFO] OpenVINO 模型加载完成 (CPU 加速已启用)")
        else:
            print(f"[INFO] 加载 PyTorch 模型: {model_path}")
            _model = YOLO(str(model_path))
            print("[INFO] PyTorch 模型加载完成")

        return _model


# ---------------------------------------------------------------------------
# 车辆检测
# ---------------------------------------------------------------------------
def detect_vehicles(frame: np.ndarray) -> tuple[np.ndarray, int, int, float]:
    """
    使用 YOLOv8 检测车辆
    返回: (标注后的帧, 汽车数, 摩托车数, 推理耗时ms)
    内部委托给 detect_vehicles_detailed，丢弃额外的目标列表数据
    """
    annotated, count_car, count_motor, inference_ms, _ = detect_vehicles_detailed(frame)
    return annotated, count_car, count_motor, inference_ms


def detect_vehicles_detailed(
    frame: np.ndarray,
) -> tuple[np.ndarray, int, int, float, list[dict]]:
    """
    使用 YOLOv8 检测车辆（详细模式）
    与 detect_vehicles 相同的检测逻辑，但额外返回每个目标的详细信息
    返回: (标注后的帧, 汽车数, 摩托车数, 推理耗时ms, 目标列表)
    目标列表: [{"class": "car"/"motor", "confidence": float, "bbox": [x1,y1,x2,y2]}]
    """
    model = _load_model()

    t0 = time.time()
    results = model(frame, conf=config.CONF_THRESHOLD, imgsz=config.IMGSZ, verbose=False)
    inference_ms = (time.time() - t0) * 1000

    count_car = 0
    count_motor = 0
    objects_list: list[dict] = []
    annotated = frame.copy()

    for r in results:
        boxes = r.boxes
        if boxes is None:
            continue
        for box in boxes:
            cls_id = int(box.cls[0])
            conf = float(box.conf[0])
            x1, y1, x2, y2 = map(int, box.xyxy[0])

            if cls_id in config.CAR_CLASSES:
                count_car += 1
                color = _COLOR_CAR
                label = f"Car {conf:.0%}"
                cls_name = "car"
            elif cls_id in config.MOTOR_CLASSES:
                count_motor += 1
                color = _COLOR_MOTOR
                label = f"Motor {conf:.0%}"
                cls_name = "motor"
            else:
                continue

            cv2.rectangle(annotated, (x1, y1), (x2, y2), color, 2)
            cv2.putText(annotated, label, (x1, y1 - 6),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.45, color, 1)

            objects_list.append({
                "class": cls_name,
                "confidence": round(conf, 4),
                "bbox": [x1, y1, x2, y2],
            })

    return annotated, count_car, count_motor, inference_ms, objects_list


def redraw_detections(frame: np.ndarray, objects_list: list[dict]) -> np.ndarray:
    """
    在新帧上重绘检测框（帧跳过时复用上次检测结果）
    返回: 标注后的帧副本
    """
    annotated = frame.copy()
    for obj in objects_list:
        x1, y1, x2, y2 = obj["bbox"]
        conf = obj["confidence"]
        if obj["class"] == "car":
            color = _COLOR_CAR
            label = f"Car {conf:.0%}"
        else:
            color = _COLOR_MOTOR
            label = f"Motor {conf:.0%}"
        cv2.rectangle(annotated, (x1, y1), (x2, y2), color, 2)
        cv2.putText(annotated, label, (x1, y1 - 6),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.45, color, 1)
    return annotated


# ---------------------------------------------------------------------------
# 速度估算（占位实现）
# ---------------------------------------------------------------------------
def estimate_speed(count: int) -> float:
    """
    简易速度估算（占位实现）
    真实场景应基于目标跟踪 + 标定距离计算
    """
    if count == 0:
        return 0.0
    # 车辆越多 → 速度越慢的简单反比模型
    base = 60.0
    factor = max(0.2, 1.0 - count * 0.05)
    return round(base * factor + random.uniform(-5, 5), 1)
