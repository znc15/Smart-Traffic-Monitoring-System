<template>
  <section class="map-page">
    <n-card size="small" class="map-toolbar">
      <n-space justify="space-between" align="center" :wrap="true">
        <div>
          <div class="toolbar-title">城市路网 GIS 总览</div>
          <div class="toolbar-subtitle">
            展示监测点位、拥堵热力、实时流量与最近快照
          </div>
        </div>
        <n-space>
          <n-tag :type="effectiveAmapKey ? 'success' : 'warning'" round>
            {{ amapStatusText }}
          </n-tag>
          <n-button tertiary @click="onlineOnly = !onlineOnly">
            {{ onlineOnly ? '显示全部节点' : '仅看在线节点' }}
          </n-button>
          <n-button :loading="loading" @click="refreshOverview">刷新</n-button>
        </n-space>
      </n-space>
    </n-card>

    <n-grid :cols="24" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <n-grid-item span="24 l:17">
        <n-card size="small" :bordered="false" class="map-card">
          <div ref="mapContainerRef" class="map-canvas" />
          <n-alert v-if="mapError" type="error" class="map-overlay">
            {{ mapError }}
          </n-alert>
          <n-alert v-else-if="!effectiveAmapKey" type="warning" class="map-overlay">
            未配置后台 `amap_key`，且缺少部署 fallback `VITE_AMAP_KEY`，当前仅展示点位列表与数据卡片。
          </n-alert>
          <n-alert v-else-if="!mappableItems.length && !loading" type="info" class="map-overlay">
            当前没有可绘制的点位坐标。
            已加载 {{ items.length }} 个节点，其中 {{ missingCoordinateCount }} 个缺少经纬度。
          </n-alert>
        </n-card>
      </n-grid-item>

      <n-grid-item span="24 l:7">
        <n-space vertical :size="16" style="width: 100%">
          <n-card size="small" title="实时摘要" :bordered="false">
            <n-statistic label="监测点位" :value="filteredItems.length" />
            <n-statistic label="在线节点" :value="onlineCount" style="margin-top: 12px" />
            <n-statistic label="高拥堵点位" :value="highCongestionCount" style="margin-top: 12px" />
          </n-card>

          <n-card size="small" title="点位明细" :bordered="false">
            <n-empty v-if="!filteredItems.length && !loading" description="暂无地图数据" />
            <div v-else class="map-list">
              <button
                v-for="item in filteredItems"
                :key="`${item.camera_id}-${item.road_name}`"
                type="button"
                class="map-list-item"
                @click="focusMarker(item)"
              >
                <div class="map-list-head">
                  <span>{{ item.road_name || item.name || '未命名节点' }}</span>
                  <n-tag size="small" :type="statusTag(item.density_status)">
                    {{ densityLabel(item.density_status) }}
                  </n-tag>
                </div>
                <div class="map-list-meta">
                  车流 {{ item.count_car + item.count_motor }} / 在线 {{ item.online ? '是' : '否' }}
                </div>
                <div class="map-list-meta">
                  拥堵指数 {{ item.congestion_index.toFixed(2) }}
                </div>
              </button>
            </div>
          </n-card>

          <n-card v-if="selectedItem" size="small" title="点位详情" :bordered="false">
            <div class="detail-grid">
              <div>道路：{{ selectedItem.road_name || selectedItem.name || '未命名' }}</div>
              <div>节点：{{ selectedItem.edge_node_id || '未配置' }}</div>
              <div>车辆：{{ selectedItem.count_car + selectedItem.count_motor }}</div>
              <div>行人：{{ selectedItem.count_person }}</div>
              <div>拥堵：{{ densityLabel(selectedItem.density_status) }}</div>
              <div>更新时间：{{ formatDateTime(selectedItem.updated_at) }}</div>
            </div>
            <img
              v-if="selectedSnapshot"
              :src="selectedSnapshot"
              :alt="selectedItem.road_name"
              class="detail-image"
            />
          </n-card>
        </n-space>
      </n-grid-item>
    </n-grid>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { NAlert, NButton, NCard, NEmpty, NGrid, NGridItem, NSpace, NStatistic, NTag, useMessage } from 'naive-ui'
