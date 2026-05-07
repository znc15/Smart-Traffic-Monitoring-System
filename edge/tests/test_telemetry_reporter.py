from pathlib import Path
import sys

sys.path.append(str(Path(__file__).resolve().parents[1]))

import config
import telemetry_reporter


class _DummyResponse:
    def __init__(self, status: int = 200):
        self.status = status

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False


def test_post_once_sends_edge_headers(monkeypatch):
    captured: dict = {}
    old_url = config.BACKEND_TELEMETRY_URL
    old_node_id = config.EDGE_NODE_ID
    old_key = config.EDGE_API_KEY

    def fake_urlopen(req, timeout):
        captured["headers"] = dict(req.header_items())
        captured["timeout"] = timeout
        return _DummyResponse(status=200)

    config.BACKEND_TELEMETRY_URL = "http://backend.example/api/v1/edge/telemetry"
    config.EDGE_NODE_ID = "edge-01"
    config.EDGE_API_KEY = "secret-edge-key"
    monkeypatch.setattr(telemetry_reporter.request, "urlopen", fake_urlopen)

    try:
        ok = telemetry_reporter._post_once({"road_name": "测试路段"})
        assert ok is True
        assert captured["headers"]["X-edge-node-id"] == "edge-01"
        assert captured["headers"]["X-edge-key"] == "secret-edge-key"
        assert captured["timeout"] == 3
    finally:
        config.BACKEND_TELEMETRY_URL = old_url
        config.EDGE_NODE_ID = old_node_id
        config.EDGE_API_KEY = old_key


def test_build_payload_includes_memory_and_uptime_metrics(monkeypatch):
    monkeypatch.setattr(
        telemetry_reporter.state,
        "get_traffic",
        lambda: {
            "count_car": 4,
            "count_motor": 2,
            "count_person": 1,
            "speed_car": 28.0,
            "speed_motor": 16.0,
            "lane_stats": [],
            "events": [],
        },
    )
    monkeypatch.setattr(
        telemetry_reporter.state,
        "get_edge_metrics",
        lambda: {
            "fps": 24.0,
            "inference_ms": 42.5,
            "cpu_percent": 61.3,
            "memory_percent": 47.8,
            "uptime_s": 5400.0,
        },
    )

    payload = telemetry_reporter._build_payload()

    assert payload["edge_metrics"] == {
        "fps": 24.0,
        "inference_ms": 42.5,
        "cpu_percent": 61.3,
        "memory_percent": 47.8,
        "uptime_s": 5400.0,
    }
