<template>
  <div>
    <h1 class="editorial-page-title">叶绿素预测</h1>
    <p class="editorial-page-subtitle">Chlorophyll Concentration Forecast</p>

    <div class="editorial-section" style="padding-bottom: 20px; margin-bottom: 20px;">
      <div class="editorial-filter-bar">
        <span class="editorial-form-label" style="margin: 0 8px 0 0;">数据筛选</span>
        <el-radio-group v-model="chlMode" @change="onModeChange" size="small">
          <el-radio-button value="concentration">浓度值</el-radio-button>
          <el-radio-button value="probability">超阈值概率</el-radio-button>
        </el-radio-group>
        <el-date-picker v-if="chlMode === 'concentration'" v-model="filterDate" type="date" placeholder="选择预报日期" value-format="YYYY-MM-DD" :default-value="systemDate ? new Date(systemDate) : undefined" style="width: 180px" />
        <template v-if="chlMode === 'probability'">
          <el-input-number v-model="probDays" :min="1" :max="90" style="width: 140px" />
          <span style="color: var(--color-text-secondary); font-size: 13px;">天</span>
          <el-input-number v-model="threshold" :min="0.1" :step="0.5" :precision="1" style="width: 140px" />
          <span style="color: var(--color-text-secondary); font-size: 13px;">阈值 mg/m³</span>
        </template>
        <el-select v-model="seaArea" placeholder="海域筛选" style="width: 160px" @change="onSeaAreaChange">
          <el-option label="全部海域" value="" />
          <el-option v-for="area in seaAreas" :key="area.name" :label="area.name" :value="area.name" />
        </el-select>
        <button class="editorial-btn-outline" @click="handleSearch">查询</button>
        <button class="editorial-btn-outline" @click="handleReset">重置</button>
      </div>
    </div>

    <div class="editorial-section">
      <p class="editorial-section-label">Interactive</p>
      <h3 class="editorial-section-heading">预报热力地图</h3>
      <OceanMap
        :grid-data="gridData"
        :color-ranges="currentColorRanges"
        :legend-labels="currentLegendLabels"
        :legend-title="chlMode === 'concentration' ? '浓度 (mg/m³)' : '概率 (%)'"
        :loading="mapLoading"
        @bbox-change="onBboxChange"
        @grid-click="onGridClick"
      />
    </div>

    <div class="editorial-section" style="border-bottom: none;">
      <p class="editorial-section-label">Feature · 趋势分析</p>
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;">
        <h3 class="editorial-section-heading" style="margin: 0;">{{ chlMode === 'concentration' ? '叶绿素浓度变化趋势' : '趋势' }}</h3>
      </div>
      <TrendChart
        :series-data="trendSeries"
        :x-axis-data="trendDates"
        :y-axis-name="chlMode === 'concentration' ? '浓度 (mg/m³)' : '值'"
        :y-axis-unit="chlMode === 'concentration' ? 'mg/m³' : ''"
        :loading="trendLoading"
        :colors="CHL_COLORS"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import OceanMap from '../../components/OceanMap.vue'
import TrendChart from '../../components/TrendChart.vue'
import { getMapGrid, getPointTrend, getSeaAreas } from '../../api/forecast'
import { getSystemDate } from '../../api/system'
import { CHL_CONC_COLORS, CHL_PROB_COLORS, CHL_COLORS } from '../../utils/chart-config'

const chlMode = ref('concentration')
const filterDate = ref('')
const systemDate = ref('')
const probDays = ref(7)
const threshold = ref(3.0)
const seaArea = ref('')
const seaAreas = ref([])
const gridData = ref([])
const mapLoading = ref(false)
const trendSeries = ref([])
const trendDates = ref([])
const trendLoading = ref(false)
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
  return systemDate.value
}

function defaultForecastDate() {
  const d = new Date(systemDate.value)
  d.setDate(d.getDate() + 1)
  return d.toISOString().slice(0, 10)
}

function pastDate(days) {
  const d = new Date(systemDate.value)
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
      dataType: 'CHL',
      precision: 0.05,
      chlMode: chlMode.value,
      ...buildBboxParams()
    }
    if (chlMode.value === 'concentration') {
      params.forecastDate = filterDate.value || defaultForecastDate()
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
    const dateEnd = filterDate.value || todayStr()
    const res = await getPointTrend({ dataType: 'CHL', lon, lat, dateEnd })
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

function onBboxChange(bbox) { customBbox.value = bbox }

function onGridClick({ lon, lat }) {
  fetchTrendData(lon, lat)
}

async function loadSeaAreas() {
  try {
    const res = await getSeaAreas()
    seaAreas.value = res.data || []
  } catch (e) { /* empty */ }
}

function onModeChange() {
  customBbox.value = null
  if (chlMode.value === 'concentration') {
    filterDate.value = defaultForecastDate()
  }
  fetchGridData()
}

async function handleSearch() { await fetchGridData() }

function handleReset() {
  filterDate.value = ''
  probDays.value = 7
  threshold.value = 3.0
  seaArea.value = ''
  customBbox.value = null
  fetchGridData()
}

function onSeaAreaChange() { customBbox.value = null }

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
