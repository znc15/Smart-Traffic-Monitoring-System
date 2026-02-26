from pathlib import Path
import sys

sys.path.append(str(Path(__file__).resolve().parents[1]))

from validators import has_openvino_cache, validate_probe_source


def test_validate_probe_source_accepts_supported_inputs():
    assert validate_probe_source("0") == "0"
    assert validate_probe_source(" 1 ") == "1"
    assert validate_probe_source("/dev/video0") == "/dev/video0"
    assert validate_probe_source("rtsp://192.168.1.2/live") == "rtsp://192.168.1.2/live"
    assert validate_probe_source("https://example.com/stream") == "https://example.com/stream"


def test_validate_probe_source_rejects_invalid_input():
    try:
        validate_probe_source("camera-source")
        assert False, "expected ValueError"
    except ValueError as ex:
        assert "仅支持设备号" in str(ex)


def test_has_openvino_cache_supports_legacy_and_new_patterns(tmp_path):
    models_dir = tmp_path / "models"
    models_dir.mkdir()

    assert has_openvino_cache(models_dir, "yolov8n") is False

    (models_dir / "yolov8n_openvino_model").mkdir()
    assert has_openvino_cache(models_dir, "yolov8n") is True

    # Remove legacy directory then verify new naming pattern
    (models_dir / "yolov8n_openvino_model").rmdir()
    (models_dir / "yolov8n_320_openvino_model").mkdir()
    assert has_openvino_cache(models_dir, "yolov8n") is True
