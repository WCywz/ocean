<template>
  <el-card shadow="hover" class="alert-panel">
    <template #header>
      <div class="card-header">
        <span class="card-title">阈值告警</span>
        <el-tag v-if="alerts.length" type="danger" size="small">{{ alerts.length }} 条</el-tag>
        <el-tag v-else type="success" size="small">正常</el-tag>
      </div>
    </template>
    <div v-if="!alerts.length && !loading" class="empty-state">
      <el-icon :size="36" color="#52c41a"><CircleCheck /></el-icon>
      <span style="margin-top: 8px; color: #999;">今日无阈值告警</span>
    </div>
    <div v-loading="loading" class="alert-list">
      <div
        v-for="(item, idx) in alerts.slice(0, 10)"
        :key="idx"
        class="alert-item"
        :class="item.value > (item.dataType === 'SST' ? 30 : 10) ? 'critical' : 'warning'"
      >
        <div class="alert-location">{{ item.locationName }}</div>
        <div class="alert-meta">
          <el-tag :type="item.dataType === 'SST' ? 'danger' : ''" size="small" effect="plain">
            {{ item.dataType }}
          </el-tag>
          <span class="alert-value">{{ item.value }}{{ item.dataType === 'SST' ? '°C' : ' mg/m³' }}</span>
          <span class="alert-threshold">阈值 {{ item.threshold }}</span>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { CircleCheck } from '@element-plus/icons-vue'

defineProps({
  alerts: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})
</script>

<style scoped>
.alert-panel { height: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: 600; color: #1a3a5c; }
.alert-list { min-height: 200px; }
.alert-item {
  padding: 10px 12px; border-radius: 6px; margin-bottom: 8px;
  border-left: 3px solid; display: flex; justify-content: space-between; align-items: center;
}
.alert-item.warning { background: #fffbe6; border-color: #fa8c16; }
.alert-item.critical { background: #fff2f0; border-color: #e74c3c; }
.alert-location { font-size: 13px; font-weight: 600; color: #333; }
.alert-meta { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #666; }
.alert-value { font-weight: 600; color: #e74c3c; }
.alert-threshold { color: #999; }
.empty-state {
  min-height: 200px; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
}
</style>
