<template>
  <div class="editorial-section">
    <p class="editorial-section-label">Alerts</p>
    <h3 class="editorial-section-heading">阈值告警</h3>
    <div v-if="!alerts.length && !loading" style="min-height: 200px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: var(--color-text-muted); font-size: 13px;">
      今日无阈值告警
    </div>
    <div v-loading="loading">
      <div
        v-for="(item, idx) in alerts.slice(0, 10)"
        :key="idx"
        class="alert-item"
        :style="{ borderLeftColor: item.value > (item.dataType === 'SST' ? 30 : 10) ? '#c0392b' : '#fa8c16' }"
      >
        <div style="font-size: 13px; font-weight: 600; color: var(--color-text);">{{ item.locationName }}</div>
        <div style="display: flex; align-items: center; gap: 8px; font-size: 12px; color: #666; margin-top: 4px;">
          <span class="editorial-tag" style="font-size: 10px;">{{ item.dataType }}</span>
          <span style="font-weight: 600; color: var(--color-alert);">{{ item.value }}{{ item.dataType === 'SST' ? '°C' : ' mg/m³' }}</span>
          <span style="color: var(--color-text-muted);">阈值 {{ item.threshold }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>

defineProps({
  alerts: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})
</script>

<style scoped>
.alert-item {
  padding: 10px 12px;
  margin-bottom: 8px;
  border-left: 3px solid;
}
</style>
