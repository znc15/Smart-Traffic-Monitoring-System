"""
YOLOv8 车辆检测模块
支持 OpenVINO 加速：首次运行自动将 .pt 导出为 OpenVINO IR，后续直接加载
"""

import time
import random
from pathlib import Path

import cv2
import numpy as np
from ultralytics import YOLO

from config import MODEL_NAME, CONF_THRESHOLD, CAR_CLASSES, MOTOR_CLASSES, USE_OPENVINO

# ---------------------------------------------------------------------------
# 模型加载（懒加载单例，支持 OpenVINO）
# ---------------------------------------------------------------------------
_model: YOLO | None = None


def _get_openvino_path() -> Path:
    """获取 OpenVINO IR 模型目录路径"""
    stem = Path(MODEL_NAME).stem  # e.g. "yolov8n"
    return Path(f"{stem}_openvino_model")


def _load_model() -> YOLO:
    """
    加载 YOLOv8 模型
    - USE_OPENVINO=True: 自动导出并加载 OpenVINO IR 格式（CPU 加速）
    - USE_OPENVINO=False: 直接加载 PyTorch 模型
    """
    global _model
    if _model is not None:
        return _model

    if USE_OPENVINO:
        ov_dir = _get_openvino_path()
        if not ov_dir.exists():
            # 首次运行：加载 .pt → 导出 OpenVINO IR
            print(f"[INFO] 首次运行，导出 OpenVINO 模型: {MODEL_NAME} → {ov_dir}")
            pt_model = YOLO(MODEL_NAME)
            pt_model.export(format="openvino")
            print(f"[INFO] OpenVINO 导出完成: {ov_dir}")

        print(f"[INFO] 加载 OpenVINO 模型: {ov_dir}")
        _model = YOLO(ov_dir)
        print("[INFO] OpenVINO 模型加载完成 (CPU 加速已启用)")
    else:
        print(f"[INFO] 加载 PyTorch 模型: {MODEL_NAME}")
        _model = YOLO(MODEL_NAME)
        print("[INFO] PyTorch 模型加载完成")

    return _model


# ---------------------------------------------------------------------------
# 车辆检测
# ---------------------------------------------------------------------------
def detect_vehicles(frame: np.ndarray) -> tuple[np.ndarray, int, int, float]:
    """
    使用 YOLOv8 检测车辆
    返回: (标注后的帧, 汽车数, 摩托车数, 推理耗时ms)
    """
    model = _load_model()

    t0 = time.time()
    results = model(frame, conf=CONF_THRESHOLD, verbose=False)
    inference_ms = (time.time() - t0) * 1000

    count_car = 0
    count_motor = 0
    annotated = frame.copy()

    for r in results:
        boxes = r.boxes
        if boxes is None:
            continue
        for box in boxes:
            cls_id = int(box.cls[0])
            conf = float(box.conf[0])
            x1, y1, x2, y2 = map(int, box.xyxy[0])

            if cls_id in CAR_CLASSES:
                count_car += 1
                color = (255, 120, 40)   # 蓝橙色 - 汽车
                label = f"Car {conf:.0%}"
            elif cls_id in MOTOR_CLASSES:
                count_motor += 1
                color = (40, 220, 120)   # 绿色 - 摩托
                label = f"Motor {conf:.0%}"
            else:
                continue

            cv2.rectangle(annotated, (x1, y1), (x2, y2), color, 2)
            cv2.putText(annotated, label, (x1, y1 - 6),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.45, color, 1)

    return annotated, count_car, count_motor, inference_ms


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
