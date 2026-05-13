<template>
  <div style="display: flex; align-items: stretch; width: 100%; height: 300px;">
    <div style="display: flex; align-items: center; justify-content: center; writing-mode: vertical-lr; text-orientation: mixed; font-size: 13px; color: #666; padding: 0 8px; white-space: nowrap; flex-shrink: 0;">{{ yAxisLabel }}</div>
    <div v-loading="loading" style="flex: 1; height: 100%; min-width: 0; position: relative;" ref="chartRef">
      <div v-show="empty" class="trend-chart-empty">点击地图上的网格以查看趋势</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
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

const yAxisLabel = computed(() => props.yAxisName || '')

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
    yAxisName: '',
    yAxisUnit: props.yAxisUnit
  })
  base.dataZoom = [{ type: 'inside' }]
  base.grid.left = 50
  base.grid.bottom = 30
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
.trend-chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 14px;
  pointer-events: none;
}
</style>
