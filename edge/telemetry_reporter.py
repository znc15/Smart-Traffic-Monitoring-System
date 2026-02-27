"""
边缘节点主动上报模块
"""

from __future__ import annotations

import json
import threading
import time
from datetime import datetime, timezone
from urllib import request, error

import config
from state import state


def start_reporter_thread() -> threading.Thread | None:
    if not config.BACKEND_TELEMETRY_URL:
        return None

    thread = threading.Thread(target=_report_loop, daemon=True, name="telemetry-reporter")
    thread.start()
    print(f"[INFO] 已启用主动上报: {config.BACKEND_TELEMETRY_URL}")
    return thread


def _report_loop() -> None:
    interval = max(1.0, config.TELEMETRY_INTERVAL_SEC)
    while not state.should_stop():
        payload = _build_payload()
        _post(payload)
        state.stop_event.wait(timeout=interval)


def _build_payload() -> dict:
    traffic = state.get_traffic()
    metrics = state.get_edge_metrics()
    total = int(traffic.get("count_car", 0)) + int(traffic.get("count_motor", 0)) + int(traffic.get("count_person", 0))

    if total > 30:
        density_status = "congested"
    elif total > 14:
        density_status = "busy"
    else:
        density_status = "clear"

    avg_speed = (float(traffic.get("speed_car", 0.0)) + float(traffic.get("speed_motor", 0.0))) / 2.0
    speed_status = "unknown" if avg_speed <= 0 else ("fast" if avg_speed >= 40 else "slow")
    density_factor = min(1.0, total / 35.0)
    speed_penalty = 1.0 if avg_speed <= 0 else max(0.0, min(1.0, 1.0 - (avg_speed / 60.0)))
    congestion_index = round(density_factor * 0.7 + speed_penalty * 0.3, 3)

    return {
        "node_id": config.EDGE_NODE_ID,
        "road_name": config.ROAD_NAME,
        "timestamp": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "count_car": int(traffic.get("count_car", 0)),
        "count_motor": int(traffic.get("count_motor", 0)),
        "count_person": int(traffic.get("count_person", 0)),
        "avg_speed_car": float(traffic.get("speed_car", 0.0)),
        "avg_speed_motor": float(traffic.get("speed_motor", 0.0)),
        "density_status": density_status,
        "speed_status": speed_status,
        "congestion_index": congestion_index,
        "lane_stats": traffic.get("lane_stats", []),
        "events": traffic.get("events", []),
        "edge_metrics": {
            "fps": metrics.get("fps"),
            "inference_ms": metrics.get("inference_ms"),
            "cpu_percent": metrics.get("cpu_percent"),
        },
    }


def _post(payload: dict) -> None:
    body = json.dumps(payload).encode("utf-8")
    req = request.Request(
        config.BACKEND_TELEMETRY_URL,
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with request.urlopen(req, timeout=3) as resp:
            if resp.status >= 300:
                print(f"[WARN] telemetry 上报返回状态码: {resp.status}")
    except (error.URLError, TimeoutError, OSError) as ex:
        print(f"[WARN] telemetry 上报失败: {ex}")
