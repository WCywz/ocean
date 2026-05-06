<template>
  <el-card shadow="hover" class="trend-card">
    <template #header>
      <div class="card-header">
        <span class="card-title">{{ title }}</span>
        <el-tag size="small" type="info">最近 7 天</el-tag>
      </div>
    </template>
    <div v-if="!series.length && !loading" class="empty-state">暂无趋势数据</div>
    <div v-loading="loading" class="chart-wrapper" ref="chartRef"></div>
  </el-card>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { SST_COLORS, CHL_COLORS, buildBaseOption, buildSeriesData } from '../../utils/chart-config'

const props = defineProps({
  title: { type: String, default: '' },
  dataType: { type: String, default: 'SST' },
  series: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const chartRef = ref(null)
let chart = null

function renderChart() {
  if (!chart || !props.series.length) return
  const colors = props.dataType === 'SST' ? SST_COLORS : CHL_COLORS
  const unit = props.dataType === 'SST' ? '°C' : 'mg/m³'
  const xAxisData = props.series[0]?.dataPoints?.map(d => d.date) || []
  const legendData = props.series.map(s => s.locationName)
  const seriesMap = {}
  props.series.forEach(s => {
    seriesMap[s.locationName] = s.dataPoints.map(d => d.value)
  })

  const base = buildBaseOption({
    legendData,
    xAxisData,
    yAxisName: props.dataType === 'SST' ? '温度 (°C)' : '浓度 (mg/m³)',
    yAxisUnit: unit
  })

  const chartSeries = buildSeriesData(seriesMap, colors, { area: true, markLine: false })

  chart.setOption({ ...base, series: chartSeries }, true)
}

function handleResize() {
  chart?.resize()
}

watch(() => props.series, () => nextTick(() => renderChart()), { deep: true })

onMounted(() => {
  nextTick(() => {
    chart = echarts.init(chartRef.value)
    window.addEventListener('resize', handleResize)
    renderChart()
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.trend-card { height: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: 600; color: #1a3a5c; }
.chart-wrapper { width: 100%; height: 280px; }
.empty-state {
  height: 280px; display: flex; align-items: center; justify-content: center;
  color: #bbb; font-size: 14px;
}
</style>
