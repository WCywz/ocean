<template>
  <div>
    <h1 class="editorial-page-title">海表温度预测</h1>
    <p class="editorial-page-subtitle">Sea Surface Temperature Forecast</p>

    <!-- Filter bar -->
    <div class="editorial-section" style="padding-bottom: 20px; margin-bottom: 20px;">
      <div class="editorial-filter-bar">
        <span class="editorial-form-label" style="margin: 0 8px 0 0;">数据筛选</span>
        <el-date-picker v-model="filterDate" type="date" placeholder="选择预报日期" value-format="YYYY-MM-DD" :default-value="systemDate ? new Date(systemDate) : undefined" style="width: 180px" />
        <el-select v-model="seaArea" placeholder="海域筛选" style="width: 160px" @change="onSeaAreaChange">
          <el-option label="全部海域" value="" />
          <el-option v-for="area in seaAreas" :key="area.name" :label="area.name" :value="area.name" />
        </el-select>
        <button class="editorial-btn-outline" @click="handleSearch">查询</button>
        <button class="editorial-btn-outline" @click="handleReset">重置</button>
        <span style="font-size: 12px; color: var(--color-text-muted); margin-left: auto;">也可在地图上拖拽框选海域</span>
      </div>
    </div>

    <!-- Map -->
    <div class="editorial-section">
      <p class="editorial-section-label">Interactive</p>
      <h3 class="editorial-section-heading">预报热力地图</h3>
      <OceanMap
        :grid-data="gridData"
        :color-ranges="SST_MAP_COLORS"
        :legend-labels="legendLabels"
        legend-title="温度 (°C)"
        :loading="mapLoading"
        @bbox-change="onBboxChange"
        @grid-click="onGridClick"
      />
    </div>

    <!-- Trend -->
    <div class="editorial-section" style="border-bottom: none;">
      <p class="editorial-section-label">Feature · 趋势分析</p>
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;">
        <h3 class="editorial-section-heading" style="margin: 0;">温度变化趋势</h3>

      </div>
      <TrendChart
        :series-data="trendSeries"
        :x-axis-data="trendDates"
        y-axis-name="温度 (°C)"
        y-axis-unit="°C"
        :loading="trendLoading"
        :colors="SST_COLORS"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import OceanMap from '../../components/OceanMap.vue'
import TrendChart from '../../components/TrendChart.vue'
import { getMapGrid, getPointTrend, getSeaAreas } from '../../api/forecast'
import { getSystemDate } from '../../api/system'
import { SST_MAP_COLORS, SST_COLORS } from '../../utils/chart-config'

const filterDate = ref('')
const systemDate = ref('')
const seaArea = ref('')
const seaAreas = ref([])
const gridData = ref([])
const mapLoading = ref(false)
const trendSeries = ref([])
const trendDates = ref([])
const trendLoading = ref(false)

const customBbox = ref(null)

function defaultForecastDate() {
  const d = new Date(systemDate.value)
  d.setDate(d.getDate() + 1)
  return d.toISOString().slice(0, 10)
}

const legendLabels = ['<10°C', '10-13°C', '13-16°C', '16-19°C', '19-22°C', '22-25°C', '25-28°C', '28-31°C', '31-34°C', '>34°C']

function buildBboxParams() {
  const params = {}
  if (customBbox.value) {
    params.minLon = customBbox.value.west
    params.maxLon = customBbox.value.east
    params.minLat = customBbox.value.south
    params.maxLat = customBbox.value.north
  } else if (seaArea.value) {
    const area = seaAreas.value.find(a => a.name === seaArea.value)
    if (area) {
      params.minLon = area.minLon
      params.maxLon = area.maxLon
      params.minLat = area.minLat
      params.maxLat = area.maxLat
    }
  }
  return params
}

async function fetchGridData() {
  mapLoading.value = true
  try {
    const params = {
      dataType: 'SST',
      forecastDate: filterDate.value || defaultForecastDate(),
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
    const dateEnd = filterDate.value || systemDate.value
    const res = await getPointTrend({ dataType: 'SST', lon, lat, dateEnd })
    const points = res.data || []
    trendDates.value = points.map(p => p.forecastDate)
    trendSeries.value = [{
      name: `(${Number(lon).toFixed(2)}, ${Number(lat).toFixed(2)})`,
      data: points.map(p => p.value)
    }]
  } finally {
    trendLoading.value = false
  }
}

function onBboxChange(bbox) {
  customBbox.value = bbox
}

function onGridClick({ lon, lat }) {
  fetchTrendData(lon, lat)
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
  seaArea.value = ''
  customBbox.value = null
  fetchGridData()
}

function onSeaAreaChange() {
  customBbox.value = null
}

onMounted(async () => {
  const res = await getSystemDate()
  systemDate.value = res.data
  filterDate.value = defaultForecastDate()
  await loadSeaAreas()
  await fetchGridData()
})
</script>

<style scoped>
/* uses editorial classes from editorial.css */
</style>
