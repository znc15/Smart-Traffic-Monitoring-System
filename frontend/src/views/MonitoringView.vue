<template>
  <div class="space-y-6">
    <Card>
      <CardContent class="p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold">节点监控汇聚</h3>
          <div class="flex items-center gap-3">
            <div class="w-48">
              <Select v-model="nodeSortKey" :options="nodeSortOptions" placeholder="排序" />
            </div>
            <Button @click="loadNodes">
              <RefreshCw class="w-4 h-4 mr-2" /> 刷新
            </Button>
          </div>
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-8"></TableHead>
              <TableHead>名称</TableHead>
              <TableHead>道路</TableHead>
              <TableHead>节点 URL</TableHead>
              <TableHead>延迟</TableHead>
              <TableHead>CPU</TableHead>
              <TableHead>内存</TableHead>
              <TableHead>状态</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <template v-for="node in sortedNodes" :key="node.road_name">
              <TableRow
                class="cursor-pointer hover:bg-muted/50"
                @click="toggleNodeExpand(node.road_name)"
              >
                <TableCell class="w-8">
                  <ChevronRight class="h-4 w-4 transition-transform" :class="expandedNodes.has(node.road_name) ? 'rotate-90' : ''" />
                </TableCell>
                <TableCell class="font-medium">{{ node.name || '-' }}</TableCell>
                <TableCell>{{ node.road_name }}</TableCell>
                <TableCell class="max-w-[200px] truncate" :title="node.node_url || '-'">{{ node.node_url || '-' }}</TableCell>
                <TableCell>{{ formatLatency(node) }}</TableCell>
                <TableCell>
                  <span :class="metricColor(node.cpu_usage)">{{ formatMetric(node.cpu_usage) }}</span>
                </TableCell>
                <TableCell>
                  <span :class="metricColor(node.memory_usage)">{{ formatMetric(node.memory_usage) }}</span>
                </TableCell>
                <TableCell>
                  <Badge :variant="node.health_status === 'online' ? 'default' : node.health_status === 'degraded' ? 'secondary' : 'destructive'">
                    {{ node.health_status === 'online' ? '在线' : node.health_status === 'degraded' ? '降级' : '离线' }}
                  </Badge>
                </TableCell>
              </TableRow>
              <TableRow v-if="expandedNodes.has(node.road_name)">
                <TableCell colspan="8" class="bg-muted/30 p-4">
                  <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 text-sm">
                    <div class="space-y-1">
                      <p class="text-muted-foreground font-medium">基本信息</p>
                      <p>摄像头 ID: {{ node.camera_id ?? '-' }}</p>
                      <p>名称: {{ node.name || '-' }}</p>
                      <p>道路: {{ node.road_name }}</p>
                      <p>边缘节点 ID: {{ node.edge_node_id || '-' }}</p>
                      <p class="max-w-[300px] truncate" :title="node.node_url">节点 URL: {{ node.node_url || '-' }}</p>
                    </div>
                    <div class="space-y-1">
                      <p class="text-muted-foreground font-medium">状态信息</p>
                      <p>健康状态: <Badge :variant="node.health_status === 'online' ? 'default' : 'destructive'" class="ml-1">{{ node.health_status }}</Badge></p>
                      <p>原因码: {{ node.status_reason_code || '-' }}</p>
                      <p>原因描述: {{ node.status_reason_message || '-' }}</p>
                      <p>最后错误阶段: {{ node.last_error_stage || '-' }}</p>
                      <p class="max-w-[300px] truncate text-destructive" :title="node.last_error ?? undefined">最后错误: {{ node.last_error || '-' }}</p>
                    </div>
                    <div class="space-y-1">
                      <p class="text-muted-foreground font-medium">性能指标</p>
                      <p>延迟: {{ formatLatency(node) }}</p>
                      <p>CPU 使用率: <span :class="metricColor(node.cpu_usage)">{{ formatMetric(node.cpu_usage) }}</span></p>
                      <p>内存使用率: <span :class="metricColor(node.memory_usage)">{{ formatMetric(node.memory_usage) }}</span></p>
                      <template v-if="node.disk_usage != null">
                        <p>磁盘使用率: <span :class="metricColor(node.disk_usage)">{{ formatMetric(node.disk_usage) }}</span></p>
                      </template>
                      <template v-if="node.uptime_seconds != null">
                        <p>运行时间: {{ formatUptime(node.uptime_seconds) }}</p>
                      </template>
                    </div>
                    <div class="space-y-1">
                      <p class="text-muted-foreground font-medium">时间与错误统计</p>
                      <p>最近轮询: {{ formatTime(node.last_poll_time) }}</p>
                      <p>最近成功: {{ formatTime(node.last_success_time) }}</p>
                      <p>错误次数: {{ node.error_count ?? 0 }}</p>
                      <p>连续失败: {{ node.consecutive_failures ?? 0 }}</p>
                    </div>
                  </div>
                </TableCell>
              </TableRow>
            </template>
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import { toast } from 'vue-sonner'
import { RefreshCw, ChevronRight } from 'lucide-vue-next'
import { authFetch, endpoints } from '../lib/api'
import { normalizeAdminNodeHealth, type AdminNodeHealth } from '../lib/normalize'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Select } from '@/components/ui/select'

