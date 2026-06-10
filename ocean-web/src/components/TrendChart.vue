<template>
  <div style="display: flex; align-items: stretch; width: 100%; height: 300px;">
    <div style="display: flex; align-items: center; justify-content: center; writing-mode: vertical-lr; text-orientation: mixed; font-size: 13px; color: var(--color-text-secondary); padding: 0 8px; white-space: nowrap; flex-shrink: 0;">{{ yAxisLabel }}</div>
    <div v-loading="loading" style="flex: 1; height: 100%; min-width: 0; position: relative;" ref="chartRef">
      <div v-show="empty" class="trend-chart-empty">点击地图上的网格以查看趋势</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { buildBaseOption, buildTooltipFormatter, getMapColor } from '../utils/chart-config'
import { useTheme } from '../composables/useTheme'

const props = defineProps({
  seriesData: { type: Array, default: () => [] },
  xAxisData: { type: Array, default: () => [] },
  yAxisName: { type: String, default: '' },
  yAxisUnit: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  colors: { type: Array, default: () => ['#3498DB'] },
  /** 'line' | 'bar' */
  type: { type: String, default: 'line' },
  /** 柱状图模式下的色阶映射 [{min,max,color}]，与地图色阶一致 */
  colorRanges: { type: Array, default: () => [] }
})

const { resolved: themeResolved } = useTheme()

const chartRef = ref(null)
const empty = ref(true)
let chart = null

const isDark = computed(() => themeResolved.value === 'dark')
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
  const dark = isDark.value
  const base = buildBaseOption({
    legendData,
    xAxisData: props.xAxisData,
    yAxisName: '',
    yAxisUnit: props.yAxisUnit,
    dark
  })
  base.dataZoom = [{ type: 'inside' }]
  base.grid.left = 50
  base.grid.bottom = 30
  base.tooltip.formatter = buildTooltipFormatter(props.yAxisUnit, {}, dark)

  const isBar = props.type === 'bar'

  /** 将 hex 颜色加深指定比例（0~1），用于柱状图渐变 */
  function darken(hex, factor) {
    const r = parseInt(hex.slice(1, 3), 16)
    const g = parseInt(hex.slice(3, 5), 16)
    const b = parseInt(hex.slice(5, 7), 16)
    const f = 1 - factor
    return '#' + [r, g, b]
      .map(v => Math.max(0, Math.round(v * f)).toString(16).padStart(2, '0'))
      .join('')
  }

  const series = props.seriesData.map((s, idx) => {
    const c = props.colors[idx % props.colors.length]

    if (isBar) {
      // 柱状图：上浅下深渐变 + 数值标签
      const ranges = props.colorRanges.length > 0 ? props.colorRanges : null
      const barData = s.data.map(v => {
        const base = ranges ? getMapColor(v, ranges) : c
        return {
          value: v,
          itemStyle: {
            borderRadius: [3, 3, 0, 0],
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0,   color: base },
                { offset: 0.4, color: darken(base, 0.08) },
                { offset: 1,   color: darken(base, 0.25) }
              ]
            }
          }
        }
      })
      return {
        name: s.name,
        type: 'bar',
        barWidth: '35%',
        emphasis: { itemStyle: { color: c } },
        label: {
          show: true,
          position: 'top',
          fontSize: 12,
          color: dark ? '#c9d1d9' : '#555',
          fontFamily: 'Georgia, serif',
          formatter: `{c} ${props.yAxisUnit || ''}`
        },
        data: barData
      }
    }

    // 折线图（默认）
    return {
      name: s.name,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      lineStyle: { width: 2, color: c },
      itemStyle: { color: c },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: c + '33' },
            { offset: 1, color: c + '05' }
          ]
        }
      },
      data: s.data
    }
  })

  chart.setOption({ ...base, series, color: props.colors }, true)
}

watch(() => [props.seriesData, props.xAxisData, props.loading], () => {
  nextTick(() => render())
}, { deep: true })

watch(isDark, () => {
  if (chart) {
    chart.dispose()
    chart = echarts.init(chartRef.value, isDark.value ? 'ocean-dark' : undefined)
    render()
  }
})

onMounted(() => {
  nextTick(() => {
    chart = echarts.init(chartRef.value, isDark.value ? 'ocean-dark' : undefined)
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
  color: var(--color-text-muted);
  font-size: 14px;
  pointer-events: none;
}
</style>
