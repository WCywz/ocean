<template>
  <div class="editorial-section">
    <p class="editorial-section-label">Alert Map</p>
    <h3 class="editorial-section-heading">点位监测</h3>

    <div class="alert-summary-bar" @click="resetView" title="点击重置视图">{{ summaryText }}</div>

    <div class="alert-split" v-loading="loading">
      <div class="alert-list">
        <p class="list-label">监测站点</p>
        <div
          v-for="s in stations"
          :key="s.stationName"
          :class="['point-item', { 'point-item--active': selectedKey === 's-' + s.stationName }]"
          @click="selectStation(s)"
        >
          <span class="point-dot" :style="{ background: gradeColor(s.overallGrade) }"></span>
          <span class="point-name">{{ s.stationName }}</span>
          <span class="point-meta">{{ gradeLabel[s.overallGrade] }} · SST {{ fmtAnomaly(s.sstAnomaly) }}</span>
        </div>

        <template v-if="hotspots.length">
          <p class="list-label" style="margin-top: 20px;">聚焦点位</p>
          <div
            v-for="(h, i) in hotspots"
            :key="'h-' + i"
            :class="['point-item', { 'point-item--active': selectedKey === 'h-' + i }]"
            @click="selectHotspot(h, i)"
          >
            <span class="point-dot point-dot--pulse" :style="{ background: gradeColor(h.grade) }"></span>
            <span class="point-name">{{ h.lat.toFixed(2) }}°, {{ h.lon.toFixed(2) }}°</span>
            <span class="point-meta">{{ fmtAnomaly(h.anomaly) }}</span>
          </div>
        </template>
      </div>

      <div class="alert-map" ref="mapContainer"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import L from 'leaflet'
import { ElMessageBox } from 'element-plus'
import { feature } from 'topojson-client'
import { useTheme } from '../../composables/useTheme'
import landTopo from 'world-atlas/land-50m.json'
import countriesTopo from 'world-atlas/countries-50m.json'

const props = defineProps({
  stations: { type: Array, default: () => [] },
  hotspots: { type: Array, default: () => [] },
  summary: { type: String, default: '' },
  loading: { type: Boolean, default: false }
})

const router = useRouter()
const mapContainer = ref(null)
const selectedKey = ref('')
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
function getCityStyle(dark) {
  return dark
    ? { radius: 3, fillColor: '#f0883e', color: '#1e2a26', weight: 1, fillOpacity: 0.9 }
    : { radius: 3, fillColor: '#e87d5c', color: '#fff', weight: 1, fillOpacity: 0.9 }
}

const gradeColor = (g) => ({ good: '#22c55e', fine: '#22c55e', warn: '#f59e0b', bad: '#ef4444' }[g || 'good'])
const gradeLabel = { good: '优', fine: '良', warn: '中', bad: '差' }

const summaryText = computed(() => {
  if (!props.stations.length && !props.loading) return '暂无站点数据'
  return props.summary || '加载中...'
})

function fmtAnomaly(val) {
  if (val == null) return '--'
  const sign = val > 0 ? '+' : ''
  return sign + val.toFixed(1) + '°C'
}

const COASTAL_CITIES = [
  ['上海', 31.23, 121.47], ['宁波', 29.87, 121.55], ['舟山', 30.02, 122.11],
  ['温州', 28.02, 120.65], ['福州', 26.07, 119.30],
]

let map = null
let landLayer = null
let countryLayer = null
let cityCircleMarkers = []
let stationMarkers = []
let hotspotMarkers = []
let hotspotLabelMarkers = []
let stationLabelMarkers = []

function selectStation(s) {
  const key = 's-' + s.stationName
  if (selectedKey.value === key) {
    selectedKey.value = ''
    return
  }
  selectedKey.value = key
  map.flyTo([s.lat, s.lon], Math.max(map.getZoom(), 8), { duration: 0.6 })
}

function selectHotspot(h, i) {
  const key = 'h-' + i
  if (selectedKey.value === key) {
    selectedKey.value = ''
    return
  }
  selectedKey.value = key
  map.flyTo([h.lat, h.lon], Math.max(map.getZoom(), 8), { duration: 0.6 })
}

function resetView() {
  selectedKey.value = ''
  if (map) {
    map.flyTo([29.5, 122.5], 7, { duration: 0.6 })
  }
}

function updateHotspotLabelOpacity() {
  const zoom = map ? map.getZoom() : 7
  const sel = selectedKey.value
  const visible = zoom >= 8
  hotspotLabelMarkers.forEach((m, i) => {
    const key = 'h-' + i
    const el = m.getElement()
    if (el) {
      if (!visible) {
        el.style.display = 'none'
      } else {
        el.style.display = ''
        el.style.opacity = sel && sel !== key ? '0.25' : '1'
      }
    }
  })
}

