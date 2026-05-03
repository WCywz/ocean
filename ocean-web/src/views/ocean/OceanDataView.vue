<template>
  <div class="ocean-data-page">
    <h2 class="page-title">海洋观测数据</h2>

    <!-- 叶绿素时间序列图表 -->
    <el-card shadow="hover" class="chart-card">
      <template #header>
        <div class="chart-header">
          <span class="chart-title">叶绿素浓度时间序列 (Chl-a Time Series)</span>
          <div class="chart-controls">
            <el-select
              v-model="chlLocations"
              placeholder="选择经纬度（不选则全部）"
              multiple
              collapse-tags
              collapse-tags-tooltip
              filterable
              size="small"
              style="width: 300px"
              @change="renderChlTimeSeries"
            >
              <el-option
                v-for="loc in locationOptions"
                :key="loc.key"
                :label="loc.label"
                :value="loc.key"
              />
            </el-select>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              value-format="YYYY-MM-DD"
              size="small"
              style="width: 260px"
              @change="onDateRangeChange"
            />
          </div>
        </div>
      </template>
      <div ref="timeSeriesChartRef" class="chart-container"></div>
    </el-card>

    <!-- 观测数据记录表格 -->
    <el-card shadow="hover" style="margin-top: 20px;">
      <template #header>
        <div class="chart-header">
          <span class="chart-title">观测数据记录</span>
          <el-button size="small" @click="loadTableData">刷新</el-button>
        </div>
      </template>
      <el-table :data="tableData" v-loading="tableLoading" size="small" stripe max-height="420">
        <el-table-column prop="time" label="日期" width="120" align="center" />
        <el-table-column prop="lat" label="纬度" width="100" align="center" />
        <el-table-column prop="lon" label="经度" width="100" align="center" />
        <el-table-column prop="depth" label="深度(m)" width="90" align="center" />
        <el-table-column prop="chl" label="叶绿素" width="100" align="center" />
      </el-table>

      <div style="margin-top: 16px; text-align: right;">
        <el-pagination
          v-model:current-page="tableQuery.pageNum"
          v-model:page-size="tableQuery.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="tableTotal"
          small
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
import { getChlTimeSeries, getOceanDataPage, getOceanLocations } from '../../api/ocean-data'

// 获取本地日期字符串（修复 UTC 时区问题）
function toLocalDateStr(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

// 经纬度选项
const locationOptions = ref([])

// 日期范围：默认最近7天
const dateRange = ref([
  toLocalDateStr(new Date(Date.now() - 7 * 86400000)),
  toLocalDateStr(new Date())
])

// 时间序列图表
const timeSeriesChartRef = ref(null)
const chlLocations = ref([])
const allChlData = ref([])
let timeSeriesChart = null

// 表格
const tableQuery = ref({ pageNum: 1, pageSize: 10 })
const tableData = ref([])
const tableTotal = ref(0)
const tableLoading = ref(false)

onMounted(() => {
  nextTick(async () => {
    timeSeriesChart = echarts.init(timeSeriesChartRef.value)

    await loadLocationOptions()
    await fetchAllChlData()
    renderChlTimeSeries()
    loadTableData()

    window.addEventListener('resize', () => {
      timeSeriesChart?.resize()
    })
  })
})

async function loadLocationOptions() {
  try {
    const res = await getOceanLocations()
    locationOptions.value = (res.data || []).map(item => ({
      key: `${item.lon},${item.lat}`,
      label: `经度: ${item.lon}, 纬度: ${item.lat}`,
      lat: item.lat,
      lon: item.lon
    }))
  } catch (e) {
    // ignored
  }
}

async function fetchAllChlData() {
  if (!dateRange.value || dateRange.value.length !== 2) return
  try {
    const res = await getChlTimeSeries(dateRange.value[0], dateRange.value[1], null, null)
    allChlData.value = res.data || []
  } catch (e) {
    allChlData.value = []
  }
}

async function onDateRangeChange() {
  await fetchAllChlData()
  renderChlTimeSeries()
}

function buildLocationMap(data) {
  const map = {}
  const dateSet = new Set()
  data.forEach(item => {
    const key = `(${item.lon}, ${item.lat})`
    if (!map[key]) {
      map[key] = {}
    }
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

  timeSeriesChart.showLoading()
  try {
    const selectedKeys = chlLocations.value
    const filtered = selectedKeys.length > 0
      ? allChlData.value.filter(item => selectedKeys.includes(`${item.lon},${item.lat}`))
      : allChlData.value

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
      data: values,
      markLine: {
        silent: true,
        symbol: 'none',
        lineStyle: { type: 'dashed', color: '#aaa' },
        label: { fontSize: 11 },
        data: [{ type: 'average', name: '均值' }]
      }
    }))

    timeSeriesChart.setOption({
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
        name: '叶绿素浓度 (mg/m³)',
        nameTextStyle: { fontSize: 14, color: '#666' },
        axisLabel: { fontSize: 13 },
        splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } }
      },
      series: seriesData,
      color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#fc8452',
              '#3ba272', '#9a60b4', '#ea7ccc', '#ff9f7f',
              '#67e0e3', '#d48265', '#61a0a8', '#c23531', '#2f4554']
    }, true)
  } finally {
    timeSeriesChart.hideLoading()
  }
}

async function loadTableData() {
  tableLoading.value = true
  try {
    const res = await getOceanDataPage({ ...tableQuery.value })
    tableData.value = res.data.records
    tableTotal.value = res.data.total
  } finally {
    tableLoading.value = false
  }
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
.chart-controls {
  display: flex;
  align-items: center;
  gap: 12px;
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
