"""
交通数据增强：ROI、车道统计、近似测速与异常事件规则。
"""

from __future__ import annotations

import math
import time
from collections import deque
from datetime import datetime, timezone
from typing import Iterable

import config

_TRACK_HISTORY: dict[int, deque[dict]] = {}
_EVENT_COOLDOWN: dict[tuple[str, int], float] = {}
_STALE_TRACK_SECONDS = 20.0
_STATIONARY_PIXEL_THRESHOLD = 6.0
_EVENT_REPEAT_SECONDS = 8.0


def count_person(objects: list[dict]) -> int:
    return sum(1 for obj in objects if obj.get("class") == "person")


def build_lane_stats(
    objects: list[dict],
    frame_width: int,
    frame_height: int | None = None,
    analysis_roi: list[float] | None = None,
    lane_split_ratios: list[float] | None = None,
) -> list[dict]:
    if frame_width <= 0:
        return []

    lanes = {
        "L1": {"turn_type": "left", "count": 0},
        "L2": {"turn_type": "straight", "count": 0},
        "L3": {"turn_type": "right", "count": 0},
    }

    split1, split2 = resolve_lane_splits(frame_width, lane_split_ratios)
    filtered = filter_objects_in_roi(objects, frame_width, frame_height, analysis_roi)

    for obj in filtered:
        if obj.get("class") not in {"car", "motor"}:
            continue
        bbox = obj.get("bbox") or [0, 0, 0, 0]
        x_center = (bbox[0] + bbox[2]) / 2
        if x_center < split1:
            lane_key = "L1"
        elif x_center < split2:
            lane_key = "L2"
        else:
            lane_key = "L3"
        lanes[lane_key]["count"] += 1

    return [
        {"lane_id": lane_id, "turn_type": value["turn_type"], "count": value["count"]}
        for lane_id, value in lanes.items()
    ]


def analyze_tracked_objects(
    objects: list[dict],
    frame_width: int,
    frame_height: int,
    now_ts: float | None = None,
) -> dict:
    now_ts = time.time() if now_ts is None else now_ts
    roi_objects = filter_objects_in_roi(objects, frame_width, frame_height)
    track_summaries = _build_track_summaries(roi_objects, now_ts)
    speeds = _average_speed_by_class(track_summaries.values())

    total_vehicle = sum(1 for obj in roi_objects if obj.get("class") in {"car", "motor"})
    events = build_events(
        total_vehicle=total_vehicle,
        speed_car=speeds["car"],
        speed_motor=speeds["motor"],
        track_summaries=list(track_summaries.values()),
        now_ts=now_ts,
    )

    return {
        "speed_car": speeds["car"],
        "speed_motor": speeds["motor"],
        "lane_stats": build_lane_stats(roi_objects, frame_width, frame_height),
        "events": events,
        "roi_vehicle_count": total_vehicle,
    }


def build_events(
    total_vehicle: int,
    speed_car: float,
    speed_motor: float,
    track_summaries: list[dict] | None = None,
    now_ts: float | None = None,
) -> list[dict]:
    now_ts = time.time() if now_ts is None else now_ts
    track_summaries = track_summaries or []
    events = []
    now = _iso_utc(now_ts)

    if total_vehicle >= 20:
        events.append({
            "event_type": "congestion",
            "level": "high",
            "start_at": now,
            "payload": {
                "roi_vehicle_count": total_vehicle,
                "speed_car": speed_car,
                "speed_motor": speed_motor,
            },
        })

    for summary in track_summaries:
        track_id = int(summary.get("track_id", -1))
        if summary.get("stationary_seconds", 0.0) >= config.PARKING_STATIONARY_SECONDS:
            if _should_emit("illegal_parking_suspected", track_id, now_ts):
                events.append({
                    "event_type": "illegal_parking_suspected",
                    "level": "medium",
                    "start_at": now,
                    "payload": _track_payload(summary),
                })

        if (
            summary.get("direction") == "up"
            and int(summary.get("track_points", 0)) >= config.WRONG_WAY_MIN_TRACK_POINTS
            and abs(float(summary.get("delta_y", 0.0))) >= 12.0
        ):
            if _should_emit("wrong_way_suspected", track_id, now_ts):
                events.append({
                    "event_type": "wrong_way_suspected",
                    "level": "high",
                    "start_at": now,
                    "payload": _track_payload(summary),
                })

    return events


def filter_objects_in_roi(
    objects: list[dict],
    frame_width: int,
    frame_height: int | None = None,
    analysis_roi: list[float] | None = None,
) -> list[dict]:
    if frame_width <= 0:
        return []
    if frame_height is None or frame_height <= 0:
        frame_height = _infer_frame_height(objects)

    roi = resolve_analysis_roi(frame_width, frame_height, analysis_roi)
    if roi is None:
        return list(objects)
    x1, y1, x2, y2 = roi
    filtered = []
    for obj in objects:
        bbox = obj.get("bbox") or [0, 0, 0, 0]
        cx = (bbox[0] + bbox[2]) / 2
        cy = (bbox[1] + bbox[3]) / 2
        if x1 <= cx <= x2 and y1 <= cy <= y2:
            filtered.append(obj)
    return filtered


def fallback_speed(count: int) -> float:
    if count <= 0:
        return 0.0
    base = max(8.0, 55.0 - count * 2.6)
    return round(base, 1)


def reset_track_history() -> None:
    _TRACK_HISTORY.clear()
    _EVENT_COOLDOWN.clear()


