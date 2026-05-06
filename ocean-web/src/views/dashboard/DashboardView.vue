<template>
  <div>
    <h1 class="editorial-page-title">系统仪表盘</h1>
    <p class="editorial-page-subtitle">System Dashboard · {{ todayStr }}</p>

    <StatCards
      :modelCount="data.modelCount"
      :runningModelCount="data.runningModelCount"
      :todayRecordCount="data.todayRecordCount"
      :alertCount="data.alertCount"
    />

    <div style="display: flex; gap: 40px;">
      <div style="flex: 1;">
        <TrendCard
          title="海表温度 SST"
          dataType="SST"
          :series="sstTrend"
          :loading="loading.trendSst"
        />
      </div>
      <div style="flex: 1;">
        <TrendCard
          title="叶绿素浓度 CHL"
          dataType="CHL"
          :series="chlTrend"
          :loading="loading.trendChl"
        />
      </div>
    </div>

    <div class="editorial-section">
      <p class="editorial-section-label">Interactive</p>
      <h3 class="editorial-section-heading">预报栅格地图</h3>
      <div style="display: flex; gap: 40px;">
        <div style="flex: 2; min-width: 0;">
          <DashboardMap
            :gridData="mapGridData"
            :colorRanges="mapColorRanges"
            :legendLabels="mapLegendLabels"
            :legendTitle="mapLegendTitle"
            :loading="loading.map"
            :activeType="mapType"
            @typeChange="onMapTypeChange"
            @cellClick="onMapCellClick"
          />
        </div>
        <div style="flex: 1;">
          <AlertPanel :alerts="alerts" :loading="loading.alerts" />
        </div>
      </div>
    </div>

    <div style="display: flex; gap: 40px;">
      <div style="flex: 1;">
        <LatestDataTable
          title="最新海表温度 (SST)"
          dataType="SST"
          :data="data.latestSstData"
          :loading="loading.dashboard"
        />
      </div>
      <div style="flex: 1;">
        <LatestDataTable
          title="最新叶绿素浓度 (CHL)"
          dataType="CHL"
          :data="data.latestChlData"
          :loading="loading.dashboard"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { getDashboard, getDashboardTrend, getTodayAlerts, getMapGrid } from '../../api/forecast'
import { SST_MAP_COLORS, CHL_CONC_COLORS } from '../../utils/chart-config'
import StatCards from './StatCards.vue'
import TrendCard from './TrendCard.vue'
import DashboardMap from './DashboardMap.vue'
import AlertPanel from './AlertPanel.vue'
import LatestDataTable from './LatestDataTable.vue'

const data = ref({
  modelCount: 0,
  runningModelCount: 0,
  todayRecordCount: 0,
  alertCount: 0,
  latestSstData: [],
  latestChlData: []
})

const sstTrend = ref([])
const chlTrend = ref([])
const alerts = ref([])
const mapGridData = ref([])
const mapType = ref('SST')

const loading = reactive({
  dashboard: false,
  trendSst: false,
  trendChl: false,
  alerts: false,
  map: false
})

const todayStr = computed(() => {
  const d = new Date()
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
  return `${months[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`
})

const mapColorRanges = ref(SST_MAP_COLORS)
const mapLegendLabels = ref(SST_MAP_COLORS.map(r => r.label))
const mapLegendTitle = ref('SST 温度 (°C)')

async function fetchDashboard() {
  loading.dashboard = true
  try {
    const res = await getDashboard()
    data.value = res.data
  } finally {
    loading.dashboard = false
  }
}

async function fetchTrend(dataType) {
  const key = dataType === 'SST' ? 'trendSst' : 'trendChl'
  loading[key] = true
  try {
    const res = await getDashboardTrend(dataType, 7)
    if (dataType === 'SST') sstTrend.value = res.data
    else chlTrend.value = res.data
  } finally {
    loading[key] = false
  }
}

async function fetchAlerts() {
  loading.alerts = true
  try {
    const res = await getTodayAlerts()
    alerts.value = res.data
  } finally {
    loading.alerts = false
  }
}

async function fetchMapData() {
  loading.map = true
  try {
    const colorRanges = mapType.value === 'SST' ? SST_MAP_COLORS : CHL_CONC_COLORS
    const res = await getMapGrid({
      dataType: mapType.value,
      forecastDate: new Date().toISOString().slice(0, 10),
      precision: 0.05,
      minLon: 121.33, maxLon: 125.58,
      minLat: 26.92, maxLat: 32.67
    })
    mapGridData.value = (res.data || []).map(r => ({ lat: r.gridLat, lon: r.gridLon, value: r.value }))
    mapColorRanges.value = colorRanges
    mapLegendLabels.value = colorRanges.map(r => r.label)
    mapLegendTitle.value = mapType.value === 'SST' ? 'SST 温度 (°C)' : 'CHL 浓度 (mg/m³)'
  } finally {
    loading.map = false
  }
}

function onMapTypeChange(type) {
  mapType.value = type
  fetchMapData()
}

function onMapCellClick() {
  const route = mapType.value === 'SST' ? '/app/forecast/sst' : '/app/forecast/chl'
  window.location.hash = '#' + route
}

onMounted(() => {
  fetchDashboard()
  fetchTrend('SST')
  fetchTrend('CHL')
  fetchAlerts()
  fetchMapData()
})
</script>

<style scoped>
/* uses editorial CSS classes from editorial.css */
</style>
