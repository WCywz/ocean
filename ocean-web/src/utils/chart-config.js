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
      left: 70,
      right: useRightLegend ? 180 : 50,
      top: 20,
      bottom: useRightLegend ? 35 : 80
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
      axisLabel: { rotate: 25, fontSize: 11, color: '#666' },
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
