"""Pure validation helpers used by routes and tests."""

from pathlib import Path


def validate_probe_source(value: str) -> str:
    source = value.strip()
    if not source:
        raise ValueError("url 不能为空")
    if source.isdigit():
        return source
    if source.startswith("/dev/"):
        return source
    if source.startswith(("rtsp://", "rtsps://", "http://", "https://")):
        return source
    raise ValueError("仅支持设备号、/dev/*、rtsp(s):// 或 http(s):// 地址")


def has_openvino_cache(models_dir: Path, stem: str) -> bool:
    """Check both legacy and new cache naming conventions."""
    legacy_candidates = [
        models_dir / f"{stem}_openvino_model",
        models_dir / f"{stem}_int8_openvino_model",
    ]
    if any(path.is_dir() for path in legacy_candidates):
        return True

    dynamic_patterns = [
        f"{stem}_*_openvino_model",
        f"{stem}_int8_*_openvino_model",
    ]
    for pattern in dynamic_patterns:
        for path in models_dir.glob(pattern):
            if path.is_dir():
                return True
    return False
