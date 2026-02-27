from pathlib import Path
import sys

sys.path.append(str(Path(__file__).resolve().parents[1]))

from simple_tracker import SimpleTracker


def test_simple_tracker_keeps_track_id_for_overlapping_boxes():
    tracker = SimpleTracker(iou_threshold=0.1, max_stale_frames=5)
    frame1 = [{"class": "car", "bbox": [10, 10, 60, 60], "confidence": 0.9}]
    frame2 = [{"class": "car", "bbox": [14, 12, 62, 62], "confidence": 0.88}]

    r1 = tracker.update(frame1)
    r2 = tracker.update(frame2)

    assert r1[0]["track_id"] == r2[0]["track_id"]
