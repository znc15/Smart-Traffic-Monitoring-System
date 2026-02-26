"""
帧信息叠加层绘制
"""

from datetime import datetime

import cv2
import numpy as np

import config


def draw_overlay(frame: np.ndarray, count_car: int, count_motor: int,
                 speed_car: float, speed_motor: float,
                 inference_ms: float, fps: float) -> None:
    """在帧上绘制半透明信息叠加层（原地修改）"""
    h, w = frame.shape[:2]

    # 顶部半透明黑色信息栏
    overlay = frame.copy()
    cv2.rectangle(overlay, (0, 0), (w, 58), (0, 0, 0), -1)
    cv2.addWeighted(overlay, 0.6, frame, 0.4, 0, frame)

    # 路段名称
    cv2.putText(frame, config.ROAD_NAME, (10, 20),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)

    # 车辆计数和速度
    info = f"Car:{count_car} Motor:{count_motor} | {speed_car:.0f}/{speed_motor:.0f} km/h"
    cv2.putText(frame, info, (10, 42),
                cv2.FONT_HERSHEY_SIMPLEX, 0.45, (0, 255, 200), 1)

    # 性能指标（右上角）
    perf = f"{inference_ms:.0f}ms  {fps:.1f}FPS"
    cv2.putText(frame, perf, (w - 160, 20),
                cv2.FONT_HERSHEY_SIMPLEX, 0.45, (100, 200, 255), 1)

    # 时间戳
    ts = datetime.now().strftime("%H:%M:%S")
    cv2.putText(frame, ts, (w - 90, 42),
                cv2.FONT_HERSHEY_SIMPLEX, 0.45, (200, 200, 200), 1)
