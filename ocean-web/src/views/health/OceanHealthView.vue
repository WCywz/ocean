<template>
  <div class="ocean-health">
    <h1 class="editorial-page-title">海洋健康指数</h1>
    <p class="editorial-page-subtitle">Ocean Health Index</p>

    <div class="health-status-bar" :style="{ borderLeftColor: statusColor }" @click="toggleAll">
      <span class="health-status-bar__level">{{ statusLabel }}</span>
      <span class="health-status-bar__dot">&middot;</span>
      <span class="health-status-bar__desc">{{ bannerText }}</span>
      <span class="health-status-bar__date" @click.stop>
        <el-date-picker
          v-model="forecastDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :default-value="forecastDate ? new Date(forecastDate) : undefined"
          @change="fetchData"
          :teleported="false"
          popper-class="health-date-popper"
        />
      </span>
    </div>

    <HealthAlertSection :stations="alertStations" :hotspots="alertHotspots" :summary="alertSummary" :loading="alertsLoading" />

    <p class="editorial-section-label">区域健康评估 &middot; 东海</p>

    <div v-loading="loading" class="health-grid">
      <template v-if="assessments.length">
        <div
          v-for="zone in assessments"
          :key="zone.id"
          :class="['health-card', { 'health-card--active': selectedIds.has(zone.id), 'health-card--dimmed': selectedIds.size > 0 && !selectedIds.has(zone.id) }]"
          :style="{ borderLeftColor: zone.overall.color }"
          @click="selectZone(zone.id)"
        >
          <div class="health-card__label">{{ zone.label }}</div>

          <!-- 收拢态摘要 -->
          <div v-if="!selectedIds.has(zone.id)" class="health-card__summary">
            <div class="health-card__trend">
              <span class="health-card__trend-label">近5日</span>
              <span class="health-card__dots">
                <span
                  v-for="(d, i) in zone.recent"
                  :key="i"
                  class="health-card__dot"
                  :style="{ background: dotColor(d.overallGrade) }"
                  :title="d.date + ' ' + levelText[d.overallGrade]"
                ></span>
              </span>
              <span class="health-card__trend-text">{{ zone.trendText }}</span>
            </div>

            <div class="health-card__metrics">
              <span>SST 异常 {{ fmtAnomaly(zone.sst.anomaly) }}</span>
              <span>Chl {{ zone.chl.value != null ? zone.chl.value.toFixed(1) : '--' }} mg/m³</span>
              <span>热浪 {{ zone.heatwave.active ? '有' : '无' }}</span>
            </div>

            <div class="health-card__tomorrow">
              明日 预计 {{ levelText[zone.forecast[0]?.overallGrade || zone.overall.level] }}
              <span v-if="zone.forecast[0]">{{ changeArrow(zone.overall.level, zone.forecast[0].overallGrade) }}</span>
            </div>
          </div>

          <div class="health-card__body">
            <span class="health-card__level" :class="{ 'health-card__level--warn': zone.overall.level === 'warn', 'health-card__level--bad': zone.overall.level === 'bad' }">{{ levelText[zone.overall.level] }}</span>
            <span v-if="!selectedIds.has(zone.id)" class="health-card__hint">&ensp;&middot;&ensp;{{ primaryConcern(zone) }}</span>
          </div>

          <!-- 展开态 -->
          <div v-if="selectedIds.has(zone.id)" class="health-card__detail" :style="{ borderColor: zone.overall.color }">

            <p class="health-card__narrative">{{ zone.trendNarrative }}</p>
            <p class="health-card__narrative">{{ zone.outlookNarrative }}</p>

            <table class="editorial-table health-detail-table">
              <thead>
                <tr>
                  <td>指标</td>
                  <td>当前值</td>
                  <td>等级</td>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>SST</td>
                  <td>{{ fmtTemp(zone.sst.value) }}（{{ fmtAnomaly(zone.sst.anomaly) }}）</td>
                  <td><span class="level-badge" :style="{ background: zone.sst.color }">{{ zone.sst.label }}</span></td>
                </tr>
                <tr>
                  <td>SST 趋势</td>
                  <td>{{ trendText(zone.sst.trend) }}</td>
                  <td class="text-muted">{{ zone.sst.level === 'bad' || zone.sst.level === 'warn' ? '关注' : '正常' }}</td>
                </tr>
                <tr>
                  <td>Chl 浓度</td>
                  <td>{{ zone.chl.value != null ? zone.chl.value.toFixed(1) + ' mg/m³' : '--' }}</td>
                  <td><span class="level-badge" :style="{ background: zone.chl.color }">{{ zone.chl.label }}</span></td>
                </tr>
                <tr>
                  <td>海洋热浪</td>
                  <td>{{ zone.heatwave.active ? '已持续 ' + zone.heatwave.days + ' 天' : '未见异常' }}</td>
                  <td><span class="level-badge" :style="{ background: zone.heatwave.color }">{{ zone.heatwave.label }}</span></td>
                </tr>
              </tbody>
            </table>

            <div class="health-card__forecast-strip">
              <span
                v-for="(f, i) in zone.forecast"
                :key="i"
                class="health-card__fc-day"
              >
                <span class="health-card__fc-date">{{ f.date.slice(5) }}</span>
                <span class="health-card__fc-grade" :style="{ color: dotColor(f.overallGrade) }">{{ levelText[f.overallGrade || 'good'] }}</span>
                <span v-if="i < zone.forecast.length - 1" class="health-card__fc-arrow">&rarr;</span>
              </span>
            </div>

            <div class="health-card__timeline">
              <span
                v-for="(d, i) in zone.recent"
                :key="'r' + i"
                class="health-card__tl-dot"
                :style="{ background: dotColor(d.overallGrade) }"
                :title="d.date + ' ' + levelText[d.overallGrade]"
              ></span>
              <span class="health-card__tl-divider"></span>
              <span
                v-for="(f, i) in zone.forecast"
                :key="'f' + i"
                class="health-card__tl-dot health-card__tl-dot--fc"
                :style="{ borderColor: dotColor(f.overallGrade) }"
                :title="f.date + ' 预报 ' + levelText[f.overallGrade || 'good']"
              ></span>
            </div>
          </div>
        </div>
      </template>
      <div v-else class="health-empty">暂无数据</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getZoneHealthV2, getAlertMap } from '../../api/health'
