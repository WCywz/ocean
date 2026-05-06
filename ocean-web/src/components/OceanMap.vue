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
import 'leaflet.heat'
import 'leaflet/dist/leaflet.css'
import 'leaflet-draw'
import 'leaflet-draw/dist/leaflet.draw.css'
import { buildHeatGradient } from '../utils/chart-config'

const props = defineProps({
  gridData: { type: Array, default: () => [] },
  colorRanges: { type: Array, default: () => [] },
  legendLabels: { type: Array, default: () => [] },
  legendTitle: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  center: { type: Array, default: () => [29.8, 123.5] },
  zoom: { type: Number, default: 7 },
  height: { type: String, default: '450px' }
})

const emit = defineEmits(['cellClick', 'bboxChange'])

const mapContainer = ref(null)
let map = null
let heatLayer = null
let drawControl = null
let drawnItems = null

const legendColors = computed(() => props.colorRanges.map(r => r.color))

function drawGrid() {
  if (!map || !props.gridData.length) return

  if (heatLayer) {
    map.removeLayer(heatLayer)
    heatLayer = null
  }

  const bounds = map.getBounds()
  const south = bounds.getSouth()
  const north = bounds.getNorth()
  const west = bounds.getWest()
  const east = bounds.getEast()

  const visible = props.gridData.filter(p =>
    p.lat >= south && p.lat <= north && p.lon >= west && p.lon <= east
  )

  if (!visible.length) return

  const heatData = visible.map(p => [p.lat, p.lon, p.value ?? 0])

  heatLayer = L.heatLayer(heatData, {
    radius: 15,
    blur: 10,
    maxZoom: 10,
    gradient: buildHeatGradient(props.colorRanges)
  })

  heatLayer.on('click', (e) => {
    const clickLat = e.latlng.lat
    const clickLng = e.latlng.lng
    let nearest = null
    let minDist = Infinity
    for (const p of props.gridData) {
      const dlat = p.lat - clickLat
      const dlng = p.lon - clickLng
      const dist = dlat * dlat + dlng * dlng
      if (dist < minDist) {
        minDist = dist
        nearest = p
      }
    }
    if (nearest) {
      emit('cellClick', { lat: nearest.lat, lon: nearest.lon, value: nearest.value })
    }
  })

  heatLayer.addTo(map)
}

function onMoveEnd() {
  drawGrid()
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

onMounted(() => {
  nextTick(() => {
    map = L.map(mapContainer.value, {
      preferCanvas: true,
      center: props.center,
      zoom: props.zoom
    })

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
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
  overflow: hidden;
}
.ocean-map-container {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
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
