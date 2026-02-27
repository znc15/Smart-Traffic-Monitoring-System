"""
跟踪引擎抽象层：统一 simple 与 ByteTrack 两种后端。
"""

from __future__ import annotations

import importlib
from dataclasses import dataclass
from typing import Any

import config
from simple_tracker import SimpleTracker

try:
    from detector import (
        detect_vehicles_detailed,
        detect_vehicles_with_bytetrack_detailed,
        redraw_detections,
    )
except Exception:
    # 测试环境可能未安装 OpenCV/numpy，允许通过 monkeypatch 注入替身函数
    detect_vehicles_detailed = None
    detect_vehicles_with_bytetrack_detailed = None
    redraw_detections = None


@dataclass
class DetectionResult:
    annotated: Any
    count_car: int
    count_motor: int
    inference_ms: float
    tracked_objects: list[dict]


class TrackingEngine:
    """统一跟踪入口，支持 ByteTrack 与 simple 两种后端。"""

    def __init__(self, backend: str | None = None):
        requested = (backend or config.TRACKER_BACKEND).strip().lower()
        self.backend = requested if requested in {"bytetrack", "simple"} else "simple"
        self._simple_tracker = SimpleTracker() if self.backend == "simple" else None

    def detect(self, frame: Any) -> DetectionResult:
        if self.backend == "bytetrack":
            if detect_vehicles_with_bytetrack_detailed is None:
                raise RuntimeError("ByteTrack 检测函数不可用，请检查 edge 运行依赖")
            try:
                annotated, count_car, count_motor, inference_ms, tracked = (
                    detect_vehicles_with_bytetrack_detailed(frame)
                )
                return DetectionResult(
                    annotated=annotated,
                    count_car=count_car,
                    count_motor=count_motor,
                    inference_ms=inference_ms,
                    tracked_objects=tracked,
                )
            except Exception as ex:  # pragma: no cover - 依赖运行环境，测试中通过 monkeypatch 覆盖
                if config.TRACKER_STRICT:
                    raise
                print(f"[WARN] ByteTrack 追踪失败，自动回退 simple: {ex}")
                self.backend = "simple"
                if self._simple_tracker is None:
                    self._simple_tracker = SimpleTracker()

        if detect_vehicles_detailed is None:
            raise RuntimeError("检测函数不可用，请检查 edge 运行依赖")
        annotated, count_car, count_motor, inference_ms, objects = detect_vehicles_detailed(frame)
        if self._simple_tracker is None:
            self._simple_tracker = SimpleTracker()
        tracked = self._simple_tracker.update(objects)
        return DetectionResult(
            annotated=annotated,
            count_car=count_car,
            count_motor=count_motor,
            inference_ms=inference_ms,
            tracked_objects=tracked,
        )

    @staticmethod
    def redraw(frame: Any, objects: list[dict]) -> Any:
        if redraw_detections is None:
            raise RuntimeError("重绘函数不可用，请检查 edge 运行依赖")
        return redraw_detections(frame, objects)


def print_tracker_runtime_info() -> None:
    backend = config.TRACKER_BACKEND
    strict = config.TRACKER_STRICT
    cfg = config.TRACKER_CFG

    print(f"[INFO] Tracker backend={backend}, strict={strict}, cfg={cfg}")

    if backend != "bytetrack":
        return

    try:
        ultralytics = importlib.import_module("ultralytics")
        version = getattr(ultralytics, "__version__", "unknown")
        print(f"[INFO] Ultralytics version={version}")
    except Exception as ex:
        msg = f"[WARN] 无法读取 Ultralytics 版本: {ex}"
        if strict:
            raise RuntimeError(msg) from ex
        print(msg)
