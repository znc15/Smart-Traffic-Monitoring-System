<template>
  <div class="space-y-6">
    <Card>
      <CardContent class="p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold">异常事件告警</h3>
          <div class="flex items-center gap-3">
            <Select v-model="alertLevelFilter" :options="alertLevelOptions" placeholder="告警级别" class="w-32" />
            <Select v-model="nodeFilter" :options="nodeOptions" placeholder="节点筛选" class="w-40" />
            <Button @click="loadAlerts">
              <RefreshCw class="w-4 h-4 mr-2" /> 刷新
            </Button>
          </div>
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>时间</TableHead>
              <TableHead>级别</TableHead>
              <TableHead>节点</TableHead>
              <TableHead>类型</TableHead>
              <TableHead>描述</TableHead>
              <TableHead>状态</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="filteredAlerts.length === 0">
              <TableCell colspan="7" class="text-center text-muted-foreground py-8">
                暂无告警数据
              </TableCell>
            </TableRow>
            <TableRow v-for="alert in filteredAlerts" :key="alert.id">
              <TableCell class="text-sm">{{ formatTime(alert.timestamp) }}</TableCell>
              <TableCell>
                <Badge :variant="getLevelVariant(alert.level)">
                  {{ alert.level }}
                </Badge>
              </TableCell>
              <TableCell>{{ alert.nodeName || '-' }}</TableCell>
              <TableCell>{{ alert.type }}</TableCell>
              <TableCell class="max-w-[300px] truncate" :title="alert.description">
                {{ alert.description }}
              </TableCell>
              <TableCell>
                <Badge :variant="alert.acknowledged ? 'secondary' : 'default'">
                  {{ alert.acknowledged ? '已确认' : '待处理' }}
                </Badge>
              </TableCell>
              <TableCell>
                <div class="flex items-center gap-2">
                  <Button
                    v-if="!alert.acknowledged"
                    variant="ghost"
                    size="sm"
                    @click="acknowledgeAlert(alert.id)"
                  >
                    确认
                  </Button>
                  <Button variant="ghost" size="sm" @click="viewAlertDetail(alert)">
                    详情
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>

        <div v-if="filteredAlerts.length > 0" class="flex items-center justify-between mt-4">
          <div class="text-sm text-muted-foreground">
            共 {{ filteredAlerts.length }} 条告警
          </div>
          <div class="flex items-center gap-2">
            <Button variant="outline" size="sm" @click="acknowledgeAll">
              全部确认
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- 告警详情弹窗 -->
    <Dialog :open="showDetailDialog" @update:open="showDetailDialog = $event">
      <DialogContent class="max-w-lg">
        <DialogHeader>
          <DialogTitle>告警详情</DialogTitle>
        </DialogHeader>
        <div v-if="selectedAlert" class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <Label class="text-muted-foreground">时间</Label>
              <p class="text-sm">{{ formatTime(selectedAlert.timestamp) }}</p>
            </div>
            <div>
              <Label class="text-muted-foreground">级别</Label>
              <p><Badge :variant="getLevelVariant(selectedAlert.level)">{{ selectedAlert.level }}</Badge></p>
            </div>
            <div>
              <Label class="text-muted-foreground">节点</Label>
              <p class="text-sm">{{ selectedAlert.nodeName || '-' }}</p>
            </div>
            <div>
              <Label class="text-muted-foreground">类型</Label>
              <p class="text-sm">{{ selectedAlert.type }}</p>
            </div>
          </div>
          <div>
            <Label class="text-muted-foreground">描述</Label>
            <p class="text-sm mt-1">{{ selectedAlert.description }}</p>
          </div>
          <div v-if="selectedAlert.metadata">
            <Label class="text-muted-foreground">附加信息</Label>
            <pre class="text-xs bg-muted p-3 rounded mt-1 overflow-x-auto">{{ JSON.stringify(selectedAlert.metadata, null, 2) }}</pre>
          </div>
        </div>
        <DialogFooter>
          <Button @click="showDetailDialog = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { toast } from 'vue-sonner'
import { RefreshCw } from 'lucide-vue-next'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Select } from '@/components/ui/select'
import { Label } from '@/components/ui/label'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { authFetch, endpoints, getWsUrl } from '@/lib/api'
import { connectAlertsWs } from '@/lib/alerts-ws'

interface Alert {
  id: string
  timestamp: string
  level: '严重' | '警告' | '信息'
  nodeName: string
  type: string
  description: string
  acknowledged: boolean
  metadata?: Record<string, unknown>
}

