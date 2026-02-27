"""
交通数据增强：车道统计与异常事件规则
"""

from __future__ import annotations

from datetime import datetime, timezone
from typing import List


def count_person(objects: List[dict]) -> int:
    return sum(1 for obj in objects if obj.get("class") == "person")


def build_lane_stats(objects: List[dict], frame_width: int) -> List[dict]:
    if frame_width <= 0:
        return []

    lanes = {
        "L1": {"turn_type": "left", "count": 0},
        "L2": {"turn_type": "straight", "count": 0},
        "L3": {"turn_type": "right", "count": 0},
    }

    split1 = frame_width / 3
    split2 = frame_width * 2 / 3

    for obj in objects:
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


def build_events(total_vehicle: int, speed_car: float, speed_motor: float) -> List[dict]:
    events = []
    now = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")

    if total_vehicle >= 20:
        events.append({
            "event_type": "congestion",
            "level": "high",
            "start_at": now,
        })

    avg_speed = (speed_car + speed_motor) / 2.0
    if total_vehicle >= 8 and avg_speed < 6:
        events.append({
            "event_type": "illegal_parking_suspected",
            "level": "medium",
            "start_at": now,
        })

    return events
