<template>
  <section class="dashboard">
    <div v-if="announcement" class="announcement">{{ announcement }}</div>

    <div class="content">
      <div class="video-panel">
        <h3>实时画面</h3>
        <div class="video-box">
          <img v-if="selectedRoad" :src="frameUrl(selectedRoad)" :alt="selectedRoad" />
          <div v-else class="empty">暂无路段</div>
        </div>
        <p v-if="selectedRoad" class="sub">当前路段：{{ selectedRoad }}</p>
      </div>

      <div class="list-panel">
        <h3>交通状态</h3>
        <div v-if="!state.roads.length" class="empty">暂无道路数据</div>
        <button
          v-for="road in state.roads"
          :key="road"
          class="road-item"
          :class="{ active: road === selectedRoad }"
          @click="selectedRoad = road"
        >
          <div class="top">
            <strong>{{ road }}</strong>
            <span class="tag" :class="statusClass(state.trafficData[road]?.density_status)">
              {{ statusLabel(state.trafficData[road]?.density_status) }}
            </span>
          </div>
          <div class="meta">
            <span>汽车 {{ state.trafficData[road]?.count_car ?? 0 }}</span>
            <span>非机动车 {{ state.trafficData[road]?.count_motor ?? 0 }}</span>
            <span>行人 {{ state.trafficData[road]?.count_person ?? 0 }}</span>
          </div>
          <div class="meta">
            <span>车速 {{ state.trafficData[road]?.speed_car ?? 0 }} km/h</span>
            <span>摩托 {{ state.trafficData[road]?.speed_motor ?? 0 }} km/h</span>
          </div>
        </button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { endpoints } from '../lib/api'
import { normalizeSiteSettings } from '../lib/normalize'
import { initializeTrafficStore, useTrafficStoreState } from '../store/traffic'

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

const statusClass = (status?: string) => {
  if (status === 'congested') return 'danger'
  if (status === 'busy') return 'warn'
  if (status === 'clear') return 'ok'
  return 'normal'
}

onMounted(async () => {
  await initializeTrafficStore()
  selectedRoad.value = state.roads[0] || null

  try {
    const res = await fetch(endpoints.siteSettings)
    if (res.ok) {
      const payload = await res.json()
      announcement.value = normalizeSiteSettings(payload).announcement
    }
  } catch {
    // ignore
  }

  timer = window.setInterval(() => {
    frameTick.value += 1
  }, 1200)
})

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<style scoped>
.dashboard {
  display: grid;
  gap: 14px;
}

.announcement {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  border-radius: 10px;
  padding: 10px 12px;
}

.content {
  display: grid;
  gap: 14px;
  grid-template-columns: 2fr 1fr;
}

.video-panel,
.list-panel {
  border-radius: 12px;
  background: #fff;
  padding: 14px;
  box-shadow: 0 8px 24px rgba(2, 6, 23, 0.08);
}

.video-box {
  margin-top: 8px;
  aspect-ratio: 16 / 9;
  border-radius: 10px;
  background: #111827;
  overflow: hidden;
}

.video-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sub {
  margin-top: 8px;
  color: #64748b;
}

.road-item {
  width: 100%;
  margin-top: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  padding: 10px;
  text-align: left;
  cursor: pointer;
}

.road-item.active {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 2px rgba(14, 165, 233, 0.15);
}

.top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tag {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 999px;
}

.tag.ok {
  background: #dcfce7;
  color: #15803d;
}

.tag.warn {
  background: #fef3c7;
  color: #b45309;
}

.tag.danger {
  background: #fee2e2;
  color: #b91c1c;
}

.tag.normal {
  background: #e2e8f0;
  color: #475569;
}

.meta {
  margin-top: 6px;
  color: #334155;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 13px;
}

.empty {
  color: #64748b;
  padding: 14px 0;
}

@media (max-width: 960px) {
  .content {
    grid-template-columns: 1fr;
  }
}
</style>
