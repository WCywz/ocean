<template>
  <div v-loading="loading" class="trend-chart-container" ref="chartRef">
    <div v-if="empty" class="trend-chart-empty">暂无趋势数据</div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { buildBaseOption, buildTooltipFormatter } from '../utils/chart-config'

const props = defineProps({
  seriesData: { type: Array, default: () => [] },
  xAxisData: { type: Array, default: () => [] },
  yAxisName: { type: String, default: '' },
  yAxisUnit: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  colors: { type: Array, default: () => ['#3498DB'] }
})

const chartRef = ref(null)
const empty = ref(true)
let chart = null

function render() {
  if (!chart) return
  if (!props.seriesData.length || !props.xAxisData.length) {
    empty.value = true
    chart.clear()
    return
  }
  empty.value = false

  const legendData = props.seriesData.map(s => s.name)
  const base = buildBaseOption({
    legendData,
    xAxisData: props.xAxisData,
    yAxisName: props.yAxisName,
    yAxisUnit: props.yAxisUnit
  })
  base.tooltip.formatter = buildTooltipFormatter(props.yAxisUnit)

  const series = props.seriesData.map((s) => ({
    name: s.name,
    type: 'line',
    smooth: true,
    symbol: 'circle',
    symbolSize: 4,
    lineStyle: { width: 2 },
    data: s.data
  }))

  chart.setOption({ ...base, series, color: props.colors }, true)
}

watch(() => [props.seriesData, props.xAxisData, props.loading], () => {
  nextTick(() => render())
}, { deep: true })

onMounted(() => {
  nextTick(() => {
    chart = echarts.init(chartRef.value)
    render()
    window.addEventListener('resize', () => chart?.resize())
  })
})

onUnmounted(() => {
  chart?.dispose()
})
</script>

<style scoped>
.trend-chart-container {
  width: 100%;
  height: 300px;
}
.trend-chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  font-size: 14px;
}
</style>
