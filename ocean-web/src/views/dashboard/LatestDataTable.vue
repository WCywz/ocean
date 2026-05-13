<template>
  <div style="cursor: pointer;" @click.stop="$emit('navigate')">
    <div style="display: flex; justify-content: space-between; align-items: baseline;">
      <p class="editorial-section-label">数据附录</p>
      <span class="table-nav-hint">观测数据 →</span>
    </div>
    <div class="table-scroll-wrap">
      <table class="editorial-table">
        <thead>
          <tr>
            <td class="sticky-th">观测点</td>
            <td class="sticky-th">{{ valueLabel }}</td>
            <td class="sticky-th">预报日期</td>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, idx) in data.slice(0, 8)" :key="idx">
            <td>{{ row.locationName }}</td>
            <td>{{ row.value }} {{ unit }}</td>
            <td class="text-muted">{{ row.forecastDate }}</td>
          </tr>
          <tr v-if="!data.length && !loading">
            <td colspan="3" class="text-muted" style="text-align: center;">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-loading="loading" style="min-height: 80px;" v-if="loading"></div>
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

.table-scroll-wrap {
  overflow-y: auto;
}

.sticky-th {
  position: sticky;
  top: 0;
  background: #fff;
  z-index: 1;
}
</style>
