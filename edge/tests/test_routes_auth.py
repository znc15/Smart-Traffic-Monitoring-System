from pathlib import Path
import sys

from fastapi import FastAPI
from fastapi.testclient import TestClient

sys.path.append(str(Path(__file__).resolve().parents[1]))

import config
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
