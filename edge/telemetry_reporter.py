"""
边缘节点主动上报模块

支持指数退避重试、失败队列缓存、完善的异常处理。
"""

from __future__ import annotations

import collections
import json
import threading
import time
from datetime import datetime, timezone
from urllib import request, error

import config
from state import state

_MAX_RETRIES = 3
_RETRY_BASE_SEC = 1.0
_FAILED_QUEUE_MAXLEN = 100

_failed_queue: collections.deque[dict] = collections.deque(maxlen=_FAILED_QUEUE_MAXLEN)
_queue_lock = threading.Lock()


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
        ok = _post_with_retry(payload)
        if ok:
            _flush_failed_queue()
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


def _is_retryable(exc: Exception) -> bool:
    """判断异常是否值得重试。4xx 客户端错误不重试，5xx 服务端错误重试。"""
    if isinstance(exc, error.HTTPError):
        return exc.code >= 500
    return isinstance(exc, (error.URLError, TimeoutError, OSError))


def _post_once(payload: dict) -> bool:
    """发送一次 HTTP POST，返回是否成功。遇到不可重试错误抛出 _NoRetryError。"""
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
                return False
            return True
    except error.HTTPError as ex:
        if ex.code < 500:
            print(f"[WARN] telemetry 上报客户端错误 (HTTP {ex.code}): {ex.reason}，不重试")
            raise _NoRetryError(ex) from ex
        print(f"[WARN] telemetry 上报服务端错误 (HTTP {ex.code}): {ex.reason}")
        raise
    except (error.URLError, TimeoutError, OSError) as ex:
        print(f"[WARN] telemetry 上报网络错误: {type(ex).__name__}: {ex}")
        raise


class _NoRetryError(Exception):
    """标记不应重试的错误（如 4xx）。"""


def _post_with_retry(payload: dict) -> bool:
    """带指数退避的重试上报，失败后入缓存队列。返回是否成功。"""
    for attempt in range(_MAX_RETRIES):
        try:
            if _post_once(payload):
                return True
        except _NoRetryError:
            return False
        except (error.HTTPError, error.URLError, TimeoutError, OSError):
            pass

        if attempt < _MAX_RETRIES - 1:
            backoff = _RETRY_BASE_SEC * (2 ** attempt)
            print(f"[INFO] telemetry 上报将在 {backoff:.0f}s 后重试 (第 {attempt + 2}/{_MAX_RETRIES} 次)")
            time.sleep(backoff)

    _enqueue_failed(payload)
    return False


def _enqueue_failed(payload: dict) -> None:
    with _queue_lock:
        _failed_queue.append(payload)
        qsize = len(_failed_queue)
    print(f"[WARN] telemetry 上报最终失败，已缓存 (队列大小: {qsize}/{_FAILED_QUEUE_MAXLEN})")


def _flush_failed_queue() -> None:
    """在一次成功上报后，尝试逐条发送队列中的缓存 payload。"""
    while True:
        with _queue_lock:
            if not _failed_queue:
                return
            payload = _failed_queue[0]

        try:
            if _post_once(payload):
                with _queue_lock:
                    if _failed_queue and _failed_queue[0] is payload:
                        _failed_queue.popleft()
                print(f"[INFO] telemetry 缓存补发成功，剩余队列: {len(_failed_queue)}")
            else:
                return
        except Exception:
            return
