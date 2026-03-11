from __future__ import annotations

import sys
import types
from pathlib import Path


EDGE_ROOT = Path(__file__).resolve().parents[1]
if str(EDGE_ROOT) not in sys.path:
    sys.path.append(str(EDGE_ROOT))


try:
    import ultralytics  # noqa: F401
except Exception:
    fake_module = types.ModuleType("ultralytics")

    class _FakeYOLO:
        def __init__(self, *args, **kwargs):
            pass

        def __call__(self, *args, **kwargs):
            return []

        def track(self, *args, **kwargs):
            return []

        def export(self, *args, **kwargs):
            return None

    fake_module.YOLO = _FakeYOLO
    sys.modules["ultralytics"] = fake_module
