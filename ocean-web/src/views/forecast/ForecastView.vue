<template>
  <div class="forecast-page">
    <h2 class="page-title">预报数据可视化</h2>

    <!-- 海表温度图表 -->
    <el-card shadow="hover" class="chart-card">
      <template #header>
        <div class="chart-header">
          <span class="chart-title">海表温度 (SST) 变化趋势</span>
          <div class="chart-header-right">
            <el-select
              v-model="sstLocations"
              placeholder="筛选观测点（默认全部）"
              multiple
              collapse-tags
              collapse-tags-tooltip
              filterable
              size="small"
              style="width: 260px"
              @change="renderSstChart"
            >
              <el-option
                v-for="loc in locationOptions"
                :key="loc.key"
                :label="loc.locationName || loc.label"
                :value="loc.key"
              />
            </el-select>
            <el-button size="small" text @click="openFullscreen('SST')">
              <el-icon><FullScreen /></el-icon>
            </el-button>
          </div>
        </div>
      </template>
      <div v-loading="sstLoading" class="chart-container" ref="sstChartRef">
        <div v-if="sstEmpty" class="chart-empty">暂无海表温度数据</div>
      </div>
    </el-card>

    <!-- 叶绿素浓度图表 -->
    <el-card shadow="hover" class="chart-card" style="margin-top: 20px;">
      <template #header>
        <div class="chart-header">
          <span class="chart-title">叶绿素浓度 (CHL) 变化趋势</span>
          <div class="chart-header-right">
            <el-select
              v-model="chlLocations"
              placeholder="筛选观测点（默认全部）"
              multiple
              collapse-tags
              collapse-tags-tooltip
              filterable
              size="small"
              style="width: 260px"
              @change="renderChlChart"
            >
              <el-option
                v-for="loc in locationOptions"
                :key="loc.key"
                :label="loc.locationName || loc.label"
                :value="loc.key"
              />
            </el-select>
            <el-button size="small" text @click="openFullscreen('CHL')">
              <el-icon><FullScreen /></el-icon>
            </el-button>
          </div>
        </div>
      </template>
      <div v-loading="chlLoading" class="chart-container" ref="chlChartRef">
        <div v-if="chlEmpty" class="chart-empty">暂无叶绿素浓度数据</div>
      </div>
    </el-card>

    <!-- Fullscreen chart modal -->
    <el-dialog
      v-model="fullscreenVisible"
      :title="fullscreenTitle"
      fullscreen
      :close-on-click-modal="false"
      @opened="onFullscreenOpened"
    >
      <div ref="fullscreenChartRef" class="chart-container" style="height: calc(100vh - 100px);"></div>
    </el-dialog>

    <!-- 历史预报记录表格 -->
    <el-card shadow="hover" style="margin-top: 20px;">
      <template #header>
        <span class="chart-title">历史预报记录</span>
      </template>

      <el-form :inline="true" :model="tableQuery" size="default" style="margin-bottom: 16px;">
        <el-form-item label="数据类型">
          <el-select v-model="tableQuery.dataType" placeholder="全部" clearable style="width: 150px">
            <el-option label="海表温度" value="SST" />
            <el-option label="叶绿素浓度" value="CHL" />
          </el-select>
        </el-form-item>
        <el-form-item label="观测点">
          <el-select v-model="tableQuery.locationName" placeholder="全部" clearable style="width: 180px">
            <el-option
              v-for="loc in locationOptions"
              :key="loc.key"
              :label="loc.locationName || loc.label"
              :value="loc.locationName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预报日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleTableSearch">查询</el-button>
          <el-button @click="handleTableReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="tableLoading" stripe border size="small">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="modelName" label="模型名称" min-width="180" />
        <el-table-column prop="dataType" label="数据类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.dataType === 'SST' ? 'primary' : 'success'" size="small">
              {{ row.dataType === 'SST' ? '海表温度' : '叶绿素浓度' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="forecastDate" label="预报日期" width="120" align="center" />
        <el-table-column prop="locationName" label="观测点" min-width="140" />
        <el-table-column prop="value" label="数值" width="100" align="center" />
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column prop="longitude" label="经度" width="110" align="center" />
        <el-table-column prop="latitude" label="纬度" width="110" align="center" />
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
      </el-table>

      <div style="margin-top: 16px; text-align: right;">
        <el-pagination
          v-model:current-page="tableQuery.pageNum"
          v-model:page-size="tableQuery.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="tableTotal"
          layout="total, sizes, prev, pager, next"
          @size-change="loadTableData"
          @current-change="loadTableData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getLocations, getSstTrend, getChlTrend, getRecordPage } from '../../api/forecast'
import {
  SST_COLORS, CHL_COLORS,
  buildBaseOption, buildTooltipFormatter, buildSeriesData
} from '../../utils/chart-config'

// ---- location options ----
const locationOptions = ref([])
const locationNameMap = {}

// ---- SST chart ----
const sstChartRef = ref(null)
const sstLocations = ref([])
const allSstData = ref([])
const sstLoading = ref(false)
const sstEmpty = ref(false)
let sstChart = null

// ---- CHL chart ----
const chlChartRef = ref(null)
const chlLocations = ref([])
const allChlData = ref([])
const chlLoading = ref(false)
const chlEmpty = ref(false)
let chlChart = null

// ---- fullscreen ----
const fullscreenVisible = ref(false)
const fullscreenTitle = ref('')
const fullscreenChartRef = ref(null)
let fullscreenChart = null

// ---- table ----
const tableQuery = ref({ pageNum: 1, pageSize: 10, dataType: '', locationName: '' })
const dateRange = ref([])
const tableData = ref([])
const tableTotal = ref(0)
const tableLoading = ref(false)

onMounted(() => {
  nextTick(() => { initCharts() })
  loadTableData()
})

async function initCharts() {
  sstChart = echarts.init(sstChartRef.value)
  chlChart = echarts.init(chlChartRef.value)

  await loadLocationOptions()
  sstLoading.value = true
  chlLoading.value = true
  await Promise.all([fetchAllSstData(), fetchAllChlData()])
  sstLoading.value = false
  chlLoading.value = false
  renderSstChart()
  renderChlChart()

  window.addEventListener('resize', () => {
    sstChart?.resize()
    chlChart?.resize()
    fullscreenChart?.resize()
  })
}

async function loadLocationOptions() {
  try {
    const res = await getLocations()
    const data = res.data || []
    locationOptions.value = data.map(item => ({
      key: `${item.longitude},${item.latitude}`,
      label: `经度: ${item.longitude}, 纬度: ${item.latitude}`,
      lon: item.longitude,
      lat: item.latitude,
      locationName: item.locationName
    }))
    data.forEach(item => {
      locationNameMap[`(${item.longitude}, ${item.latitude})`] = item.locationName
    })
  } catch (e) { /* empty */ }
}

async function fetchAllSstData() {
  try { allSstData.value = (await getSstTrend(null, null)).data || [] }
  catch (e) { allSstData.value = [] }
}

async function fetchAllChlData() {
  try { allChlData.value = (await getChlTrend(null, null)).data || [] }
  catch (e) { allChlData.value = [] }
}

function renderSstChart() {
  if (!sstChart) return
  const selectedKeys = sstLocations.value
  const filtered = selectedKeys.length > 0
    ? allSstData.value.filter(item => selectedKeys.includes(`${item.longitude},${item.latitude}`))
    : allSstData.value

  sstEmpty.value = filtered.length === 0
  if (filtered.length === 0) { sstChart.clear(); return }

  const { allDates, seriesMap } = buildLocationMap(filtered)
  const seriesData = buildSeriesData(seriesMap, SST_COLORS, { area: true, markLine: true })

  const base = buildBaseOption({
    legendData: Object.keys(seriesMap),
    xAxisData: allDates,
    yAxisName: '温度 (°C)',
    yAxisUnit: '°C'
  })
  base.tooltip.formatter = buildTooltipFormatter('°C', locationNameMap)

  sstChart.setOption({ ...base, series: seriesData, color: SST_COLORS }, true)
}

function renderChlChart() {
  if (!chlChart) return
  const selectedKeys = chlLocations.value
  const filtered = selectedKeys.length > 0
    ? allChlData.value.filter(item => selectedKeys.includes(`${item.longitude},${item.latitude}`))
    : allChlData.value

  chlEmpty.value = filtered.length === 0
  if (filtered.length === 0) { chlChart.clear(); return }

  const { allDates, seriesMap } = buildLocationMap(filtered)
  const seriesData = buildSeriesData(seriesMap, CHL_COLORS, { area: true })

  const base = buildBaseOption({
    legendData: Object.keys(seriesMap),
    xAxisData: allDates,
    yAxisName: '浓度 (mg/m³)',
    yAxisUnit: 'mg/m³'
  })
  base.tooltip.formatter = buildTooltipFormatter('mg/m³', locationNameMap)

  chlChart.setOption({ ...base, series: seriesData, color: CHL_COLORS }, true)
}

function buildLocationMap(data) {
  const map = {}
  const dateSet = new Set()
  data.forEach(item => {
    const key = `(${item.longitude}, ${item.latitude})`
    if (!map[key]) map[key] = {}
    map[key][item.forecastDate] = item.value
    dateSet.add(item.forecastDate)
  })
  const allDates = Array.from(dateSet).sort()
  const result = {}
  Object.entries(map).forEach(([key, dateValueMap]) => {
    result[key] = allDates.map(d => dateValueMap[d] ?? null)
  })
  return { seriesMap: result, allDates }
}

// ---- fullscreen ----
function openFullscreen(type) {
  fullscreenTitle.value = type === 'SST' ? '海表温度 (SST) 趋势' : '叶绿素浓度 (CHL) 趋势'
  fullscreenVisible.value = true
}

function onFullscreenOpened() {
  nextTick(() => {
    fullscreenChart = echarts.init(fullscreenChartRef.value)
    const targetChart = fullscreenTitle.value.includes('SST') ? sstChart : chlChart
    fullscreenChart.setOption(targetChart.getOption(), true)
    window.addEventListener('resize', () => { fullscreenChart?.resize() })
  })
}

// ---- table ----
async function loadTableData() {
  tableLoading.value = true
  try {
    const params = { ...tableQuery.value }
    if (dateRange.value && dateRange.value.length === 2) {
      params.forecastDateBegin = dateRange.value[0]
      params.forecastDateEnd = dateRange.value[1]
    }
    const res = await getRecordPage(params)
    tableData.value = res.data.records
    tableTotal.value = res.data.total
  } finally { tableLoading.value = false }
}

function handleTableSearch() {
  tableQuery.value.pageNum = 1
  loadTableData()
}

function handleTableReset() {
  tableQuery.value.dataType = ''
  tableQuery.value.locationName = ''
  tableQuery.value.pageNum = 1
  dateRange.value = []
  loadTableData()
}
</script>

<style scoped>
.page-title {
  margin-bottom: 24px;
  color: #1a3a5c;
  font-size: 22px;
}
.chart-card {
  background: #fff;
}
.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chart-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.chart-title {
  font-weight: 600;
  color: #1a3a5c;
  font-size: 16px;
}
.chart-container {
  width: 100%;
  height: 400px;
}
.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  font-size: 14px;
}
</style>
