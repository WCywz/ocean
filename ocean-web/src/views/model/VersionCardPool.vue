<template>
  <div class="card-pool">
    <div class="card-pool__header">
      <span class="editorial-section-label">可调度版本</span>
      <span class="card-pool__count">{{ filteredVersions.length }}</span>
    </div>
    <input
      v-model="search"
      class="editorial-search"
      style="width:100%;box-sizing:border-box;margin-bottom:10px;"
      placeholder="搜索版本..."
    />
    <label class="card-pool__filter">
      <input type="checkbox" v-model="runningOnly" /> 仅显示运行中
    </label>
    <div class="card-pool__list">
      <div
        v-for="(v, idx) in filteredVersions"
        :key="v.versionId || v.id"
        class="version-card"
        :class="{ 'version-card--running': isRunning(v) }"
        :draggable="true"
        @dragstart="onDragStart($event, v, idx)"
        @dragend="onDragEnd"
      >
        <div class="version-card__name">{{ v.modelName }} {{ v.versionLabel }}</div>
        <div class="version-card__meta">
          <span>{{ v.modelType || '' }}</span>
          <span class="version-card__status">{{ isRunning(v) ? '运行中' : '已停止' }}</span>
        </div>
        <div v-if="v.schedules && v.schedules.length" class="version-card__schedules">
          已调度: {{ v.schedules.map(s => scheduleBrief(s)).join(', ') }}
        </div>
        <div v-else class="version-card__schedules" style="color: var(--color-text-muted);">
          未配置调度
        </div>
      </div>
      <div v-if="filteredVersions.length === 0" class="card-pool__empty">
        暂无匹配版本
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  versions: { type: Array, default: () => [] }
})

const emit = defineEmits(['drag-start', 'drag-end'])

const search = ref('')
const runningOnly = ref(true)

const filteredVersions = computed(() => {
  return props.versions.filter(v => {
    const label = (v.modelName || '') + ' ' + (v.versionLabel || '')
    const match = !search.value || label.toLowerCase().includes(search.value.toLowerCase())
    const status = !runningOnly.value || v.status === 'RUNNING'
    return match && status
  })
})

function isRunning(v) {
  return v.status === 'RUNNING'
}

function scheduleBrief(s) {
  if (s.repetition === 'DAILY') return '每天'
  if (s.repetition === 'WEEKLY') {
    const days = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
    return (days[s.dayOfWeek] || '每周') + (s.scheduleTime ? ' ' + s.scheduleTime : '')
  }
  if (s.repetition === 'ONCE') return '仅一次（' + (s.scheduleDate || '') + '）'
  return ''
}

function onDragStart(e, v, idx) {
  e.dataTransfer.effectAllowed = 'copy'
  e.dataTransfer.setData('application/json', JSON.stringify(v))
  emit('drag-start', v)
}

function onDragEnd() {
  emit('drag-end')
}
</script>

<style scoped>
.card-pool {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.card-pool__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-pool__count {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--color-text-muted);
}
.card-pool__filter {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}
.card-pool__list {
  flex: 1;
  overflow-y: auto;
}
.version-card {
  border: 1px solid var(--color-border-light);
  padding: 10px 12px;
  margin-bottom: 6px;
  cursor: grab;
  background: var(--color-bg);
  transition: border-color 0.15s;
}
.version-card:hover {
  border-color: var(--color-text);
}
.version-card--running {
  border-left: 2px solid var(--color-text);
}
.version-card__name {
  font-weight: 600;
  font-size: 13px;
  color: var(--color-text);
  margin-bottom: 4px;
}
.version-card__meta {
  font-size: 11px;
  color: var(--color-text-muted);
  display: flex;
  justify-content: space-between;
}
.version-card__status {
  font-family: var(--font-mono);
  font-size: 10px;
}
.version-card__schedules {
  font-size: 10px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}
.card-pool__empty {
  font-size: 12px;
  color: var(--color-text-muted);
  text-align: center;
  padding: 24px 0;
}
</style>
