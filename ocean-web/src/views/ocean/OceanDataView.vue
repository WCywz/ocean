<template>
  <div>
    <h1 class="editorial-page-title">海洋观测数据</h1>
    <p class="editorial-page-subtitle">Ocean Observation Data</p>

    <!-- Chl time series chart -->
    <div class="editorial-section">
      <p class="editorial-section-label">Feature · 时间序列</p>
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
        <h3 class="editorial-section-heading" style="margin: 0;">叶绿素浓度时间序列</h3>
        <div style="display: flex; align-items: center; gap: 8px;">
          <el-select
            v-model="chlLocations"
            placeholder="筛选观测点"
            multiple collapse-tags collapse-tags-tooltip filterable
            size="small" style="width: 280px"
            @change="renderChlTimeSeries"
          >
            <el-option v-for="loc in locationOptions" :key="loc.key" :label="loc.label" :value="loc.key" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange" range-separator="至"
            start-placeholder="开始" end-placeholder="结束"
            value-format="YYYY-MM-DD" size="small" style="width: 260px"
            @change="onDateRangeChange"
          />
          <el-button size="small" text @click="openFullscreen">
            <el-icon><FullScreen /></el-icon>
          </el-button>
        </div>
      </div>
      <div v-loading="chartLoading" class="chart-container" ref="timeSeriesChartRef">
        <div v-if="chartEmpty" style="display: flex; align-items: center; justify-content: center; height: 100%; color: var(--color-text-muted); font-size: 13px;">暂无符合条件的观测数据</div>
      </div>
    </div>

    <!-- Fullscreen modal unchanged -->
    <el-dialog v-model="fullscreenVisible" title="叶绿素浓度时间序列" fullscreen :close-on-click-modal="false" @opened="onFullscreenOpened" @close="onFullscreenClosed">
      <div ref="fullscreenChartRef" style="height: calc(100vh - 100px);"></div>
    </el-dialog>

    <!-- Data table -->
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
      <h3 class="editorial-section-heading" style="margin: 0;">观测数据记录</h3>
      <button class="editorial-btn-outline" @click="loadTableData">刷新</button>
    </div>
    <table class="editorial-table" v-loading="tableLoading">
      <thead>
        <tr>
          <td>日期</td><td>纬度</td><td>经度</td><td>深度(m)</td><td>叶绿素</td>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, idx) in tableData" :key="idx">
          <td>{{ row.time }}</td>
          <td>{{ row.lat }}</td>
          <td>{{ row.lon }}</td>
          <td>{{ row.depth }}</td>
          <td>{{ row.chl }}</td>
        </tr>
      </tbody>
    </table>
    <div class="editorial-pagination">
      <span>共 {{ tableTotal }} 条</span>
      <select v-model="tableQuery.pageSize" class="editorial-select" style="width: 80px;" @change="loadTableData">
        <option :value="10">10</option>
        <option :value="20">20</option>
        <option :value="50">50</option>
      </select>
      <a class="editorial-link" @click="tableQuery.pageNum > 1 && (tableQuery.pageNum--, loadTableData())">&larr;</a>
      <span class="editorial-pagination__page editorial-pagination__page--active">{{ tableQuery.pageNum }}</span>
      <a class="editorial-link" @click="tableQuery.pageNum++; loadTableData()">&rarr;</a>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getChlTimeSeries, getOceanDataPage, getOceanLocations } from '../../api/ocean-data'
import {
  CHL_COLORS,
  buildBaseOption, buildTooltipFormatter, buildSeriesData
} from '../../utils/chart-config'

