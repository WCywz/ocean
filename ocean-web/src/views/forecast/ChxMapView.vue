<template>
  <div class="chl-page">
    <h2 class="page-title">叶绿素预测</h2>

    <!-- Filter bar -->
    <el-card shadow="hover" class="filter-card">
      <div class="filter-bar">
        <span class="filter-label">数据筛选</span>

        <!-- Mode toggle -->
        <div class="mode-toggle">
          <el-radio-group v-model="chlMode" @change="onModeChange" size="small">
            <el-radio-button value="concentration">浓度值</el-radio-button>
            <el-radio-button value="probability">超阈值概率</el-radio-button>
          </el-radio-group>
        </div>

        <el-date-picker
          v-if="chlMode === 'concentration'"
          v-model="filterDate"
          type="date"
          placeholder="选择预报日期"
          value-format="YYYY-MM-DD"
          style="width: 180px"
        />

        <template v-if="chlMode === 'probability'">
          <el-input-number v-model="probDays" :min="1" :max="90" style="width: 140px" />
          <span style="color: #666; font-size: 13px;">天</span>
          <el-input-number v-model="threshold" :min="0.1" :step="0.5" :precision="1" style="width: 140px" />
          <span style="color: #666; font-size: 13px;">阈值 mg/m³</span>
        </template>

        <el-select v-model="seaArea" placeholder="海域筛选" style="width: 160px" @change="onSeaAreaChange">
          <el-option v-for="area in seaAreas" :key="area.name" :label="area.name" :value="area" />
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
        :color-ranges="currentColorRanges"
        :legend-labels="currentLegendLabels"
        :legend-title="chlMode === 'concentration' ? '浓度 (mg/m³)' : '概率 (%)'"
        :loading="mapLoading"
        @cell-click="onMapCellClick"
        @bbox-change="onBboxChange"
      />
    </el-card>

    <!-- Trend chart -->
    <el-card shadow="hover" class="trend-card">
      <template #header>
        <div class="trend-header">
          <span class="trend-title">
            {{ chlMode === 'concentration' ? '叶绿素浓度变化趋势' : '趋势' }}
          </span>
          <span v-if="selectedPoint" class="trend-subtitle">
            选中: ({{ selectedPoint.lon.toFixed(2) }}, {{ selectedPoint.lat.toFixed(2) }})
          </span>
        </div>
      </template>
      <TrendChart
        :series-data="trendSeries"
        :x-axis-data="trendDates"
        :y-axis-name="chlMode === 'concentration' ? '浓度 (mg/m³)' : '值'"
        :y-axis-unit="chlMode === 'concentration' ? 'mg/m³' : ''"
        :loading="trendLoading"
        :colors="CHL_COLORS"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import OceanMap from '../../components/OceanMap.vue'
import TrendChart from '../../components/TrendChart.vue'
import { getMapGrid, getPointTrend, getSeaAreas } from '../../api/forecast'
import { CHL_CONC_COLORS, CHL_PROB_COLORS, CHL_COLORS } from '../../utils/chart-config'

const chlMode = ref('concentration')
const filterDate = ref('')
const probDays = ref(7)
const threshold = ref(3.0)
const seaArea = ref(null)
const seaAreas = ref([])
const gridData = ref([])
const mapLoading = ref(false)
const trendSeries = ref([])
const trendDates = ref([])
const trendLoading = ref(false)
const selectedPoint = ref(null)
const customBbox = ref(null)

const currentColorRanges = computed(() =>
  chlMode.value === 'concentration' ? CHL_CONC_COLORS : CHL_PROB_COLORS
)

const currentLegendLabels = computed(() =>
  chlMode.value === 'concentration'
    ? ['<0.5', '0.5-1.5', '1.5-3.0', '3.0-5.0', '>5.0 mg/m³']
    : ['<20%', '20-40%', '40-60%', '60-80%', '>80%']
)

function todayStr() {
  return new Date().toISOString().slice(0, 10)
}

function pastDate(days) {
  const d = new Date()
  d.setDate(d.getDate() - days)
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
      dataType: 'CHL',
      precision: 0.05,
      chlMode: chlMode.value,
      ...buildBboxParams()
    }
    if (chlMode.value === 'concentration') {
      params.forecastDate = filterDate.value || todayStr()
    } else {
      params.dateStart = pastDate(probDays.value)
      params.dateEnd = todayStr()
      params.threshold = threshold.value
    }
    const res = await getMapGrid(params)
    gridData.value = (res.data || []).map(r => ({
      lon: r.gridLon != null ? Number(r.gridLon) : Number(r.longitude),
      lat: r.gridLat != null ? Number(r.gridLat) : Number(r.latitude),
      value: r.value != null ? r.value : r.probability
    }))
  } finally {
    mapLoading.value = false
  }
}

async function fetchTrendData(lon, lat) {
  trendLoading.value = true
  try {
    const res = await getPointTrend({ dataType: 'CHL', lon, lat })
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

function onMapCellClick({ lat, lon }) { fetchTrendData(lon, lat) }
function onBboxChange(bbox) { customBbox.value = bbox }

async function loadSeaAreas() {
  try {
    const res = await getSeaAreas()
    seaAreas.value = res.data || []
  } catch (e) { /* empty */ }
}

function onModeChange() {
  customBbox.value = null
  if (chlMode.value === 'concentration') {
    filterDate.value = todayStr()
  }
  fetchGridData()
}

async function handleSearch() { await fetchGridData() }

function handleReset() {
  filterDate.value = todayStr()
  probDays.value = 7
  threshold.value = 3.0
  seaArea.value = null
  customBbox.value = null
  fetchGridData()
}

function onSeaAreaChange() { customBbox.value = null }

onMounted(async () => {
  filterDate.value = todayStr()
  await loadSeaAreas()
  await fetchGridData()
})
</script>

<style scoped>
.chl-page { padding: 0; }
.page-title { margin-bottom: 20px; color: #1a3a5c; font-size: 22px; }
.filter-card { margin-bottom: 16px; }
.filter-bar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.filter-label { font-weight: 600; color: #1a3a5c; }
.filter-hint { font-size: 12px; color: #999; margin-left: auto; }
.mode-toggle { margin-right: 4px; }
.map-card { margin-bottom: 16px; }
.trend-card { margin-bottom: 16px; }
.trend-header { display: flex; align-items: center; justify-content: space-between; }
.trend-title { font-weight: 600; color: #1a3a5c; font-size: 16px; }
.trend-subtitle { font-size: 13px; color: #409EFF; }
</style>
