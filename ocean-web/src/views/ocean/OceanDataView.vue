<template>
  <div>
    <h1 class="editorial-page-title">海洋观测数据</h1>
    <p class="editorial-page-subtitle">Ocean Observation Data</p>

    <!-- SST time series chart -->
    <div class="editorial-section">
      <p class="editorial-section-label">Feature · 时间序列</p>
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
        <h3 class="editorial-section-heading" style="margin: 0;">海表温度时间序列</h3>
        <div style="display: flex; align-items: center; gap: 8px;">
          <el-select
            v-model="sstLocations"
            placeholder="筛选观测点"
            multiple collapse-tags collapse-tags-tooltip filterable
            size="small" style="width: 280px"
            @change="renderSstTimeSeries"
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
          <el-button size="small" text @click="openSstFullscreen">
            <el-icon><FullScreen /></el-icon>
          </el-button>
        </div>
      </div>
      <div v-loading="sstChartLoading" style="display: flex; align-items: stretch; position: relative; width: 100%; height: 320px;">
        <div style="display: flex; align-items: center; justify-content: center; writing-mode: vertical-lr; text-orientation: mixed; font-size: 13px; color: #666; padding: 0 8px; white-space: nowrap; flex-shrink: 0;">海表温度 (°C)</div>
        <div ref="sstChartRef" style="flex: 1; height: 100%; min-width: 0;"></div>
        <div v-if="sstChartEmpty" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: var(--color-text-muted); font-size: 13px; pointer-events: none;">暂无符合条件的水温数据</div>
      </div>
    </div>

    <!-- SST fullscreen modal -->
    <el-dialog v-model="sstFullscreenVisible" title="海表温度时间序列" fullscreen :close-on-click-modal="false" @opened="onSstFullscreenOpened" @close="onSstFullscreenClosed">
      <div ref="sstFullscreenChartRef" style="height: calc(100vh - 100px);"></div>
    </el-dialog>

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
          <el-button size="small" text @click="openChlFullscreen">
            <el-icon><FullScreen /></el-icon>
          </el-button>
        </div>
      </div>
      <div v-loading="chlChartLoading" style="display: flex; align-items: stretch; position: relative; width: 100%; height: 400px;">
        <div style="display: flex; align-items: center; justify-content: center; writing-mode: vertical-lr; text-orientation: mixed; font-size: 13px; color: #666; padding: 0 8px; white-space: nowrap; flex-shrink: 0;">叶绿素浓度 (mg/m³)</div>
        <div ref="chlChartRef" style="flex: 1; height: 100%; min-width: 0;"></div>
        <div v-if="chlChartEmpty" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: var(--color-text-muted); font-size: 13px; pointer-events: none;">暂无符合条件的观测数据</div>
      </div>
    </div>

    <!-- Chl fullscreen modal -->
    <el-dialog v-model="chlFullscreenVisible" title="叶绿素浓度时间序列" fullscreen :close-on-click-modal="false" @opened="onChlFullscreenOpened" @close="onChlFullscreenClosed">
      <div ref="chlFullscreenChartRef" style="height: calc(100vh - 100px);"></div>
    </el-dialog>

    <!-- Data tables: SST left, CHL right -->
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
      <h3 class="editorial-section-heading" style="margin: 0;">观测数据记录</h3>
      <button class="editorial-btn-outline" @click="loadTableData">刷新</button>
    </div>
    <div v-loading="tableLoading" style="display: flex; gap: 24px;">
      <!-- SST table -->
      <div style="flex: 1; min-width: 0;">
        <h4 style="font-size: 13px; font-weight: 600; color: var(--color-text); margin-bottom: 8px;">海表温度</h4>
        <table class="editorial-table">
          <thead>
            <tr>
              <td>日期</td><td>纬度</td><td>经度</td><td>温度 (°C)</td>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in sstTableData" :key="idx">
              <td>{{ row.time }}</td>
              <td>{{ row.lat }}</td>
              <td>{{ row.lon }}</td>
              <td>{{ row.sst }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <!-- CHL table -->
      <div style="flex: 1; min-width: 0;">
        <h4 style="font-size: 13px; font-weight: 600; color: var(--color-text); margin-bottom: 8px;">叶绿素浓度</h4>
        <table class="editorial-table">
          <thead>
            <tr>
              <td>日期</td><td>纬度</td><td>经度</td><td>深度 (m)</td><td>叶绿素</td>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in chlTableData" :key="idx">
              <td>{{ row.time }}</td>
              <td>{{ row.lat }}</td>
              <td>{{ row.lon }}</td>
              <td>{{ row.depth }}</td>
              <td>{{ row.chl }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
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
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getChlTimeSeries, getSstTimeSeries, getOceanDataPage, getOceanLocations } from '../../api/ocean-data'
import {
  SST_COLORS, CHL_COLORS,
  buildBaseOption, buildTooltipFormatter, buildSeriesData
} from '../../utils/chart-config'

function toLocalDateStr(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const locationOptions = ref([])

const dateRange = ref([
  toLocalDateStr(new Date('2025-12-25')),
  toLocalDateStr(new Date('2026-01-01'))
])

// ---- SST chart ----
const sstChartRef = ref(null)
const sstLocations = ref([])
const allSstData = ref([])
const sstChartLoading = ref(false)
const sstChartEmpty = ref(false)
let sstChart = null

// ---- CHL chart ----
const chlChartRef = ref(null)
const chlLocations = ref([])
const allChlData = ref([])
const chlChartLoading = ref(false)
const chlChartEmpty = ref(false)
let chlChart = null

// ---- fullscreen ----
const sstFullscreenVisible = ref(false)
const sstFullscreenChartRef = ref(null)
let sstFullscreenChart = null

const chlFullscreenVisible = ref(false)
const chlFullscreenChartRef = ref(null)
let chlFullscreenChart = null

// ---- table ----
const tableQuery = ref({ pageNum: 1, pageSize: 10 })
const tableData = ref([])
const tableTotal = ref(0)
const tableLoading = ref(false)

const sstTableData = computed(() => {
  const seen = new Set()
  return tableData.value.filter(row => {
    if (row.sst == null) return false
    const key = `${row.lat},${row.lon},${row.time}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
})

const chlTableData = computed(() => tableData.value)

let isAlive = true

onMounted(async () => {
  await nextTick()
  if (!isAlive) return
  if (sstChartRef.value) sstChart = echarts.init(sstChartRef.value)
  if (chlChartRef.value) chlChart = echarts.init(chlChartRef.value)

  await loadLocationOptions()
  if (!isAlive) return

  await Promise.all([fetchAllSstData(), fetchAllChlData()])
  if (!isAlive) return
  renderSstTimeSeries()
  renderChlTimeSeries()
  loadTableData()

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  isAlive = false
  window.removeEventListener('resize', handleResize)
  sstChart?.dispose()
  chlChart?.dispose()
  sstFullscreenChart?.dispose()
  chlFullscreenChart?.dispose()
})

function handleResize() {
  sstChart?.resize()
  chlChart?.resize()
  sstFullscreenChart?.resize()
  chlFullscreenChart?.resize()
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

async function fetchAllSstData() {
  if (!dateRange.value || dateRange.value.length !== 2) return
  sstChartLoading.value = true
  try {
    allSstData.value = (await getSstTimeSeries(dateRange.value[0], dateRange.value[1], null, null)).data || []
  } catch (e) { allSstData.value = [] }
  sstChartLoading.value = false
}

async function fetchAllChlData() {
  if (!dateRange.value || dateRange.value.length !== 2) return
  chlChartLoading.value = true
  try {
    allChlData.value = (await getChlTimeSeries(dateRange.value[0], dateRange.value[1], null, null)).data || []
  } catch (e) { allChlData.value = [] }
  chlChartLoading.value = false
}

async function onDateRangeChange() {
  await Promise.all([fetchAllSstData(), fetchAllChlData()])
  renderSstTimeSeries()
  renderChlTimeSeries()
}

function buildLocationMap(data, valueKey) {
  const map = {}
  const dateSet = new Set()
  data.forEach(item => {
    const key = `(${item.lon}, ${item.lat})`
    if (!map[key]) map[key] = {}
    map[key][item.time] = item[valueKey]
    dateSet.add(item.time)
  })
  const allDates = Array.from(dateSet).sort()
  const result = {}
  Object.entries(map).forEach(([key, dateValueMap]) => {
    result[key] = allDates.map(d => dateValueMap[d] ?? null)
  })
  return { seriesMap: result, allDates }
}

function renderTimeSeries(chart, allData, selectedKeys, colors, unit, setEmpty) {
  if (!chart || chart.isDisposed?.()) return
  if (!dateRange.value || dateRange.value.length !== 2) return

  const filtered = selectedKeys.length > 0
    ? allData.filter(item => selectedKeys.includes(`${item.lon},${item.lat}`))
    : allData

  setEmpty(filtered.length === 0)
  if (filtered.length === 0) { chart.clear(); return }

  const valueKey = unit === '°C' ? 'sst' : 'chl'
  const { allDates, seriesMap } = buildLocationMap(filtered, valueKey)
  const seriesData = buildSeriesData(seriesMap, colors, { area: true, markLine: true })

  const base = buildBaseOption({
    legendData: Object.keys(seriesMap),
    xAxisData: allDates,
    yAxisName: '',
    yAxisUnit: unit
  })
  base.tooltip.formatter = buildTooltipFormatter(unit, {})

  chart.setOption({ ...base, series: seriesData, color: colors }, true)
}

function renderSstTimeSeries() {
  renderTimeSeries(sstChart, allSstData.value, sstLocations.value, SST_COLORS, '°C', (v) => { sstChartEmpty.value = v })
}

function renderChlTimeSeries() {
  renderTimeSeries(chlChart, allChlData.value, chlLocations.value, CHL_COLORS, 'mg/m³', (v) => { chlChartEmpty.value = v })
}

// ---- fullscreen ----
function openSstFullscreen() { sstFullscreenVisible.value = true }
function onSstFullscreenOpened() {
  nextTick(() => {
    sstFullscreenChart = echarts.init(sstFullscreenChartRef.value)
    if (sstChart) sstFullscreenChart.setOption(sstChart.getOption(), true)
  })
}
function onSstFullscreenClosed() { sstFullscreenChart?.dispose(); sstFullscreenChart = null }

function openChlFullscreen() { chlFullscreenVisible.value = true }
function onChlFullscreenOpened() {
  nextTick(() => {
    chlFullscreenChart = echarts.init(chlFullscreenChartRef.value)
    if (chlChart) chlFullscreenChart.setOption(chlChart.getOption(), true)
  })
}
function onChlFullscreenClosed() { chlFullscreenChart?.dispose(); chlFullscreenChart = null }

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
</style>
