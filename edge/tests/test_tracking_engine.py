from pathlib import Path
import sys

import pytest

sys.path.append(str(Path(__file__).resolve().parents[1]))

import config  # noqa: E402
from tracking_engine import TrackingEngine  # noqa: E402


def _fake_detection_result(track_id: int | None = None):
    obj = {"class": "car", "bbox": [10, 10, 40, 40], "confidence": 0.9}
    if track_id is not None:
        obj["track_id"] = track_id
    return object(), 1, 0, 12.3, [obj]


def test_tracking_engine_simple_backend_adds_track_id(monkeypatch):
    monkeypatch.setattr(
        "tracking_engine.detect_vehicles_detailed",
        lambda frame: _fake_detection_result(),
    )

    engine = TrackingEngine(backend="simple")
    result = engine.detect(object())

    assert engine.backend == "simple"
    assert result.count_car == 1
    assert "track_id" in result.tracked_objects[0]


def test_tracking_engine_bytetrack_success(monkeypatch):
    monkeypatch.setattr(
        "tracking_engine.detect_vehicles_with_bytetrack_detailed",
        lambda frame: _fake_detection_result(track_id=7),
    )

    engine = TrackingEngine(backend="bytetrack")
    result = engine.detect(object())

    assert engine.backend == "bytetrack"
    assert result.tracked_objects[0]["track_id"] == 7


def test_tracking_engine_bytetrack_fallback_to_simple(monkeypatch):
    monkeypatch.setattr(
        "tracking_engine.detect_vehicles_with_bytetrack_detailed",
        lambda frame: (_ for _ in ()).throw(RuntimeError("boom")),
    )
    monkeypatch.setattr(
        "tracking_engine.detect_vehicles_detailed",
        lambda frame: _fake_detection_result(),
    )
    monkeypatch.setattr(config, "TRACKER_STRICT", False)

    engine = TrackingEngine(backend="bytetrack")
    result = engine.detect(object())

    assert engine.backend == "simple"
    assert "track_id" in result.tracked_objects[0]


def test_tracking_engine_bytetrack_strict_raises(monkeypatch):
    monkeypatch.setattr(
        "tracking_engine.detect_vehicles_with_bytetrack_detailed",
        lambda frame: (_ for _ in ()).throw(RuntimeError("boom")),
    )
    monkeypatch.setattr(config, "TRACKER_STRICT", True)

    engine = TrackingEngine(backend="bytetrack")
    with pytest.raises(RuntimeError):
        engine.detect(object())
