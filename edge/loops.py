"""
后台检测循环
支持摄像头模式（YOLOv8 实时检测）和模拟模式（合成数据）
"""

import time
import random

import cv2
import numpy as np

import config
from state import state
from detector import detect_vehicles, estimate_speed
from overlay import draw_overlay


# ---------------------------------------------------------------------------
# 摄像头模式
# ---------------------------------------------------------------------------
def camera_loop() -> None:
    """持续读取视频流并进行 YOLOv8 检测"""
    source = config.camera_source

    cap = cv2.VideoCapture(source)
    if not cap.isOpened():
        print(f"[ERROR] 无法打开视频源: {source}")
        return

    print(f"[INFO] 视频源已连接: {source}")
    fps_counter = 0
    fps_timer = time.time()
    current_fps = 0.0

    while True:
        ret, frame = cap.read()
        if not ret:
            print("[WARN] 读取帧失败，1秒后重试...")
            time.sleep(1)
            cap.release()
            cap = cv2.VideoCapture(source)
            continue

        # YOLOv8 检测
        annotated, count_car, count_motor, inference_ms = detect_vehicles(frame)

        # 计算 FPS
        fps_counter += 1
        elapsed = time.time() - fps_timer
        if elapsed >= 1.0:
            current_fps = round(fps_counter / elapsed, 1)
            fps_counter = 0
            fps_timer = time.time()

        # 速度估算
        speed_car = estimate_speed(count_car)
        speed_motor = estimate_speed(count_motor)

        # 在帧上叠加信息栏
        draw_overlay(annotated, count_car, count_motor, speed_car, speed_motor,
                     inference_ms, current_fps)

        # 编码 JPEG 并更新全局状态
        _, jpeg = cv2.imencode(".jpg", annotated, [cv2.IMWRITE_JPEG_QUALITY, 80])

        state.update_traffic(count_car, count_motor, speed_car, speed_motor,
                             inference_ms, current_fps)
        state.update_frame(jpeg.tobytes())


# ---------------------------------------------------------------------------
# 模拟模式
# ---------------------------------------------------------------------------
def sim_loop() -> None:
    """生成合成帧和模拟交通数据（演示用）"""
    fps_timer = time.time()
    fps_counter = 0
    current_fps = 0.0

    while True:
        # 随机交通数据
        count_car = random.randint(0, 12)
        count_motor = random.randint(0, 8)
        speed_car = estimate_speed(count_car)
        speed_motor = estimate_speed(count_motor)

        # 生成模拟帧（深灰底图）
        frame = np.full((480, 640, 3), 35, dtype=np.uint8)

        # 画模拟汽车（蓝色系矩形）
        for _ in range(count_car):
            x1 = random.randint(20, 540)
            y1 = random.randint(60, 380)
            w, h = random.randint(45, 75), random.randint(28, 42)
            color = (random.randint(180, 255), random.randint(80, 140), 40)
            cv2.rectangle(frame, (x1, y1), (x1 + w, y1 + h), color, -1)

        # 画模拟摩托车（绿色系小矩形）
        for _ in range(count_motor):
            x1 = random.randint(20, 580)
            y1 = random.randint(60, 420)
            w, h = random.randint(15, 30), random.randint(12, 22)
            color = (40, random.randint(180, 255), random.randint(80, 140))
            cv2.rectangle(frame, (x1, y1), (x1 + w, y1 + h), color, -1)

        # 模拟推理耗时
        inference_ms = random.uniform(15, 45)

        # 计算 FPS
        fps_counter += 1
        elapsed = time.time() - fps_timer
        if elapsed >= 1.0:
            current_fps = round(fps_counter / elapsed, 1)
            fps_counter = 0
            fps_timer = time.time()

        # 叠加信息栏
        draw_overlay(frame, count_car, count_motor, speed_car, speed_motor,
                     inference_ms, current_fps)

        # 编码 JPEG 并更新全局状态
        _, jpeg = cv2.imencode(".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, 80])

        state.update_traffic(count_car, count_motor, speed_car, speed_motor,
                             inference_ms, current_fps)
        state.update_frame(jpeg.tobytes())

        time.sleep(1)
