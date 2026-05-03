<template>
  <div class="forecast-page">
    <h2 class="page-title">预报数据可视化</h2>

    <!-- 海表温度图表 -->
    <el-card shadow="hover" class="chart-card">
      <template #header>
        <div class="chart-header">
          <span class="chart-title">海表温度 (SST) 变化趋势</span>
          <el-select
            v-model="sstLocations"
            placeholder="选择经纬度"
            multiple
            collapse-tags
            collapse-tags-tooltip
            filterable
            size="small"
            style="width: 280px"
            @change="renderSstChart"
          >
            <el-option
              v-for="loc in locationOptions"
              :key="loc.key"
              :label="loc.label"
              :value="loc.key"
            />
          </el-select>
        </div>
      </template>
      <div ref="sstChartRef" class="chart-container"></div>
    </el-card>

    <!-- 叶绿素浓度图表 -->
    <el-card shadow="hover" class="chart-card" style="margin-top: 20px;">
      <template #header>
        <div class="chart-header">
          <span class="chart-title">叶绿素浓度 (CHL) 变化趋势</span>
          <el-select
            v-model="chlLocations"
            placeholder="选择经纬度"
            multiple
            collapse-tags
            collapse-tags-tooltip
            filterable
            size="small"
            style="width: 280px"
            @change="renderChlChart"
          >
            <el-option
              v-for="loc in locationOptions"
              :key="loc.key"
              :label="loc.label"
              :value="loc.key"
            />
          </el-select>
        </div>
      </template>
      <div ref="chlChartRef" class="chart-container"></div>
    </el-card>

    <!-- 历史预报记录表格 -->
    <el-card shadow="hover" style="margin-top: 20px;">
      <template #header>
        <span class="chart-title">历史预报记录</span>
      </template>

      <!-- 查询条件 -->
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

// 经纬度选项列表
const locationOptions = ref([])

// SST 图表
const sstChartRef = ref(null)
const sstLocations = ref([])
const allSstData = ref([])
let sstChart = null

// CHL 图表
const chlChartRef = ref(null)
const chlLocations = ref([])
const allChlData = ref([])
let chlChart = null

// 表格数据
const tableQuery = ref({ pageNum: 1, pageSize: 10, dataType: '', locationName: '' })
const dateRange = ref([])
const tableData = ref([])
const tableTotal = ref(0)
const tableLoading = ref(false)

onMounted(() => {
  nextTick(() => {
    initCharts()
  })
})

async function initCharts() {
  sstChart = echarts.init(sstChartRef.value)
  chlChart = echarts.init(chlChartRef.value)

  await loadLocationOptions()
  await Promise.all([fetchAllSstData(), fetchAllChlData()])
  renderSstChart()
  renderChlChart()

  window.addEventListener('resize', () => {
    sstChart?.resize()
    chlChart?.resize()
  })
}

async function fetchAllSstData() {
  try {
    const res = await getSstTrend(null, null)
    allSstData.value = res.data || []
  } catch (e) {
    allSstData.value = []
  }
}

async function fetchAllChlData() {
  try {
    const res = await getChlTrend(null, null)
    allChlData.value = res.data || []
  } catch (e) {
    allChlData.value = []
  }
}

async function loadLocationOptions() {
  try {
    const res = await getLocations()
    locationOptions.value = (res.data || []).map(item => ({
      key: `${item.longitude},${item.latitude}`,
      label: `经度: ${item.longitude}, 纬度: ${item.latitude}`,
      lon: item.longitude,
      lat: item.latitude,
      locationName: item.locationName
    }))
  } catch (e) {
    // ignored
  }
}