type MonitoringNode = AdminNodeHealth & {
  node_id: string
  last_heartbeat: string
}

type MonitoringSortKey =
  | 'last_heartbeat'
  | 'cpu_usage'
  | 'memory_usage'
  | 'latency_ms'
  | 'error_count'

const nodes = ref<MonitoringNode[]>([])

const nodeSortKey = ref<MonitoringSortKey>('last_heartbeat')
const nodeSortOptions = [
  { label: '心跳时间', value: 'last_heartbeat' },
  { label: 'CPU使用率', value: 'cpu_usage' },
  { label: '内存使用率', value: 'memory_usage' },
  { label: '延迟', value: 'latency_ms' },
  { label: '错误次数', value: 'error_count' },
]

const expandedNodes = reactive(new Set<string>())

function toggleNodeExpand(roadName: string) {
  if (expandedNodes.has(roadName)) {
    expandedNodes.delete(roadName)
  } else {
    expandedNodes.add(roadName)
  }
}

function formatMetric(val: number | null | undefined): string {
  if (typeof val !== 'number') return '-'
  return val.toFixed(1) + '%'
}

function formatLatency(node: Pick<MonitoringNode, 'health_status' | 'latency_ms'>): string {
  if (node.health_status === 'offline' || typeof node.latency_ms !== 'number') return '-'
  return node.latency_ms + ' ms'
}

function metricColor(val: number | null | undefined): string {
  if (typeof val !== 'number') return ''
  if (val > 90) return 'text-destructive font-medium'
  if (val > 70) return 'text-warning font-medium'
  return 'text-success font-medium'
}

function formatTime(val: string | null | undefined): string {
  if (!val) return '-'
  try {
    return new Date(val).toLocaleString('zh-CN')
  } catch {
    return val
  }
}

function formatUptime(seconds: number): string {
  if (seconds < 60) return seconds + ' 秒'
  if (seconds < 3600) return Math.floor(seconds / 60) + ' 分钟'
  if (seconds < 86400) return Math.floor(seconds / 3600) + ' 小时'
  return Math.floor(seconds / 86400) + ' 天'
}

const sortedNodes = computed(() => {
  return [...nodes.value].sort((a, b) => {
    if (nodeSortKey.value === 'last_heartbeat') {
      return new Date(b.last_heartbeat).getTime() - new Date(a.last_heartbeat).getTime()
    }
    return Number(b[nodeSortKey.value] ?? 0) - Number(a[nodeSortKey.value] ?? 0)
  })
})

onMounted(() => {
  loadNodes()
})

async function loadNodes() {
  try {
    const res = await authFetch(endpoints.adminNodes)
    if (res.ok) {
      const data = await res.json()
      const rawNodes = data.nodes ? Object.values(data.nodes) : (Array.isArray(data) ? data : [])
      nodes.value = rawNodes.map((raw): MonitoringNode => {
        const node = normalizeAdminNodeHealth(raw)
        return {
          ...node,
          node_id: node.edge_node_id || String(node.camera_id || '') || node.name || node.road_name || '未知',
          last_heartbeat: node.last_poll_time || node.last_success_time || new Date().toISOString(),
        }
      })
    }
  } catch (error) {
    console.error('加载节点监控失败', error)
    toast.error('加载节点监控失败')
  }
}
</script>
