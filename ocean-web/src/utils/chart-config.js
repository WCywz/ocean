// ocean-web/src/utils/chart-config.js

export const SST_COLORS = [
  '#E74C3C', '#F39C12', '#E67E22', '#3498DB', '#1A5276',
  '#D35400', '#C0392B', '#2E86C1', '#154360', '#F5B041'
]

export const CHL_COLORS = [
  '#27AE60', '#2ECC71', '#1ABC9C', '#16A085', '#0D6E6E',
  '#229954', '#148F77', '#117A65', '#0B5345', '#1E8449'
]

export const OCEAN_CHART_COLORS = SST_COLORS

/**
 * Build base ECharts option with ocean-theme defaults.
 */
export function buildBaseOption({ legendData = [], xAxisData = [], yAxisName, yAxisUnit } = {}) {
  const seriesCount = legendData.length
  const useRightLegend = seriesCount >= 2 && seriesCount <= 8

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#e0e0e0',
      borderWidth: 1,
      textStyle: { fontSize: 13, color: '#333' },
      confine: true,
      extraCssText: 'box-shadow: 0 2px 12px rgba(0,0,0,0.1); border-radius: 6px;'
    },
    legend: {
      data: legendData,
      type: useRightLegend ? 'plain' : 'scroll',
      ...(seriesCount === 1 ? { show: false } : {}),
      ...(useRightLegend
        ? { orient: 'vertical', right: 0, top: 10, itemGap: 8, textStyle: { fontSize: 12 } }
        : { orient: 'horizontal', bottom: 0, textStyle: { fontSize: 12 }, pageTextStyle: { fontSize: 11 } }
      )
    },
    grid: {
      left: 80,
      right: useRightLegend ? 180 : 50,
      top: 20,
      bottom: useRightLegend ? 55 : 80
    },
    dataZoom: useRightLegend
      ? [{ type: 'inside' }]
      : [
          { type: 'slider', bottom: 35, height: 22, textStyle: { fontSize: 11 } },
          { type: 'inside' }
        ],
    xAxis: {
      type: 'category',
      data: xAxisData,
      axisLabel: {
        rotate: 0,
        fontSize: 11,
        color: '#666',
        formatter: (value) => {
          if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) {
            const parts = value.split('-')
            return parts[0] + '\n' + parts[1] + '-' + parts[2]
          }
          return value
        }
      },
      axisLine: { lineStyle: { color: '#ccc' } },
      axisTick: { lineStyle: { color: '#ccc' } }
    },
    yAxis: {
      type: 'value',
      name: yAxisName || '',
      nameTextStyle: { fontSize: 13, color: '#666' },
      axisLabel: {
        fontSize: 12,
        ...(yAxisUnit ? { formatter: `{value} ${yAxisUnit}` } : {})
      },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    }
  }
}

/**
 * Build a tooltip formatter that shows location names, sorts by value desc.
 */
export function buildTooltipFormatter(unit, locationMap = {}) {
  return (params) => {
    if (!params || params.length === 0) return ''
    const sorted = [...params].sort((a, b) => (b.value ?? 0) - (a.value ?? 0))
    let html = `<b style="font-size:14px;color:#1a3a5c">${sorted[0].axisValue}</b><br/>`
    sorted.forEach(p => {
      const key = p.seriesName
      const label = locationMap[key] || key
      html += `<span style="display:inline-block;margin-right:6px;border-radius:50%;width:8px;height:8px;background:${p.color}"></span>`
      html += ` ${label}: <b>${p.value} ${unit}</b><br/>`
    })
    return html
  }
}

/**
 * Build ECharts series entries from a seriesMap produced by buildLocationMap.
 */
export function buildSeriesData(seriesMap, colors, { area, markLine } = {}) {
  return Object.entries(seriesMap).map(([name, values], idx) => {
    const color = colors[idx % colors.length]
    return {
      name,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 0,
      emphasis: { symbolSize: 5, focus: 'series' },
      lineStyle: { width: 2, color },
      itemStyle: { color },
      data: values,
      ...(area ? { areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: color + '33' }, { offset: 1, color: color + '05' }] } } } : {}),
      ...(markLine ? { markLine: { silent: true, symbol: 'none', lineStyle: { type: 'dashed', color: color + '88' }, label: { fontSize: 10, formatter: '均值 {c}' }, data: [{ type: 'average', name: '均值' }] } } : {})
    }
  })
}

// ---- Map color configs ----

export const SST_MAP_COLORS = [
  { min: -Infinity, max: 16,  color: '#1A5276', label: '<16°C' },
  { min: 16,       max: 20,  color: '#2E86C1', label: '16-20°C' },
  { min: 20,       max: 24,  color: '#F39C12', label: '20-24°C' },
  { min: 24,       max: 28,  color: '#E67E22', label: '24-28°C' },
  { min: 28,       max: Infinity, color: '#E74C3C', label: '>28°C' }
]

export const CHL_CONC_COLORS = [
  { min: -Infinity, max: 0.5,  color: '#0B5345', label: '<0.5 mg/m³' },
  { min: 0.5,      max: 1.5,  color: '#148F77', label: '0.5-1.5' },
  { min: 1.5,      max: 3.0,  color: '#1ABC9C', label: '1.5-3.0' },
  { min: 3.0,      max: 5.0,  color: '#27AE60', label: '3.0-5.0' },
  { min: 5.0,      max: Infinity, color: '#2ECC71', label: '>5.0' }
]

export const CHL_PROB_COLORS = [
  { min: -Infinity, max: 20,  color: '#27AE60', label: '<20%' },
  { min: 20,       max: 40,  color: '#F1C40F', label: '20-40%' },
  { min: 40,       max: 60,  color: '#F39C12', label: '40-60%' },
  { min: 60,       max: 80,  color: '#E67E22', label: '60-80%' },
  { min: 80,       max: Infinity, color: '#E74C3C', label: '>80%' }
]

/**
 * Get the color for a value from a color range config array.
 * Returns '#999' as fallback for null/undefined values.
 */
export function getMapColor(value, colorRanges) {
  if (value == null) return '#999'
  for (const range of colorRanges) {
    if (value > range.min && value <= range.max) return range.color
    if (value <= range.min && range.min === -Infinity) return range.color
  }
  return '#999'
}

/**
 * Convert color range config to leaflet.heat gradient object.
 * Keys are 0.0–1.0 normalized positions, values are hex colors.
 */
export function buildHeatGradient(colorRanges) {
  if (!colorRanges || !colorRanges.length) {
    return { 0: '#999', 1: '#999' }
  }
  if (colorRanges.length === 1) {
    return { 0: colorRanges[0].color, 1: colorRanges[0].color }
  }
  const gradient = {}
  const n = colorRanges.length
  colorRanges.forEach((range, i) => {
    gradient[i / (n - 1)] = range.color
  })
  return gradient
}
