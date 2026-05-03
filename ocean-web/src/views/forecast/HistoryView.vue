<template>
  <div class="history-page">
    <h2 class="page-title">历史预报记录</h2>

    <el-card shadow="hover">
      <el-form :inline="true" :model="tableQuery" size="default" style="margin-bottom: 16px;">
        <el-form-item label="数据类型">
          <el-select v-model="tableQuery.dataType" placeholder="全部" clearable style="width: 150px">
            <el-option label="海表温度" value="SST" />
            <el-option label="叶绿素浓度" value="CHL" />
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
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
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
import { ref, onMounted } from 'vue'
import { getRecordPage } from '../../api/forecast'

const tableQuery = ref({ pageNum: 1, pageSize: 10, dataType: '', locationName: '' })
const dateRange = ref([])
const tableData = ref([])
const tableTotal = ref(0)
const tableLoading = ref(false)

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
.page-title { margin-bottom: 20px; color: #1a3a5c; font-size: 22px; }
</style>