interface BackendAlert {
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

const LEVEL_MAP: Record<string, '严重' | '警告' | '信息'> = {
  CRITICAL: '严重',
  WARNING: '警告',
  INFO: '信息',
}

const alerts = ref<Alert[]>([])
const loading = ref(false)
const alertLevelFilter = ref<string>('all')
const nodeFilter = ref<string>('all')
const showDetailDialog = ref(false)
const selectedAlert = ref<Alert | null>(null)
let alertsWsClient: ReturnType<typeof connectAlertsWs> | null = null

const alertLevelOptions = [
  { label: '全部级别', value: 'all' },
  { label: '严重', value: '严重' },
  { label: '警告', value: '警告' },
  { label: '信息', value: '信息' },
]

const nodeOptions = computed(() => {
  const nodes = new Set(alerts.value.map(a => a.nodeName).filter(Boolean))
  return [
    { label: '全部节点', value: 'all' },
    ...Array.from(nodes).map(name => ({ label: name, value: name })),
  ]
})

const filteredAlerts = computed(() => {
  return alerts.value.filter(alert => {
    if (alertLevelFilter.value !== 'all' && alert.level !== alertLevelFilter.value) return false
    if (nodeFilter.value !== 'all' && alert.nodeName !== nodeFilter.value) return false
    return true
  })
})

function mapAlert(raw: BackendAlert): Alert {
  const createdAt = raw.createdAt ?? raw.created_at ?? ''
  const roadName = raw.roadName ?? raw.road_name ?? ''
  return {
    id: String(raw.id),
    timestamp: createdAt,
    level: LEVEL_MAP[raw.level] ?? '信息',
    nodeName: roadName || '-',
    type: raw.type,
    description: raw.message,
    acknowledged: raw.status !== 'UNCONFIRMED',
  }
}

function formatTime(timestamp: string): string {
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) {
    return timestamp || '—'
  }
  return date.toLocaleString('zh-CN')
}

function getLevelVariant(level: string): 'default' | 'secondary' | 'destructive' {
  switch (level) {
    case '严重': return 'destructive'
    case '警告': return 'default'
    default: return 'secondary'
  }
}

async function loadAlerts(showToast = true) {
  loading.value = true
  try {
    const res = await authFetch(endpoints.alerts)
    if (!res.ok) throw new Error('加载失败')
    const data: BackendAlert[] = await res.json()
    alerts.value = data.map(mapAlert)
    if (showToast) {
      toast.success('告警数据已刷新')
    }
  } catch {
    toast.error('加载告警数据失败')
  } finally {
    loading.value = false
  }
}

function handleIncomingAlert(payload: unknown) {
  const raw = payload as Partial<BackendAlert>
  if (typeof raw?.id !== 'number') return
  const mapped = mapAlert(raw as BackendAlert)
  const idx = alerts.value.findIndex((item) => item.id === String(raw.id))
  if (idx >= 0) {
    alerts.value[idx] = mapped
  } else {
    alerts.value = [mapped, ...alerts.value]
    toast.warning('收到新告警')
  }
}

async function acknowledgeAlert(id: string) {
  try {
    const res = await authFetch(endpoints.alertStatus(Number(id)), {
      method: 'PUT',
      body: JSON.stringify({ status: 'CONFIRMED' }),
    })
    if (!res.ok) throw new Error()
    const alert = alerts.value.find(a => a.id === id)
    if (alert) alert.acknowledged = true
    toast.success('告警已确认')
  } catch {
    toast.error('确认告警失败')
  }
}

async function acknowledgeAll() {
  const unacknowledged = filteredAlerts.value.filter(a => !a.acknowledged)
  if (unacknowledged.length === 0) {
    toast.info('没有待处理的告警')
    return
  }
  try {
    await Promise.all(
      unacknowledged.map(a =>
        authFetch(endpoints.alertStatus(Number(a.id)), {
          method: 'PUT',
          body: JSON.stringify({ status: 'CONFIRMED' }),
        }).then(res => { if (!res.ok) throw new Error() })
      )
    )
    unacknowledged.forEach(a => { a.acknowledged = true })
    toast.success('所有告警已确认')
  } catch {
    toast.error('批量确认失败，请重试')
  }
}

function viewAlertDetail(alert: Alert) {
  selectedAlert.value = alert
  showDetailDialog.value = true
}

onMounted(() => {
  loadAlerts(false)
  alertsWsClient = connectAlertsWs({
    url: getWsUrl(endpoints.alertsWs),
    onAlert: handleIncomingAlert,
  })
})

onUnmounted(() => {
  alertsWsClient?.close()
  alertsWsClient = null
})
</script>
