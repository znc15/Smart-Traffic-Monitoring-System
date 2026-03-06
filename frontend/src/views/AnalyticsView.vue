<template>
  <section class="analytics">
    <!-- 筛选面板 -->
    <n-card size="small">
      <n-space align="center" :wrap="true" :size="[16, 12]">
        <n-select
          v-model:value="granularity"
          :options="granularityOptions"
          placeholder="粒度"
          style="width: 120px"
        />
        <n-select
          v-model:value="roadName"
          :options="roadOptions"
          placeholder="路段"
          clearable
          style="width: 160px"
        />
        <n-date-picker
          v-model:value="dateRange"
          type="datetimerange"
          clearable
          start-placeholder="开始时间"
          end-placeholder="结束时间"
        />
        <n-button type="primary" @click="loadReport">
          <template #icon>
            <n-icon><search-outline /></n-icon>
          </template>
          查询
        </n-button>
        <n-button-group>
          <n-button @click="downloadJson">
            <template #icon>
              <n-icon><download-outline /></n-icon>
            </template>
            JSON
          </n-button>
          <n-button @click="downloadXlsx">
            <template #icon>
              <n-icon><download-outline /></n-icon>
            </template>
            XLSX
          </n-button>
        </n-button-group>
      </n-space>
    </n-card>

    <!-- 图表区域 -->
    <n-grid :cols="24" :x-gap="14" :y-gap="14" responsive="screen" item-responsive>
      <n-grid-item span="24 m:12">
        <n-card title="路段流量趋势" size="small">
          <div ref="lineRef" class="chart" />
        </n-card>
      </n-grid-item>
      <n-grid-item span="24 m:12">
        <n-card title="当前流量占比" size="small">
          <div ref="pieRef" class="chart" />
        </n-card>
      </n-grid-item>
    </n-grid>

    <!-- 数据表格 -->
    <n-card title="报表结果" size="small">
      <n-data-table
        :columns="columns"
        :data="rows"
        :pagination="pagination"
        :loading="tableLoading"
        :bordered="false"
        striped
      />
    </n-card>
  </section>
</template>

<script setup lang="ts">
import { computed, h, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import {
  NCard, NSpace, NSelect, NDatePicker, NButton, NButtonGroup,
  NIcon, NGrid, NGridItem, NDataTable,
  useMessage,
  type DataTableColumns, type PaginationProps
} from 'naive-ui'
import * as echarts from 'echarts'
import { SearchOutline, DownloadOutline } from '@vicons/ionicons5'
import { authFetch, endpoints } from '../lib/api'
import { initializeTrafficStore, useTrafficStoreState } from '../store/traffic'

const message = useMessage()
const state = useTrafficStoreState()
const lineRef = ref<HTMLElement | null>(null)
const pieRef = ref<HTMLElement | null>(null)
let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const granularity = ref('hourly')
const roadName = ref<string | null>(null)
const dateRange = ref<[number, number] | null>(null)
const rows = ref<Array<Record<string, string | number>>>([])
const tableLoading = ref(false)

// 筛选选项
const granularityOptions = [
  { label: '小时', value: 'hourly' },
  { label: '日', value: 'daily' },
  { label: '周', value: 'weekly' },
  { label: '月', value: 'monthly' }
]

const roadOptions = computed(() =>
  state.roads.map((road) => ({ label: road, value: road }))
)

// 表格列定义
const columns: DataTableColumns = [
  { title: '时间', key: 'bucket_at', sorter: 'default' },
  { title: '道路', key: 'road_name', sorter: 'default' },
  {
    title: '总流量',
    key: 'total_flow',
    sorter: (a: any, b: any) => Number(a.total_flow) - Number(b.total_flow)
  },
  {
    title: '拥堵指数',
    key: 'avg_congestion_index',
    sorter: (a: any, b: any) =>
      Number(a.avg_congestion_index) - Number(b.avg_congestion_index),
    render(row: any) {
      const val = Number(row.avg_congestion_index)
      return h(
        'span',
        { style: { color: val > 0.7 ? '#d03050' : val > 0.4 ? '#f0a020' : '#18a058' } },
        val.toFixed(2)
      )
    }
  }
]

// 分页配置
const pagination: PaginationProps = {
  pageSize: 15,
  showSizePicker: true,
  pageSizes: [10, 15, 30, 50],
  prefix: ({ itemCount }: { itemCount: number | undefined }) =>
    `共 ${itemCount ?? 0} 条`
}

// 图表数据 — 实时
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
        { offset: 0, color: 'rgba(32,128,240,0.25)' },
        { offset: 1, color: 'rgba(32,128,240,0.02)' }
      ])
    },
    data: state.historicalData.map((p) => Number(p[`${road}_total`] || 0))
  }))
)