import { AMAP_KEY, authFetch, endpoints } from '../lib/api'
import { ensureAmap } from '../lib/amap'
import {
  normalizeMapOverviewPoint,
  normalizeSiteSettings,
  type MapOverviewPoint,
  type SiteSettings,
} from '../lib/normalize'

const message = useMessage()
const loading = ref(false)
const mapError = ref('')
const items = ref<MapOverviewPoint[]>([])
const selectedItem = ref<MapOverviewPoint | null>(null)
const onlineOnly = ref(false)
const mapContainerRef = ref<HTMLDivElement | null>(null)
const siteSettings = ref<SiteSettings>(normalizeSiteSettings({}))

let mapInstance: any = null
let infoWindow: any = null
let heatmapLayer: any = null
const markerMap = new Map<string, any>()

const filteredItems = computed(() =>
  onlineOnly.value ? items.value.filter((item) => item.online) : items.value,
)
const mappableItems = computed(() =>
  filteredItems.value.filter((item) => Number.isFinite(item.latitude) && Number.isFinite(item.longitude) && item.latitude !== 0 && item.longitude !== 0),
)
const missingCoordinateCount = computed(
  () => filteredItems.value.length - mappableItems.value.length,
)
const onlineCount = computed(() => filteredItems.value.filter((item) => item.online).length)
const highCongestionCount = computed(
  () => filteredItems.value.filter((item) => item.congestion_index >= 0.7).length,
)
const effectiveAmapKey = computed(() => {
  const runtimeKey = siteSettings.value.amap_key.trim()
  return runtimeKey || String(AMAP_KEY || '').trim()
})
const amapStatusText = computed(() => {
  if (!effectiveAmapKey.value) {
    return '缺少高德地图 Key'
  }
  return siteSettings.value.amap_key.trim() ? 'AMap 已由后台配置' : 'AMap 使用部署 fallback'
})
const selectedSnapshot = computed(() => {
  if (!selectedItem.value) return ''
  return (
    selectedItem.value.snapshot_url ||
    endpoints.frameNoAuth(selectedItem.value.road_name || selectedItem.value.name)
  )
})

const statusTag = (densityStatus: string) => {
  if (densityStatus === 'congested') return 'error'
  if (densityStatus === 'busy') return 'warning'
  return 'success'
}

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
  if (!item.online) return '#64748b'
  if (item.congestion_index >= 0.7) return '#dc2626'
  if (item.congestion_index >= 0.4) return '#f59e0b'
  return '#16a34a'
}

const markerHtml = (item: MapOverviewPoint) => `
  <div style="
    width: 18px;
    height: 18px;
    border-radius: 999px;
    background: ${markerColor(item)};
    border: 3px solid rgba(255,255,255,0.92);
    box-shadow: 0 10px 28px rgba(15,23,42,0.24);
  "></div>
`

const infoHtml = (item: MapOverviewPoint) => `
  <div style="min-width:240px;padding:4px 2px;line-height:1.8">
    <div style="font-weight:700;font-size:15px">${item.road_name || item.name || '未命名'}</div>
    <div>节点：${item.edge_node_id || '未配置'}</div>
    <div>在线：${item.online ? '是' : '否'} / 拥堵：${densityLabel(item.density_status)}</div>
    <div>车辆：${item.count_car + item.count_motor} / 行人：${item.count_person}</div>
    <div>速度：${item.speed_car.toFixed(1)} km/h</div>
    <div>更新时间：${formatDateTime(item.updated_at)}</div>
    ${
      (item.snapshot_url || endpoints.frameNoAuth(item.road_name || item.name))
        ? `<img src="${item.snapshot_url || endpoints.frameNoAuth(item.road_name || item.name)}" alt="${
            item.road_name || item.name
          }" style="width:100%;margin-top:8px;border-radius:10px" />`
        : ''
    }
  </div>