async function onMarkerClick(lat, lon, name) {
  try {
    const action = await ElMessageBox.confirm(
      `坐标 ${lat.toFixed(2)}°, ${lon.toFixed(2)}°`,
      name || '点位详情',
      { confirmButtonText: '温度', cancelButtonText: '叶绿素', distinguishCancelAndClose: true, type: 'info' }
    )
    router.push({ path: '/app/observation/sst', query: { lat: lat.toFixed(4), lon: lon.toFixed(4) } })
  } catch (e) {
    if (e === 'cancel') {
      router.push({ path: '/app/observation/chl', query: { lat: lat.toFixed(4), lon: lon.toFixed(4) } })
    }
  }
}

function renderMarkers() {
  clearMarkers()

  const markerBorder = isDark.value ? '#1e2a26' : '#fff'
  props.stations.forEach(s => {
    const color = gradeColor(s.overallGrade)
    const icon = L.divIcon({
      className: 'alert-station-marker',
      html: `<div style="width:12px;height:12px;border-radius:50%;background:${color};border:2px solid ${markerBorder};box-shadow:0 0 4px rgba(0,0,0,0.3);"></div>`,
      iconSize: [16, 16],
      iconAnchor: [8, 8]
    })
    const marker = L.marker([s.lat, s.lon], { icon, pane: 'labels' })
      .bindTooltip(`<b>${s.stationName}</b><br>${gradeLabel[s.overallGrade]} · SST ${fmtAnomaly(s.sstAnomaly)}`, { direction: 'top', permanent: true, className: 'station-label' })
      .on('click', () => onMarkerClick(s.lat, s.lon, s.stationName))
      .addTo(map)
    stationMarkers.push(marker)
  })

  props.hotspots.forEach((h, i) => {
    const color = gradeColor(h.grade)
    const icon = L.divIcon({
      className: 'alert-hotspot-marker',
      html: `<div class="hotspot-pulse" style="--pulse-color:${color};"></div>`,
      iconSize: [22, 22],
      iconAnchor: [11, 11]
    })
    const marker = L.marker([h.lat, h.lon], { icon, pane: 'labels' })
      .bindTooltip(`热点 · 异常 ${fmtAnomaly(h.anomaly)}`, { direction: 'top' })
      .on('click', () => onMarkerClick(h.lat, h.lon, '热点'))
      .addTo(map)
    hotspotMarkers.push(marker)

    const coordLabel = `${h.lat.toFixed(2)}°, ${h.lon.toFixed(2)}°`
    const labelIcon = L.divIcon({
      className: 'hotspot-coord-label',
      html: coordLabel,
      iconSize: [80, 14],
      iconAnchor: [-7, 7]
    })
    const labelMarker = L.marker([h.lat, h.lon], { icon: labelIcon, pane: 'labels', interactive: false }).addTo(map)
    hotspotLabelMarkers.push(labelMarker)
  })

  updateHotspotLabelOpacity()
}

function clearMarkers() {
  stationMarkers.forEach(m => map.removeLayer(m))
  hotspotMarkers.forEach(m => map.removeLayer(m))
  hotspotLabelMarkers.forEach(m => map.removeLayer(m))
  stationLabelMarkers.forEach(m => map.removeLayer(m))
  stationMarkers = []
  hotspotMarkers = []
  hotspotLabelMarkers = []
  stationLabelMarkers = []
}

function initMap() {
  if (!mapContainer.value) return
  map = L.map(mapContainer.value, {
    center: [29.5, 122.5],
    zoom: 7,
    minZoom: 4,
    maxZoom: 10,
    maxBounds: [[0, 100], [48, 145]],
    maxBoundsViscosity: 0.8,
    scrollWheelZoom: false,
    doubleClickZoom: false,
    zoomControl: false,
    dragging: true,
    attributionControl: false
  })

  map.createPane('basemap')
  map.getPane('basemap').style.zIndex = 250
  map.createPane('labels')
  map.getPane('labels').style.zIndex = 650

  const dark = isDark.value
  landLayer = L.geoJSON(feature(landTopo, landTopo.objects.land), {
    pane: 'basemap',
    style: getLandStyle(dark),
  }).addTo(map)

  countryLayer = L.geoJSON(feature(countriesTopo, countriesTopo.objects.countries), {
    pane: 'basemap',
    style: getCountryStyle(dark),
  }).addTo(map)

  COASTAL_CITIES.forEach(([name, lat, lon]) => {
    const cm = L.circleMarker([lat, lon], {
      ...getCityStyle(dark),
      pane: 'labels', interactive: false,
    }).addTo(map)
    cityCircleMarkers.push(cm)
    L.marker([lat, lon], {
      pane: 'labels',
      icon: L.divIcon({ className: 'city-label', html: name, iconSize: [40, 16], iconAnchor: [-8, 4] }),
      interactive: false,
    }).addTo(map)
  })

  renderMarkers()

  map.on('zoomend', updateHotspotLabelOpacity)
}

