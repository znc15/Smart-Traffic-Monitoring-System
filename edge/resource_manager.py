"""
Resource manager: auto-detect hardware capabilities and apply resource-level presets.

Levels:
  - low:    <=2 cores OR <=2 GB RAM  → aggressive throttling
  - medium: default
  - high:   >=8 cores AND >=8 GB RAM → allow larger models
"""

import logging
import psutil

import config

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Resource level definitions
# ---------------------------------------------------------------------------
_resource_level: str = "medium"

# Preset overrides per level (only fields that differ from config defaults)
_PRESETS = {
    "low": {
        "FRAME_SKIP": 3,
        "IMGSZ": 320,
        "JPEG_QUALITY": 60,
        "MAX_MJPEG_CLIENTS": 2,
    },
    "medium": {
        # keep config defaults
    },
    "high": {
        "FRAME_SKIP": 1,
        "IMGSZ": 640,
    },
}


# ---------------------------------------------------------------------------
# Detection helpers
# ---------------------------------------------------------------------------
def _detect_level() -> str:
    """Determine resource level based on CPU cores and available memory."""
    cpu_count = psutil.cpu_count(logical=True) or 1
    mem = psutil.virtual_memory()
    mem_gb = mem.total / (1024 ** 3)

    logger.info("Hardware: %d CPU cores, %.1f GB RAM", cpu_count, mem_gb)

    if cpu_count <= 2 or mem_gb <= 2.0:
        return "low"
    if cpu_count >= 8 and mem_gb >= 8.0:
        return "high"
    return "medium"


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------
def get_resource_level() -> str:
    """Return the current resource level string ('low', 'medium', 'high')."""
    return _resource_level


def get_resource_params() -> dict:
    """Return a snapshot of the active resource-aware parameters."""
    return {
        "level": _resource_level,
        "cpu_cores": psutil.cpu_count(logical=True) or 1,
        "memory_total_gb": round(psutil.virtual_memory().total / (1024 ** 3), 1),
        "FRAME_SKIP": config.FRAME_SKIP,
        "IMGSZ": config.IMGSZ,
        "JPEG_QUALITY": config.JPEG_QUALITY,
        "MAX_MJPEG_CLIENTS": config.MAX_MJPEG_CLIENTS,
    }


def init_resource_manager() -> str:
    """
    Detect hardware, determine resource level, and apply preset overrides
    to the config module.  Should be called once at startup (before the
    detection loop starts).

    Returns the detected level.
    """
    global _resource_level

    _resource_level = _detect_level()
    preset = _PRESETS.get(_resource_level, {})

    for key, value in preset.items():
        setattr(config, key, value)

    logger.info(
        "Resource level: %s | FRAME_SKIP=%d, IMGSZ=%d, JPEG_QUALITY=%d, MAX_MJPEG_CLIENTS=%d",
        _resource_level,
        config.FRAME_SKIP,
        config.IMGSZ,
        config.JPEG_QUALITY,
        config.MAX_MJPEG_CLIENTS,
    )

    return _resource_level
