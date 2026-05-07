<template>
  <div class="h-full min-h-[calc(100vh-120px)] relative rounded-2xl overflow-hidden shadow-lg border border-border">
    <div ref="mapContainerRef" class="absolute inset-0 bg-muted/20" />

    <div class="absolute left-6 right-6 top-6 flex flex-col gap-4 pointer-events-none z-10">
      <div class="p-5 rounded-2xl bg-background/85 backdrop-blur-xl border border-border/50 shadow-xl pointer-events-auto flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 class="text-xl font-bold text-foreground">城市路网实时状态</h1>
          <p class="text-sm text-muted-foreground mt-1">
            当前页面仅显示地图，点位详情请直接点击地图标记查看
          </p>
        </div>
        <div class="flex items-center gap-3">
          <Badge :variant="effectiveAmapKey ? 'default' : 'warning'" class="px-3 py-1 text-sm">
            {{ amapStatusText }}
          </Badge>
          <Button variant="secondary" @click="onlineOnly = !onlineOnly">
            {{ onlineOnly ? '显示全部节点' : '仅看在线节点' }}
          </Button>
          <Button :disabled="loading" @click="refreshOverview">
            <RefreshCw class="w-4 h-4 mr-2" :class="{ 'animate-spin': loading }" />
            刷新
          </Button>
        </div>
      </div>

      <Alert v-if="mapError" variant="destructive" class="pointer-events-auto shadow-lg bg-destructive/10 backdrop-blur-md">
        <AlertTriangle class="w-4 h-4" />
        <AlertTitle>错误</AlertTitle>
        <AlertDescription>{{ mapError }}</AlertDescription>
      </Alert>
      <Alert v-else-if="!effectiveAmapKey" variant="warning" class="pointer-events-auto shadow-lg bg-warning/10 backdrop-blur-md border-warning/50">
        <AlertTriangle class="w-4 h-4 text-warning" />
        <AlertTitle class="text-warning">配置缺失</AlertTitle>
        <AlertDescription class="text-warning">未配置后台 `amap_key`，且缺少部署 fallback `VITE_AMAP_KEY`，当前无法显示地图。</AlertDescription>
      </Alert>
      <Alert v-else-if="!mappableItems.length && !loading" variant="default" class="pointer-events-auto shadow-lg bg-background/85 backdrop-blur-md">
        <Info class="w-4 h-4" />
        <AlertTitle>提示</AlertTitle>
        <AlertDescription>
          当前没有可绘制的点位坐标。已加载 {{ items.length }} 个节点，其中 {{ missingCoordinateCount }} 个缺少经纬度。
        </AlertDescription>
      </Alert>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { toast } from 'vue-sonner'
import { RefreshCw, AlertTriangle, Info } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert'
import { connectAlertsWs } from '@/lib/alerts-ws'
import {
  AMAP_KEY,
  AMAP_SECURITY_JS_CODE,
  AMAP_SERVICE_HOST,
  authFetch,
  endpoints,
  getWsUrl,
} from '../lib/api'
import { ensureAmap } from '../lib/amap'
import {
  normalizeMapOverviewPoint,
  normalizeSiteSettings,
  type MapOverviewPoint,
  type SiteSettings,
} from '../lib/normalize'

const loading = ref(false)
const mapError = ref('')
const items = ref<MapOverviewPoint[]>([])
const onlineOnly = ref(false)
const mapContainerRef = ref<HTMLDivElement | null>(null)
const siteSettings = ref<SiteSettings>(normalizeSiteSettings({}))
let alertsWsClient: ReturnType<typeof connectAlertsWs> | null = null
const alertNodeIds = new Set<string>()

let mapInstance: any = null
let infoWindow: any = null
let heatmapLayer: any = null
let mapPluginsReady = false
let renderEpoch = 0
const AMAP_PLUGINS = ['AMap.Scale', 'AMap.ToolBar', 'AMap.HeatMap']

const filteredItems = computed(() =>
  onlineOnly.value ? items.value.filter((item) => item.online) : items.value,
)
const mappableItems = computed(() =>
  filteredItems.value.filter((item) => Number.isFinite(item.latitude) && Number.isFinite(item.longitude) && item.latitude !== 0 && item.longitude !== 0),
)
const missingCoordinateCount = computed(
  () => filteredItems.value.length - mappableItems.value.length,
)
const effectiveAmapKey = computed(() => {
  const runtimeKey = siteSettings.value.amap_key.trim()
  return runtimeKey || String(AMAP_KEY || '').trim()
})
const effectiveAmapSecurityJsCode = computed(() => {
  const runtimeCode = siteSettings.value.amap_security_js_code.trim()
  return runtimeCode || String(AMAP_SECURITY_JS_CODE || '').trim()
})
const effectiveAmapServiceHost = computed(() => {
  const runtimeHost = siteSettings.value.amap_service_host.trim()
  return runtimeHost || String(AMAP_SERVICE_HOST || '').trim()
})
const amapStatusText = computed(() => {
  if (!effectiveAmapKey.value) {
    return '缺少高德地图 Key'
  }
  return siteSettings.value.amap_key.trim() ? 'AMap 已由后台配置' : 'AMap 使用部署 fallback'
})