const xAxisData = computed(() =>
  state.historicalData.map((p) => String(p.time))
)

// 查询构建
const toIsoOrEmpty = (ts: number | undefined) => {
  if (!ts) return ''
  const d = new Date(ts)
  if (Number.isNaN(d.getTime())) return ''
  return d.toISOString()
}

const buildQuery = (format: 'json' | 'xlsx') => {
  const params = new URLSearchParams()
  params.set('granularity', granularity.value)
  params.set('format', format)
  if (roadName.value) params.set('road_name', roadName.value)
  if (dateRange.value) {
    const start = toIsoOrEmpty(dateRange.value[0])
    const end = toIsoOrEmpty(dateRange.value[1])
    if (start) params.set('start_at', start)
    if (end) params.set('end_at', end)
  }
  return `${endpoints.reportExport}?${params.toString()}`
}

// 数据加载
const loadReport = async () => {
  tableLoading.value = true
  try {
    const res = await authFetch(buildQuery('json'), { method: 'GET' })
    if (!res.ok) {
      message.error('查询报表失败')
      rows.value = []
      return
    }
    const payload = await res.json()
    rows.value = Array.isArray(payload?.rows) ? payload.rows : []
  } catch (e) {
    message.error(e instanceof DOMException && e.name === 'AbortError' ? '请求超时' : '查询报表出错')
    rows.value = []
  } finally {
    tableLoading.value = false
  }
}

// 导出 JSON
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
    message.error('导出 JSON 失败')
  }
}

// 导出 XLSX
const downloadXlsx = async () => {
  try {
    const res = await authFetch(buildQuery('xlsx'), {
      method: 'GET',
      headers: {
        Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      }
    })
    if (!res.ok) {
      message.error('导出 XLSX 失败')
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
    message.error('导出 XLSX 出错')
  }
}

// ECharts 商务配色
const chartColors = ['#2080f0', '#18a058', '#f0a020', '#d03050', '#8a2be2']

const renderCharts = () => {
  if (lineRef.value) {
    lineChart = lineChart || echarts.init(lineRef.value)
    lineChart.setOption({
      color: chartColors,
      tooltip: { trigger: 'axis' },
      legend: { top: 4, textStyle: { color: '#6b7280' } },
      grid: { left: 48, right: 24, bottom: 32, top: 48 },
      xAxis: {
        type: 'category',
        data: xAxisData.value,
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisLabel: { color: '#6b7280' }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#f0f0f0' } },
        axisLabel: { color: '#6b7280' }
      },
      series: historicalSeries.value
    })
  }

  if (pieRef.value) {
    pieChart = pieChart || echarts.init(pieRef.value)
    pieChart.setOption({
      color: chartColors,
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          label: { color: '#6b7280' },
          data: latestTotals.value
        }
      ]
    })
  }
}

// 窗口 resize
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

watch([historicalSeries, latestTotals], () => {
  renderCharts()
})

onUnmounted(() => {
  lineChart?.dispose()
  pieChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.analytics {
  display: grid;
  gap: 14px;
}

.chart {
  width: 100%;
  height: 320px;
}
</style>
