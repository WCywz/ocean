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
        <span class="section-jump" @click.stop="goHealth">海洋健康 &rarr;</span>
      </div>
      <div v-loading="healthLoading" class="dashboard-health-grid">
        <div
          v-for="zone in assessments"
          :key="zone.id"
          class="dh-card"
          :style="{ borderLeftColor: zone.overall.color }"
        >
          <div class="dh-card__label">{{ zone.label }}</div>

          <div class="dh-card__trend">
            <span class="dh-card__trend-label">近5日</span>
            <span class="dh-card__dots">
              <span
                v-for="(d, i) in zone.recent"
                :key="i"
                class="dh-card__dot"
                :style="{ background: dotColor(d.overallGrade) }"
              ></span>
            </span>
            <span class="dh-card__trend-text">{{ zone.trendText }}</span>
          </div>

          <div class="dh-card__body">
            <span class="dh-card__level" :class="{ 'dh-card__level--warn': zone.overall.level === 'warn', 'dh-card__level--bad': zone.overall.level === 'bad' }">{{ levelText[zone.overall.level] }}</span>
            <span class="dh-card__hint">&ensp;&middot;&ensp;{{ primaryConcern(zone) }}</span>
          </div>

          <div class="dh-card__metrics">
            <span>SST 异常 {{ fmtAnomaly(zone.sst.anomaly) }}</span>
            <span>Chl {{ zone.chl.value != null ? zone.chl.value.toFixed(1) : '--' }} mg/m³</span>
            <span>热浪 {{ zone.heatwave.active ? '有' : '无' }}</span>
          </div>

          <div class="dh-card__tomorrow">
            明日 预计 {{ levelText[zone.forecast[0]?.overallGrade || zone.overall.level] }}
          </div>
        </div>
      </div>
    </div>

    <!-- Row 2: Trend Charts -->
    <div class="dashboard-row">
      <p class="editorial-section-label">Trends</p>
      <h3 class="editorial-section-heading" style="margin: 0 0 12px 0;">近期趋势</h3>
      <div style="display: flex; gap: 40px;">
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
    </div>

    <!-- Row 3: Point Monitoring + Data Tables -->
    <div class="dashboard-row dashboard-row--last" style="display: flex; gap: 40px; align-items: flex-start;">
      <div style="flex: 1.3;">
        <div class="editorial-section">
          <div style="display: flex; align-items: baseline; justify-content: space-between;">
            <div>
              <p class="editorial-section-label">Monitoring</p>
              <h3 class="editorial-section-heading">点位监测</h3>
            </div>
            <span class="section-jump" @click="goSst">查看点位 &rarr;</span>
          </div>
          <div class="alert-summary-bar" @click="goHealth">{{ alertSummary }}</div>
          <div v-loading="loading.alertMap">
            <p class="list-label">监测站点</p>
            <div
              v-for="s in alertStations"
              :key="s.stationName"
              class="point-item"
              @click="goHealth"
            >
              <span class="point-dot" :style="{ background: gradeColor(s.overallGrade) }"></span>
              <span class="point-name">{{ s.stationName }}</span>
              <span class="point-meta">{{ gradeLabel[s.overallGrade] }} · SST {{ fmtAnomaly(s.sstAnomaly) }}</span>
            </div>

            <template v-if="alertHotspots.length">
              <p class="list-label" style="margin-top: 12px;">聚焦点位</p>
              <div
                v-for="(h, i) in alertHotspots"
                :key="'h-' + i"
                class="point-item"
                @click="goHealth"
              >
                <span class="point-dot point-dot--pulse" :style="{ background: gradeColor(h.grade) }"></span>
                <span class="point-name">{{ h.lat.toFixed(2) }}°, {{ h.lon.toFixed(2) }}°</span>
                <span class="point-meta">{{ fmtAnomaly(h.anomaly) }}</span>
              </div>
            </template>

            <div v-if="!alertStations.length && !loading.alertMap" class="dh-empty">暂无数据</div>
          </div>
        </div>
      </div>
      <div style="flex: 1.7; display: flex; flex-direction: column; gap: 24px;">
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
import { getDashboard, getDashboardTrend } from '../../api/forecast'
import { getSystemDate } from '../../api/system'
import { getZoneHealthV2, getAlertMap } from '../../api/health'
import { buildZoneAssessment, buildOverallSummary } from '../../utils/health-assessment'
import StatCards from './StatCards.vue'
import TrendCard from './TrendCard.vue'
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

