<template>
  <section class="analytics">
    <div class="filters">
      <label>
        粒度
        <select v-model="granularity">
          <option value="hourly">小时</option>
          <option value="daily">日</option>
          <option value="weekly">周</option>
          <option value="monthly">月</option>
        </select>
      </label>
      <label>
        路段
        <select v-model="roadName">
          <option value="">全部</option>
          <option v-for="road in state.roads" :key="road" :value="road">{{ road }}</option>
        </select>
      </label>
      <label>
        开始时间
        <input v-model="startAt" type="datetime-local" />
      </label>
      <label>
        结束时间
        <input v-model="endAt" type="datetime-local" />
      </label>
      <button @click="loadReport">查询</button>
      <button class="secondary" @click="downloadJson">下载 JSON</button>
      <button class="secondary" @click="downloadXlsx">下载 XLSX</button>
    </div>

    <div class="charts">
      <div class="card">
        <h3>路段流量趋势</h3>
        <div ref="lineRef" class="chart"></div>
      </div>
      <div class="card">
        <h3>当前流量占比</h3>
        <div ref="pieRef" class="chart"></div>
      </div>
    </div>

    <div class="card">
      <h3>报表结果</h3>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>bucket_at</th>
              <th>road_name</th>
              <th>total_flow</th>
              <th>avg_congestion_index</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="`${row.bucket_at}_${row.road_name}`">
              <td>{{ row.bucket_at }}</td>
              <td>{{ row.road_name }}</td>
              <td>{{ row.total_flow }}</td>
              <td>{{ row.avg_congestion_index }}</td>
            </tr>
            <tr v-if="rows.length === 0">
              <td colspan="4" class="empty">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { authFetch, endpoints } from '../lib/api'
import { initializeTrafficStore, useTrafficStoreState } from '../store/traffic'

const state = useTrafficStoreState()
const lineRef = ref<HTMLElement | null>(null)
const pieRef = ref<HTMLElement | null>(null)
let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const granularity = ref('hourly')
const roadName = ref('')
const startAt = ref('')
const endAt = ref('')
const rows = ref<Array<Record<string, string | number>>>([])

const latestTotals = computed(() => {
  return state.roads.map((road) => ({
    name: road,
    value: (state.trafficData[road]?.count_car || 0) + (state.trafficData[road]?.count_motor || 0)
  }))
})

const historicalSeries = computed(() => {
  return state.roads.map((road) => ({
    name: road,
    type: 'line',
    smooth: true,
    data: state.historicalData.map((p) => Number(p[`${road}_total`] || 0))
  }))
})

const xAxisData = computed(() => state.historicalData.map((p) => String(p.time)))

const toIsoOrEmpty = (value: string) => {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  return d.toISOString()
}

const buildQuery = (format: 'json' | 'xlsx') => {
  const params = new URLSearchParams()
  params.set('granularity', granularity.value)
  params.set('format', format)
  if (roadName.value) params.set('road_name', roadName.value)
  const start = toIsoOrEmpty(startAt.value)
  const end = toIsoOrEmpty(endAt.value)
  if (start) params.set('start_at', start)
  if (end) params.set('end_at', end)
  return `${endpoints.reportExport}?${params.toString()}`
}

const loadReport = async () => {
  const res = await authFetch(buildQuery('json'), { method: 'GET' })
  if (!res.ok) {
    rows.value = []
    return
  }
  const payload = await res.json()
  rows.value = Array.isArray(payload?.rows) ? payload.rows : []
}

const downloadJson = async () => {
  await loadReport()
  const blob = new Blob([JSON.stringify(rows.value, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `traffic_report_${granularity.value}.json`
  a.click()
  URL.revokeObjectURL(url)
}

const downloadXlsx = async () => {
  const res = await authFetch(buildQuery('xlsx'), {
    method: 'GET',
    headers: { Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }
  })
  if (!res.ok) return
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `traffic_report_${granularity.value}.xlsx`
  a.click()
  URL.revokeObjectURL(url)
}

const renderCharts = () => {
  if (lineRef.value) {
    lineChart = lineChart || echarts.init(lineRef.value)
    lineChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { top: 4 },
      xAxis: { type: 'category', data: xAxisData.value },
      yAxis: { type: 'value' },
      series: historicalSeries.value
    })
  }

  if (pieRef.value) {
    pieChart = pieChart || echarts.init(pieRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          data: latestTotals.value
        }
      ]
    })
  }
}

onMounted(async () => {
  await initializeTrafficStore()
  await nextTick()
  renderCharts()
  await loadReport()
})

watch([historicalSeries, latestTotals], () => {
  renderCharts()
})

onUnmounted(() => {
  lineChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.analytics {
  display: grid;
  gap: 14px;
}

.filters {
  border-radius: 12px;
  background: #fff;
  padding: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: flex-end;
  box-shadow: 0 8px 24px rgba(2, 6, 23, 0.08);
}

label {
  display: grid;
  gap: 6px;
  font-size: 13px;
}

select,
input {
  height: 36px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 10px;
}

button {
  height: 36px;
  border: 0;
  border-radius: 8px;
  background: #0284c7;
  color: #fff;
  padding: 0 12px;
  cursor: pointer;
}

button.secondary {
  background: #475569;
}

.charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.card {
  border-radius: 12px;
  background: #fff;
  padding: 12px;
  box-shadow: 0 8px 24px rgba(2, 6, 23, 0.08);
}

.chart {
  width: 100%;
  height: 320px;
}

.table-wrap {
  overflow: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  border-bottom: 1px solid #e2e8f0;
  padding: 8px;
  text-align: left;
  white-space: nowrap;
}

.empty {
  text-align: center;
  color: #64748b;
}

@media (max-width: 960px) {
  .charts {
    grid-template-columns: 1fr;
  }
}
</style>
