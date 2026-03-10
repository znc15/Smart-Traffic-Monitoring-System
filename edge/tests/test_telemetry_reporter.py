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
