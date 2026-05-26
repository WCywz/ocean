<template>
  <div class="editorial-stats" style="cursor: pointer;" @click.stop="$emit('navigate')">
    <div v-for="card in cards" :key="card.label" class="editorial-stat" :class="{ 'editorial-stat--alert': card.isAlert }">
      <span class="editorial-stat__value">{{ card.value }}</span>
      <span class="editorial-stat__label">{{ card.label }}</span>
    </div>
    <span class="stats-nav-hint">模型管理 →</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelCount: { type: Number, default: 0 },
  runningModelCount: { type: Number, default: 0 },
  todayRecordCount: { type: Number, default: 0 },
  alertCount: { type: Number, default: 0 }
})

defineEmits(['navigate'])

const cards = computed(() => [
  { label: '模型总数', value: props.modelCount, isAlert: false },
  { label: '运行中', value: props.runningModelCount, isAlert: false },
  { label: '今日预报', value: props.todayRecordCount, isAlert: false },
  { label: '告警', value: props.alertCount, isAlert: props.alertCount > 0 }
])
</script>

<style scoped>
.editorial-stat {
  display: flex;
  align-items: baseline;
}

/* Remove global 1px border — parent DashboardView provides the 2px divider */
.editorial-stats {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.stats-nav-hint {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-left: auto;
  align-self: center;
  white-space: nowrap;
  transition: color 0.15s;
}
.stats-nav-hint:hover {
  color: var(--color-text);
}
</style>
