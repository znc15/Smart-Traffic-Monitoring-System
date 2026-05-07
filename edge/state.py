"""
边缘节点全局状态管理（线程安全）
"""

import time
import threading
from typing import Callable, Optional

import psutil

import config


class EdgeState:
    """边缘节点全局状态（线程安全）"""

    def __init__(self):
        self._lock = threading.Lock()
        self.frame_jpeg: bytes = b""
        self.count_car: int = 0
        self.count_motor: int = 0
        self.count_person: int = 0
        self.speed_car: float = 0.0
        self.speed_motor: float = 0.0
        self.lane_stats: list[dict] = []
        self.events: list[dict] = []
        self.tracked_objects: list[dict] = []
        self.inference_ms: float = 0.0
        self.fps: float = 0.0
        self.start_time: float = time.time()

        # 停止事件，用于通知检测循环退出
        self.stop_event = threading.Event()

        # 重启回调，由 main.py 在 lifespan 中注册，供 routes.py 调用
        self.restart_callback: Optional[Callable] = None
        self.loop_state: str = "stopped"

    def stop(self) -> None:
        """设置停止事件，通知检测循环退出"""
        self.stop_event.set()

    def reset_stop(self) -> None:
        """清除停止事件，为新循环做准备"""
        self.stop_event.clear()

    def should_stop(self) -> bool:
        """检查是否应该停止循环"""
        return self.stop_event.is_set()

    def update_traffic(self, count_car: int, count_motor: int,
                       speed_car: float, speed_motor: float,
                       inference_ms: float, fps: float,
                       count_person: int = 0,
                       lane_stats: list[dict] | None = None,
                       events: list[dict] | None = None,
                       tracked_objects: list[dict] | None = None):
        with self._lock:
            self.count_car = count_car
            self.count_motor = count_motor
            self.count_person = count_person
            self.speed_car = speed_car
            self.speed_motor = speed_motor
            self.lane_stats = lane_stats if lane_stats is not None else []
            self.events = events if events is not None else []
            self.tracked_objects = tracked_objects if tracked_objects is not None else []
            self.inference_ms = inference_ms
            self.fps = fps

    def update_frame(self, jpeg_bytes: bytes):
        with self._lock:
            self.frame_jpeg = jpeg_bytes

    def get_traffic(self) -> dict:
        with self._lock:
            total = self.count_car + self.count_motor + self.count_person
            if total > 12:
                density_status = "congested"
            elif total > 8:
                density_status = "busy"
            else:
                density_status = "clear"

            avg_speed = (self.speed_car + self.speed_motor) / 2.0
            speed_status = "unknown" if avg_speed <= 0 else ("fast" if avg_speed >= 40 else "slow")
            density_factor = min(1.0, total / 35.0)
            speed_penalty = 1.0 if avg_speed <= 0 else max(0.0, min(1.0, 1.0 - (avg_speed / 60.0)))

            return {
                "count_car": self.count_car,
                "count_motor": self.count_motor,
                "count_person": self.count_person,
                "speed_car": self.speed_car,
                "speed_motor": self.speed_motor,
                "density_status": density_status,
                "speed_status": speed_status,
                "congestion_index": round(density_factor * 0.7 + speed_penalty * 0.3, 3),
                "lane_stats": self.lane_stats,
                "events": self.events,
                "tracked_objects": self.tracked_objects,
            }

    def clear_frame(self) -> None:
        """清空帧缓存（模式切换时调用，避免残留旧模式的帧导致花屏）"""
        with self._lock:
            self.frame_jpeg = b""

    def get_frame(self) -> bytes:
        with self._lock:
            return self.frame_jpeg

    def get_edge_metrics(self) -> dict:
        """收集边缘节点性能指标"""
        cpu = psutil.cpu_percent(interval=None)
        mem = psutil.virtual_memory()
        uptime = time.time() - self.start_time

        metrics = {
            "cpu_percent": cpu,
            "memory_percent": mem.percent,
            "memory_used": mem.used,
            "memory_total": mem.total,
            "inference_ms": self.inference_ms,
            "fps": self.fps,
            "uptime_s": round(uptime),
            "model": config.MODEL_NAME,
        }

        # 尝试获取 GPU 信息
        try:
            import pynvml
            pynvml.nvmlInit()
            handle = pynvml.nvmlDeviceGetHandleByIndex(0)
            util = pynvml.nvmlDeviceGetUtilizationRates(handle)
            mem_info = pynvml.nvmlDeviceGetMemoryInfo(handle)
            metrics["gpu_percent"] = util.gpu
            metrics["gpu_memory_percent"] = (
                round(mem_info.used / mem_info.total * 100, 1)
                if mem_info.total > 0 else 0
            )
            pynvml.nvmlShutdown()
        except Exception:
            metrics["gpu_percent"] = None
            metrics["gpu_memory_percent"] = None

        return metrics

    def set_loop_state(self, value: str) -> None:
        with self._lock:
            self.loop_state = value

    def get_loop_state(self) -> str:
        with self._lock:
            return self.loop_state


# 全局单例
state = EdgeState()
