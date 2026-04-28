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
import { ref, computed, onMounted } from 'vue'
import { toast } from 'vue-sonner'
import { RefreshCw } from 'lucide-vue-next'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Select } from '@/components/ui/select'
import { Label } from '@/components/ui/label'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'

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

const alerts = ref<Alert[]>([])
const alertLevelFilter = ref<string>('all')
const nodeFilter = ref<string>('all')
const showDetailDialog = ref(false)
const selectedAlert = ref<Alert | null>(null)

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

function formatTime(timestamp: string): string {
  return new Date(timestamp).toLocaleString('zh-CN')
}

function getLevelVariant(level: string): 'default' | 'secondary' | 'destructive' {
  switch (level) {
    case '严重': return 'destructive'
    case '警告': return 'default'
    default: return 'secondary'
  }
}

async function loadAlerts() {
  // 模拟数据，后续对接后端 API
  alerts.value = [
    {
      id: '1',
      timestamp: new Date(Date.now() - 1000 * 60 * 5).toISOString(),
      level: '严重',
      nodeName: '中关村大街节点',
      type: '设备离线',
      description: '摄像头 CAM-001 已离线超过 5 分钟，请检查网络连接',
      acknowledged: false,
    },
    {
      id: '2',
      timestamp: new Date(Date.now() - 1000 * 60 * 15).toISOString(),
      level: '警告',
      nodeName: '西直门桥节点',
      type: '流量异常',
      description: '当前车流量较历史同期增长 45%，可能存在拥堵风险',
      acknowledged: false,
    },
    {
      id: '3',
      timestamp: new Date(Date.now() - 1000 * 60 * 30).toISOString(),
      level: '信息',
      nodeName: '国贸桥节点',
      type: '系统通知',
      description: '节点已完成固件升级，版本 v2.1.0',
      acknowledged: true,
    },
  ]
  toast.success('告警数据已刷新')
}

function acknowledgeAlert(id: string) {
  const alert = alerts.value.find(a => a.id === id)
  if (alert) {
    alert.acknowledged = true
    toast.success('告警已确认')
  }
}

function acknowledgeAll() {
  alerts.value.forEach(alert => {
    alert.acknowledged = true
  })
  toast.success('所有告警已确认')
}

function viewAlertDetail(alert: Alert) {
  selectedAlert.value = alert
  showDetailDialog.value = true
}

onMounted(() => {
  loadAlerts()
})
</script>