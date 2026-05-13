<template>
  <div class="trend-card-wrapper" style="cursor: pointer;" @click.stop="$emit('navigate')">
    <p class="editorial-section-label">Feature · 趋势分析</p>
    <h3 class="editorial-section-heading" style="display: flex; justify-content: space-between; align-items: baseline;">
      <span>{{ title }}</span>
      <span class="trend-nav-hint">{{ dataType === 'SST' ? 'SST 预测' : 'CHL 预测' }} →</span>
    </h3>
    <p v-if="pointInfo" class="trend-point-info">{{ pointInfo }}</p>
    <p class="editorial-narrative">{{ narrativeText }}</p>
    <div v-if="!series.length && !loading" class="editorial-narrative">暂无趋势数据</div>
    <div style="display: flex; align-items: stretch; width: 100%; height: 280px;">
      <div style="display: flex; align-items: center; justify-content: center; writing-mode: vertical-lr; text-orientation: mixed; font-size: 13px; color: #666; padding: 0 8px; white-space: nowrap; flex-shrink: 0;">{{ yAxisLabel }}</div>
      <div v-loading="loading" style="flex: 1; height: 100%; min-width: 0;" ref="chartRef"></div>
    </div>
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

defineEmits(['navigate'])

const pointInfo = computed(() => {
  const s = props.series[0]
  if (!s || s.longitude == null || s.latitude == null) return ''
  return `观测点: ${s.locationName} · 经度 ${Number(s.longitude).toFixed(2)}°E · 纬度 ${Number(s.latitude).toFixed(2)}°N`
})

const narrativeText = computed(() => {
  if (!props.series.length) return ''
  const s = props.series[0]
  const loc = s.locationName || '该海域'
  if (props.dataType === 'SST') {
    const vals = s.dataPoints?.map(d => d.value) || []
    if (!vals.length) return ''
    const avg = (vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(1)
    const trend = vals[vals.length - 1] > vals[0] ? '上升' : '下降'
    return `未来 ${vals.length} 天${loc}海表温度预测呈${trend}趋势，均值 ${avg}°C。`
  }
  const vals = s.dataPoints?.map(d => d.value) || []
  if (!vals.length) return ''
  const avg = (vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(1)
  return `未来 ${vals.length} 天${loc}叶绿素浓度预测均值 ${avg} mg/m³。`
})

const yAxisLabel = computed(() => props.dataType === 'SST' ? '温度 (°C)' : '浓度 (mg/m³)')

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
    yAxisName: '',
    yAxisUnit: unit
  })
  base.dataZoom = [{ type: 'inside' }]
  base.grid.left = 50
  base.grid.bottom = 30

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
.trend-nav-hint {
  font-size: 11px;
  color: var(--color-text-muted);
  font-weight: 400;
  font-family: var(--font-sans);
  white-space: nowrap;
}

.trend-card-wrapper {
  transition: opacity 0.15s;
}
.trend-card-wrapper:hover {
  opacity: 0.85;
}

.trend-point-info {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin: 0 0 4px 0;
}
</style>
