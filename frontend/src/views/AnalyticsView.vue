<template>
  <div class="space-y-6">
    <!-- 筛选面板 -->
    <Card>
      <CardContent class="p-4">
        <div class="flex flex-wrap items-center gap-4">
          <div class="w-32">
            <Select v-model="granularity" :options="granularityOptions" placeholder="粒度" />
          </div>
          <div class="w-40">
            <Select v-model="roadName" :options="roadOptions" placeholder="全部路段" />
          </div>
          <div class="flex items-center gap-2">
            <Input type="datetime-local" v-model="startDate" class="w-48" />
            <span class="text-muted-foreground">-</span>
            <Input type="datetime-local" v-model="endDate" class="w-48" />
          </div>
          <Button @click="loadReport" class="ml-auto sm:ml-0">
            <Search class="w-4 h-4 mr-2" />
            查询
          </Button>
          <div class="flex gap-2">
            <Button variant="outline" @click="downloadJson">
              <Download class="w-4 h-4 mr-2" />
              JSON
            </Button>
            <Button variant="outline" @click="downloadXlsx">
              <Download class="w-4 h-4 mr-2" />
              XLSX
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- 图表区域 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <Card>
        <CardHeader class="pb-2">
          <CardTitle>路段流量趋势</CardTitle>
        </CardHeader>
        <CardContent>
          <div ref="lineRef" class="w-full h-80" />
        </CardContent>
      </Card>
      <Card>
        <CardHeader class="pb-2">
          <CardTitle>当前流量占比</CardTitle>
        </CardHeader>
        <CardContent>
          <div ref="pieRef" class="w-full h-80" />
        </CardContent>
      </Card>
    </div>

    <!-- 数据表格 -->
    <Card>
      <CardHeader class="pb-2">
        <CardTitle>报表结果</CardTitle>
      </CardHeader>
      <CardContent>
        <div v-if="tableLoading" class="py-10 text-center text-muted-foreground">
          加载中...
        </div>
        <Table v-else>
          <TableHeader>
            <TableRow>
              <TableHead>时间</TableHead>
              <TableHead>道路</TableHead>
              <TableHead>总流量</TableHead>
              <TableHead>拥堵指数</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="!rows.length">
              <TableCell colspan="4" class="text-center text-muted-foreground py-10">暂无数据</TableCell>
            </TableRow>
            <TableRow v-for="(row, idx) in paginatedRows" :key="idx">
              <TableCell>{{ row.bucket_at }}</TableCell>
              <TableCell>{{ row.road_name }}</TableCell>
              <TableCell>{{ row.total_flow }}</TableCell>
              <TableCell>
                <span :class="congestionColor(Number(row.avg_congestion_index))">
                  {{ Number(row.avg_congestion_index).toFixed(2) }}
                </span>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>

        <!-- 简易分页 -->
        <div v-if="rows.length > 0" class="flex items-center justify-between mt-4">
          <div class="text-sm text-muted-foreground">
            共 {{ rows.length }} 条
          </div>
          <div class="flex items-center gap-2">
            <Button variant="outline" size="sm" :disabled="currentPage === 1" @click="currentPage--">上一页</Button>
            <span class="text-sm">第 {{ currentPage }} 页 / 共 {{ Math.ceil(rows.length / pageSize) }} 页</span>
            <Button variant="outline" size="sm" :disabled="currentPage >= Math.ceil(rows.length / pageSize)" @click="currentPage++">下一页</Button>
          </div>
        </div>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { toast } from 'vue-sonner'
import { useDark } from '@vueuse/core'
import { Search, Download } from 'lucide-vue-next'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import * as echarts from 'echarts/core'

use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])
import { authFetch, endpoints } from '../lib/api'
import { initializeTrafficStore, useTrafficStoreState } from '../store/traffic'

const isDark = useDark()
const state = useTrafficStoreState()
const lineRef = ref<HTMLElement | null>(null)
const pieRef = ref<HTMLElement | null>(null)
let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const granularity = ref('hourly')
const roadName = ref<string>('')
const startDate = ref<string>('')
const endDate = ref<string>('')

const rows = ref<Array<Record<string, string | number>>>([])
const tableLoading = ref(false)

const currentPage = ref(1)
const pageSize = 15

const paginatedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return rows.value.slice(start, start + pageSize)
})

const granularityOptions = [
  { label: '小时', value: 'hourly' },
  { label: '日', value: 'daily' },
  { label: '周', value: 'weekly' },
  { label: '月', value: 'monthly' }
]

const roadOptions = computed(() => {
  const opts = state.roads.map((road) => ({ label: road, value: road }))
  return [{ label: '全部路段', value: '' }, ...opts]
})

