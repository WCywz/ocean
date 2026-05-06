<template>
  <div class="editorial-stats">
    <div v-for="card in cards" :key="card.label" class="editorial-stat" :class="{ 'editorial-stat--alert': card.isAlert }">
      <span class="editorial-stat__value">{{ card.value }}</span>
      <span class="editorial-stat__label">{{ card.label }}</span>
    </div>
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
</style>