const densityLabel = (densityStatus: string) => {
  if (densityStatus === 'congested') return '拥堵'
  if (densityStatus === 'busy') return '繁忙'
  if (densityStatus === 'clear') return '通畅'
  return densityStatus || '未知'
}

const formatDateTime = (value: string | null) => {
  if (!value) return '—'
  return new Date(value).toLocaleString('zh-CN')
}

const markerColor = (item: MapOverviewPoint) => {
  if (item.edge_node_id && alertNodeIds.has(item.edge_node_id)) return '#dc2626'
  if (!item.online) return '#64748b'
  if (item.congestion_index >= 0.7) return '#ef4444'
  if (item.congestion_index >= 0.4) return '#f59e0b'
  return '#22c55e'
}

const markerHtml = (item: MapOverviewPoint) => `
  <div style="position:relative;width:20px;height:20px;">
    ${item.edge_node_id && alertNodeIds.has(item.edge_node_id) ? `
    <div style="
      position:absolute;
      left:50%;
      top:50%;
      width: 36px;
      height: 36px;
      border-radius: 999px;
      background: rgba(239,68,68,0.15);
      border: 2px solid rgba(239,68,68,0.35);
      transform: translate(-50%, -50%);
    "></div>
    ` : ''}
    <div style="
      position:absolute;
      left:0;
      top:0;
      width: 20px;
      height: 20px;
      border-radius: 999px;
      background: ${markerColor(item)};
      border: 3px solid rgba(255,255,255,0.95);
      box-shadow: 0 4px 12px rgba(0,0,0,0.3);
    "></div>
  </div>
`

const infoHtml = (item: MapOverviewPoint) => `
  <div style="min-width:260px;padding:8px 4px;font-family:ui-sans-serif,system-ui,sans-serif;color:#1e293b;">
    <div style="font-weight:700;font-size:16px;margin-bottom:8px;color:#0f172a;">${item.road_name || item.name || '未命名'}</div>
    <div style="font-size:13px;margin-bottom:4px;color:#475569;">节点 ID：<span style="font-family:monospace;background:#f1f5f9;padding:2px 4px;border-radius:4px;">${item.edge_node_id || '未配置'}</span></div>
    <div style="font-size:13px;margin-bottom:4px;color:#475569;">状态：<strong style="color:${item.online?'#22c55e':'#ef4444'}">${item.online ? '在线' : '离线'}</strong> / 拥堵：<strong>${densityLabel(item.density_status)}</strong></div>
    <div style="font-size:13px;margin-bottom:4px;color:#475569;">车辆：<strong>${item.count_car + item.count_motor}</strong> / 行人：<strong>${item.count_person}</strong></div>
    <div style="font-size:13px;margin-bottom:4px;color:#475569;">速度：<strong>${item.speed_car.toFixed(1)}</strong> km/h</div>
    <div style="font-size:12px;color:#94a3b8;margin-top:8px;">更新时间：${formatDateTime(item.updated_at)}</div>
    ${
      (item.snapshot_url || endpoints.frameNoAuth(item.road_name || item.name))
        ? `<img src="${item.snapshot_url || endpoints.frameNoAuth(item.road_name || item.name)}" alt="${
            item.road_name || item.name
          }" style="width:100%;margin-top:12px;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.1)" />`
        : ''
    }
  </div>
