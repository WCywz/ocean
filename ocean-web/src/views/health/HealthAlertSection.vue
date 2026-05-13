<template>
  <div class="editorial-section">
    <p class="editorial-section-label">Alerts</p>
    <h3 class="editorial-section-heading">阈值告警</h3>

    <div class="health-status-bar" :style="{ borderLeftColor: accentColor }">
      <span class="health-status-bar__level">{{ summaryText }}</span>
    </div>

    <div v-if="!alerts.length && !loading" style="min-height: 120px; display: flex; align-items: center; justify-content: center; color: var(--color-text-muted); font-size: 13px;">
      所选日期无阈值告警
    </div>

    <div v-loading="loading">
      <div
        v-for="(item, idx) in alerts.slice(0, 10)"
        :key="idx"
        class="alert-item"
        :style="{ borderLeftColor: item.dataType === 'SST' ? '#c0392b' : '#e67e22' }"
      >
        <div style="font-size: 13px; font-weight: 600; color: var(--color-text);">{{ item.locationName }}</div>
        <div style="display: flex; align-items: center; gap: 8px; font-size: 12px; color: #666; margin-top: 4px;">
          <span class="editorial-tag" style="font-size: 10px;">{{ item.dataType }}</span>
          <span style="font-weight: 600; color: var(--color-alert);">{{ item.value }}{{ item.dataType === 'SST' ? '°C' : ' mg/m³' }}</span>
          <span style="color: var(--color-text-muted);">阈值 {{ item.threshold }}</span>
        </div>
      </div>
    </div>

    <div class="drilldown-links" v-if="alerts.length">
      <span class="drilldown-label">查看详情：</span>
      <router-link to="/app/forecast/sst" class="drilldown-link">海表温度预测地图 →</router-link>
      <router-link to="/app/forecast/chl" class="drilldown-link">叶绿素浓度预测地图 →</router-link>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  alerts: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const sstCount = computed(() =>
  props.alerts.filter(a => a.dataType === 'SST').length
)

const chlCount = computed(() =>
  props.alerts.filter(a => a.dataType === 'CHL').length
)

const summaryText = computed(() => {
  if (!props.alerts.length) return '暂无告警'
  const parts = []
  if (sstCount.value) parts.push(`${sstCount.value} 个区域超过 SST 阈值`)
  if (chlCount.value) parts.push(`${chlCount.value} 个区域超过 Chl 阈值`)
  return parts.join('，')
})

const accentColor = computed(() => {
  if (!props.alerts.length) return '#22c55e'
  const hasSst = sstCount.value > 0
  const hasChl = chlCount.value > 0
  if (hasSst && hasChl) return '#ef4444'
  if (hasSst) return '#c0392b'
  return '#e67e22'
})

</script>

<style scoped>
.health-status-bar {
  display: flex;
  align-items: center;
  border-left: 3px solid;
  padding: 10px 14px;
  background: #fafafa;
  font-size: 13px;
  margin-bottom: 16px;
}

.health-status-bar__level {
  font-family: var(--font-serif);
  font-size: 15px;
  color: var(--color-text);
}

.alert-item {
  padding: 10px 12px;
  margin-bottom: 8px;
  border-left: 3px solid;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  border-right: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.drilldown-links {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
}

.drilldown-label {
  color: var(--color-text-muted);
  margin-right: 16px;
}

.drilldown-link {
  color: var(--color-text);
  cursor: pointer;
  margin-right: 20px;
  text-decoration: none;
  border-bottom: 1px dashed #ccc;
}

.drilldown-link:hover {
  color: var(--color-alert);
  border-bottom-color: var(--color-alert);
}
</style>
