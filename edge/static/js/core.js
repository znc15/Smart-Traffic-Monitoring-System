export const appState = {
  MAX_POINTS: 60,
  POLL_INTERVAL: 2000,
  isOnline: false,
  currentMode: "sim",
  chartLabels: [],
  chartCarData: [],
  chartMotorData: [],
  trendChart: null,
  EDGE_KEY_STORAGE_KEY: "edge_access_key",
  edgeAccessKey: "",
  edgeAuthWarningShown: false,
  currentResourceLevel: "medium",
  resourceParams: {},
  currentTab: "monitor",
  settingsLoaded: false,
  currentConfig: null,
  testHistory: [],
  testCurrentResult: null,
  trafficPollTimer: null,
  partialsReady: false,
};

let settingsLoader = null;

export const $ = (id) => document.getElementById(id);

export function registerSettingsLoader(loader) {
  settingsLoader = loader;
}

export function formatUptime(seconds) {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  return [h, m, s].map((v) => String(v).padStart(2, "0")).join(":");
}

export function getBarColor(percent) {
  if (percent >= 80) return "#ef4444";
  if (percent >= 60) return "#eab308";
  return "#22c55e";
}

export function escapeHtml(str) {
  const div = document.createElement("div");
  div.appendChild(document.createTextNode(str));
  return div.innerHTML;
}

export function debounce(fn, wait) {
  let timer = null;
  return function debounced(...args) {
    clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), wait);
  };
}

export function getEdgeAccessKey() {
  return (appState.edgeAccessKey || "").trim();
}

export function updateEdgeAccessHint(edgeKeyConfigured) {
  const hint = $("edgeAccessHint");
  if (!hint) return;

  const localKey = getEdgeAccessKey();
  if (edgeKeyConfigured === true) {
    hint.textContent = localKey
      ? "节点已启用访问密钥，当前浏览器会自动附加 X-Edge-Key。"
      : "节点已启用访问密钥，请输入后保存，否则配置与监控接口会返回 401。";
    return;
  }

  if (localKey) {
    hint.textContent = "当前浏览器已保存 X-Edge-Key；如果节点未启用密钥，可直接清空。";
    return;
  }

  hint.textContent = "仅保存在当前浏览器 localStorage，用于为受保护接口自动附加 X-Edge-Key。";
}

export function syncEdgeAccessKeyUi() {
  const input = $("edgeAccessKey");
  if (input) input.value = getEdgeAccessKey();
  updateEdgeAccessHint(appState.currentConfig ? !!appState.currentConfig.edge_api_key_configured : null);
}

export function setEdgeAccessKey(value, options) {
  const opts = options || {};
  appState.edgeAccessKey = String(value || "").trim();
  appState.edgeAuthWarningShown = false;

  if (opts.persist !== false) {
    if (appState.edgeAccessKey) {
      localStorage.setItem(appState.EDGE_KEY_STORAGE_KEY, appState.edgeAccessKey);
    } else {
      localStorage.removeItem(appState.EDGE_KEY_STORAGE_KEY);
    }
  }

  syncEdgeAccessKeyUi();

  if (opts.notify) {
    showToast(appState.edgeAccessKey ? "X-Edge-Key 已保存" : "X-Edge-Key 已清空", "success");
  }
}

export function loadEdgeAccessKey() {
  setEdgeAccessKey(localStorage.getItem(appState.EDGE_KEY_STORAGE_KEY) || "", { persist: false });
}

export function saveEdgeAccessKey() {
  const input = $("edgeAccessKey");
  setEdgeAccessKey(input ? input.value : "", { notify: true });

  if (appState.currentTab === "settings" && settingsLoader) {
    appState.settingsLoaded = false;
    settingsLoader();
  }
}

export function clearEdgeAccessKey() {
  setEdgeAccessKey("", { notify: true });
}

export function withEdgeKeyHeaders(headers) {
  const merged = new Headers(headers || {});
  const key = getEdgeAccessKey();
  if (key && !merged.has("X-Edge-Key")) {
    merged.set("X-Edge-Key", key);
  }
  return merged;
}

export async function apiFetch(url, init) {
  const options = Object.assign({}, init || {});
  options.headers = withEdgeKeyHeaders(options.headers);
  const resp = await fetch(url, options);

  if (resp.status === 401 && !appState.edgeAuthWarningShown) {
    appState.edgeAuthWarningShown = true;
    showToast(
      getEdgeAccessKey()
        ? "X-Edge-Key 无效或已变更，请在设置页更新后重试"
        : "节点已启用访问密钥，请在设置页填写 X-Edge-Key",
      "error"
    );
  }

  return resp;
}

export function appendEdgeAccessParams(url) {
  if (!url) return "";

  const target = new URL(url, window.location.origin);
  const key = getEdgeAccessKey();
  if (key) {
    target.searchParams.set("edge_key", key);
  }

  const nodeId = appState.currentConfig && appState.currentConfig.edge_node_id
    ? String(appState.currentConfig.edge_node_id).trim()
    : "";
  if (nodeId) {
    target.searchParams.set("edge_node_id", nodeId);
  }

  return target.toString();
}

export function updateBar(barId, valueId, percent, suffix) {
  const bar = $(barId);
  const val = $(valueId);
  if (!bar || !val) return;

  if (percent === null || percent === undefined) {
    val.textContent = "N/A";
    bar.style.width = "0%";
    bar.style.backgroundColor = "#cbd5e1";
    return;
  }

  const p = Math.min(100, Math.max(0, percent));
  val.textContent = p.toFixed(1) + (suffix || "%");
  bar.style.width = p + "%";
  bar.style.backgroundColor = getBarColor(p);
}

export function setOnlineStatus(online) {
  const dot = $("statusDot");
  if (dot) {
    if (online && !appState.isOnline) {
      dot.className = "w-3 h-3 rounded-full bg-green-500 status-online inline-block";
    } else if (!online && appState.isOnline) {
      dot.className = "w-3 h-3 rounded-full bg-red-500 status-offline inline-block";
    }
  }

  appState.isOnline = online;
}

export function switchTab(tabName) {
  appState.currentTab = tabName;

  document.querySelectorAll(".tab-item").forEach((el) => {
    el.classList.toggle("active", el.dataset.tab === tabName);
  });

  if (!appState.partialsReady) {
    return;
  }

  const tabMonitor = $("tabMonitor");
  const tabSettings = $("tabSettings");
  const tabTest = $("tabTest");

  if (tabMonitor) tabMonitor.style.display = tabName === "monitor" ? "block" : "none";
  if (tabSettings) tabSettings.style.display = tabName === "settings" ? "block" : "none";
  if (tabTest) tabTest.style.display = tabName === "test" ? "block" : "none";

  if (tabName === "settings" && settingsLoader) {
    settingsLoader();
  }
}

export function showToast(message, type) {
  const container = $("toastContainer");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = "toast " + (type || "success");
  toast.textContent = message;
  container.appendChild(toast);

  setTimeout(() => {
    toast.classList.add("removing");
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}
