<template>
  <div class="dashboard">
    <h2 class="page-title">系统仪表盘</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #e6f7ff;">
              <el-icon :size="32" color="#1890ff"><Setting /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ dashboard.modelCount }}</div>
              <div class="stat-label">模型总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f6ffed;">
              <el-icon :size="32" color="#52c41a"><VideoPlay /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ dashboard.runningModelCount }}</div>
              <div class="stat-label">运行中模型</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #fff7e6;">
              <el-icon :size="32" color="#fa8c16"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ dashboard.todayRecordCount }}</div>
              <div class="stat-label">今日预报记录</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最新数据展示 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">最新海表温度 (SST)</span>
          </template>
          <el-table :data="dashboard.latestSstData" size="small" stripe>
            <el-table-column prop="locationName" label="观测点" />
            <el-table-column prop="value" label="温度值">
              <template #default="{ row }">{{ row.value }} {{ row.unit }}</template>
            </el-table-column>
            <el-table-column prop="forecastDate" label="预报日期" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">最新叶绿素浓度 (CHL)</span>
          </template>
          <el-table :data="dashboard.latestChlData" size="small" stripe>
            <el-table-column prop="locationName" label="观测点" />
            <el-table-column prop="value" label="浓度值">
              <template #default="{ row }">{{ row.value }} {{ row.unit }}</template>
            </el-table-column>
            <el-table-column prop="forecastDate" label="预报日期" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDashboard } from '../../api/forecast'

const dashboard = ref({
  modelCount: 0,
  runningModelCount: 0,
  todayRecordCount: 0,
  latestSstData: [],
  latestChlData: []
})

onMounted(async () => {
  try {
    const res = await getDashboard()
    dashboard.value = res.data
  } catch (e) {
    // ignored
  }
})
</script>

<style scoped>
.page-title {
  margin-bottom: 20px;
  color: #1a3a5c;
  font-size: 20px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a3a5c;
}

.stat-label {
  color: #8899aa;
  font-size: 14px;
  margin-top: 4px;
}

.card-title {
  font-weight: 600;
  color: #1a3a5c;
}
</style>
