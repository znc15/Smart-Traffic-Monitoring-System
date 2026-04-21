<template>
  <div class="space-y-6">
    <!-- 顶部公告栏 -->
    <Alert v-if="announcement" variant="default" class="bg-blue-50/50 dark:bg-blue-950/20 border-blue-200 dark:border-blue-900">
      <Info class="h-4 w-4 text-blue-600 dark:text-blue-400" />
      <AlertDescription class="text-blue-800 dark:text-blue-300 ml-2">
        {{ announcement }}
      </AlertDescription>
    </Alert>

    <!-- 加载骨架屏 -->
    <template v-if="!state.initialized">
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>实时监控</CardTitle>
            </CardHeader>
            <CardContent>
              <Skeleton class="w-full aspect-video rounded-xl" />
              <Skeleton class="h-4 w-1/3 mt-4" />
            </CardContent>
          </Card>
        </div>
        <div>
          <Card>
            <CardHeader>
              <CardTitle>道路状态</CardTitle>
            </CardHeader>
            <CardContent class="space-y-4">
              <Skeleton class="h-20 w-full" v-for="i in 4" :key="i" />
            </CardContent>
          </Card>
        </div>
      </div>
    </template>

    <!-- 主体内容 -->
    <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 左侧：视频流区域 -->
      <div class="lg:col-span-2 space-y-6">
        <Card class="overflow-hidden">
          <CardHeader class="border-b bg-muted/30">
            <CardTitle>实时监控</CardTitle>
          </CardHeader>
          <CardContent class="p-6">
            <div class="aspect-video bg-black rounded-xl overflow-hidden shadow-inner relative flex items-center justify-center">
              <img
                v-if="selectedRoad"
                :src="frameUrl(selectedRoad)"
                :alt="selectedRoad"
                class="w-full h-full object-cover"
              />
              <div v-else class="text-muted-foreground flex flex-col items-center">
                <VideoOff class="h-10 w-10 mb-2 opacity-50" />
                <span>暂无路段</span>
              </div>
            </div>
            <p v-if="selectedRoad" class="text-sm text-muted-foreground mt-4 flex items-center">
              <MapPin class="h-4 w-4 mr-1.5" />
              当前路段：<span class="font-medium text-foreground">{{ selectedRoad }}</span>
            </p>
          </CardContent>
        </Card>
      </div>

      <!-- 右侧：道路状态列表 -->
      <div class="space-y-6">
        <Card class="h-full flex flex-col">
          <CardHeader class="border-b bg-muted/30 shrink-0">
            <CardTitle>道路状态</CardTitle>
          </CardHeader>
          <CardContent class="p-4 flex-1 overflow-y-auto">
            <div v-if="!state.roads.length" class="h-40 flex items-center justify-center text-muted-foreground">
              暂无道路数据
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="road in state.roads"
                :key="road"
                class="group p-4 rounded-xl border transition-all cursor-pointer hover:border-primary/50 hover:shadow-md"
                :class="road === selectedRoad ? 'border-primary bg-primary/5 ring-1 ring-primary/20' : 'bg-card'"
                @click="selectedRoad = road"
              >
                <div class="flex items-center justify-between mb-3">
                  <h4 class="font-semibold text-base">{{ road }}</h4>
                  <Badge :variant="tagVariant(state.trafficData[road]?.density_status)">
                    {{ statusLabel(state.trafficData[road]?.density_status) }}
                  </Badge>
                </div>
                
                <div class="grid grid-cols-3 gap-2 mb-3">
                  <div class="flex flex-col">
                    <span class="text-xs text-muted-foreground mb-1">汽车</span>
                    <span class="text-lg font-bold font-mono">{{ state.trafficData[road]?.count_car ?? 0 }}</span>
                  </div>
                  <div class="flex flex-col">
                    <span class="text-xs text-muted-foreground mb-1">非机动车</span>
                    <span class="text-lg font-bold font-mono">{{ state.trafficData[road]?.count_motor ?? 0 }}</span>
                  </div>
                  <div class="flex flex-col">
                    <span class="text-xs text-muted-foreground mb-1">行人</span>
                    <span class="text-lg font-bold font-mono">{{ state.trafficData[road]?.count_person ?? 0 }}</span>
                  </div>
                </div>

                <div class="flex gap-4 text-xs text-muted-foreground bg-muted/50 rounded-md p-2">
                  <span class="flex items-center"><Gauge class="h-3 w-3 mr-1" />车速 {{ state.trafficData[road]?.speed_car ?? 0 }} km/h</span>
                  <span class="flex items-center"><Gauge class="h-3 w-3 mr-1" />摩托 {{ state.trafficData[road]?.speed_motor ?? 0 }} km/h</span>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { toast } from 'vue-sonner'
import { Info, VideoOff, MapPin, Gauge } from 'lucide-vue-next'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import { endpoints } from '../lib/api'
import { normalizeSiteSettings } from '../lib/normalize'
import { initializeTrafficStore, refreshRoads, useTrafficStoreState } from '../store/traffic'

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

const tagVariant = (status?: string): 'destructive' | 'warning' | 'default' | 'secondary' => {
  if (status === 'congested') return 'destructive'
  if (status === 'busy') return 'warning'
  if (status === 'clear') return 'default'
  return 'secondary'
}

async function doRefreshRoads() {
  try {
    const latestRoads = await refreshRoads()
    if (selectedRoad.value === null || !latestRoads.includes(selectedRoad.value)) {
      selectedRoad.value = latestRoads[0] || null
    }
  } catch {
    toast.warning('刷新道路列表失败')
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

  await doRefreshRoads()

  try {
    const res = await fetch(endpoints.siteSettings)
    if (res.ok) {
      const payload = await res.json()
      announcement.value = normalizeSiteSettings(payload).announcement
    }
  } catch {
    toast.warning('获取站点公告失败')
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
