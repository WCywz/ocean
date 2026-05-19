// ocean-web/src/utils/__tests__/chart-config.test.js
import { describe, it, expect } from 'vitest'
import { SST_COLORS, CHL_COLORS, OCEAN_CHART_COLORS, buildBaseOption, buildTooltipFormatter, buildSeriesData, SST_MAP_COLORS, CHL_CONC_COLORS, CHL_PROB_COLORS, getMapColor, buildHeatGradient } from '../chart-config'

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

  it('buildBaseOption — single series: hidden legend, compact grid', () => {
    const opt = buildBaseOption({ legendData: ['A'], xAxisData: ['2026-01'] })
    expect(opt.grid.left).toBe(70)
    expect(opt.grid.right).toBe(50)
    expect(opt.legend.show).toBe(false)
    expect(opt.xAxis.data).toEqual(['2026-01'])
  })

  it('buildBaseOption — 2 series: right-side legend', () => {
    const opt = buildBaseOption({ legendData: ['A', 'B'], xAxisData: ['2026-01'] })
    expect(opt.legend.orient).toBe('vertical')
    expect(opt.legend.right).toBe(0)
    expect(opt.grid.right).toBe(180)
  })

  it('buildBaseOption — 8 series (boundary): right-side legend', () => {
    const data = Array.from({ length: 8 }, (_, i) => `S${i}`)
    const opt = buildBaseOption({ legendData: data, xAxisData: [] })
    expect(opt.legend.orient).toBe('vertical')
    expect(opt.grid.right).toBe(180)
  })

  it('buildBaseOption — 9 series: bottom scroll legend', () => {
    const data = Array.from({ length: 9 }, (_, i) => `S${i}`)
    const opt = buildBaseOption({ legendData: data, xAxisData: [] })
    expect(opt.legend.orient).toBe('horizontal')
    expect(opt.legend.bottom).toBe(0)
    expect(opt.grid.right).toBe(50)
  })

  it('buildTooltipFormatter sorts by value descending', () => {
    const formatter = buildTooltipFormatter('°C', {})
    const params = [
      { seriesName: '(119.5, 38.5)', value: 15.8, color: '#E74C3C', axisValue: '2026-04-29' },
      { seriesName: '(122.5, 36.0)', value: 17.2, color: '#F39C12', axisValue: '2026-04-29' },
      { seriesName: '(116.0, 18.0)', value: 27.3, color: '#3498DB', axisValue: '2026-04-29' },
    ]
    const result = formatter(params)
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

  it('buildTooltipFormatter returns empty string for empty params', () => {
    const formatter = buildTooltipFormatter('°C', {})
    expect(formatter([])).toBe('')
    expect(formatter(null)).toBe('')
  })

  it('buildSeriesData — basic series with color cycling', () => {
    const seriesMap = {
      'A': [1, 2, 3],
      'B': [4, 5, 6],
      'C': [7, 8, 9]
    }
    const colors = ['#111', '#222']
    const result = buildSeriesData(seriesMap, colors)
    expect(result).toHaveLength(3)
    expect(result[0].name).toBe('A')
    expect(result[0].type).toBe('line')
    expect(result[0].smooth).toBe(true)
    expect(result[0].data).toEqual([1, 2, 3])
    expect(result[0].lineStyle.color).toBe('#111')
    expect(result[0].symbolSize).toBe(0)
    // Color cycling: 3rd series wraps to first color
    expect(result[2].lineStyle.color).toBe('#111')
  })

  it('buildSeriesData — with area', () => {
    const seriesMap = { 'X': [10, 20] }
    const result = buildSeriesData(seriesMap, ['#abc'], { area: true })
    expect(result[0].areaStyle).toBeDefined()
    expect(result[0].areaStyle.color.type).toBe('linear')
    expect(result[0].areaStyle.color.colorStops[0].color).toBe('#abc33')
    expect(result[0].areaStyle.color.colorStops[1].color).toBe('#abc05')
  })

  it('buildSeriesData — with markLine', () => {
    const seriesMap = { 'Y': [30, 40] }
    const result = buildSeriesData(seriesMap, ['#def'], { markLine: true })
    expect(result[0].markLine).toBeDefined()
    expect(result[0].markLine.lineStyle.color).toBe('#def88')
    expect(result[0].markLine.data[0].type).toBe('average')
  })

  it('buildSeriesData — without area and markLine (defaults)', () => {
    const seriesMap = { 'Z': [50] }
    const result = buildSeriesData(seriesMap, ['#000'])
    expect(result[0].areaStyle).toBeUndefined()
    expect(result[0].markLine).toBeUndefined()
  })

  describe('map color configs', () => {
    it('SST_MAP_COLORS has 10 temperature ranges', () => {
      expect(SST_MAP_COLORS).toHaveLength(10)
      expect(SST_MAP_COLORS[0]).toEqual({ min: -Infinity, max: 10, color: '#313695' })
      expect(SST_MAP_COLORS[9]).toEqual({ min: 34, max: Infinity, color: '#67001f' })
    })

    it('CHL_CONC_COLORS has 5 concentration ranges', () => {
      expect(CHL_CONC_COLORS).toHaveLength(5)
      expect(CHL_CONC_COLORS[0].color).toBe('#0B5345')
      expect(CHL_CONC_COLORS[4].color).toBe('#2ECC71')
    })

    it('CHL_PROB_COLORS has 5 probability ranges', () => {
      expect(CHL_PROB_COLORS).toHaveLength(5)
      expect(CHL_PROB_COLORS[0]).toEqual({ min: -Infinity, max: 20, color: '#27AE60', label: '<20%' })
    })

    it('getMapColor returns correct color for value', () => {
      expect(getMapColor(8, SST_MAP_COLORS)).toBe('#313695')
      expect(getMapColor(23, SST_MAP_COLORS)).toBe('#fdae61')
      expect(getMapColor(30, SST_MAP_COLORS)).toBe('#d73027')
    })

    it('getMapColor returns fallback for undefined value', () => {
      expect(getMapColor(null, SST_MAP_COLORS)).toBe('#999')
    })
  })

  describe('buildHeatGradient', () => {
    it('converts SST color ranges to leaflet.heat gradient object', () => {
      const gradient = buildHeatGradient(SST_MAP_COLORS)
      expect(gradient).toBeTypeOf('object')
      expect(Object.keys(gradient)).toHaveLength(10)
      expect(gradient['0']).toBe('#313695')
      expect(gradient['1']).toBe('#67001f')
    })

    it('normalizes gradient keys between 0 and 1', () => {
      const gradient = buildHeatGradient(SST_MAP_COLORS)
      const keys = Object.keys(gradient).map(Number)
      expect(Math.min(...keys)).toBeGreaterThanOrEqual(0)
      expect(Math.max(...keys)).toBeLessThanOrEqual(1)
    })

    it('handles a single-element range without NaN keys', () => {
      const gradient = buildHeatGradient([{ color: '#FF0000' }])
      expect(gradient).toEqual({ '0': '#FF0000', '1': '#FF0000' })
    })

    it('handles empty ranges gracefully', () => {
      const gradient = buildHeatGradient([])
      expect(gradient).toEqual({ '0': '#999', '1': '#999' })
    })
  })
})