import { buildZoneAssessment, buildOverallSummary } from '../../utils/health-assessment'
import { getSystemDate } from '../../api/system'
import HealthAlertSection from './HealthAlertSection.vue'

const forecastDate = ref('')
const loading = ref(false)
const selectedIds = ref(new Set())
const assessments = ref([])
const alertStations = ref([])
const alertHotspots = ref([])
const alertSummary = ref('')
const alertsLoading = ref(false)

const levelText = { good: '优良', fine: '良好', warn: '中等', bad: '较差' }

const bannerText = computed(() => {
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

function changeArrow(currentGrade, nextGrade) {
  const order = ['good', 'fine', 'warn', 'bad']
  const cur = order.indexOf(currentGrade)
  const nxt = order.indexOf(nextGrade)
  if (nxt < cur) return '↑'
  if (nxt > cur) return '↓'
  return '→'
}

function trendText(trend) {
  if (trend === 'rising') return '上升'
  if (trend === 'falling') return '下降'
  return '平稳'
}

function fmtTemp(val) {
  return val != null ? val.toFixed(1) + '°C' : '--'
}

function fmtAnomaly(val) {
  if (val == null) return '--'
  const sign = val > 0 ? '+' : ''
  return sign + val.toFixed(1) + '°C'
}

function primaryConcern(zone) {
  if (zone.heatwave.active) return '海洋热浪活跃'
  if (zone.sst.level === 'bad' || zone.sst.level === 'warn') return 'SST 偏高 ' + (zone.sst.anomaly != null ? zone.sst.anomaly.toFixed(1) + '°C' : '')
  if (zone.chl.level === 'bad' || zone.chl.level === 'warn') return 'Chl ' + (zone.chl.value != null ? zone.chl.value.toFixed(1) + ' mg/m³' : '偏高')
  return '各项正常'
}

function selectZone(id) {
  const next = new Set(selectedIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  selectedIds.value = next
}

function toggleAll() {
  if (selectedIds.value.size === assessments.value.length) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(assessments.value.map(z => z.id))
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getZoneHealthV2({ date: forecastDate.value, lookback: 5, lookahead: 3 })
    assessments.value = (res.data && res.data.zones || []).map(buildZoneAssessment)
    selectedIds.value = new Set(assessments.value.map(z => z.id))
    fetchAlerts()
  } catch (e) {
    console.error('Failed to fetch zone health data', e)
  } finally {
    loading.value = false
  }
}

async function fetchAlerts() {
  alertsLoading.value = true
  try {
    const res = await getAlertMap({ date: forecastDate.value })
    alertStations.value = (res.data && res.data.stations) || []
    alertHotspots.value = (res.data && res.data.hotspots) || []
    alertSummary.value = (res.data && res.data.summary) || ''
  } catch (e) {
    console.error('Failed to fetch alert map', e)
  } finally {
    alertsLoading.value = false
  }
}

onMounted(async () => {
  const res = await getSystemDate()
  forecastDate.value = res.data
  fetchData()
})
</script>

<style scoped>
/* ---- status bar ---- */
.health-status-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  border-left: 3px solid;
  padding: 10px 14px;
  background: var(--color-surface);
  font-size: 13px;
  margin-bottom: 28px;
  cursor: pointer;
  user-select: none;
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

.health-status-bar__date {
  font-size: 11px;
  color: var(--color-text-muted);
  cursor: pointer;
  white-space: nowrap;
}

.health-status-bar__date :deep(.el-input__wrapper) {
  box-shadow: none;
  padding: 0;
  background: transparent;
  border-bottom: 1px dashed var(--color-border);
  border-radius: 0;
}

/* ---- grid ---- */
.health-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

/* ---- card ---- */
.health-card {
  background: var(--color-bg);
  padding: 14px 16px;
  border-top: 1px solid var(--color-divider);
  border-right: 1px solid var(--color-divider);
  border-bottom: 1px solid var(--color-divider);
  border-left: 3px solid;
  cursor: pointer;
  transition: opacity 0.2s;
}

.health-card--dimmed {
  opacity: 0.45;
}

.health-card__label {
  font-size: 11px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 6px;
}

/* ---- trend strip ---- */
.health-card__trend {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.health-card__trend-label {
  font-size: 10px;
  color: var(--color-text-muted);
  letter-spacing: 0.05em;
}

.health-card__dots {
  display: flex;
  gap: 3px;
}

.health-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.health-card__trend-text {
  font-size: 11px;
  color: var(--color-text-secondary);
  line-height: 1.3;
}

/* ---- body ---- */
.health-card__body {
  display: flex;
  align-items: baseline;
  margin-bottom: 4px;
}

.health-card__level {
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 400;
  color: var(--color-text);
}

.health-card__level--warn {
  color: #d29922;
}

.health-card__level--bad {
  color: var(--color-alert);
}

.health-card__hint {
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* ---- metrics ---- */
.health-card__metrics {
  display: flex;
  gap: 12px;
  padding: 6px 0 4px 0;
  font-size: 11px;
  color: var(--color-text-muted);
}

/* ---- tomorrow ---- */
.health-card__tomorrow {
  font-size: 11px;
  color: var(--color-text-muted);
  padding-bottom: 2px;
  border-bottom: 1px solid var(--color-divider);
}

/* ---- detail ---- */
.health-card__detail {
  margin: 10px -16px -14px -16px;
  padding: 16px 20px;
}

.health-card__narrative {
  font-size: 13px;
  line-height: 1.9;
  color: var(--color-text-secondary);
  margin: 0 0 10px 0;
}

.health-detail-table {
  margin-bottom: 12px;
}

.level-badge {
  color: #fff;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 11px;
}

/* ---- forecast strip ---- */
.health-card__forecast-strip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 8px 0 12px 0;
  color: var(--color-text-muted);
}

.health-card__fc-day {
  display: flex;
  align-items: center;
  gap: 3px;
}

.health-card__fc-date {
  color: var(--color-text-muted);
}

.health-card__fc-grade {
  font-weight: 500;
}

.health-card__fc-arrow {
  color: var(--color-text-muted);
  margin: 0 2px;
}

/* ---- timeline ---- */
.health-card__timeline {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-top: 8px;
  position: relative;
}

.health-card__tl-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}

.health-card__tl-dot--fc {
  background: transparent;
  border: 1.5px solid;
}

.health-card__tl-divider {
  width: 12px;
  height: 1px;
  background: var(--color-border);
  margin: 0 4px;
}

/* ---- empty ---- */
.health-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px;
  color: var(--color-text-muted);
  font-size: 14px;
}

.health-date-popper {
  font-family: var(--font-sans);
}

.text-muted {
  color: var(--color-text-muted);
}

@media (max-width: 800px) {
  .health-grid {
    grid-template-columns: 1fr;
  }
}
</style>
