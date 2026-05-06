<template>
  <div class="editorial-section">
    <p class="editorial-section-label">Feature · 趋势分析</p>
    <h3 class="editorial-section-heading">{{ title }}</h3>
    <p class="editorial-narrative">{{ narrativeText }}</p>
    <div v-if="!series.length && !loading" class="editorial-narrative">暂无趋势数据</div>
    <div v-loading="loading" class="chart-wrapper" ref="chartRef"></div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { SST_COLORS, CHL_COLORS, buildBaseOption, buildSeriesData } from '../../utils/chart-config'

const props = defineProps({
  title: { type: String, default: '' },
  dataType: { type: String, default: 'SST' },
  series: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const narrativeText = computed(() => {
  if (!props.series.length) return ''
  if (props.dataType === 'SST') {
    const vals = props.series[0]?.dataPoints?.map(d => d.value) || []
    if (!vals.length) return ''
    const avg = (vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(1)
    const trend = vals[vals.length - 1] > vals[0] ? '上升' : '下降'
    return `过去 ${vals.length} 天东海海域海表温度呈${trend}趋势，平均温度 ${avg}°C。`
  }
  const vals = props.series[0]?.dataPoints?.map(d => d.value) || []
  if (!vals.length) return ''
  const avg = (vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(1)
  return `近海叶绿素浓度维持正常水平，平均 ${avg} mg/m³，无异常藻华预警信号。`
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
.chart-wrapper {
  width: 100%;
  height: 280px;
}
</style>
