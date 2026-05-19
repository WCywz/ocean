<template>
  <div class="editorial-section">
    <p class="editorial-section-label">Alerts</p>
    <h3 class="editorial-section-heading">阈值告警</h3>

    <div class="health-status-bar" :style="{ borderLeftColor: accentColor }">
      <span class="health-status-bar__level">{{ summaryText }}</span>
    </div>

    <div v-if="!alerts.length && !loading" class="alert-empty">
      所选日期无阈值告警
    </div>

    <div v-else class="alert-split" v-loading="loading">
      <div class="alert-cards">
        <div
          v-for="(item, idx) in alerts.slice(0, 10)"
          :key="idx"
          :class="['alert-card', { 'alert-card--active': selectedIdx === idx }]"
          :style="{ borderLeftColor: item.dataType === 'SST' ? '#c0392b' : '#e67e22' }"
          @click="selectAlert(idx)"
        >
          <div class="alert-card__name">{{ item.locationName }}</div>
          <div class="alert-card__meta">
            <span class="editorial-tag alert-card__tag">{{ item.dataType }}</span>
            <span class="alert-card__value">{{ item.value }}{{ item.dataType === 'SST' ? '°C' : ' mg/m³' }}</span>
            <span class="alert-card__threshold">阈值 {{ item.threshold }}</span>
          </div>
        </div>

        <div class="drilldown-links" v-if="alerts.length">
          <span class="drilldown-label">查看详情：</span>
          <router-link to="/app/forecast/sst" class="drilldown-link">海表温度预测地图 →</router-link>
          <router-link to="/app/forecast/chl" class="drilldown-link">叶绿素浓度预测地图 →</router-link>
        </div>
      </div>

      <div class="alert-map" ref="mapContainer"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import L from 'leaflet'
import { feature } from 'topojson-client'
import landTopo from 'world-atlas/land-50m.json'
import countriesTopo from 'world-atlas/countries-50m.json'

const props = defineProps({
  alerts: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const mapContainer = ref(null)
const selectedIdx = ref(-1)

let map = null
let pulseMarker = null

const sstCount = computed(() => props.alerts.filter(a => a.dataType === 'SST').length)
const chlCount = computed(() => props.alerts.filter(a => a.dataType === 'CHL').length)

const summaryText = computed(() => {
  if (!props.alerts.length) return '暂无告警'
  const parts = []
  if (sstCount.value) parts.push(`${sstCount.value} 个区域超过 SST 阈值`)
  if (chlCount.value) parts.push(`${chlCount.value} 个区域超过 Chl 阈值`)
  return parts.join('，')
})

const accentColor = computed(() => {
  if (!props.alerts.length) return '#22c55e'
  if (sstCount.value && chlCount.value) return '#ef4444'
  if (sstCount.value) return '#c0392b'
  return '#e67e22'
})

function selectAlert(idx) {
  if (selectedIdx.value === idx) {
    selectedIdx.value = -1
    clearMarker()
    return
  }
  selectedIdx.value = idx
  flyToAlert(props.alerts[idx])
}

function flyToAlert(item) {
  if (!map) { console.warn('[HealthAlert] map not ready'); return }
  if (item.longitude == null || item.latitude == null) { console.warn('[HealthAlert] missing coords', item); return }
  map.flyTo([item.latitude, item.longitude], map.getZoom(), { duration: 0.8 })
  placeMarker(item)
}

function placeMarker(item) {
  clearMarker()
  const color = item.dataType === 'SST' ? '#c0392b' : '#e67e22'
  const icon = L.divIcon({
    className: 'alert-pulse-marker',
    html: `<div class="pulse-dot" style="background:${color}; width:16px; height:16px;"></div>`,
    iconSize: [48, 48],
    iconAnchor: [24, 24]
  })
  pulseMarker = L.marker([item.latitude, item.longitude], { icon }).addTo(map)
}

function clearMarker() {
  if (pulseMarker) {
    map.removeLayer(pulseMarker)
    pulseMarker = null
  }
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

  // Vector basemap (same as OceanMap)
  map.createPane('basemap')
  map.getPane('basemap').style.zIndex = 250

  const landGeojson = feature(landTopo, landTopo.objects.land)
  L.geoJSON(landGeojson, {
    pane: 'basemap',
    style: { fillColor: '#f5f0e8', fillOpacity: 1, color: '#c8c0b0', weight: 0.8 },
  }).addTo(map)

  L.geoJSON(feature(countriesTopo, countriesTopo.objects.countries), {
    pane: 'basemap',
    style: { fill: false, color: '#b8a88a', weight: 1.2, dashArray: '6 3' },
  }).addTo(map)
}

function destroyMap() {
  clearMarker()
  if (map) {
    map.remove()
    map = null
  }
}

onMounted(() => {
  nextTick(initMap)
})

watch(() => props.alerts, () => {
  selectedIdx.value = -1
  clearMarker()
})

watch([() => props.alerts, () => props.loading], async ([alertList, loading]) => {
  await nextTick()

  // Empty state: destroy map
  if (!alertList.length && !loading) {
    destroyMap()
    return
  }

  // Non-empty state: ensure map exists
  if (!map && mapContainer.value) {
    initMap()
  }

  // After loading overlay is removed, Leaflet needs size recalculation
  if (map && !loading) {
    map.invalidateSize()
  }
})

onUnmounted(() => {
  destroyMap()
})
</script>

<style scoped>
.health-status-bar {
  display: flex;
  align-items: center;
  border-left: 3px solid;
  padding: 10px 14px;
  background: #fafafa;
  font-size: 13px;
  margin-bottom: 16px;
}

.health-status-bar__level {
  font-family: var(--font-serif);
  font-size: 15px;
  color: var(--color-text);
}

.alert-empty {
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  font-size: 13px;
}

.alert-split {
  display: flex;
  gap: 24px;
  min-height: 360px;
}

.alert-cards {
  flex: 0 0 38%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.alert-card {
  padding: 8px 12px;
  border-left: 3px solid;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  border-right: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;
}

.alert-card:hover {
  background: #fafafa;
}

.alert-card--active {
  background: #f5f5f5;
}

.alert-card__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}

.alert-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
  margin-top: 2px;
}

.alert-card__tag {
  font-size: 10px;
}

.alert-card__value {
  font-weight: 600;
  color: var(--color-alert);
}

.alert-card__threshold {
  color: var(--color-text-muted);
}

.alert-map {
  flex: 1;
  min-height: 360px;
  background: #a8d8ea;
}

.drilldown-links {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
}

.drilldown-label {
  color: var(--color-text-muted);
  margin-right: 16px;
}

.drilldown-link {
  color: var(--color-text);
  cursor: pointer;
  margin-right: 20px;
  text-decoration: none;
  border-bottom: 1px dashed #ccc;
}

.drilldown-link:hover {
  color: var(--color-alert);
  border-bottom-color: var(--color-alert);
}
</style>

<style>
.alert-pulse-marker {
  background: transparent !important;
  border: none !important;
}

.pulse-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  position: relative;
  box-shadow: 0 0 6px rgba(0,0,0,0.3);
}

.pulse-dot::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: inherit;
  opacity: 0.35;
  animation: alert-pulse 1.5s cubic-bezier(0.2, 0.6, 0.35, 1) infinite;
}

@keyframes alert-pulse {
  0% {
    transform: translate(-50%, -50%) scale(0.2);
    opacity: 0.5;
  }
  80%, 100% {
    transform: translate(-50%, -50%) scale(1.4);
    opacity: 0;
  }
}
</style>
