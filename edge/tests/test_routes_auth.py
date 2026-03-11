from pathlib import Path
import sys

import numpy as np
from fastapi import FastAPI
from fastapi.testclient import TestClient

sys.path.append(str(Path(__file__).resolve().parents[1]))

import config
import routes
from routes import router
from state import state


def _build_client() -> TestClient:
    app = FastAPI()
    app.include_router(router)
    return TestClient(app)


def setup_function():
    state.restart_callback = None
    state.update_traffic(
        count_car=3,
        count_motor=1,
        speed_car=24.5,
        speed_motor=18.0,
        inference_ms=12.0,
        fps=20.0,
        count_person=0,
        lane_stats=[],
        events=[],
        tracked_objects=[],
    )
    state.set_loop_state("stopped")


def test_protected_routes_require_edge_key_when_configured():
    client = _build_client()
    old_key = config.EDGE_API_KEY
    old_node_id = config.EDGE_NODE_ID
    config.EDGE_API_KEY = "secret-edge-key"
    config.EDGE_NODE_ID = "edge-01"

    try:
        assert client.get("/api/traffic").status_code == 401
        assert client.get("/api/metrics").status_code == 401
        assert client.get("/api/config").status_code == 401

        headers = {
            "X-Edge-Key": "secret-edge-key",
            "X-Edge-Node-Id": "edge-01",
        }
        assert client.get("/api/traffic", headers=headers).status_code == 200
        assert client.get("/api/metrics", headers=headers).status_code == 200
        assert client.get("/api/config", headers=headers).status_code == 200
    finally:
        config.EDGE_API_KEY = old_key
        config.EDGE_NODE_ID = old_node_id


def test_media_routes_accept_query_edge_key_when_configured(monkeypatch):
    client = _build_client()
    old_key = config.EDGE_API_KEY
    old_node_id = config.EDGE_NODE_ID
    config.EDGE_API_KEY = "secret-edge-key"
    config.EDGE_NODE_ID = "edge-01"

    async def fake_mjpeg_generator():
        yield (
            b"--frame\r\n"
            b"Content-Type: image/jpeg\r\n\r\n"
            b"frame"
            b"\r\n"
        )

    monkeypatch.setattr(routes, "_mjpeg_generator", fake_mjpeg_generator)

    try:
        assert client.get("/api/frame").status_code == 401
        assert client.get("/api/video").status_code == 401

        frame_response = client.get("/api/frame?edge_key=secret-edge-key&edge_node_id=edge-01")
        assert frame_response.status_code == 200
        assert frame_response.headers["content-type"].startswith("image/jpeg")

        with client.stream(
            "GET",
            "/api/stream?edge_key=secret-edge-key&edge_node_id=edge-01",
        ) as stream_response:
            assert stream_response.status_code == 200
            assert stream_response.headers["content-type"].startswith(
                "multipart/x-mixed-replace",
            )
    finally:
        config.EDGE_API_KEY = old_key
        config.EDGE_NODE_ID = old_node_id


def test_high_cost_routes_require_edge_key_when_configured(monkeypatch):
    client = _build_client()
    old_key = config.EDGE_API_KEY
    old_node_id = config.EDGE_NODE_ID
    config.EDGE_API_KEY = "secret-edge-key"
    config.EDGE_NODE_ID = "edge-01"

    def _should_not_run(*args, **kwargs):
        raise AssertionError("detector should not run before auth")

    monkeypatch.setattr("routes.detect_vehicles_detailed", _should_not_run)

    try:
        assert client.post(
            "/api/cameras/probe",
            json={"url": "0"},
        ).status_code == 401

        assert client.post(
            "/api/detect/image",
            files={"file": ("demo.jpg", b"fake-image", "image/jpeg")},
        ).status_code == 401

        assert client.post(
            "/api/detect/video",
            files={"file": ("demo.mp4", b"fake-video", "video/mp4")},
        ).status_code == 401

        assert client.get("/api/detect/video/result/abcdef123456").status_code == 401
    finally:
        config.EDGE_API_KEY = old_key
        config.EDGE_NODE_ID = old_node_id


