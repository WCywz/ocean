<template>
  <div>
    <h1 class="editorial-page-title">系统仪表盘</h1>
    <p class="editorial-page-subtitle">System Dashboard · {{ todayStr }}</p>

    <!-- Row 0: Stat Cards -->
    <div class="dashboard-row" @click="goModel">
      <StatCards
        :modelCount="data.modelCount"
        :runningModelCount="data.runningModelCount"
        :todayRecordCount="data.todayRecordCount"
        :alertCount="data.alertCount"
        @navigate="goModel"
      />
    </div>

    <!-- Row 1: Zone Health -->
    <div class="dashboard-row" style="cursor: pointer;" @click="goHealth">
      <p class="editorial-section-label">Feature · 区域健康评估</p>
      <h3 class="editorial-section-heading" style="margin: 0 0 4px 0;">东海海洋健康指数</h3>
      <div class="health-status-bar" :style="{ borderLeftColor: statusColor }">
        <span class="health-status-bar__level">{{ statusLabel }}</span>
        <span class="health-status-bar__dot">&middot;</span>
        <span class="health-status-bar__desc">{{ healthSummary }}</span>
        <span class="health-status-bar__hint">点击查看详情 →</span>
      </div>
      <div v-loading="healthLoading" class="dashboard-health-grid">
        <div
          v-for="zone in assessments"
          :key="zone.id"
          class="dh-card"
          :style="{ borderLeftColor: zone.overall.color }"
        >
          <div class="dh-card__label">{{ zone.label }}</div>
          <div class="dh-card__body">
            <span class="dh-card__level" :class="{ 'dh-card__level--warn': zone.overall.level === 'warn', 'dh-card__level--bad': zone.overall.level === 'bad' }">{{ levelText[zone.overall.level] }}</span>
            <span class="dh-card__hint">&ensp;&middot;&ensp;{{ primaryConcern(zone) }}</span>
          </div>
          <div class="dh-card__tags">
            <span>SST {{ trendSymbol(zone.sst.trend) }}</span>
            <span>Chl {{ trendSymbol(zone.chl.trend) }}</span>
            <span>热浪 {{ zone.heatwave.active ? '有' : '无' }}</span>
          </div>
        </div>
      </div>
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
    <div class="dashboard-row dashboard-row--last" style="display: flex; gap: 40px; align-items: flex-start;">
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
import { getDashboard, getDashboardTrend, getAlerts } from '../../api/forecast'
import { getSystemDate } from '../../api/system'
import { getZoneHealth } from '../../api/health'
import { buildZoneAssessment, buildOverallSummary } from '../../utils/health-assessment'
import StatCards from './StatCards.vue'
import TrendCard from './TrendCard.vue'
import AlertPanel from './AlertPanel.vue'
import LatestDataTable from './LatestDataTable.vue'

const router = useRouter()

// ---- zone health ----
const assessments = ref([])
const healthLoading = ref(false)
const levelText = { good: '优良', fine: '良好', warn: '中等', bad: '较差' }

const healthSummary = computed(() => {
  if (!assessments.value.length) return '加载中...'
  return buildOverallSummary(assessments.value)
})

const statusColor = computed(() => {
  if (!assessments.value.length) return '#22c55e'
  const order = ['good', 'fine', 'warn', 'bad']
  const worst = assessments.value.reduce((w, a) => {
    return order.indexOf(a.overall.level) > order.indexOf(w) ? a.overall.level : w
  }, 'good')
  const colors = { good: '#22c55e', fine: '#22c55e', warn: '#f59e0b', bad: '#ef4444' }
  return colors[worst]
})

const statusLabel = computed(() => {
  if (!assessments.value.length) return '--'
  const order = ['good', 'fine', 'warn', 'bad']
  const worst = assessments.value.reduce((w, a) => {
    return order.indexOf(a.overall.level) > order.indexOf(w) ? a.overall.level : w
  }, 'good')
  return levelText[worst]
})

function trendSymbol(trend) {
  if (trend === 'rising') return '↑'
  if (trend === 'falling') return '↓'
  return '→'
}

function primaryConcern(zone) {
  if (zone.heatwave.active) return '海洋热浪活跃'
  if (zone.sst.level === 'bad' || zone.sst.level === 'warn') return `SST 偏高 ${zone.sst.anomaly.toFixed(1)}°C`
  if (zone.chl.level === 'bad' || zone.chl.level === 'warn') return `Chl ${zone.chl.value.toFixed(1)} mg/m³`
  return '各项正常'
}

async function fetchHealth() {
  healthLoading.value = true
  try {
    const res = await getZoneHealth({
      centerLon: 122.5,
      centerLat: 29.5,
      coastLon: 121.5,
      forecastDate: systemDate.value
    })
    assessments.value = (res.data && res.data.zones || []).map(buildZoneAssessment)
  } catch (e) {
    console.error('Failed to fetch zone health', e)
  } finally {
    healthLoading.value = false
  }
}

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

const systemDate = ref('')
const todayStr = computed(() => {
  if (!systemDate.value) return ''
  return new Date(systemDate.value).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
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
    const res = await getAlerts(systemDate.value)
    alerts.value = res.data
  } finally {
    loading.alerts = false
  }
}

function goModel() { router.push('/app/model') }
function goSst() { router.push('/app/forecast/sst') }
function goChl() { router.push('/app/forecast/chl') }
function goOceanData() { router.push('/app/ocean-data') }
function goHealth() { router.push('/app/ocean-health') }

onMounted(async () => {
  const res = await getSystemDate()
  systemDate.value = res.data
  fetchHealth()
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
  border-bottom: 2px solid var(--color-divider-strong);
}
.dashboard-row--last {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

/* ---- zone health ---- */
.health-status-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  border-left: 3px solid;
  padding: 10px 14px;
  background: var(--color-surface);
  font-size: 13px;
  margin-bottom: 16px;
}

.health-status-bar__level {
  font-family: var(--font-serif);
  font-size: 15px;
  color: var(--color-text);
}

.health-status-bar__dot {
  color: var(--color-text-muted);
}

.health-status-bar__desc {
  color: var(--color-text-secondary);
  flex: 1;
}

.health-status-bar__hint {
  font-size: 11px;
  color: var(--color-text-muted);
}

.dashboard-health-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  row-gap: 24px;
  align-items: start;
}

.dh-card {
  background: var(--color-bg);
  padding: 14px 16px;
  border-top: 1px solid var(--color-divider);
  border-right: 1px solid var(--color-divider);
  border-bottom: 1px solid var(--color-divider);
  border-left: 3px solid;
}

.dh-card__label {
  font-size: 11px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 4px;
}

.dh-card__body {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
}

.dh-card__level {
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 400;
  color: var(--color-text);
}

.dh-card__level--warn {
  color: #d29922;
}

.dh-card__level--bad {
  color: var(--color-alert);
}

.dh-card__hint {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.dh-card__tags {
  display: flex;
  gap: 14px;
  padding-top: 8px;
  border-top: 1px solid var(--color-divider);
  font-size: 11px;
  color: var(--color-text-muted);
}

</style>
