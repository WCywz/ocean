<template>
  <div style="cursor: pointer;" @click.stop="$emit('navigate')">
    <div style="display: flex; justify-content: space-between; align-items: baseline;">
      <p class="editorial-section-label">数据附录</p>
      <span class="table-nav-hint">观测数据 →</span>
    </div>
    <table class="editorial-table">
      <thead>
        <tr>
          <td>观测点</td>
          <td>{{ valueLabel }}</td>
          <td>预报日期</td>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, idx) in data" :key="idx">
          <td>{{ row.locationName }}</td>
          <td>{{ row.value }} {{ unit }}</td>
          <td class="text-muted">{{ row.forecastDate }}</td>
        </tr>
        <tr v-if="!data.length && !loading">
          <td colspan="3" class="text-muted" style="text-align: center;">暂无数据</td>
        </tr>
      </tbody>
    </table>
    <div v-loading="loading" style="min-height: 120px;" v-if="loading"></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  data: { type: Array, default: () => [] },
  dataType: { type: String, default: 'SST' },
  loading: { type: Boolean, default: false }
})

defineEmits(['navigate'])

const unit = computed(() => props.dataType === 'SST' ? '°C' : 'mg/m³')
const valueLabel = computed(() => props.dataType === 'SST' ? '温度值' : '浓度值')
</script>

<style scoped>
.table-nav-hint {
  font-size: 11px;
  color: var(--color-text-muted);
  white-space: nowrap;
}
</style>
