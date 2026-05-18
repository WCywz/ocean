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
import { wgs84ToGcj02, gcj02ToWgs84 } from '../utils/coord-transform'
import { isPointInLand } from '../utils/land-mask'

const props = defineProps({
  gridData: { type: Array, default: () => [] },
  colorRanges: { type: Array, default: () => [] },
  legendLabels: { type: Array, default: () => [] },
  legendTitle: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  center: { type: Array, default: () => [29.8, 123.5] },
  zoom: { type: Number, default: 7 },
  height: { type: String, default: '450px' },
  maxZoom: { type: Number, default: 10 }
})

const emit = defineEmits(['bboxChange'])

const mapContainer = ref(null)
let map = null
let heatLayer = null
let drawControl = null
let drawnItems = null

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
    // Convert GCJ-02 (map tile coords) back to WGS-84 for grid lookup
    const [wgsLng, wgsLat] = gcj02ToWgs84(lng, lat)
    lat = wgsLat
    lng = wgsLng

    let i0 = 0, i1 = 0
    for (let i = 0; i < g.lats.length - 1; i++) {
      if (lat >= g.lats[i] && lat <= g.lats[i + 1]) { i0 = i; i1 = i + 1; break }
    }
    if (lat < g.lats[0]) { i0 = 0; i1 = 0 }
    if (lat > g.lats[g.lats.length - 1]) { i0 = g.lats.length - 1; i1 = g.lats.length - 1 }
    if (i0 === i1 && (lat < g.lats[0] || lat > g.lats[g.lats.length - 1])) return null

    let j0 = 0, j1 = 0
    for (let j = 0; j < g.lons.length - 1; j++) {
      if (lng >= g.lons[j] && lng <= g.lons[j + 1]) { j0 = j; j1 = j + 1; break }
    }
    if (lng < g.lons[0]) { j0 = 0; j1 = 0 }
    if (lng > g.lons[g.lons.length - 1]) { j0 = g.lons.length - 1; j1 = g.lons.length - 1 }
    if (j0 === j1 && (lng < g.lons[0] || lng > g.lons[g.lons.length - 1])) return null

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
    map.getPanes().overlayPane.appendChild(this._canvas)
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

  updateData(gridData) {
    this._rawData = gridData
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

  // Filter land points, keep WGS-84 coords for grid structure integrity
  const oceanPts = props.gridData
    .filter(p => !isPointInLand(p.lon, p.lat))

  if (!oceanPts.length) return

  heatLayer = new HeatInterpLayer(oceanPts, props.colorRanges)
  heatLayer.addTo(map)
}

// ── Map event handlers ──

function onMoveEnd() {
  const b = map.getBounds()
  const [swLng, swLat] = gcj02ToWgs84(b.getWest(), b.getSouth())
  const [neLng, neLat] = gcj02ToWgs84(b.getEast(), b.getNorth())
  emit('bboxChange', {
    north: neLat,
    south: swLat,
    east: neLng,
    west: swLng
  })
}

function onDrawCreated(e) {
  drawnItems.addLayer(e.layer)
  const b = e.layer.getBounds()
  const [swLng, swLat] = gcj02ToWgs84(b.getWest(), b.getSouth())
  const [neLng, neLat] = gcj02ToWgs84(b.getEast(), b.getNorth())
  emit('bboxChange', {
    north: neLat,
    south: swLat,
    east: neLng,
    west: swLng
  })
}

function onDrawDeleted() {
  const b = map.getBounds()
  const [swLng, swLat] = gcj02ToWgs84(b.getWest(), b.getSouth())
  const [neLng, neLat] = gcj02ToWgs84(b.getEast(), b.getNorth())
  emit('bboxChange', {
    north: neLat,
    south: swLat,
    east: neLng,
    west: swLng
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

onMounted(() => {
  nextTick(() => {
    const [gcjCenterLng, gcjCenterLat] = wgs84ToGcj02(props.center[1], props.center[0])
    map = L.map(mapContainer.value, {
      preferCanvas: true,
      center: [gcjCenterLat, gcjCenterLng],
      zoom: props.zoom,
      maxZoom: props.maxZoom
    })

    L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}&key=c286f049621bc825d06c2203774b2ef3', {
      subdomains: ['1', '2', '3', '4'],
      maxZoom: 18
    }).addTo(map)

    map.on('moveend', onMoveEnd)
    initDraw()
    drawGrid()
  })
})

watch(() => props.gridData, () => {
  nextTick(() => drawGrid())
}, { deep: true })

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
  border: 2px solid #d0d0d0;
}
.map-legend {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  padding: 10px 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  font-size: 12px;
  z-index: 1000;
}
.legend-title {
  font-weight: 600;
  margin-bottom: 6px;
  color: #1a3a5c;
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
  color: #555;
  white-space: nowrap;
}
</style>