function dotColor(grade) {
  const colors = { good: '#22c55e', fine: '#22c55e', warn: '#f59e0b', bad: '#ef4444' }
  return colors[grade || 'good'] || '#22c55e'
}

function primaryConcern(zone) {
  if (zone.heatwave.active) return '海洋热浪活跃'
  if (zone.sst.level === 'bad' || zone.sst.level === 'warn') return 'SST 偏高 ' + (zone.sst.anomaly != null ? zone.sst.anomaly.toFixed(1) + '°C' : '')
  if (zone.chl.level === 'bad' || zone.chl.level === 'warn') return 'Chl ' + (zone.chl.value != null ? zone.chl.value.toFixed(1) + ' mg/m³' : '偏高')
  return '各项正常'
}

async function fetchHealth() {
  healthLoading.value = true
  try {
    const res = await getZoneHealthV2({ date: systemDate.value, lookback: 5, lookahead: 3 })
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
  latestSstData: [],
  latestChlData: []
})

const sstTrend = ref([])
const chlTrend = ref([])
const alertStations = ref([])
const alertHotspots = ref([])
const alertSummary = ref('')

const gradeColor = (g) => ({ good: '#22c55e', fine: '#22c55e', warn: '#f59e0b', bad: '#ef4444' }[g || 'good'])
const gradeLabel = { good: '优', fine: '良', warn: '中', bad: '差' }

function fmtAnomaly(val) {
  if (val == null) return '--'
  const sign = val > 0 ? '+' : ''
  return sign + val.toFixed(1) + '°C'
}

const loading = reactive({
  dashboard: false,
  trendSst: false,
  trendChl: false,
  alertMap: false
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

async function fetchAlertMap() {
  loading.alertMap = true
  try {
    const res = await getAlertMap({ date: systemDate.value })
    alertStations.value = (res.data && res.data.stations) || []
    alertHotspots.value = (res.data && res.data.hotspots) || []
    alertSummary.value = (res.data && res.data.summary) || ''
  } catch (e) {
    console.error('Failed to fetch alert map', e)
  } finally {
    loading.alertMap = false
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
  fetchAlertMap()
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
  margin-bottom: 4px;
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

/* ---- trend strip ---- */
.dh-card__trend {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.dh-card__trend-label {
  font-size: 10px;
  color: var(--color-text-muted);
  letter-spacing: 0.05em;
}

.dh-card__dots {
  display: flex;
  gap: 3px;
}

.dh-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.dh-card__trend-text {
  font-size: 11px;
  color: var(--color-text-secondary);
  line-height: 1.3;
}

.dh-card__metrics {
  display: flex;
  gap: 12px;
  padding: 6px 0 4px 0;
  font-size: 11px;
  color: var(--color-text-muted);
}

.dh-card__tomorrow {
  font-size: 11px;
  color: var(--color-text-muted);
  padding-top: 4px;
  border-top: 1px solid var(--color-divider);
}

.dh-card__tags {
  display: flex;
  gap: 14px;
  padding-top: 8px;
  border-top: 1px solid var(--color-divider);
  font-size: 11px;
  color: var(--color-text-muted);
}

/* ---- point monitoring ---- */
.alert-summary-bar {
  display: flex;
  align-items: center;
  border-left: 3px solid #f59e0b;
  padding: 12px 16px;
  background: var(--color-surface);
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
  cursor: pointer;
  user-select: none;
}

.list-label {
  font-size: 11px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin: 0 0 8px 0;
}

.point-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  cursor: pointer;
  transition: background 0.15s;
  border-left: 3px solid transparent;
}

.point-item:hover {
  background: var(--color-surface);
  border-left-color: var(--color-text);
}

.point-dot {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  flex-shrink: 0;
}

.point-dot--pulse {
  width: 13px;
  height: 13px;
  box-shadow: 0 0 6px currentColor;
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
  font-size: 12px;
  color: var(--color-text-muted);
  white-space: nowrap;
}

.dh-empty {
  text-align: center;
  padding: 40px 0;
  color: var(--color-text-muted);
  font-size: 13px;
}

.section-jump {
  font-size: 12px;
  color: var(--color-text-muted);
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.15s;
}

.section-jump:hover {
  color: var(--color-text);
}

</style>
