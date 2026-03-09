<template>
  <section class="dashboard">
    <!-- 顶部公告栏 -->
    <n-alert v-if="announcement" type="info" closable>
      {{ announcement }}
    </n-alert>

    <!-- 加载骨架屏 -->
    <template v-if="!state.initialized">
      <n-grid :cols="24" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
        <n-grid-item span="24 m:16">
          <n-card title="实时监控" size="small">
            <n-skeleton height="360px" />
            <n-skeleton text style="margin-top: 12px; width: 40%" />
          </n-card>
        </n-grid-item>
        <n-grid-item span="24 m:8">
          <n-card title="道路状态" size="small">
            <n-skeleton text :repeat="8" />
          </n-card>
        </n-grid-item>
      </n-grid>
    </template>

    <!-- 主体内容 -->
    <n-grid v-else :cols="24" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <!-- 左侧：视频流区域 -->
      <n-grid-item span="24 m:16">
        <n-card title="实时监控" size="small" :segmented="{ content: true }">
          <div class="video-box">
            <img
              v-if="selectedRoad"
              :src="frameUrl(selectedRoad)"
              :alt="selectedRoad"
            />
            <n-empty v-else description="暂无路段" class="video-empty" />
          </div>
          <n-text v-if="selectedRoad" depth="3" style="margin-top: 10px; display: block">
            当前路段：{{ selectedRoad }}
          </n-text>
        </n-card>
      </n-grid-item>

      <!-- 右侧：道路状态列表 -->
      <n-grid-item span="24 m:8">
        <n-card title="道路状态" size="small" :segmented="{ content: true }">
          <n-empty v-if="!state.roads.length" description="暂无道路数据" />
          <div v-else class="road-list">
            <n-card
              v-for="road in state.roads"
              :key="road"
              size="small"
              hoverable
              class="road-item"
              :class="{ 'road-item--active': road === selectedRoad }"
              @click="selectedRoad = road"
            >
              <div class="road-header">
                <n-text strong>{{ road }}</n-text>
                <n-tag
                  :type="tagType(state.trafficData[road]?.density_status)"
                  size="small"
                  round
                >
                  {{ statusLabel(state.trafficData[road]?.density_status) }}
                </n-tag>
              </div>
              <div class="road-stats">
                <n-statistic label="汽车" tabular-nums>
                  <n-number-animation
                    :from="0"
                    :to="state.trafficData[road]?.count_car ?? 0"
                    :duration="600"
                  />
                </n-statistic>
                <n-statistic label="非机动车" tabular-nums>
                  <n-number-animation
                    :from="0"
                    :to="state.trafficData[road]?.count_motor ?? 0"
                    :duration="600"
                  />
                </n-statistic>
                <n-statistic label="行人" tabular-nums>
                  <n-number-animation
                    :from="0"
                    :to="state.trafficData[road]?.count_person ?? 0"
                    :duration="600"
                  />
                </n-statistic>
              </div>
              <div class="road-speed">
                <n-text depth="3">
                  车速 {{ state.trafficData[road]?.speed_car ?? 0 }} km/h
                </n-text>
                <n-text depth="3">
                  摩托 {{ state.trafficData[road]?.speed_motor ?? 0 }} km/h
                </n-text>
              </div>
            </n-card>
          </div>
        </n-card>
      </n-grid-item>
    </n-grid>
  </section>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useMessage } from 'naive-ui'
import { endpoints } from '../lib/api'
import { normalizeSiteSettings } from '../lib/normalize'
import { initializeTrafficStore, refreshRoads, useTrafficStoreState } from '../store/traffic'

const message = useMessage()
const state = useTrafficStoreState()
const selectedRoad = ref<string | null>(null)
const announcement = ref('')
const frameTick = ref(0)
let timer: number | null = null

const frameUrl = (road: string) => `${endpoints.frameNoAuth(road)}?t=${frameTick.value}`

const statusLabel = (status?: string) => {
  if (status === 'congested') return '拥堵'
  if (status === 'busy') return '繁忙'
  if (status === 'clear') return '畅通'
  if (status === 'offline') return '离线'
  return '未知'
}

const tagType = (status?: string): 'error' | 'warning' | 'success' | 'default' => {
  if (status === 'congested') return 'error'
  if (status === 'busy') return 'warning'
  if (status === 'clear') return 'success'
  return 'default'
}

async function doRefreshRoads() {
  try {
    const latestRoads = await refreshRoads()
    if (selectedRoad.value === null || !latestRoads.includes(selectedRoad.value)) {
      selectedRoad.value = latestRoads[0] || null
    }
  } catch {
    message.warning('刷新道路列表失败')
  }
}

function handleVisibilityChange() {
  if (document.visibilityState === 'visible') {
    doRefreshRoads()
  }
}

onMounted(async () => {
  await initializeTrafficStore()
  selectedRoad.value = state.roads[0] || null

  // Refresh road list to pick up any new cameras added since last load
  await doRefreshRoads()

  try {
    const res = await fetch(endpoints.siteSettings)
    if (res.ok) {
      const payload = await res.json()
      announcement.value = normalizeSiteSettings(payload).announcement
    }
  } catch {
    message.warning('获取站点公告失败')
  }

  document.addEventListener('visibilitychange', handleVisibilityChange)

  timer = window.setInterval(() => {
    frameTick.value += 1
  }, 1200)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<style scoped>
.dashboard {
  display: grid;
  gap: 16px;
}

.video-box {
  aspect-ratio: 16 / 9;
  border-radius: 8px;
  background: #111827;
  overflow: hidden;
}

.video-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.road-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.road-item {
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.road-item--active {
  border-color: #2080f0;
  box-shadow: 0 0 0 2px rgba(32, 128, 240, 0.15);
}

.road-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.road-stats {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.road-stats :deep(.n-statistic .n-statistic-value) {
  font-size: 20px;
}

.road-stats :deep(.n-statistic .n-statistic__label) {
  font-size: 12px;
}

.road-speed {
  margin-top: 8px;
  display: flex;
  gap: 16px;
  font-size: 13px;
}
</style>
