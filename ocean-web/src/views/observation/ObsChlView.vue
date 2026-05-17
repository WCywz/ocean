<template>
  <div>
    <h1 class="editorial-page-title">叶绿素观测</h1>
    <p class="editorial-page-subtitle">Chlorophyll Concentration Observation</p>

    <div class="editorial-section" style="padding-bottom: 20px; margin-bottom: 20px;">
      <div class="editorial-filter-bar">
        <span class="editorial-form-label" style="margin: 0 8px 0 0;">数据筛选</span>
        <el-date-picker v-model="filterDate" type="date" placeholder="选择观测日期" value-format="YYYY-MM-DD" style="width: 180px" />
        <el-select v-model="seaArea" placeholder="海域筛选" style="width: 160px" @change="onSeaAreaChange">
          <el-option v-for="area in seaAreas" :key="area.name" :label="area.name" :value="area" />
        </el-select>
        <button class="editorial-btn-outline" @click="handleSearch">查询</button>
        <button class="editorial-btn-outline" @click="handleReset">重置</button>
        <span style="font-size: 12px; color: var(--color-text-muted); margin-left: auto;">也可在地图上拖拽框选海域</span>
      </div>
    </div>

    <div class="editorial-section">
      <p class="editorial-section-label">Interactive</p>
      <h3 class="editorial-section-heading">观测栅格地图</h3>
      <OceanMap
        :grid-data="gridData"
        :color-ranges="CHL_CONC_COLORS"
        :legend-labels="legendLabels"
        legend-title="浓度 (mg/m³)"
        :loading="mapLoading"
        @bbox-change="onBboxChange"
      />
    </div>

    <div class="editorial-section" style="border-bottom: none;">
      <p class="editorial-section-label">Feature · 趋势分析</p>
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;">
        <h3 class="editorial-section-heading" style="margin: 0;">叶绿素浓度变化趋势</h3>
      </div>
      <TrendChart
        :series-data="trendSeries"
        :x-axis-data="trendDates"
        y-axis-name="浓度 (mg/m³)"
        y-axis-unit="mg/m³"
        :loading="trendLoading"
        :colors="CHL_COLORS"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import OceanMap from '../../components/OceanMap.vue'
import TrendChart from '../../components/TrendChart.vue'
import { getObsMapGrid, getObsPointTrend } from '../../api/ocean-data'
import { getSeaAreas } from '../../api/forecast'
import { CHL_CONC_COLORS, CHL_COLORS } from '../../utils/chart-config'

const filterDate = ref('')
const seaArea = ref(null)
const seaAreas = ref([])
const gridData = ref([])
const mapLoading = ref(false)
const trendSeries = ref([])
const trendDates = ref([])
const trendLoading = ref(false)
const customBbox = ref(null)

const legendLabels = ['<0.5', '0.5-1.5', '1.5-3.0', '3.0-5.0', '>5.0 mg/m³']

function defaultDate() {
  return '2026-01-01'
}

function buildBboxParams() {
  const params = {}
  if (customBbox.value) {
    params.minLon = customBbox.value.west
    params.maxLon = customBbox.value.east
    params.minLat = customBbox.value.south
    params.maxLat = customBbox.value.north
  } else if (seaArea.value) {
    params.minLon = seaArea.value.minLon
    params.maxLon = seaArea.value.maxLon
    params.minLat = seaArea.value.minLat
    params.maxLat = seaArea.value.maxLat
  }
  return params
}

async function fetchGridData() {
  mapLoading.value = true
  try {
    const params = {
      dataType: 'chl',
      obsDate: filterDate.value || defaultDate(),
      ...buildBboxParams()
    }
    const res = await getObsMapGrid(params)
    gridData.value = (res.data || []).map(r => ({
      lon: r.gridLon != null ? Number(r.gridLon) : Number(r.longitude),
      lat: r.gridLat != null ? Number(r.gridLat) : Number(r.latitude),
      value: r.value
    }))
  } finally {
    mapLoading.value = false
  }
}

async function fetchTrendData(lon, lat) {
  trendLoading.value = true
  try {
    const res = await getObsPointTrend({ dataType: 'chl', lon, lat })
    const points = res.data || []
    trendDates.value = points.map(p => p.obsDate)
    trendSeries.value = [{
      name: `(${Number(lon).toFixed(2)}, ${Number(lat).toFixed(2)})`,
      data: points.map(p => p.value)
    }]
  } finally {
    trendLoading.value = false
  }
}

function onBboxChange(bbox) { customBbox.value = bbox }

async function loadSeaAreas() {
  try {
    const res = await getSeaAreas()
    seaAreas.value = res.data || []
  } catch (e) { /* empty */ }
}

async function handleSearch() { await fetchGridData() }

function handleReset() {
  filterDate.value = defaultDate()
  seaArea.value = null
  customBbox.value = null
  fetchGridData()
}

function onSeaAreaChange() { customBbox.value = null }

onMounted(async () => {
  filterDate.value = '2026-01-01'
  await loadSeaAreas()
  await fetchGridData()
  fetchTrendData(123.5, 29.8)
})
</script>

<style scoped>
</style>