function destroyMap() {
  clearMarkers()
  if (map) {
    map.remove()
    map = null
  }
  landLayer = null
  countryLayer = null
  cityCircleMarkers = []
}

let resizeObserver = null

watch(selectedKey, () => {
  updateHotspotLabelOpacity()
})

watch([() => props.stations, () => props.hotspots, () => props.loading], async ([sts, hots, loading]) => {
  await nextTick()
  if (!sts.length && !hots.length && !loading) {
    destroyMap()
    return
  }
  if (!map && mapContainer.value) {
    initMap()
    if (mapContainer.value && !resizeObserver) {
      resizeObserver = new ResizeObserver(() => { if (map) map.invalidateSize() })
      resizeObserver.observe(mapContainer.value)
    }
    return
  }
  if (map) {
    renderMarkers()
    if (!loading) map.invalidateSize()
  }
})

watch(isDark, () => {
  if (!map) return
  const dark = isDark.value
  if (landLayer) landLayer.setStyle(getLandStyle(dark))
  if (countryLayer) countryLayer.setStyle(getCountryStyle(dark))
  const cs = getCityStyle(dark)
  cityCircleMarkers.forEach(m => m.setStyle(cs))
  renderMarkers()
})

onUnmounted(() => {
  if (resizeObserver) { resizeObserver.disconnect(); resizeObserver = null }
  destroyMap()
})
</script>

<style scoped>
.alert-summary-bar {
  display: flex;
  align-items: center;
  border-left: 3px solid #f59e0b;
  padding: 10px 14px;
  background: var(--color-surface);
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
  cursor: pointer;
  user-select: none;
}

.alert-split {
  display: flex;
  gap: 24px;
  min-height: 380px;
}

.alert-list {
  flex: 0 0 34%;
}

.list-label {
  font-size: 10px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin: 0 0 8px 0;
}

.point-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  cursor: pointer;
  transition: background 0.15s;
  border-left: 3px solid transparent;
}

.point-item:hover {
  background: var(--color-surface);
}

.point-item--active {
  background: var(--color-surface);
  border-left-color: var(--color-text);
}

.point-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.point-dot--pulse {
  width: 14px;
  height: 14px;
  box-shadow: 0 0 8px currentColor;
}

.point-name {
  font-size: 13px;
  color: var(--color-text);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.point-meta {
  font-size: 11px;
  color: var(--color-text-muted);
  white-space: nowrap;
}

.alert-map {
  flex: 1;
  min-height: 380px;
  background: var(--map-ocean-bg);
}
</style>

<style>
.alert-station-marker {
  background: transparent !important;
  border: none !important;
}

.alert-hotspot-marker {
  background: transparent !important;
  border: none !important;
}

.hotspot-pulse {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--pulse-color);
  position: relative;
  box-shadow: 0 0 4px var(--pulse-color);
}

.hotspot-pulse::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--pulse-color);
  opacity: 0.4;
  transform: translate(-50%, -50%);
  animation: hotspot-pulse 1.8s cubic-bezier(0.2, 0.6, 0.35, 1) infinite;
}

@keyframes hotspot-pulse {
  0% { transform: translate(-50%, -50%) scale(0.3); opacity: 0.6; }
  80%, 100% { transform: translate(-50%, -50%) scale(1.6); opacity: 0; }
}

.station-label {
  background: transparent;
  border: none;
  box-shadow: none;
  font-size: 11px;
  color: var(--map-label-country);
  text-shadow: 0 0 3px var(--map-label-country-shadow);
}

.station-label::before {
  display: none;
}

.city-label {
  font-size: 12px;
  color: var(--map-label-city);
  text-shadow: 0 0 3px var(--map-label-city-shadow);
  white-space: nowrap;
  pointer-events: none;
}

.hotspot-coord-label {
  font-size: 10px;
  color: var(--map-label-city);
  text-shadow: 0 0 2px var(--map-label-city-shadow);
  white-space: nowrap;
  pointer-events: none;
  background: transparent !important;
  border: none !important;
  transition: opacity 0.3s;
}
</style>
