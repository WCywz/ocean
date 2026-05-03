// ocean-web/src/utils/__tests__/chart-config.test.js
import { describe, it, expect } from 'vitest'
import { SST_COLORS, CHL_COLORS, OCEAN_CHART_COLORS, buildBaseOption, buildTooltipFormatter } from '../chart-config'

describe('chart-config', () => {
  it('SST_COLORS has 10 warm-spectrum colors', () => {
    expect(SST_COLORS).toHaveLength(10)
    expect(SST_COLORS[0]).toBe('#E74C3C')
  })

  it('CHL_COLORS has 10 green-teal-spectrum colors', () => {
    expect(CHL_COLORS).toHaveLength(10)
    expect(CHL_COLORS[0]).toBe('#27AE60')
  })

  it('OCEAN_CHART_COLORS is SST_COLORS by default', () => {
    expect(OCEAN_CHART_COLORS).toEqual(SST_COLORS)
  })

  it('buildBaseOption returns correct grid and axis defaults', () => {
    const opt = buildBaseOption({ legendData: ['A'], xAxisData: ['2026-01'] })
    expect(opt.grid.left).toBe(70)
    expect(opt.grid.right).toBe(180)
    expect(opt.legend.orient).toBe('vertical')
    expect(opt.legend.right).toBe(0)
    expect(opt.xAxis.data).toEqual(['2026-01'])
  })

  it('buildBaseOption uses bottom legend when series count > 8', () => {
    const data = Array.from({ length: 9 }, (_, i) => `Series ${i}`)
    const opt = buildBaseOption({ legendData: data, xAxisData: [] })
    expect(opt.legend.orient).toBe('horizontal')
    expect(opt.legend.bottom).toBe(0)
  })

  it('buildBaseOption hides legend for single series', () => {
    const opt = buildBaseOption({ legendData: ['Only'], xAxisData: [] })
    expect(opt.legend.show).toBe(false)
  })

  it('buildTooltipFormatter sorts by value descending', () => {
    const formatter = buildTooltipFormatter('°C', {})
    const params = [
      { seriesName: '(119.5, 38.5)', value: 15.8, color: '#E74C3C', axisValue: '2026-04-29' },
      { seriesName: '(122.5, 36.0)', value: 17.2, color: '#F39C12', axisValue: '2026-04-29' },
      { seriesName: '(116.0, 18.0)', value: 27.3, color: '#3498DB', axisValue: '2026-04-29' },
    ]
    const result = formatter(params)
    // 27.3 should appear first in the HTML (highest value)
    const idx27 = result.indexOf('27.3')
    const idx15 = result.indexOf('15.8')
    expect(idx27).toBeLessThan(idx15)
  })

  it('buildTooltipFormatter shows location name when provided', () => {
    const locationMap = { '(119.5, 38.5)': '渤海观测站A' }
    const formatter = buildTooltipFormatter('°C', locationMap)
    const params = [
      { seriesName: '(119.5, 38.5)', value: 15.8, color: '#E74C3C', axisValue: '2026-04-29' },
    ]
    const result = formatter(params)
    expect(result).toContain('渤海观测站A')
  })
})
