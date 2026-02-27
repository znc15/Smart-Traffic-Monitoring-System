<template>
  <section>
    <h2>分析报表（ECharts）</h2>
    <div ref="lineRef" class="chart"></div>
    <div ref="pieRef" class="chart"></div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'
import * as echarts from 'echarts'

const lineRef = ref<HTMLElement | null>(null)
const pieRef = ref<HTMLElement | null>(null)
let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

onMounted(() => {
  if (lineRef.value) {
    lineChart = echarts.init(lineRef.value)
    lineChart.setOption({
      xAxis: { type: 'category', data: ['08:00', '09:00', '10:00', '11:00'] },
      yAxis: { type: 'value' },
      series: [{ type: 'line', data: [120, 160, 150, 180], smooth: true }]
    })
  }

  if (pieRef.value) {
    pieChart = echarts.init(pieRef.value)
    pieChart.setOption({
      series: [
        {
          type: 'pie',
          radius: '60%',
          data: [
            { name: '汽车', value: 62 },
            { name: '非机动车', value: 28 },
            { name: '行人', value: 10 }
          ]
        }
      ]
    })
  }
})

onBeforeUnmount(() => {
  lineChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.chart {
  width: 100%;
  height: 320px;
  margin: 16px 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 22px rgba(2, 6, 23, 0.08);
}
</style>