def test_detect_video_result_accepts_query_edge_key_when_configured():
    client = _build_client()
    old_key = config.EDGE_API_KEY
    old_node_id = config.EDGE_NODE_ID
    config.EDGE_API_KEY = "secret-edge-key"
    config.EDGE_NODE_ID = "edge-01"

    try:
        response = client.get(
            "/api/detect/video/result/abcdef123456?edge_key=secret-edge-key&edge_node_id=edge-01"
        )
        assert response.status_code == 404
    finally:
        config.EDGE_API_KEY = old_key
        config.EDGE_NODE_ID = old_node_id


def test_protected_routes_allow_access_without_key_when_not_configured():
    client = _build_client()
    old_key = config.EDGE_API_KEY
    config.EDGE_API_KEY = ""

    try:
        assert client.get("/api/traffic").status_code == 200
        assert client.get("/api/metrics").status_code == 200
        assert client.get("/api/config").status_code == 200
    finally:
        config.EDGE_API_KEY = old_key


def test_high_cost_routes_require_auth_when_configured(monkeypatch):
    client = _build_client()
    old_key = config.EDGE_API_KEY
    old_node_id = config.EDGE_NODE_ID
    config.EDGE_API_KEY = "secret-edge-key"
    config.EDGE_NODE_ID = "edge-01"

    monkeypatch.setattr(
        routes.camera_discovery,
        "probe_rtsp",
        lambda url: {"width": 1280, "height": 720},
    )
    monkeypatch.setattr(
        routes,
        "detect_vehicles_detailed",
        lambda frame: (np.zeros((2, 2, 3), dtype=np.uint8), 1, 0, 1.2, []),
    )

    try:
        image_resp = client.post(
            "/api/detect/image",
            files={"file": ("frame.jpg", b"not-a-real-image", "image/jpeg")},
        )
        assert image_resp.status_code == 401

        video_resp = client.post(
            "/api/detect/video",
            files={"file": ("clip.mp4", b"fake-video", "video/mp4")},
        )
        assert video_resp.status_code == 401

        probe_resp = client.post("/api/cameras/probe", json={"url": "0"})
        assert probe_resp.status_code == 401

        ok_resp = client.post(
            "/api/cameras/probe",
            headers={
                "X-Edge-Key": "secret-edge-key",
                "X-Edge-Node-Id": "edge-01",
            },
            json={"url": "0"},
        )
        assert ok_resp.status_code == 200
        assert ok_resp.json()["reachable"] is True
    finally:
        config.EDGE_API_KEY = old_key
        config.EDGE_NODE_ID = old_node_id


def test_video_result_download_accepts_query_edge_key(tmp_path):
    client = _build_client()
    old_key = config.EDGE_API_KEY
    old_node_id = config.EDGE_NODE_ID
    old_tmp_dir = routes._TMP_DIR
    config.EDGE_API_KEY = "secret-edge-key"
    config.EDGE_NODE_ID = "edge-01"
    routes._TMP_DIR = tmp_path
    result_path = tmp_path / "result_abc123def456.mp4"
    result_path.write_bytes(b"fake-video")

    try:
        assert client.get("/api/detect/video/result/abc123def456").status_code == 401

        response = client.get(
            "/api/detect/video/result/abc123def456?edge_key=secret-edge-key&edge_node_id=edge-01"
        )
        assert response.status_code == 200
        assert response.headers["content-type"].startswith("video/mp4")
        assert response.content == b"fake-video"
    finally:
        config.EDGE_API_KEY = old_key
        config.EDGE_NODE_ID = old_node_id
        routes._TMP_DIR = old_tmp_dir


