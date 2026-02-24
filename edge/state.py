"""
边缘节点全局状态管理（线程安全）
"""

import time
import threading

import psutil

import config


class EdgeState:
    """边缘节点全局状态（线程安全）"""

    def __init__(self):
        self._lock = threading.Lock()
        self.frame_jpeg: bytes = b""
        self.count_car: int = 0
        self.count_motor: int = 0
        self.speed_car: float = 0.0
        self.speed_motor: float = 0.0
        self.inference_ms: float = 0.0
        self.fps: float = 0.0
        self.start_time: float = time.time()

    def update_traffic(self, count_car: int, count_motor: int,
                       speed_car: float, speed_motor: float,
                       inference_ms: float, fps: float):
        with self._lock:
            self.count_car = count_car
            self.count_motor = count_motor
            self.speed_car = speed_car
            self.speed_motor = speed_motor
            self.inference_ms = inference_ms
            self.fps = fps

    def update_frame(self, jpeg_bytes: bytes):
        with self._lock:
            self.frame_jpeg = jpeg_bytes

    def get_traffic(self) -> dict:
        with self._lock:
            return {
                "count_car": self.count_car,
                "count_motor": self.count_motor,
                "speed_car": self.speed_car,
                "speed_motor": self.speed_motor,
            }

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


# 全局单例
state = EdgeState()