const congestionColor = (val: number) => {
  if (val > 0.7) return 'text-destructive font-medium'
  if (val > 0.4) return 'text-warning font-medium'
  return 'text-success font-medium'
}

const latestTotals = computed(() =>
  state.roads.map((road) => ({
    name: road,
    value:
      (state.trafficData[road]?.count_car || 0) +
      (state.trafficData[road]?.count_motor || 0)
  }))
)

const historicalSeries = computed(() =>
  state.roads.map((road) => ({
    name: road,
    type: 'line' as const,
    smooth: true,
    areaStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(59, 130, 246, 0.25)' },
        { offset: 1, color: 'rgba(59, 130, 246, 0.02)' }
      ])
    },
    data: state.historicalData.map((p) => Number(p[`${road}_total`] || 0))
  }))
)

const xAxisData = computed(() =>
  state.historicalData.map((p) => String(p.time))
)

const toIsoOrEmpty = (val: string) => {
  if (!val) return ''
  const d = new Date(val)
  if (Number.isNaN(d.getTime())) return ''
  return d.toISOString()
}

const buildQuery = (format: 'json' | 'xlsx') => {
  const params = new URLSearchParams()
  params.set('granularity', granularity.value)
  params.set('format', format)
  if (roadName.value) params.set('road_name', roadName.value)
  
  const startIso = toIsoOrEmpty(startDate.value)
  const endIso = toIsoOrEmpty(endDate.value)
  if (startIso) params.set('start_at', startIso)
  if (endIso) params.set('end_at', endIso)
  
  return `${endpoints.reportExport}?${params.toString()}`
}

const loadReport = async () => {
  tableLoading.value = true
  currentPage.value = 1
  try {
    const res = await authFetch(buildQuery('json'), { method: 'GET' })
    if (!res.ok) {
      toast.error('查询报表失败')
      rows.value = []
      return
    }
    const payload = await res.json()
    rows.value = Array.isArray(payload?.rows) ? payload.rows : []
  } catch (e) {
    toast.error(e instanceof DOMException && e.name === 'AbortError' ? '请求超时' : '查询报表出错')
    rows.value = []
  } finally {
    tableLoading.value = false
  }
}

const downloadJson = async () => {
  try {
    await loadReport()
    const blob = new Blob(
      [JSON.stringify(rows.value, null, 2)],
      { type: 'application/json;charset=utf-8' }
    )
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `traffic_report_${granularity.value}.json`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    toast.error('导出 JSON 失败')
  }
}

const downloadXlsx = async () => {
  try {
    const res = await authFetch(buildQuery('xlsx'), {
      method: 'GET',
      headers: {
        Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      }
    })
    if (!res.ok) {
      toast.error('导出 XLSX 失败')
      return
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `traffic_report_${granularity.value}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    toast.error('导出 XLSX 出错')
  }
}

const chartColors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6']

const renderCharts = () => {
  const textColor = isDark.value ? '#94a3b8' : '#64748b'
  const splitLineColor = isDark.value ? '#334155' : '#e2e8f0'

  if (lineRef.value) {
    lineChart = lineChart || echarts.init(lineRef.value)
    lineChart.setOption({
      color: chartColors,
      tooltip: { trigger: 'axis', backgroundColor: isDark.value ? '#1e293b' : '#fff', textStyle: { color: isDark.value ? '#f8fafc' : '#0f172a' } },
      legend: { top: 4, textStyle: { color: textColor } },
      grid: { left: 48, right: 24, bottom: 32, top: 48 },
      xAxis: {
        type: 'category',
        data: xAxisData.value,
        axisLine: { lineStyle: { color: splitLineColor } },
        axisLabel: { color: textColor }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: splitLineColor } },
        axisLabel: { color: textColor }
      },
      series: historicalSeries.value
    })
  }

  if (pieRef.value) {
    pieChart = pieChart || echarts.init(pieRef.value)
    pieChart.setOption({
      color: chartColors,
      tooltip: { trigger: 'item', backgroundColor: isDark.value ? '#1e293b' : '#fff', textStyle: { color: isDark.value ? '#f8fafc' : '#0f172a' } },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          itemStyle: {
            borderColor: isDark.value ? '#0f172a' : '#fff',
            borderWidth: 2
          },
          label: { color: textColor },
          data: latestTotals.value
        }
      ]
    })
  }
}

const handleResize = () => {
  lineChart?.resize()
  pieChart?.resize()
}

onMounted(async () => {
  await initializeTrafficStore()
  await nextTick()
  renderCharts()
  await loadReport()
  window.addEventListener('resize', handleResize)
})

watch([historicalSeries, latestTotals, isDark], () => {
  renderCharts()
})

onUnmounted(() => {
  lineChart?.dispose()
  pieChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>
