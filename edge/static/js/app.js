import {
  $,
  appState,
  clearEdgeAccessKey,
  loadEdgeAccessKey,
  saveEdgeAccessKey,
  switchTab,
} from "./core.js";
import { initMonitorModule, VideoRenderer } from "./monitor.js";
import {
  applySettings,
  initSettingsModule,
  resetSettings,
  setMode,
  settings_previewCamera,
  settings_probeConnection,
  settings_scanCameras,
  settings_scanCamerasServer,
  settings_updateTestBtn,
  switchToCamera,
  switchToSim,
  toggleMode,
} from "./settings.js";
import { initTestPanel } from "./test-panel.js";

const DASHBOARD_VERSION =
  document.documentElement.dataset.dashboardVersion || "dev";

const PARTIAL_URLS = [
  buildVersionedUrl("../partials/monitor.html"),
  buildVersionedUrl("../partials/settings.html"),
  buildVersionedUrl("../partials/test.html"),
];

function buildVersionedUrl(relativePath) {
  const url = new URL(relativePath, import.meta.url);
  url.searchParams.set("v", DASHBOARD_VERSION);
  return url;
}

function exposeGlobals() {
  window.switchTab = switchTab;
  window.toggleMode = toggleMode;
  window.switchToSim = switchToSim;
  window.switchToCamera = switchToCamera;
  window.setMode = setMode;
  window.settings_scanCameras = settings_scanCameras;
  window.settings_scanCamerasServer = settings_scanCamerasServer;
  window.settings_probeConnection = settings_probeConnection;
  window.settings_previewCamera = settings_previewCamera;
  window.settings_updateTestBtn = settings_updateTestBtn;
  window.saveEdgeAccessKey = saveEdgeAccessKey;
  window.clearEdgeAccessKey = clearEdgeAccessKey;
  window.resetSettings = resetSettings;
  window.applySettings = applySettings;
  window.VideoRenderer = VideoRenderer;
}

async function fetchPartial(url) {
  const resp = await fetch(url);
  if (!resp.ok) {
    throw new Error(`${url.pathname} 返回 ${resp.status}`);
  }
  return resp.text();
}

function renderLoadError(message) {
  const target = $("dashboardContent");
  if (!target) return;

  target.innerHTML = `
    <div class="partial-load-error">
      <h2>仪表盘资源加载失败</h2>
      <p id="dashboardLoadErrorText"></p>
    </div>
  `;
  const text = $("dashboardLoadErrorText");
  if (text) {
    text.textContent = message;
  }
}

async function loadPartials() {
  const target = $("dashboardContent");
  if (!target) return false;

  try {
    const partials = await Promise.all(PARTIAL_URLS.map(fetchPartial));
    target.innerHTML = partials.join("\n");
    return true;
  } catch (err) {
    console.error("加载 partials 失败:", err);
    renderLoadError(err.message);
    return false;
  }
}

exposeGlobals();

document.addEventListener("DOMContentLoaded", async () => {
  const loaded = await loadPartials();
  if (!loaded) return;

  appState.partialsReady = true;
  initSettingsModule();
  initMonitorModule();
  initTestPanel();
  loadEdgeAccessKey();
  switchTab(appState.currentTab);
});
