from pathlib import Path
import sys

sys.path.append(str(Path(__file__).resolve().parents[1]))

import config
from traffic_enrichment import (
    analyze_tracked_objects,
    build_lane_stats,
    build_events,
    count_person,
    filter_objects_in_roi,
    reset_track_history,
)


def setup_function():
    reset_track_history()
    config.ANALYSIS_ROI = [0.2, 0.2, 0.8, 0.9]
    config.LANE_SPLIT_RATIOS = [0.4, 0.7]
    config.SPEED_METERS_PER_PIXEL = 0.1
    config.PARKING_STATIONARY_SECONDS = 4.0
    config.WRONG_WAY_MIN_TRACK_POINTS = 3


def test_count_person_counts_person_class_only():
    objects = [
        {"class": "person"},
        {"class": "car"},
        {"class": "person"},
    ]
    assert count_person(objects) == 2


def test_build_lane_stats_counts_only_roi_objects():
    objects = [
        {"class": "car", "bbox": [30, 80, 70, 100]},     # outside ROI
        {"class": "car", "bbox": [90, 80, 120, 110]},    # L1
        {"class": "car", "bbox": [170, 90, 210, 120]},   # L2
        {"class": "motor", "bbox": [220, 100, 240, 140]},  # L3
    ]
    stats = build_lane_stats(objects, frame_width=300, frame_height=200)
    assert [s["lane_id"] for s in stats] == ["L1", "L2", "L3"]
    assert [s["count"] for s in stats] == [1, 1, 1]


def test_filter_objects_in_roi_uses_bbox_center():
    objects = [
        {"class": "car", "bbox": [40, 50, 80, 90]},
        {"class": "car", "bbox": [110, 70, 150, 110]},
        {"class": "motor", "bbox": [220, 120, 260, 160]},
    ]

    filtered = filter_objects_in_roi(objects, frame_width=300, frame_height=200)

    assert filtered == [
        {"class": "car", "bbox": [110, 70, 150, 110]},
        {"class": "motor", "bbox": [220, 120, 260, 160]},
    ]


def test_analyze_tracked_objects_estimates_speed_from_track_history():
    frame_w, frame_h = 320, 240
    obj_a = {"class": "car", "track_id": 101, "bbox": [120, 100, 150, 130]}
    obj_b = {"class": "car", "track_id": 101, "bbox": [150, 100, 180, 130]}

    analyze_tracked_objects([obj_a], frame_w, frame_h, now_ts=10.0)
    result = analyze_tracked_objects([obj_b], frame_w, frame_h, now_ts=11.0)

    assert result["speed_car"] > 0
    assert result["speed_motor"] == 0
    assert result["roi_vehicle_count"] == 1


def test_build_events_detects_wrong_way_with_reverse_track():
    summaries = [{
        "track_id": 9,
        "class": "car",
        "track_points": 4,
        "direction": "up",
        "delta_y": -32.0,
        "stationary_seconds": 0.0,
        "speed_kmh": 18.0,
        "pixel_distance": 32.0,
        "start_point": [180.0, 180.0],
        "end_point": [180.0, 148.0],
        "bbox": [160, 138, 200, 170],
    }]
    events = build_events(total_vehicle=3, speed_car=18.0, speed_motor=0.0, track_summaries=summaries, now_ts=20.0)
    assert any(event["event_type"] == "wrong_way_suspected" for event in events)


def test_build_events_detects_illegal_parking_when_stationary_too_long():
    summaries = [{
        "track_id": 12,
        "class": "car",
        "track_points": 5,
        "direction": "down",
        "delta_y": 1.2,
        "stationary_seconds": 6.5,
        "speed_kmh": 0.8,
        "pixel_distance": 2.0,
        "start_point": [150.0, 150.0],
        "end_point": [151.0, 151.0],
        "bbox": [135, 135, 165, 165],
    }]
    events = build_events(total_vehicle=2, speed_car=0.8, speed_motor=0.0, track_summaries=summaries, now_ts=30.0)
    assert any(event["event_type"] == "illegal_parking_suspected" for event in events)
