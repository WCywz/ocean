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

    <HealthAlertSection :alerts="alerts" :loading="alertsLoading" />

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
          <div class="health-card__body">
            <span class="health-card__level" :class="{ 'health-card__level--warn': zone.overall.level === 'warn', 'health-card__level--bad': zone.overall.level === 'bad' }">{{ levelText[zone.overall.level] }}</span>
            <span class="health-card__hint">&ensp;&middot;&ensp;{{ primaryConcern(zone) }}</span>
          </div>
          <div class="health-card__tags">
            <span>SST {{ trendSymbol(zone.sst.trend) }}</span>
            <span>Chl {{ trendSymbol(zone.chl.trend) }}</span>
            <span>热浪 {{ zone.heatwave.active ? '有' : '无' }}</span>
          </div>

          <div v-if="selectedIds.has(zone.id)" class="health-card__detail" :style="{ borderColor: zone.overall.color }">
            <span class="editorial-section-label">Detail &middot; {{ zone.label }}</span>
            <p class="health-card__interpretation">{{ buildInterpretation(zone) }}</p>
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
                  <td>{{ zone.chl.value.toFixed(1) }} mg/m³</td>
                  <td><span class="level-badge" :style="{ background: zone.chl.color }">{{ zone.chl.label }}</span></td>
                </tr>
                <tr>
                  <td>海洋热浪</td>
                  <td>{{ zone.heatwave.active ? '已持续 ' + zone.heatwave.days + ' 天' : '未见异常' }}</td>
                  <td><span class="level-badge" :style="{ background: zone.heatwave.color }">{{ zone.heatwave.label }}</span></td>
                </tr>
              </tbody>
            </table>
            <div class="health-card__advice">
              <strong>建议：</strong>
              <ul>
                <li v-for="(item, i) in zone.advice" :key="i">{{ item }}</li>
              </ul>
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
import { getZoneHealth } from '../../api/health'
import { buildZoneAssessment, buildOverallSummary } from '../../utils/health-assessment'
import { getAlerts } from '../../api/forecast'
import { getSystemDate } from '../../api/system'
import HealthAlertSection from './HealthAlertSection.vue'

const forecastDate = ref('')
const loading = ref(false)
const rawData = ref(null)
const selectedIds = ref(new Set())
const assessments = ref([])
const alerts = ref([])
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

function trendSymbol(trend) {
  if (trend === 'rising') return '↑'
  if (trend === 'falling') return '↓'
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
  return `${sign}${val.toFixed(1)}°C`
}

function primaryConcern(zone) {
  if (zone.heatwave.active) return '海洋热浪活跃'
  if (zone.sst.level === 'bad' || zone.sst.level === 'warn') return `SST 偏高 ${zone.sst.anomaly.toFixed(1)}°C`
  if (zone.chl.level === 'bad' || zone.chl.level === 'warn') return `Chl ${zone.chl.value.toFixed(1)} mg/m³`
  return '各项正常'
}

function buildInterpretation(zone) {
  const parts = []
  parts.push(`${zone.label}海域`)
  if (zone.sst.anomaly && Math.abs(zone.sst.anomaly) > 0.1) {
    const sign = zone.sst.anomaly > 0 ? '偏高' : '偏低'
    parts.push(`SST 较常年同期${sign} ${Math.abs(zone.sst.anomaly).toFixed(1)}°C`)
  }
  if (zone.heatwave.active) {
    parts.push(`海洋热浪持续活跃，已维持 ${zone.heatwave.days} 天`)
  }
  if (zone.chl.level === 'bad' || zone.chl.level === 'warn') {
    parts.push('叶绿素浓度偏高，赤潮风险需关注')
  } else {
    parts.push('叶绿素浓度正常，暂无赤潮风险')
  }
  return parts.join('，') + '。'
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
    const res = await getZoneHealth({
      centerLon: 122.5,
      centerLat: 29.5,
      coastLon: 121.5,
      forecastDate: forecastDate.value
    })
    rawData.value = res.data
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
    const res = await getAlerts(forecastDate.value)
    alerts.value = res.data || []
  } catch (e) {
    console.error('Failed to fetch alerts', e)
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
  background: #fafafa;
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
  color: #666;
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
  border-bottom: 1px dashed #ccc;
  border-radius: 0;
}

/* ---- grid ---- */
.health-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  row-gap: 64px;
  align-items: start;
}

/* ---- card ---- */
.health-card {
  background: #fff;
  padding: 14px 16px;
  border-top: 1px solid #f0f0f0;
  border-right: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
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
  margin-bottom: 4px;
}

.health-card__body {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
}

.health-card__level {
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 400;
  color: var(--color-text);
}

.health-card__level--warn {
  color: #92400e;
}

.health-card__level--bad {
  color: var(--color-alert);
}

.health-card__hint {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.health-card__tags {
  display: flex;
  gap: 14px;
  padding-top: 8px;
  border-top: 1px solid #f5f5f5;
  font-size: 11px;
  color: var(--color-text-muted);
}

/* ---- detail panel ---- */
.health-card__detail {
  margin: 14px -16px -14px -16px;
  padding: 16px 20px;
}

.health-card__interpretation {
  font-size: 13px;
  color: #555;
  line-height: 1.8;
  margin: 6px 0 14px 0;
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

.health-card__advice {
  background: var(--color-surface);
  padding: 10px 12px;
  font-size: 12px;
}

.health-card__advice ul {
  margin: 4px 0 0;
  padding-left: 16px;
}

.health-card__advice li {
  margin-bottom: 4px;
  color: #555;
}

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

@media (max-width: 800px) {
  .health-grid {
    grid-template-columns: 1fr;
  }
}
</style>