`

const isLatestRender = (epoch: number) => epoch === renderEpoch

const loadAmapPlugins = (AMap: any) =>
  new Promise<void>((resolve) => {
    AMap.plugin(AMAP_PLUGINS, () => resolve())
  })

const destroyMap = () => {
  renderEpoch += 1
  if (heatmapLayer) {
    heatmapLayer.setMap?.(null)
    heatmapLayer = null
  }
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
  mapPluginsReady = false
  infoWindow = null
}

const renderMap = async () => {
  if (!mapContainerRef.value || !mappableItems.value.length || !effectiveAmapKey.value) {
    mapError.value = ''
    destroyMap()
    return
  }

  const currentEpoch = ++renderEpoch

  try {
    const AMap = await ensureAmap({
      key: effectiveAmapKey.value,
      securityJsCode: effectiveAmapSecurityJsCode.value,
      serviceHost: effectiveAmapServiceHost.value,
    })
    if (!isLatestRender(currentEpoch) || !AMap || !mapContainerRef.value) return

    if (!mapInstance) {
      mapInstance = new AMap.Map(mapContainerRef.value, {
        viewMode: '3D',
        zoom: 13,
        mapStyle: 'amap://styles/dark', // Dark map style for industrial look
        center: [mappableItems.value[0].longitude, mappableItems.value[0].latitude],
      })
    }

    if (!mapPluginsReady) {
      await loadAmapPlugins(AMap)
      if (!isLatestRender(currentEpoch) || !mapInstance) return

      mapInstance.addControl(new AMap.Scale())
      mapInstance.addControl(new AMap.ToolBar({ position: 'RB' }))
      infoWindow = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -22) })
      heatmapLayer = new AMap.HeatMap(mapInstance, {
        radius: 40,
        opacity: [0.15, 0.85],
        gradient: {
          0.2: '#3b82f6',
          0.45: '#eab308',
          0.7: '#f43f5e',
          1.0: '#b91c1c',
        },
      })
      mapPluginsReady = true
    }

    if (!isLatestRender(currentEpoch) || !mapInstance) return

    mapInstance.clearMap()
    mapError.value = ''

    const heatPoints = mappableItems.value
      .map((item) => ({
        lng: item.longitude,
        lat: item.latitude,
        count: Math.max(1, Math.round(item.congestion_index * 100)),
      }))

    const markers = mappableItems.value
      .map((item) => {
        const marker = new AMap.Marker({
          position: [item.longitude, item.latitude],
          anchor: 'bottom-center',
          content: markerHtml(item),
        })
        marker.on('click', () => {
          infoWindow.setContent(infoHtml(item))
          infoWindow.open(mapInstance, marker.getPosition())
        })
        return marker
      })

    if (markers.length) {
      mapInstance.add(markers)
      mapInstance.setFitView(markers, false, [80, 80, 80, 80])
    }

    if (heatmapLayer) {
      heatmapLayer.setDataSet({ data: heatPoints, max: 100 })
    }
  } catch (error) {
    if (!isLatestRender(currentEpoch)) return
    mapError.value = error instanceof Error ? error.message : '地图初始化失败'
  }
}

const fetchSiteSettings = async () => {
  try {
    const res = await fetch(endpoints.siteSettings)
    if (!res.ok) {
      return normalizeSiteSettings({})
    }
    return normalizeSiteSettings(await res.json())
  } catch {
    return normalizeSiteSettings({})
  }
}

const refreshOverview = async () => {
  loading.value = true
  mapError.value = ''
  try {
    const [res, latestSiteSettings] = await Promise.all([
      authFetch(endpoints.mapOverview),
      fetchSiteSettings(),
    ])
    if (!res.ok) {
      throw new Error('获取地图总览失败')
    }
    siteSettings.value = latestSiteSettings
    const payload = await res.json()
    const rawItems = Array.isArray(payload)
      ? payload
      : Array.isArray(payload?.items)
        ? payload.items
        : Array.isArray(payload?.content)
          ? payload.content
          : []
    items.value = rawItems.map((item: unknown) => normalizeMapOverviewPoint(item))
  } catch (error) {
    mapError.value = error instanceof Error ? error.message : '加载地图数据失败'
    toast.error(mapError.value)
  } finally {
    loading.value = false
  }
}

type BackendAlert = {
  id: number
  type: string
  level: string
  roadName?: string
  road_name?: string
  nodeId?: string | null
  node_id?: string | null
  message: string
  status: string
  createdAt?: string
  created_at?: string
  updatedAt?: string | null
  updated_at?: string | null
}

function handleIncomingAlert(payload: unknown) {
  const raw = payload as Partial<BackendAlert>
  const nodeCandidate = raw?.nodeId ?? raw?.node_id ?? ''
  const nodeId = typeof nodeCandidate === 'string' && nodeCandidate.trim() ? nodeCandidate.trim() : ''
  if (!nodeId) return
  if (!alertNodeIds.has(nodeId)) {
    alertNodeIds.add(nodeId)
    setTimeout(() => {
      alertNodeIds.delete(nodeId)
      renderMap()
    }, 60_000)
  }
  if (typeof raw?.message === 'string' && raw.message.trim()) {
    toast.warning(raw.message.trim())
  } else {
    toast.warning('收到节点告警')
  }
  renderMap()
}

watch([mappableItems, effectiveAmapKey, effectiveAmapSecurityJsCode, effectiveAmapServiceHost], async ([drawable, key]) => {
  if (!drawable.length || !key) {
    mapError.value = ''
    destroyMap()
    return
  }
  await nextTick()
  await renderMap()
}, { flush: 'post' })

onMounted(() => {
  refreshOverview()
  alertsWsClient = connectAlertsWs({
    url: getWsUrl(endpoints.alertsWs),
    onAlert: handleIncomingAlert,
  })
})

onUnmounted(() => {
  alertsWsClient?.close()
  alertsWsClient = null
  destroyMap()
})
</script>
