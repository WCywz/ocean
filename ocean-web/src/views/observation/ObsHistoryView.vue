<template>
  <div>
    <h1 class="editorial-page-title">历史观测记录</h1>
    <p class="editorial-page-subtitle">Observation History · 共 {{ tableTotal }} 条记录</p>

    <div class="editorial-filter-bar">
      <select v-model="tableQuery.dataType" class="editorial-select" style="width: 150px;">
        <option value="">全部类型</option>
        <option value="thetao">海表温度</option>
        <option value="chl">叶绿素浓度</option>
      </select>
      <el-date-picker
        v-model="dateRange"
        type="daterange" range-separator="至"
        start-placeholder="开始日期" end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 280px"
      />
      <button class="editorial-btn-outline" @click="handleSearch">查询</button>
      <button class="editorial-btn-outline" @click="handleReset">重置</button>
    </div>

    <table class="editorial-table" v-loading="tableLoading">
      <thead>
        <tr>
          <td>数据类型</td><td>观测日期</td><td>数值</td><td>单位</td><td>深度</td><td>经度</td><td>纬度</td><td>创建时间</td>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in tableData" :key="row.id">
          <td><span class="editorial-tag">{{ row.variable }}</span></td>
          <td>{{ row.obsDate || row.obsTime }}</td>
          <td>{{ row.value }}</td>
          <td>{{ row.unit }}</td>
          <td>{{ row.depth }}</td>
          <td>{{ row.lon || row.longitude }}</td>
          <td>{{ row.lat || row.latitude }}</td>
          <td class="text-muted">{{ row.createTime }}</td>
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
import { ref, onMounted } from 'vue'
import { getOceanDataPage } from '../../api/ocean-data'

const tableQuery = ref({ pageNum: 1, pageSize: 10, dataType: '' })
const dateRange = ref([])
const tableData = ref([])
const tableTotal = ref(0)
const tableLoading = ref(false)

async function loadTableData() {
  tableLoading.value = true
  try {
    const params = { ...tableQuery.value }
    if (tableQuery.value.dataType) {
      params.variable = tableQuery.value.dataType
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getOceanDataPage(params)
    tableData.value = res.data.records
    tableTotal.value = res.data.total
  } finally { tableLoading.value = false }
}

function handleSearch() {
  tableQuery.value.pageNum = 1
  loadTableData()
}

function handleReset() {
  tableQuery.value.dataType = ''
  tableQuery.value.pageNum = 1
  dateRange.value = []
  loadTableData()
}

onMounted(() => { loadTableData() })
</script>

<style scoped>
</style>