def resolve_analysis_roi(
    frame_width: int,
    frame_height: int,
    analysis_roi: list[float] | None = None,
) -> tuple[float, float, float, float] | None:
    values = analysis_roi if analysis_roi is not None else getattr(config, "ANALYSIS_ROI", None)
    if not values or len(values) != 4:
        return None
    x1, y1, x2, y2 = [float(v) for v in values]
    x1 = max(0.0, min(1.0, x1))
    y1 = max(0.0, min(1.0, y1))
    x2 = max(0.0, min(1.0, x2))
    y2 = max(0.0, min(1.0, y2))
    if x2 <= x1 or y2 <= y1:
        return None
    return (x1 * frame_width, y1 * frame_height, x2 * frame_width, y2 * frame_height)


def resolve_lane_splits(frame_width: int, lane_split_ratios: list[float] | None = None) -> tuple[float, float]:
    ratios = lane_split_ratios if lane_split_ratios is not None else getattr(config, "LANE_SPLIT_RATIOS", None)
    if not ratios or len(ratios) != 2:
        return (frame_width / 3, frame_width * 2 / 3)
    left, right = sorted(max(0.05, min(0.95, float(v))) for v in ratios)
    if abs(right - left) < 0.05:
        return (frame_width / 3, frame_width * 2 / 3)
    return (frame_width * left, frame_width * right)


def _build_track_summaries(objects: list[dict], now_ts: float) -> dict[int, dict]:
    summaries: dict[int, dict] = {}
    stale_before = now_ts - _STALE_TRACK_SECONDS

    for track_id, history in list(_TRACK_HISTORY.items()):
        if not history or history[-1]["ts"] < stale_before:
            _TRACK_HISTORY.pop(track_id, None)

    for obj in objects:
        if obj.get("class") not in {"car", "motor"}:
            continue
        track_id = obj.get("track_id")
        if track_id is None:
            continue
        bbox = obj.get("bbox") or [0, 0, 0, 0]
        cx = (bbox[0] + bbox[2]) / 2
        cy = (bbox[1] + bbox[3]) / 2
        history = _TRACK_HISTORY.setdefault(int(track_id), deque(maxlen=12))
        history.append({
            "ts": now_ts,
            "x": cx,
            "y": cy,
            "bbox": list(bbox),
            "class": obj.get("class"),
        })
        summaries[int(track_id)] = _summarize_track(int(track_id), history)
    return summaries


def _summarize_track(track_id: int, history: deque[dict]) -> dict:
    first = history[0]
    last = history[-1]
    dt = max(last["ts"] - first["ts"], 0.0)
    dx = last["x"] - first["x"]
    dy = last["y"] - first["y"]
    pixel_distance = math.hypot(dx, dy)
    speed_mps = 0.0
    if dt > 0:
        speed_mps = pixel_distance * config.SPEED_METERS_PER_PIXEL / dt
    stationary_seconds = dt if pixel_distance <= _STATIONARY_PIXEL_THRESHOLD else 0.0
    direction = "up" if dy < 0 else "down"
    return {
        "track_id": track_id,
        "class": last["class"],
        "track_points": len(history),
        "pixel_distance": round(pixel_distance, 3),
        "delta_x": round(dx, 3),
        "delta_y": round(dy, 3),
        "speed_kmh": round(speed_mps * 3.6, 2),
        "stationary_seconds": round(stationary_seconds, 2),
        "direction": direction,
        "start_point": [round(first["x"], 2), round(first["y"], 2)],
        "end_point": [round(last["x"], 2), round(last["y"], 2)],
        "bbox": last["bbox"],
    }


def _average_speed_by_class(track_summaries: Iterable[dict]) -> dict[str, float]:
    buckets: dict[str, list[float]] = {"car": [], "motor": []}
    for summary in track_summaries:
        cls_name = str(summary.get("class", ""))
        if cls_name in buckets and summary.get("speed_kmh", 0.0) > 0:
            buckets[cls_name].append(float(summary["speed_kmh"]))
    return {
        "car": round(sum(buckets["car"]) / len(buckets["car"]), 2) if buckets["car"] else 0.0,
        "motor": round(sum(buckets["motor"]) / len(buckets["motor"]), 2) if buckets["motor"] else 0.0,
    }


def _should_emit(event_type: str, track_id: int, now_ts: float) -> bool:
    key = (event_type, track_id)
    last_ts = _EVENT_COOLDOWN.get(key)
    if last_ts is not None and (now_ts - last_ts) < _EVENT_REPEAT_SECONDS:
        return False
    _EVENT_COOLDOWN[key] = now_ts
    return True


def _track_payload(summary: dict) -> dict:
    return {
        "track_id": summary.get("track_id"),
        "class": summary.get("class"),
        "speed_kmh": summary.get("speed_kmh"),
        "direction": summary.get("direction"),
        "stationary_seconds": summary.get("stationary_seconds"),
        "track_points": summary.get("track_points"),
        "pixel_distance": summary.get("pixel_distance"),
        "delta_y": summary.get("delta_y"),
        "start_point": summary.get("start_point"),
        "end_point": summary.get("end_point"),
        "bbox": summary.get("bbox"),
    }


def _infer_frame_height(objects: list[dict]) -> int:
    max_y = 0
    for obj in objects:
        bbox = obj.get("bbox") or [0, 0, 0, 0]
        max_y = max(max_y, int(bbox[3]))
    return max(max_y, 1)


def _iso_utc(timestamp: float) -> str:
    return datetime.fromtimestamp(timestamp, timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
