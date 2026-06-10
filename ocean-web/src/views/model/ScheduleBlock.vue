<template>
  <div
    class="schedule-block"
    :style="{ backgroundColor: blockColor }"
    @click.stop="$emit('click', schedule)"
    :title="`${schedule.modelName} ${schedule.versionLabel} · ${labelText}`"
  >
    {{ schedule.modelName }} {{ schedule.versionLabel }}
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  schedule: { type: Object, required: true },
  colorIndex: { type: Number, default: 0 }
})

defineEmits(['click'])

const COLORS = ['#2c3e50', '#555', '#777', '#999', '#bbb']

const blockColor = computed(() => COLORS[props.colorIndex % COLORS.length])

const labelText = computed(() => {
  const s = props.schedule
  if (s.repetition === 'DAILY') return '每天'
  if (s.repetition === 'WEEKLY') {
    const days = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
    return days[s.dayOfWeek] || '每周'
  }
  if (s.repetition === 'ONCE') return '仅一次（' + (s.scheduleDate || '') + '）'
  return ''
})
</script>

<style scoped>
.schedule-block {
  color: #fff;
  font-size: 10px;
  padding: 2px 5px;
  margin: 1px 0;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
