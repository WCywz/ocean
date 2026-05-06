<template>
  <el-row :gutter="20" class="stat-row">
    <el-col :span="6" v-for="card in cards" :key="card.label">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" :style="{ background: card.bg }">
            <el-icon :size="28" :color="card.color"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value" :style="{ color: card.valueColor || '#1a3a5c' }">
              {{ card.value }}
            </div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { computed } from 'vue'
import { Setting, VideoPlay, TrendCharts, Warning } from '@element-plus/icons-vue'

const props = defineProps({
  modelCount: { type: Number, default: 0 },
  runningModelCount: { type: Number, default: 0 },
  todayRecordCount: { type: Number, default: 0 },
  alertCount: { type: Number, default: 0 }
})

const cards = computed(() => [
  {
    label: '模型总数',
    value: props.modelCount,
    icon: Setting,
    bg: '#e6f7ff',
    color: '#1890ff',
    valueColor: '#1a3a5c'
  },
  {
    label: '运行中模型',
    value: props.runningModelCount,
    icon: VideoPlay,
    bg: '#f6ffed',
    color: '#52c41a',
    valueColor: '#1a3a5c'
  },
  {
    label: '今日预报记录',
    value: props.todayRecordCount,
    icon: TrendCharts,
    bg: '#fff7e6',
    color: '#fa8c16',
    valueColor: '#1a3a5c'
  },
  {
    label: '阈值告警',
    value: props.alertCount,
    icon: Warning,
    bg: props.alertCount > 0 ? '#fff2f0' : '#f6ffed',
    color: props.alertCount > 0 ? '#e74c3c' : '#52c41a',
    valueColor: props.alertCount > 0 ? '#e74c3c' : '#52c41a'
  }
])
</script>

<style scoped>
.stat-row { margin-bottom: 20px; }
.stat-card { cursor: pointer; transition: transform 0.2s; }
.stat-card:hover { transform: translateY(-2px); }
.stat-content { display: flex; align-items: center; gap: 14px; }
.stat-icon {
  width: 52px; height: 52px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.stat-value { font-size: 26px; font-weight: 700; }
.stat-label { color: #8899aa; font-size: 13px; margin-top: 2px; }
</style>
