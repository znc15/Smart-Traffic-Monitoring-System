"""
后台检测循环
支持摄像头模式（YOLOv8 实时检测）和模拟模式（示例素材真实推理 / 随机数据回退）
"""

import time
import random
from pathlib import Path

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
    """持续读取视频流并进行 YOLOv8 检测，收到停止信号时安全退出"""
    source = config.camera_source

    cap = cv2.VideoCapture(source)
    if not cap.isOpened():
        print(f"[ERROR] 无法打开视频源: {source}")
        return

    print(f"[INFO] 视频源已连接: {source}")
    fps_counter = 0
    fps_timer = time.time()
    current_fps = 0.0

    try:
        while not state.should_stop():
            ret, frame = cap.read()
            if not ret:
                print("[WARN] 读取帧失败，1秒后重试...")
                # 等待期间也检查停止信号，避免阻塞退出
                if state.stop_event.wait(timeout=1.0):
                    break
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
    finally:
        # 循环退出时释放 VideoCapture 资源
        cap.release()
        print("[INFO] camera_loop 已停止，VideoCapture 资源已释放")


# ---------------------------------------------------------------------------
# 模拟模式
# ---------------------------------------------------------------------------
def sim_loop() -> None:
    """模拟模式 - 使用示例视频/图片做真实推理，无素材时回退到随机数据"""
    samples_dir = Path(__file__).parent / "samples"

    # 查找示例素材
    videos: list[Path] = []
    images: list[Path] = []
    if samples_dir.exists():
        videos = sorted(samples_dir.glob("*.mp4")) + sorted(samples_dir.glob("*.avi"))
        images = sorted(samples_dir.glob("*.jpg")) + sorted(samples_dir.glob("*.png"))

    if videos:
        print(f"[SIM] 找到 {len(videos)} 个视频文件，启用真实推理模式")
        _sim_video_loop(videos)
    elif images:
        print(f"[SIM] 找到 {len(images)} 张图片，启用真实推理模式")
        _sim_image_loop(images)
    else:
        print("[SIM] 未找到示例素材，使用随机数据模式")
        print("[SIM] 提示：将视频(.mp4/.avi)或图片(.jpg/.png)放入 edge/samples/ 目录可启用真实推理")
        _sim_random_loop()

    print("[INFO] sim_loop 已停止")


# ---------------------------------------------------------------------------
# 模拟模式 - 视频循环推理
# ---------------------------------------------------------------------------
def _sim_video_loop(videos: list[Path]) -> None:
    """循环播放视频列表，对每帧做真实 YOLO 推理"""
    fps_counter = 0
    fps_timer = time.time()
    current_fps = 0.0
    video_idx = 0

    while not state.should_stop():
        video_path = videos[video_idx % len(videos)]
        print(f"[SIM] 播放视频: {video_path.name}")
        cap = cv2.VideoCapture(str(video_path))

        if not cap.isOpened():
            print(f"[SIM] 无法打开视频: {video_path.name}，跳过")
            video_idx += 1
            continue

        # 获取视频原始帧率，用于控制播放速度
        src_fps = cap.get(cv2.CAP_PROP_FPS)
        frame_delay = 1.0 / src_fps if src_fps > 0 else 1.0 / 25.0

        try:
            while not state.should_stop():
                ret, frame = cap.read()
                if not ret:
                    # 当前视频播放完毕，切换到下一个
                    break

                frame_start = time.time()

                # 真实 YOLO 推理
                annotated, count_car, count_motor, inference_ms = detect_vehicles(frame)

                # 计算实际 FPS
                fps_counter += 1
                elapsed = time.time() - fps_timer
                if elapsed >= 1.0:
                    current_fps = round(fps_counter / elapsed, 1)
                    fps_counter = 0
                    fps_timer = time.time()

                # 速度估算
                speed_car = estimate_speed(count_car)
                speed_motor = estimate_speed(count_motor)

                # 叠加信息栏
                draw_overlay(annotated, count_car, count_motor, speed_car, speed_motor,
                             inference_ms, current_fps)

                # 编码 JPEG 并更新全局状态
                _, jpeg = cv2.imencode(".jpg", annotated, [cv2.IMWRITE_JPEG_QUALITY, 80])
                state.update_traffic(count_car, count_motor, speed_car, speed_motor,
                                     inference_ms, current_fps)
                state.update_frame(jpeg.tobytes())

                # 按视频原始帧率控制播放节奏（扣除推理耗时）
                processing_time = time.time() - frame_start
                wait_time = max(0.0, frame_delay - processing_time)
                if wait_time > 0 and state.stop_event.wait(timeout=wait_time):
                    break
        finally:
            cap.release()

        video_idx += 1


# ---------------------------------------------------------------------------
# 模拟模式 - 图片循环推理
# ---------------------------------------------------------------------------
def _sim_image_loop(images: list[Path]) -> None:
    """循环遍历图片列表，对每张图片做真实 YOLO 推理，每张停留约 2 秒"""
    fps_counter = 0
    fps_timer = time.time()
    current_fps = 0.0
    img_idx = 0

    while not state.should_stop():
        img_path = images[img_idx % len(images)]
        frame = cv2.imread(str(img_path))

        if frame is None:
            print(f"[SIM] 无法读取图片: {img_path.name}，跳过")
            img_idx += 1
            continue

        # 真实 YOLO 推理
        annotated, count_car, count_motor, inference_ms = detect_vehicles(frame)

        # 计算实际 FPS
        fps_counter += 1
        elapsed = time.time() - fps_timer
        if elapsed >= 1.0:
            current_fps = round(fps_counter / elapsed, 1)
            fps_counter = 0
            fps_timer = time.time()

        # 速度估算
        speed_car = estimate_speed(count_car)
        speed_motor = estimate_speed(count_motor)

        # 叠加信息栏
        draw_overlay(annotated, count_car, count_motor, speed_car, speed_motor,
                     inference_ms, current_fps)

        # 编码 JPEG 并更新全局状态
        _, jpeg = cv2.imencode(".jpg", annotated, [cv2.IMWRITE_JPEG_QUALITY, 80])
        state.update_traffic(count_car, count_motor, speed_car, speed_motor,
                             inference_ms, current_fps)
        state.update_frame(jpeg.tobytes())

        # 每张图片停留约 2 秒，可被停止信号中断
        if state.stop_event.wait(timeout=2.0):
            break

        img_idx += 1


# ---------------------------------------------------------------------------
# 模拟模式 - 随机数据回退（无示例素材时使用）
# ---------------------------------------------------------------------------
def _sim_random_loop() -> None:
    """生成合成帧和随机交通数据（原始行为），收到停止信号时安全退出"""
    fps_timer = time.time()
    fps_counter = 0
    current_fps = 0.0

    while not state.should_stop():
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

        # 使用 stop_event.wait 代替 time.sleep，可被停止信号中断
        state.stop_event.wait(timeout=1.0)
