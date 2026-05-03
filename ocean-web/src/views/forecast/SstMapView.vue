<template>
  <div class="sst-page">
    <h2 class="page-title">海表温度预测</h2>

    <!-- Filter bar -->
    <el-card shadow="hover" class="filter-card">
      <div class="filter-bar">
        <span class="filter-label">数据筛选</span>
        <el-date-picker
          v-model="filterDate"
          type="date"
          placeholder="选择预报日期"
          value-format="YYYY-MM-DD"
          style="width: 180px"
        />
        <el-select v-model="seaArea" placeholder="海域筛选" style="width: 160px" @change="onSeaAreaChange">
          <el-option
            v-for="area in seaAreas"
            :key="area.name"
            :label="area.name"
            :value="area"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <span class="filter-hint">也可在地图上拖拽框选海域</span>
      </div>
    </el-card>

    <!-- Map -->
    <el-card shadow="hover" class="map-card">
      <OceanMap
        :grid-data="gridData"
        :color-ranges="SST_MAP_COLORS"
        :legend-labels="legendLabels"
        legend-title="温度 (°C)"
        :loading="mapLoading"
        @cell-click="onMapCellClick"
        @bbox-change="onBboxChange"
      />
    </el-card>

    <!-- Trend chart -->
    <el-card shadow="hover" class="trend-card">
      <template #header>
        <div class="trend-header">
          <span class="trend-title">温度变化趋势</span>
          <span v-if="selectedPoint" class="trend-subtitle">
            当前选中: ({{ selectedPoint.lon.toFixed(2) }}, {{ selectedPoint.lat.toFixed(2) }})
          </span>
        </div>
      </template>
      <TrendChart
        :series-data="trendSeries"
        :x-axis-data="trendDates"
        y-axis-name="温度 (°C)"
        y-axis-unit="°C"
        :loading="trendLoading"
        :colors="SST_COLORS"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import OceanMap from '../../components/OceanMap.vue'
import TrendChart from '../../components/TrendChart.vue'
import { getMapGrid, getPointTrend, getSeaAreas } from '../../api/forecast'
import { SST_MAP_COLORS, SST_COLORS } from '../../utils/chart-config'

const filterDate = ref('')
const seaArea = ref(null)
const seaAreas = ref([])
const gridData = ref([])
const mapLoading = ref(false)
const trendSeries = ref([])
const trendDates = ref([])
const trendLoading = ref(false)
const selectedPoint = ref(null)
const customBbox = ref(null)

const legendLabels = ['<16°C', '16-20°C', '20-24°C', '24-28°C', '>28°C']

function todayStr() {
  const d = new Date()
  return d.toISOString().slice(0, 10)
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
      dataType: 'SST',
      forecastDate: filterDate.value || todayStr(),
      precision: 0.05,
      ...buildBboxParams()
    }
    const res = await getMapGrid(params)
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
    const res = await getPointTrend({ dataType: 'SST', lon, lat })
    const points = res.data || []
    trendDates.value = points.map(p => p.forecastDate)
    trendSeries.value = [{
      name: `(${Number(lon).toFixed(2)}, ${Number(lat).toFixed(2)})`,
      data: points.map(p => p.value)
    }]
    selectedPoint.value = { lon: Number(lon), lat: Number(lat) }
  } finally {
    trendLoading.value = false
  }
}

function onMapCellClick({ lat, lon }) {
  fetchTrendData(lon, lat)
}

function onBboxChange(bbox) {
  customBbox.value = bbox
}

async function loadSeaAreas() {
  try {
    const res = await getSeaAreas()
    seaAreas.value = res.data || []
  } catch (e) { /* empty */ }
}

async function handleSearch() {
  await fetchGridData()
}

function handleReset() {
  filterDate.value = ''
  seaArea.value = null
  customBbox.value = null
  fetchGridData()
}

function onSeaAreaChange() {
  customBbox.value = null
}

onMounted(async () => {
  filterDate.value = todayStr()
  await loadSeaAreas()
  await fetchGridData()
})
</script>

<style scoped>
.sst-page { padding: 0; }
.page-title { margin-bottom: 20px; color: #1a3a5c; font-size: 22px; }
.filter-card { margin-bottom: 16px; }
.filter-bar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.filter-label { font-weight: 600; color: #1a3a5c; }
.filter-hint { font-size: 12px; color: #999; margin-left: auto; }
.map-card { margin-bottom: 16px; }
.trend-card { margin-bottom: 16px; }
.trend-header { display: flex; align-items: center; justify-content: space-between; }
.trend-title { font-weight: 600; color: #1a3a5c; font-size: 16px; }
.trend-subtitle { font-size: 13px; color: #409EFF; }
</style>
