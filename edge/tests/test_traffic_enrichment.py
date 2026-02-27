from pathlib import Path
import sys

sys.path.append(str(Path(__file__).resolve().parents[1]))

from traffic_enrichment import build_lane_stats, build_events, count_person


def test_count_person_counts_person_class_only():
    objects = [
        {"class": "person"},
        {"class": "car"},
        {"class": "person"},
    ]
    assert count_person(objects) == 2


def test_build_lane_stats_splits_into_three_lanes():
    objects = [
        {"class": "car", "bbox": [0, 0, 90, 40]},
        {"class": "car", "bbox": [120, 0, 180, 40]},
        {"class": "motor", "bbox": [220, 0, 280, 40]},
        {"class": "person", "bbox": [10, 0, 20, 30]},
    ]
    stats = build_lane_stats(objects, frame_width=300)
    assert [s["lane_id"] for s in stats] == ["L1", "L2", "L3"]
    assert [s["count"] for s in stats] == [1, 1, 1]


def test_build_events_generates_congestion_event():
    events = build_events(total_vehicle=25, speed_car=12.0, speed_motor=9.0)
    assert any(event["event_type"] == "congestion" for event in events)
