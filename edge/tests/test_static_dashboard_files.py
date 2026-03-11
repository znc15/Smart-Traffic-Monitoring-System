from pathlib import Path
import sys

from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from fastapi.testclient import TestClient


EDGE_ROOT = Path(__file__).resolve().parents[1]
if str(EDGE_ROOT) not in sys.path:
    sys.path.append(str(EDGE_ROOT))


from routes import router


def _build_client() -> TestClient:
    app = FastAPI()
    app.include_router(router)
    app.mount("/static", StaticFiles(directory=str(EDGE_ROOT / "static")), name="static")
    return TestClient(app)


def test_root_redirects_to_static_index():
    client = _build_client()
    response = client.get("/", follow_redirects=False)

    assert response.status_code in (302, 307)
    assert response.headers["location"] == "/static/index.html"


def test_static_dashboard_resources_are_served():
    client = _build_client()

    for path in (
        "/static/index.html",
        "/static/partials/monitor.html",
        "/static/partials/settings.html",
        "/static/partials/test.html",
        "/static/js/app.js",
        "/static/css/base.css",
    ):
        response = client.get(path)
        assert response.status_code == 200, path


def test_index_shell_uses_split_assets():
    index_path = EDGE_ROOT / "static" / "index.html"
    html = index_path.read_text(encoding="utf-8")

    assert "./css/base.css?v=2026-03-11-v2" in html
    assert "./css/components.css?v=2026-03-11-v2" in html
    assert "./css/pages.css?v=2026-03-11-v2" in html
    assert "./js/app.js?v=2026-03-11-v2" in html
    assert "data-dashboard-version=\"2026-03-11-v2\"" in html
    assert "<style>" not in html
