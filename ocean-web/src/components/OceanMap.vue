<template>
  <div class="ocean-map-wrapper">
    <div v-loading="loading" class="ocean-map-container" ref="mapContainer" :style="{ height: height }"></div>

    <!-- Legend overlay -->
    <div class="map-legend" v-if="legendLabels.length">
      <div class="legend-title">{{ legendTitle }}</div>
      <div
        v-for="(item, idx) in legendLabels"
        :key="idx"
        class="legend-item"
      >
        <span class="legend-dot" :style="{ background: legendColors[idx] }"></span>
        <span class="legend-label">{{ item }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import L from 'leaflet'
import 'leaflet-draw'
import { feature } from 'topojson-client'
import landTopo from 'world-atlas/land-50m.json'
import countriesTopo from 'world-atlas/countries-50m.json'
import { useTheme } from '../composables/useTheme'


const props = defineProps({
  gridData: { type: Array, default: () => [] },
  colorRanges: { type: Array, default: () => [] },
  legendLabels: { type: Array, default: () => [] },
  legendTitle: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  center: { type: Array, default: () => [29.8, 123.5] },
  zoom: { type: Number, default: 7 },
  height: { type: String, default: '800px' },
  minZoom: { type: Number, default: 4 },
  maxZoom: { type: Number, default: 10 }
})

const emit = defineEmits(['bboxChange', 'gridClick'])

const mapContainer = ref(null)
let map = null
let heatLayer = null
let drawControl = null
let drawnItems = null
let selectedMarker = null
let landLayer = null
let countryLayer = null
let gridLines = []
let cityCircleMarkers = []

const { resolved } = useTheme()
const isDark = computed(() => resolved.value === 'dark')

function getLandStyle(dark) {
  return dark
    ? { fillColor: '#1e2a26', fillOpacity: 1, color: '#2e3a34', weight: 0.8 }
    : { fillColor: '#f5f0e8', fillOpacity: 1, color: '#c8c0b0', weight: 0.8 }
}

function getCountryStyle(dark) {
  return dark
    ? { fill: false, color: '#3a4050', weight: 1.2, dashArray: '6 3' }
    : { fill: false, color: '#b8a88a', weight: 1.2, dashArray: '6 3' }
}

function getGridStyle(dark) {
  return dark
    ? { color: 'rgba(140,170,200,0.12)', weight: 0.5 }
    : { color: 'rgba(120,160,180,0.25)', weight: 0.5 }
}

function getCityStyle(dark) {
  return dark
    ? { radius: 3, fillColor: '#f0883e', color: '#1e2a26', weight: 1, fillOpacity: 0.9 }
    : { radius: 3, fillColor: '#e87d5c', color: '#fff', weight: 1, fillOpacity: 0.9 }
}

function getSelectedStyle(dark) {
  return dark
    ? { radius: 6, fillColor: '#58a6ff', color: '#e6edf3', weight: 2, fillOpacity: 0.9 }
    : { radius: 6, fillColor: '#1a3a5c', color: '#fff', weight: 2, fillOpacity: 0.9 }
}

const legendColors = computed(() => props.colorRanges.map(r => r.color))

// ── Interpolated heat layer (canvas-based bilinear interpolation) ──

const HeatInterpLayer = L.Layer.extend({
  initialize(gridData, colorRanges) {
    this._rawData = gridData
    this._colorRanges = colorRanges
    this._buildGrid()
  },

  _buildGrid() {
    const pts = this._rawData.filter(p => p.lat != null && p.lon != null && p.value != null)
    if (!pts.length) { this._grid = null; return }

    const lats = [...new Set(pts.map(p => p.lat))].sort((a, b) => a - b)
    const lons = [...new Set(pts.map(p => p.lon))].sort((a, b) => a - b)

    const map = new Map()
    for (const p of pts) {
      map.set(`${p.lat.toFixed(6)},${p.lon.toFixed(6)}`, p.value)
    }

    const values = lats.map(lat => lons.map(lon => {
      const v = map.get(`${lat.toFixed(6)},${lon.toFixed(6)}`)
      return v != null ? v : null
    }))

    this._grid = { lats, lons, values }
  },

  _interpolate(lat, lng) {
    const g = this._grid
    if (!g) return null

    // Buffer margin: allow interpolation slightly beyond grid extent
    // so the heatmap reaches the land mask without visible edge gap,
    // but prevents infinite color bleeding across the open ocean.
    const latExtent = g.lats[g.lats.length - 1] - g.lats[0]
    const lonExtent = g.lons[g.lons.length - 1] - g.lons[0]
    const latBuf = latExtent * 0.08
    const lonBuf = lonExtent * 0.08
    if (lat < g.lats[0] - latBuf || lat > g.lats[g.lats.length - 1] + latBuf) return null
    if (lng < g.lons[0] - lonBuf || lng > g.lons[g.lons.length - 1] + lonBuf) return null

    let i0 = 0, i1 = 0
    for (let i = 0; i < g.lats.length - 1; i++) {
      if (lat >= g.lats[i] && lat <= g.lats[i + 1]) { i0 = i; i1 = i + 1; break }
    }
    if (lat < g.lats[0]) { i0 = 0; i1 = 0 }
    if (lat > g.lats[g.lats.length - 1]) { i0 = g.lats.length - 1; i1 = g.lats.length - 1 }

    let j0 = 0, j1 = 0
    for (let j = 0; j < g.lons.length - 1; j++) {
      if (lng >= g.lons[j] && lng <= g.lons[j + 1]) { j0 = j; j1 = j + 1; break }
    }
    if (lng < g.lons[0]) { j0 = 0; j1 = 0 }
    if (lng > g.lons[g.lons.length - 1]) { j0 = g.lons.length - 1; j1 = g.lons.length - 1 }

    const v00 = g.values[i0][j0], v10 = g.values[i1][j0]
    const v01 = g.values[i0][j1], v11 = g.values[i1][j1]
    const vals = [v00, v10, v01, v11].filter(v => v != null)
    if (vals.length === 0) return null
    if (vals.length < 4) return vals.reduce((a, b) => a + b, 0) / vals.length

    const t = i0 === i1 ? 0 : (lat - g.lats[i0]) / (g.lats[i1] - g.lats[i0])
    const u = j0 === j1 ? 0 : (lng - g.lons[j0]) / (g.lons[j1] - g.lons[j0])
    return (1 - t) * (1 - u) * v00 + t * (1 - u) * v10 + (1 - t) * u * v01 + t * u * v11
  },

  _getColor(value) {
    if (value == null) return null
    for (const r of this._colorRanges) {
      if (r.min === -Infinity && value <= r.max) return r.color
      if (r.max === Infinity && value > r.min) return r.color
      if (value > r.min && value <= r.max) return r.color
    }
    return null
  },

  onAdd(map) {
    this._map = map
    this._canvas = L.DomUtil.create('canvas', 'leaflet-zoom-animated')
    this._canvas.style.position = 'absolute'
    this._canvas.style.top = '0'
    this._canvas.style.left = '0'
    this._canvas.style.pointerEvents = 'none'
    this._ctx = this._canvas.getContext('2d', { willReadFrequently: false })
    map.getPane('heatmap').appendChild(this._canvas)
    this._debounce = null
    map.on('moveend zoomend viewreset', this._scheduleDraw, this)
    this._resize()
    this._draw()
  },

  onRemove(map) {
    L.DomUtil.remove(this._canvas)
    map.off('moveend zoomend viewreset', this._scheduleDraw, this)
    if (this._debounce) clearTimeout(this._debounce)
  },

  _scheduleDraw() {
    if (this._debounce) clearTimeout(this._debounce)
    this._debounce = setTimeout(() => {
      this._resize()
      this._draw()
    }, 50)
  },

  _resize() {
    const s = this._map.getSize()
    if (this._canvas.width === s.x && this._canvas.height === s.y) return
    this._canvas.width = s.x
    this._canvas.height = s.y
    this._canvas.style.width = s.x + 'px'
    this._canvas.style.height = s.y + 'px'
  },

  _draw() {
    const g = this._grid
    if (!g) return
    const map = this._map
    const size = map.getSize()
    if (size.x === 0 || size.y === 0) return

    const step = 3
    const w = Math.ceil(size.x / step)
    const h = Math.ceil(size.y / step)

    const off = document.createElement('canvas')
    off.width = w; off.height = h
    const octx = off.getContext('2d')
    const img = octx.createImageData(w, h)

    const zoom = map.getZoom()
    const worldSize = 256 * Math.pow(2, zoom)
    const pixelOrigin = map.getPixelOrigin()

    // Precompute lat for each row (Mercator inverse)
    const latByRow = new Array(h)
    const nBase = Math.PI - 2 * Math.PI * pixelOrigin.y / worldSize
    const nStep = -2 * Math.PI * step / worldSize
    for (let py = 0; py < h; py++) {
      latByRow[py] = (180 / Math.PI) * Math.atan(Math.sinh(nBase + nStep * py))
    }

    // Precompute lng for each column (linear in Mercator)
    const lngByCol = new Array(w)
    const lngBase = (pixelOrigin.x / worldSize) * 360 - 180
    const lngStep = (step / worldSize) * 360
    for (let px = 0; px < w; px++) {
      lngByCol[px] = lngBase + lngStep * px
    }

    for (let py = 0; py < h; py++) {
      const lat = latByRow[py]
      for (let px = 0; px < w; px++) {
        const lng = lngByCol[px]
        const value = this._interpolate(lat, lng)
        const idx = (py * w + px) * 4
        if (value != null) {
          const color = this._getColor(value)
          if (color) {
            img.data[idx] = parseInt(color.slice(1, 3), 16)
            img.data[idx + 1] = parseInt(color.slice(3, 5), 16)
            img.data[idx + 2] = parseInt(color.slice(5, 7), 16)
            img.data[idx + 3] = 255
          }
        }
      }
    }

    octx.putImageData(img, 0, 0)

    const ctx = this._ctx
    ctx.clearRect(0, 0, size.x, size.y)
    ctx.imageSmoothingEnabled = true
    ctx.drawImage(off, 0, 0, w, h, 0, 0, size.x, size.y)
  },

  updateData(gridData, colorRanges) {
    this._rawData = gridData
    if (colorRanges) this._colorRanges = colorRanges
    this._buildGrid()
    if (this._map) {
      this._resize()
      this._draw()
    }
  }
})

// ── drawGrid (replaces leaflet.heat with interpolated canvas) ──

function drawGrid() {
  if (!map || !props.gridData.length) {
    if (heatLayer) { map.removeLayer(heatLayer); heatLayer = null }
    return
  }

  if (heatLayer) {
    map.removeLayer(heatLayer)
  }

  heatLayer = new HeatInterpLayer(props.gridData, props.colorRanges)
  heatLayer.addTo(map)
}

// ── Grid point interaction ──

function findNearestGridPoint(lat, lng) {
  if (!props.gridData.length) return null
  let best = null
  let bestDist = Infinity
  for (const p of props.gridData) {
    const dlat = p.lat - lat
    const dlng = p.lon - lng
    const dist = dlat * dlat + dlng * dlng
    if (dist < bestDist) { bestDist = dist; best = p }
  }
  return best
}

function placeSelectedMarker(pt) {
  clearSelectedMarker()
  selectedMarker = L.circleMarker([pt.lat, pt.lon], {
    ...getSelectedStyle(isDark.value),
    pane: 'labels',
    interactive: false
  }).addTo(map)
}

function clearSelectedMarker() {
  if (selectedMarker) { map.removeLayer(selectedMarker); selectedMarker = null }
}

function onMapClick(e) {
  const pt = findNearestGridPoint(e.latlng.lat, e.latlng.lng)
  if (!pt) return
  placeSelectedMarker(pt)
  emit('gridClick', { lon: pt.lon, lat: pt.lat })
}

function emitCenterPoint() {
  if (!props.gridData.length) return
  const pt = findNearestGridPoint(props.center[0], props.center[1])
  if (pt) {
    placeSelectedMarker(pt)
    emit('gridClick', { lon: pt.lon, lat: pt.lat })
  }
}

// ── Map event handlers ──

function onMoveEnd() {
  const b = map.getBounds()
  emit('bboxChange', {
    north: b.getNorth(),
    south: b.getSouth(),
    east: b.getEast(),
    west: b.getWest()
  })
}

function onDrawCreated(e) {
  drawnItems.addLayer(e.layer)
  const b = e.layer.getBounds()
  emit('bboxChange', {
    north: b.getNorth(),
    south: b.getSouth(),
    east: b.getEast(),
    west: b.getWest()
  })
}

function onDrawDeleted() {
  const b = map.getBounds()
  emit('bboxChange', {
    north: b.getNorth(),
    south: b.getSouth(),
    east: b.getEast(),
    west: b.getWest()
  })
}

function initDraw() {
  drawnItems = new L.FeatureGroup()
  map.addLayer(drawnItems)

  drawControl = new L.Control.Draw({
    draw: {
      polygon: { allowIntersection: false },
      rectangle: {},
      circle: false,
      circlemarker: false,
      marker: false,
      polyline: false
    },
    edit: { featureGroup: drawnItems }
  })
  map.addControl(drawControl)

  map.on(L.Draw.Event.CREATED, onDrawCreated)
  map.on(L.Draw.Event.DELETED, onDrawDeleted)
}

function initBaseLayers() {
  // Pane for heatmap — below land so land masks the interpolation bleed
  map.createPane('heatmap')
  map.getPane('heatmap').style.zIndex = 200

  // Pane for basemap (land, borders, grid) — above heatmap
  map.createPane('basemap')
  map.getPane('basemap').style.zIndex = 250

  // Pane for labels — above heatmap so text is always visible
  map.createPane('labels')
  map.getPane('labels').style.zIndex = 650

  const landGeojson = feature(landTopo, landTopo.objects.land)
  const countriesGeojson = feature(countriesTopo, countriesTopo.objects.countries)

  // Land fill
  landLayer = L.geoJSON(landGeojson, {
    pane: 'basemap',
    style: getLandStyle(isDark.value),
  }).addTo(map)

  // Country borders
  countryLayer = L.geoJSON(countriesGeojson, {
    pane: 'basemap',
    style: getCountryStyle(isDark.value),
  }).addTo(map)

  // Grid lines (every 5°)
  const gridStyle = getGridStyle(isDark.value)
  for (let lat = 10; lat <= 45; lat += 5) {
    gridLines.push(L.polyline([[lat, 105], [lat, 135]], {
      pane: 'basemap', ...gridStyle, interactive: false,
    }).addTo(map))
  }
  for (let lon = 105; lon <= 135; lon += 5) {
    gridLines.push(L.polyline([[5, lon], [45, lon]], {
      pane: 'basemap', ...gridStyle, interactive: false,
    }).addTo(map))
  }

  // Country labels
  const countries = [
    ['中国', 35.0, 108.0], ['俄罗斯', 55.0, 95.0], ['蒙古', 46.5, 105.0],
    ['朝鲜', 39.5, 127.0], ['韩国', 36.0, 127.8], ['日本', 37.0, 138.0],
    ['菲律宾', 12.5, 122.5], ['越南', 16.5, 106.5], ['老挝', 18.5, 103.5],
    ['泰国', 15.5, 101.0], ['缅甸', 21.0, 96.0], ['柬埔寨', 12.5, 105.0],
    ['印度', 22.0, 79.0], ['尼泊尔', 28.0, 83.5], ['不丹', 27.5, 90.5],
    ['哈萨克斯坦', 47.0, 66.0], ['马来西亚', 3.5, 110.0], ['印度尼西亚', -2.0, 115.0],
  ]
  for (const [name, lat, lon] of countries) {
    L.marker([lat, lon], {
      pane: 'labels',
      icon: L.divIcon({ className: 'country-label', html: name, iconSize: [60, 20], iconAnchor: [30, 10] }),
    }).addTo(map)
  }

  // Sea area labels
  const seas = [
    ['渤  海', 38.8, 119.5], ['黄  海', 35.5, 123.5], ['东  海', 28.5, 124.5],
    ['南  海', 15.0, 115.0], ['日本海', 39.0, 133.0], ['菲律宾海', 18.0, 128.0],
  ]
  for (const [name, lat, lon] of seas) {
    L.marker([lat, lon], {
      pane: 'labels',
      icon: L.divIcon({ className: 'sea-label', html: name, iconSize: [100, 24], iconAnchor: [50, 12] }),
    }).addTo(map)
  }

  // Coastal cities
  const cities = [
    ['大连', 38.92, 121.63], ['天津', 39.08, 117.20], ['青岛', 36.07, 120.38],
    ['上海', 31.23, 121.47], ['宁波', 29.87, 121.55], ['舟山', 30.02, 122.11],
    ['温州', 28.02, 120.65], ['福州', 26.07, 119.30], ['厦门', 24.48, 118.09],
    ['广州', 23.13, 113.26], ['深圳', 22.54, 114.06], ['海口', 20.04, 110.34],
    ['三亚', 18.25, 109.51], ['台北', 25.03, 121.57],
    ['首尔', 37.57, 126.98], ['东京', 35.68, 139.76], ['马尼拉', 14.60, 120.98],
    ['胡志明', 10.82, 106.63],
  ]
  const cityStyle = getCityStyle(isDark.value)
  for (const [name, lat, lon] of cities) {
    cityCircleMarkers.push(L.circleMarker([lat, lon], {
      ...cityStyle, pane: 'labels', interactive: false,
    }).addTo(map))
    L.marker([lat, lon], {
      pane: 'labels',
      icon: L.divIcon({ className: 'city-label', html: name, iconSize: [40, 16], iconAnchor: [-8, 4] }),
    }).addTo(map)
  }
}

watch(isDark, (dark) => {
  if (landLayer) landLayer.setStyle(getLandStyle(dark))
  if (countryLayer) countryLayer.setStyle(getCountryStyle(dark))
  const gs = getGridStyle(dark)
  for (const line of gridLines) line.setStyle(gs)
  const cs = getCityStyle(dark)
  for (const m of cityCircleMarkers) m.setStyle(cs)
  if (selectedMarker) selectedMarker.setStyle(getSelectedStyle(dark))
})

onMounted(() => {
  nextTick(() => {
    map = L.map(mapContainer.value, {
      preferCanvas: true,
      center: [props.center[0], props.center[1]],
      zoom: props.zoom,
      minZoom: props.minZoom,
      maxZoom: props.maxZoom,
      maxBounds: [[0, 100], [48, 145]],
      maxBoundsViscosity: 0.8,
      scrollWheelZoom: false
    })

    initBaseLayers()
    map.on('moveend', onMoveEnd)
    map.on('click', onMapClick)
    map.getContainer().addEventListener('wheel', (e) => {
      if (e.ctrlKey || e.metaKey) {
        e.preventDefault()
        const delta = e.deltaY > 0 ? -1 : 1
        map.zoomIn(delta, { animate: false })
      }
    }, { passive: false })
    initDraw()
    drawGrid()
  })
})

let gridEmitted = false

watch(() => props.gridData, () => {
  nextTick(() => {
    drawGrid()
    if (!gridEmitted && props.gridData.length) {
      gridEmitted = true
      emitCenterPoint()
    }
  })
}, { deep: true })

watch(() => props.colorRanges, (newRanges) => {
  if (heatLayer && props.gridData.length) {
    heatLayer.updateData(props.gridData, newRanges)
  }
})

onUnmounted(() => {
  map?.remove()
})
</script>

<style scoped>
.ocean-map-wrapper {
  position: relative;
  width: 100%;
}
.ocean-map-container {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  background: var(--map-ocean-bg);
}
.map-legend {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: var(--color-bg);
  border: 1px solid var(--color-divider-strong);
  border-radius: 0;
  padding: 10px 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  font-size: 12px;
  z-index: 1000;
}
.legend-title {
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--color-text);
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.legend-label {
  color: var(--color-text-secondary);
  white-space: nowrap;
}
</style>

<style>
/* Basemap labels (unscoped so Leaflet divIcon classes work) */
.country-label {
  font-size: 15px; font-weight: 700; color: var(--map-label-country);
  text-shadow: 0 0 4px var(--map-label-country-shadow);
  white-space: nowrap; pointer-events: none;
  text-align: center;
}
.city-label {
  font-size: 12px; color: var(--map-label-city);
  text-shadow: 0 0 3px var(--map-label-city-shadow);
  white-space: nowrap; pointer-events: none;
}
.sea-label {
  font-size: 14px; font-style: italic; color: var(--map-label-sea);
  text-shadow: 0 0 4px var(--map-label-sea-shadow);
  white-space: nowrap; pointer-events: none;
  text-align: center; letter-spacing: 4px;
}
</style>