`

const destroyMap = () => {
  markerMap.clear()
  if (heatmapLayer) {
    heatmapLayer.setMap?.(null)
    heatmapLayer = null
  }
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
  infoWindow = null
}

const renderMap = async () => {
  if (!mapContainerRef.value || !mappableItems.value.length || !effectiveAmapKey.value) {
    destroyMap()
    return
  }
  try {
    const AMap = await ensureAmap(effectiveAmapKey.value)
    if (!AMap || !mapContainerRef.value) return

    if (!mapInstance) {
      mapInstance = new AMap.Map(mapContainerRef.value, {
        viewMode: '3D',
        zoom: 12,
        center: [mappableItems.value[0].longitude, mappableItems.value[0].latitude],
        mapStyle: 'amap://styles/light',
      })
      mapInstance.addControl(new AMap.Scale())
      mapInstance.addControl(new AMap.ToolBar({ position: 'RB' }))
      infoWindow = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -18) })
    }

    mapInstance.clearMap()
    markerMap.clear()

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
          selectedItem.value = item
          infoWindow.setContent(infoHtml(item))
          infoWindow.open(mapInstance, marker.getPosition())
        })
        markerMap.set(item.road_name || item.name, marker)
        return marker
      })

    if (markers.length) {
      mapInstance.add(markers)
      mapInstance.setFitView(markers, false, [64, 64, 64, 64])
    }

    mapInstance.plugin(['AMap.HeatMap'], () => {
      if (heatmapLayer) {
        heatmapLayer.setMap(null)
      }
      heatmapLayer = new AMap.HeatMap(mapInstance, {
        radius: 35,
        opacity: [0.15, 0.82],
        gradient: {
          0.2: '#38bdf8',
          0.45: '#facc15',
          0.7: '#fb7185',
          1.0: '#b91c1c',
        },
      })
      heatmapLayer.setDataSet({ data: heatPoints, max: 100 })
    })
  } catch (error) {
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
    selectedItem.value = items.value[0] || null
    await nextTick()
    await renderMap()
  } catch (error) {
    mapError.value = error instanceof Error ? error.message : '加载地图数据失败'
    message.error(mapError.value)
  } finally {
    loading.value = false
  }
}

const focusMarker = (item: MapOverviewPoint) => {
  selectedItem.value = item
  const marker = markerMap.get(item.road_name || item.name)
  if (marker && mapInstance && infoWindow) {
    mapInstance.setCenter(marker.getPosition())
    infoWindow.setContent(infoHtml(item))
    infoWindow.open(mapInstance, marker.getPosition())
  }
}

watch([filteredItems, mappableItems, effectiveAmapKey], async ([value, drawable, key]) => {
  if (!value.length || !drawable.length || !key) {
    destroyMap()
    return
  }
  await nextTick()
  await renderMap()
})

onMounted(() => {
  refreshOverview()
})

onUnmounted(() => {
  destroyMap()
})
</script>

<style scoped>
.map-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.map-toolbar {
  border-radius: 18px;
}

.toolbar-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.toolbar-subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.map-card {
  position: relative;
  overflow: hidden;
  min-height: 620px;
}

.map-canvas {
  min-height: 620px;
  width: 100%;
  border-radius: 18px;
  background:
    radial-gradient(circle at top, rgba(56, 189, 248, 0.16), transparent 36%),
    linear-gradient(180deg, #f8fafc 0%, #e2e8f0 100%);
}

.map-overlay {
  position: absolute;
  left: 16px;
  right: 16px;
  top: 16px;
}

.map-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 420px;
  overflow: auto;
}

.map-list-item {
  border: 0;
  text-align: left;
  background: linear-gradient(135deg, #f8fafc 0%, #eef2ff 100%);
  border-radius: 14px;
  padding: 12px 14px;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.map-list-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.map-list-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  font-weight: 600;
  color: #0f172a;
}

.map-list-meta {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  color: #334155;
  font-size: 13px;
}

.detail-image {
  width: 100%;
  margin-top: 14px;
  border-radius: 16px;
  object-fit: cover;
  min-height: 180px;
  background: #e2e8f0;
}

@media (max-width: 768px) {
  .map-card,
  .map-canvas {
    min-height: 420px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