function toLocalDateStr(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

// ---- location options ----
const locationOptions = ref([])

const dateRange = ref([
  toLocalDateStr(new Date(Date.now() - 7 * 86400000)),
  toLocalDateStr(new Date())
])

// ---- chart ----
const timeSeriesChartRef = ref(null)
const chlLocations = ref([])
const allChlData = ref([])
const chartLoading = ref(false)
const chartEmpty = ref(false)
let timeSeriesChart = null

// ---- fullscreen ----
const fullscreenVisible = ref(false)
const fullscreenChartRef = ref(null)
let fullscreenChart = null

// ---- table ----
const tableQuery = ref({ pageNum: 1, pageSize: 10 })
const tableData = ref([])
const tableTotal = ref(0)
const tableLoading = ref(false)

onMounted(() => {
  nextTick(async () => {
    timeSeriesChart = echarts.init(timeSeriesChartRef.value)

    await loadLocationOptions()
    chartLoading.value = true
    await fetchAllChlData()
    chartLoading.value = false
    renderChlTimeSeries()
    loadTableData()

    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  timeSeriesChart?.dispose()
  fullscreenChart?.dispose()
})

function handleResize() {
  timeSeriesChart?.resize()
  fullscreenChart?.resize()
}

async function loadLocationOptions() {
  try {
    const res = await getOceanLocations()
    locationOptions.value = (res.data || []).map(item => ({
      key: `${item.lon},${item.lat}`,
      label: `经度: ${item.lon}, 纬度: ${item.lat}`,
      lat: item.lat,
      lon: item.lon
    }))
  } catch (e) { /* empty */ }
}

async function fetchAllChlData() {
  if (!dateRange.value || dateRange.value.length !== 2) return
  try {
    allChlData.value = (await getChlTimeSeries(dateRange.value[0], dateRange.value[1], null, null)).data || []
  } catch (e) { allChlData.value = [] }
}

async function onDateRangeChange() {
  chartLoading.value = true
  await fetchAllChlData()
  chartLoading.value = false
  renderChlTimeSeries()
}

function buildLocationMap(data) {
  const map = {}
  const dateSet = new Set()
  data.forEach(item => {
    const key = `(${item.lon}, ${item.lat})`
    if (!map[key]) map[key] = {}
    map[key][item.time] = item.chl
    dateSet.add(item.time)
  })
  const allDates = Array.from(dateSet).sort()
  const result = {}
  Object.entries(map).forEach(([key, dateValueMap]) => {
    result[key] = allDates.map(d => dateValueMap[d] ?? null)
  })
  return { seriesMap: result, allDates }
}

function renderChlTimeSeries() {
  if (!timeSeriesChart) return
  if (!dateRange.value || dateRange.value.length !== 2) return

  const selectedKeys = chlLocations.value
  const filtered = selectedKeys.length > 0
    ? allChlData.value.filter(item => selectedKeys.includes(`${item.lon},${item.lat}`))
    : allChlData.value

  chartEmpty.value = filtered.length === 0
  if (filtered.length === 0) { timeSeriesChart.clear(); return }

  const { allDates, seriesMap } = buildLocationMap(filtered)
  const seriesData = buildSeriesData(seriesMap, CHL_COLORS, { area: true, markLine: true })

  const base = buildBaseOption({
    legendData: Object.keys(seriesMap),
    xAxisData: allDates,
    yAxisName: '叶绿素浓度 (mg/m³)',
    yAxisUnit: 'mg/m³'
  })
  base.tooltip.formatter = buildTooltipFormatter('mg/m³', {})

  timeSeriesChart.setOption({ ...base, series: seriesData, color: CHL_COLORS }, true)
}

// ---- fullscreen ----
function openFullscreen() {
  fullscreenVisible.value = true
}

function onFullscreenOpened() {
  nextTick(() => {
    fullscreenChart = echarts.init(fullscreenChartRef.value)
    fullscreenChart.setOption(timeSeriesChart.getOption(), true)
  })
}

function onFullscreenClosed() {
  fullscreenChart?.dispose()
  fullscreenChart = null
}

// ---- table ----
async function loadTableData() {
  tableLoading.value = true
  try {
    const res = await getOceanDataPage({ ...tableQuery.value })
    tableData.value = res.data.records
    tableTotal.value = res.data.total
  } finally { tableLoading.value = false }
}
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 400px;
}
</style>
