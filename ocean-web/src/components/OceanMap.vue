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
import 'leaflet.heat'
import { buildHeatGradient } from '../utils/chart-config'
import { wgs84ToGcj02, gcj02ToWgs84 } from '../utils/coord-transform'

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

const emit = defineEmits(['bboxChange'])

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
  }

  const bounds = map.getBounds()
  const pad = 0.01
  const visible = props.gridData.filter(p =>
    p.lat >= bounds.getSouth() - pad &&
    p.lat <= bounds.getNorth() + pad &&
    p.lon >= bounds.getWest() - pad &&
    p.lon <= bounds.getEast() + pad
  )

  if (!visible.length) return

  const data = visible.map(p => {
    const [gcjLng, gcjLat] = wgs84ToGcj02(p.lon, p.lat)
    return [gcjLat, gcjLng, p.value]
  })
  heatLayer = L.heatLayer(data, {
    radius: 15,
    blur: 10,
    maxZoom: 10,
    gradient: buildHeatGradient(props.colorRanges)
  }).addTo(map)
}

function onMoveEnd() {
  drawGrid()
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
      zoom: props.zoom
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
