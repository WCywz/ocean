<template>
  <div>
    <h1 class="editorial-page-title">系统仪表盘</h1>
    <p class="editorial-page-subtitle">System Dashboard · {{ todayStr }}</p>

    <!-- Row 1: Stat Cards -->
    <div class="dashboard-row" @click="goModel">
      <StatCards
        :modelCount="data.modelCount"
        :runningModelCount="data.runningModelCount"
        :todayRecordCount="data.todayRecordCount"
        :alertCount="data.alertCount"
        @navigate="goModel"
      />
    </div>

    <!-- Row 2: Trend Charts -->
    <div class="dashboard-row" style="display: flex; gap: 40px;">
      <div style="flex: 1;">
        <TrendCard
          title="海表温度 SST"
          dataType="SST"
          :series="sstTrend"
          :loading="loading.trendSst"
          @navigate="goSst"
        />
      </div>
      <div style="flex: 1;">
        <TrendCard
          title="叶绿素浓度 CHL"
          dataType="CHL"
          :series="chlTrend"
          :loading="loading.trendChl"
          @navigate="goChl"
        />
      </div>
    </div>

    <!-- Row 3: Alerts + Data Tables -->
    <div class="dashboard-row dashboard-row--last" style="display: flex; gap: 40px;">
      <div style="flex: 1;">
        <AlertPanel :alerts="alerts" :loading="loading.alerts" />
      </div>
      <div style="flex: 2; display: flex; flex-direction: column; gap: 24px;">
        <LatestDataTable
          title="最新海表温度 (SST)"
          dataType="SST"
          :data="data.latestSstData"
          :loading="loading.dashboard"
          @navigate="goOceanData"
        />
        <LatestDataTable
          title="最新叶绿素浓度 (CHL)"
          dataType="CHL"
          :data="data.latestChlData"
          :loading="loading.dashboard"
          @navigate="goOceanData"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboard, getDashboardTrend, getTodayAlerts } from '../../api/forecast'
import StatCards from './StatCards.vue'
import TrendCard from './TrendCard.vue'
import AlertPanel from './AlertPanel.vue'
import LatestDataTable from './LatestDataTable.vue'

const router = useRouter()

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

const loading = reactive({
  dashboard: false,
  trendSst: false,
  trendChl: false,
  alerts: false
})

const todayStr = computed(() => {
  const d = new Date()
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
  return `${months[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`
})

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

function goModel() { router.push('/app/model') }
function goSst() { router.push('/app/forecast/sst') }
function goChl() { router.push('/app/forecast/chl') }
function goOceanData() { router.push('/app/ocean-data') }

onMounted(() => {
  fetchDashboard()
  fetchTrend('SST')
  fetchTrend('CHL')
  fetchAlerts()
})
</script>

<style scoped>
.dashboard-row {
  padding-bottom: 28px;
  margin-bottom: 32px;
  border-bottom: 2px solid #e0e0e0;
}
.dashboard-row--last {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}
</style>