def test_put_config_updates_analysis_fields_with_valid_key():
    client = _build_client()
    old_values = {
        "edge_api_key": config.EDGE_API_KEY,
        "edge_node_id": config.EDGE_NODE_ID,
        "analysis_roi": list(config.ANALYSIS_ROI),
        "lane_split_ratios": list(config.LANE_SPLIT_RATIOS),
        "speed_meters_per_pixel": config.SPEED_METERS_PER_PIXEL,
        "parking_stationary_seconds": config.PARKING_STATIONARY_SECONDS,
        "wrong_way_min_track_points": config.WRONG_WAY_MIN_TRACK_POINTS,
        "telemetry_interval_sec": config.TELEMETRY_INTERVAL_SEC,
    }
    config.EDGE_API_KEY = "secret-edge-key"
    config.EDGE_NODE_ID = "edge-01"

    try:
        response = client.put(
            "/api/config",
            headers={
                "X-Edge-Key": "secret-edge-key",
                "X-Edge-Node-Id": "edge-01",
            },
            json={
                "analysis_roi": [0.1, 0.2, 0.9, 0.95],
                "lane_split_ratios": [0.25, 0.65],
                "speed_meters_per_pixel": 0.12,
                "parking_stationary_seconds": 6.0,
                "wrong_way_min_track_points": 5,
                "telemetry_interval_sec": 4.5,
            },
        )

        assert response.status_code == 200
        body = response.json()
        assert body["analysis_roi"] == [0.1, 0.2, 0.9, 0.95]
        assert body["lane_split_ratios"] == [0.25, 0.65]
        assert body["speed_meters_per_pixel"] == 0.12
        assert body["parking_stationary_seconds"] == 6.0
        assert body["wrong_way_min_track_points"] == 5
        assert body["telemetry_interval_sec"] == 4.5
        assert body["restarted"] is False
        assert body["telemetry_interval_updated"] is True
    finally:
        config.EDGE_API_KEY = old_values["edge_api_key"]
        config.EDGE_NODE_ID = old_values["edge_node_id"]
        config.ANALYSIS_ROI = old_values["analysis_roi"]
        config.LANE_SPLIT_RATIOS = old_values["lane_split_ratios"]
        config.SPEED_METERS_PER_PIXEL = old_values["speed_meters_per_pixel"]
        config.PARKING_STATIONARY_SECONDS = old_values["parking_stationary_seconds"]
        config.WRONG_WAY_MIN_TRACK_POINTS = old_values["wrong_way_min_track_points"]
        config.TELEMETRY_INTERVAL_SEC = old_values["telemetry_interval_sec"]


def test_put_config_rolls_back_when_restart_fails():
    client = _build_client()
    old_values = {
        "edge_api_key": config.EDGE_API_KEY,
        "edge_node_id": config.EDGE_NODE_ID,
        "mode": config.MODE,
        "camera_source": config.camera_source,
    }
    config.EDGE_API_KEY = "secret-edge-key"
    config.EDGE_NODE_ID = "edge-01"
    config.MODE = "sim"
    config.camera_source = "0"

    def fail_restart(*args, **kwargs):
        raise RuntimeError("restart failed")

    state.restart_callback = fail_restart

    try:
        response = client.put(
            "/api/config",
            headers={
                "X-Edge-Key": "secret-edge-key",
                "X-Edge-Node-Id": "edge-01",
            },
            json={
                "mode": "camera",
                "camera_source": "rtsp://demo.local/live",
            },
        )

        assert response.status_code == 503
        body = response.json()
        assert body["rolled_back"] is True
        assert body["restarted"] is False
        assert config.MODE == "sim"
        assert config.camera_source == "0"
    finally:
        state.restart_callback = None
        config.EDGE_API_KEY = old_values["edge_api_key"]
        config.EDGE_NODE_ID = old_values["edge_node_id"]
        config.MODE = old_values["mode"]
        config.camera_source = old_values["camera_source"]


def test_put_config_rolls_back_when_restart_fails():
    client = _build_client()
    old_values = {
        "edge_api_key": config.EDGE_API_KEY,
        "edge_node_id": config.EDGE_NODE_ID,
        "mode": config.MODE,
        "camera_source": config.camera_source,
    }
    config.EDGE_API_KEY = "secret-edge-key"
    config.EDGE_NODE_ID = "edge-01"
    config.MODE = "sim"
    config.camera_source = "old-source"
    state.restart_callback = lambda model_changed=False: (_ for _ in ()).throw(RuntimeError("restart timeout"))

    try:
        response = client.put(
            "/api/config?edge_key=secret-edge-key&edge_node_id=edge-01",
            json={
                "mode": "camera",
                "camera_source": "rtsp://new-source/live",
            },
        )

        assert response.status_code == 503
        body = response.json()
        assert body["detail"] == "restart timeout"
        assert body["rolled_back"] is True
        assert body["restarted"] is False
        assert config.MODE == "sim"
        assert config.camera_source == "old-source"
    finally:
        config.EDGE_API_KEY = old_values["edge_api_key"]
        config.EDGE_NODE_ID = old_values["edge_node_id"]
        config.MODE = old_values["mode"]
        config.camera_source = old_values["camera_source"]
        state.restart_callback = None
