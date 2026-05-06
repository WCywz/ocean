<template>
  <el-card shadow="hover" class="data-table-card">
    <template #header>
      <span class="card-title">{{ title }}</span>
    </template>
    <el-table :data="data" size="small" stripe v-loading="loading" empty-text="暂无数据" max-height="280">
      <el-table-column prop="locationName" label="观测点" />
      <el-table-column prop="value" :label="valueLabel">
        <template #default="{ row }">{{ row.value }} {{ unit }}</template>
      </el-table-column>
      <el-table-column prop="forecastDate" label="预报日期" />
    </el-table>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  data: { type: Array, default: () => [] },
  dataType: { type: String, default: 'SST' },
  loading: { type: Boolean, default: false }
})

const unit = computed(() => props.dataType === 'SST' ? '°C' : 'mg/m³')
const valueLabel = computed(() => props.dataType === 'SST' ? '温度值' : '浓度值')
</script>

<style scoped>
.data-table-card { height: 100%; }
.card-title { font-weight: 600; color: #1a3a5c; }
</style>