function renderSstChart() {
  if (!sstChart) return
  const selectedKeys = sstLocations.value
  const filtered = selectedKeys.length > 0
    ? allSstData.value.filter(item => selectedKeys.includes(`${item.longitude},${item.latitude}`))
    : []

  const { allDates } = buildLocationMap(allSstData.value)
  const { seriesMap } = buildLocationMap(filtered)
  const seriesData = Object.entries(seriesMap).map(([name, values], idx) => ({
    name,
    type: 'line',
    smooth: true,
    symbol: idx > 15 ? 'none' : 'circle',
    symbolSize: 5,
    lineStyle: { width: 2 },
    data: values,
    markLine: {
      silent: true,
      symbol: 'none',
      lineStyle: { type: 'dashed', color: '#aaa' },
      label: { fontSize: 11 },
      data: [{ type: 'average', name: '均值' }]
    }
  }))

  sstChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#ddd',
      borderWidth: 1,
      textStyle: { fontSize: 13, color: '#333' },
      ...(seriesData.length === 0 ? {} : {
        formatter: params => {
          let html = `<b style="font-size:14px">${params[0].axisValue}</b><br/>`
          params.forEach(p => {
            html += `<span style="display:inline-block;margin-right:4px;border-radius:50%;width:10px;height:10px;background:${p.color}"></span>`
            html += ` ${p.seriesName}: <b>${p.value} °C</b><br/>`
          })
          return html
        }
      })
    },
    legend: {
      data: Object.keys(seriesMap),
      type: 'scroll',
      bottom: 0,
      textStyle: { fontSize: 13 },
      pageTextStyle: { fontSize: 13 }
    },
    grid: { left: 90, right: 50, top: 30, bottom: 80 },
    dataZoom: [
      { type: 'slider', bottom: 35, height: 22, textStyle: { fontSize: 11 } },
      { type: 'inside' }
    ],
    xAxis: {
      type: 'category',
      data: allDates,
      axisLabel: { rotate: 25, fontSize: 12, color: '#666' },
      axisLine: { lineStyle: { color: '#ccc' } },
      axisTick: { lineStyle: { color: '#ccc' } }
    },
    yAxis: {
      type: 'value',
      name: '温度 (°C)',
      nameTextStyle: { fontSize: 14, color: '#666' },
      axisLabel: { fontSize: 13, formatter: '{value} °C' },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    series: seriesData,
    color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#fc8452',
            '#3ba272', '#9a60b4', '#ea7ccc', '#ff9f7f',
            '#67e0e3', '#d48265', '#61a0a8', '#c23531', '#2f4554']
  }, true)
}

function renderChlChart() {
  if (!chlChart) return
  const selectedKeys = chlLocations.value
  const filtered = selectedKeys.length > 0
    ? allChlData.value.filter(item => selectedKeys.includes(`${item.longitude},${item.latitude}`))
    : []

  const { allDates } = buildLocationMap(allChlData.value)
  const { seriesMap } = buildLocationMap(filtered)
  const seriesData = Object.entries(seriesMap).map(([name, values], idx) => ({
    name,
    type: 'line',
    smooth: true,
    symbol: idx > 15 ? 'none' : 'circle',
    symbolSize: 5,
    lineStyle: { width: 2 },
    areaStyle: { opacity: 0.06 },
    data: values
  }))

  chlChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#ddd',
      borderWidth: 1,
      textStyle: { fontSize: 13, color: '#333' },
      ...(seriesData.length === 0 ? {} : {
        formatter: params => {
          let html = `<b style="font-size:14px">${params[0].axisValue}</b><br/>`
          params.forEach(p => {
            html += `<span style="display:inline-block;margin-right:4px;border-radius:50%;width:10px;height:10px;background:${p.color}"></span>`
            html += ` ${p.seriesName}: <b>${p.value} mg/m³</b><br/>`
          })
          return html
        }
      })
    },
    legend: {
      data: Object.keys(seriesMap),
      type: 'scroll',
      bottom: 0,
      textStyle: { fontSize: 13 },
      pageTextStyle: { fontSize: 13 }
    },
    grid: { left: 90, right: 50, top: 30, bottom: 80 },
    dataZoom: [
      { type: 'slider', bottom: 35, height: 22, textStyle: { fontSize: 11 } },
      { type: 'inside' }
    ],
    xAxis: {
      type: 'category',
      data: allDates,
      axisLabel: { rotate: 25, fontSize: 12, color: '#666' },
      axisLine: { lineStyle: { color: '#ccc' } },
      axisTick: { lineStyle: { color: '#ccc' } }
    },
    yAxis: {
      type: 'value',
      name: '浓度 (mg/m³)',
      nameTextStyle: { fontSize: 14, color: '#666' },
      axisLabel: { fontSize: 13, formatter: '{value} mg/m³' },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
    },
    series: seriesData,
    color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#fc8452',
            '#3ba272', '#9a60b4', '#ea7ccc', '#ff9f7f',
            '#67e0e3', '#d48265', '#61a0a8', '#c23531', '#2f4554']
  }, true)
}

/** 按 (经度, 纬度) 分组，返回 { seriesMap, allDates } */
function buildLocationMap(data) {
  const map = {}
  const dateSet = new Set()
  data.forEach(item => {
    const key = `(${item.longitude}, ${item.latitude})`
    if (!map[key]) {
      map[key] = {}
    }
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

// 表格数据加载
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
  } finally {
    tableLoading.value = false
  }
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

onMounted(() => {
  loadTableData()
})
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

.chart-title {
  font-weight: 600;
  color: #1a3a5c;
  font-size: 16px;
}

.chart-container {
  width: 100%;
  height: 520px;
}
</style>
