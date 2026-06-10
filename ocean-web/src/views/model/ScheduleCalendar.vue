<template>
  <div class="schedule-calendar">
    <div class="calendar-header">
      <button class="week-nav" @click="prevWeek">← 上一周</button>
      <span class="calendar-header__range">{{ weekRangeText }}</span>
      <div class="calendar-header__right">
        <el-date-picker
          v-model="pickerDate"
          type="date"
          placeholder="选择日期"
          style="width: 140px;"
          @change="onPickerChange"
        />
        <button class="week-nav" @click="nextWeek">下一周 →</button>
      </div>
    </div>

    <div
      class="calendar-grid-wrapper"
      @dragleave="onGridLeave"
    >
      <table class="calendar-grid">
        <thead>
          <tr>
            <th class="time-col">时间</th>
            <th
              v-for="(day, idx) in weekDays"
              :key="idx"
              class="day-col"
              :class="{
                'day-col--today': isToday(day),
                'day-col--drag-over': dragHeader === day
              }"
              @dragover.prevent="onHeaderDragOver($event, day)"
              @drop.prevent="onHeaderDrop($event, day)"
            >
              <div class="day-label">{{ dayNames[idx] }}</div>
              <div class="day-date">{{ day }}</div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="hour in hours" :key="hour">
            <td class="time-label">{{ pad(hour) }}:00</td>
            <td
              v-for="(day, dIdx) in weekDays"
              :key="dIdx"
              class="cell"
              :class="{
                'cell--today': isToday(day),
                'cell--drag-over': isDragOverCell(day, hour)
              }"
              @dragover.prevent="onDragOver($event, day, hour)"
              @drop.prevent="onDrop($event, day, hour)"
            >
              <ScheduleBlock
                v-for="(s, sIdx) in getSchedulesForDayHour(day, hour)"
                :key="s.id"
                :schedule="s"
                :color-index="getColorIndex(s)"
                @click="handleBlockClick(s)"
              />
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="allSchedules.length === 0" class="calendar-empty">
      暂无调度配置，从右侧拖拽版本卡片到此区域
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import ScheduleBlock from './ScheduleBlock.vue'

const props = defineProps({
  weekStart: { type: String, required: true },
  schedules: { type: Array, default: () => [] },
  colorMap: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['prev-week', 'next-week', 'cell-drop', 'header-drop', 'block-click'])

const dayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const hours = Array.from({ length: 24 }, (_, i) => i)

const currentStart = ref(props.weekStart)

// 拖拽高亮状态
const dragCell = ref(null)
const dragHeader = ref(null)

// 日期选择器
const pickerDate = ref(null)

function getMonday(d) {
  const date = new Date(d)
  const day = date.getDay()
  const diff = date.getDate() - day + (day === 0 ? -6 : 1)
  date.setDate(diff)
  return date
}

function onPickerChange(val) {
  if (!val) return
  const monday = getMonday(val)
  currentStart.value = formatDate(monday)
  emit('prev-week', currentStart.value)
  pickerDate.value = null
}

watch(() => props.weekStart, (v) => { currentStart.value = v })

const weekDays = computed(() => {
  const start = new Date(currentStart.value)
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(start)
    d.setDate(d.getDate() + i)
    return formatDate(d)
  })
})

const weekRangeText = computed(() => {
  return weekDays.value[0] + ' — ' + weekDays.value[6]
})

const allSchedules = computed(() => props.schedules)

function formatDate(d) {
  return d.getFullYear() + '-' +
    String(d.getMonth() + 1).padStart(2, '0') + '-' +
    String(d.getDate()).padStart(2, '0')
}

function isToday(dateStr) {
  const today = formatDate(new Date())
  return dateStr === today
}

function pad(n) { return String(n).padStart(2, '0') }

function isDragOverCell(day, hour) {
  return dragCell.value && dragCell.value.day === day && dragCell.value.hour === hour
}

function getSchedulesForDayHour(day, hour) {
  return allSchedules.value.filter(s => {
    const timeStr = s.scheduleTime
    if (!timeStr) return false
    const h = typeof timeStr === 'string'
      ? parseInt(timeStr.substring(0, 2))
      : timeStr.hour || 0
    if (h !== hour) return false

    if (s.repetition === 'DAILY') return true
    if (s.repetition === 'ONCE') return s.scheduleDate === day
    if (s.repetition === 'WEEKLY') {
      const d = new Date(day)
      const dow = d.getDay()
      const expected = dow === 0 ? 7 : dow
      return s.dayOfWeek === expected
    }
    return false
  })
}

function getColorIndex(schedule) {
  const key = schedule.modelName || ''
  return props.colorMap[key] ?? 0
}

function onDragOver(e, day, hour) {
  e.dataTransfer.dropEffect = 'copy'
  dragCell.value = { day, hour }
  dragHeader.value = null
}

function onDrop(e, day, hour) {
  dragCell.value = null
  try {
    const json = e.dataTransfer.getData('application/json')
    const version = JSON.parse(json)
    emit('cell-drop', { version, date: day, hour })
  } catch (err) {
    // ignore invalid drops
  }
}

function onHeaderDragOver(e, day) {
  e.dataTransfer.dropEffect = 'copy'
  dragHeader.value = day
  dragCell.value = null
}

function onHeaderDrop(e, day) {
  dragHeader.value = null
  try {
    const json = e.dataTransfer.getData('application/json')
    const version = JSON.parse(json)
    emit('header-drop', { version, date: day })
  } catch (err) {
    // ignore invalid drops
  }
}

function onGridLeave() {
  dragCell.value = null
  dragHeader.value = null
}

function handleBlockClick(schedule) {
  emit('block-click', schedule)
}

function prevWeek() {
  const d = new Date(currentStart.value)
  d.setDate(d.getDate() - 7)
  currentStart.value = formatDate(d)
  emit('prev-week', currentStart.value)
}

function nextWeek() {
  const d = new Date(currentStart.value)
  d.setDate(d.getDate() + 7)
  currentStart.value = formatDate(d)
  emit('next-week', currentStart.value)
}
</script>

<style scoped>
.schedule-calendar {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
}
.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-divider-strong);
}
.week-nav {
  font-family: inherit;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: none;
  border: 1px solid var(--color-divider);
  padding: 5px 16px;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
  letter-spacing: 0.04em;
}
.week-nav:hover {
  color: var(--color-text);
  border-color: var(--color-text-secondary);
}
.calendar-header__right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.calendar-header__range {
  font-family: var(--font-serif);
  font-size: 13px;
  color: var(--color-text-muted);
  font-style: italic;
}
.calendar-grid-wrapper {
  flex: 1;
  overflow-y: auto;
}
.calendar-grid {
  width: 100%;
  border-collapse: collapse;
  font-size: 11px;
}
.time-col {
  width: 50px;
  padding: 4px 6px;
  color: var(--color-text-muted);
  font-size: 9px;
  text-align: right;
  border-right: 1px solid var(--color-divider-strong);
}
.day-col {
  padding: 6px 4px;
  border-bottom: 1px solid var(--color-divider);
  text-align: center;
}
.day-col--today {
  background: var(--color-surface);
}
.day-label {
  font-size: 10px;
  color: var(--color-text-muted);
}
.day-date {
  font-family: var(--font-serif);
  font-size: 13px;
  color: var(--color-text);
}
.time-label {
  padding: 2px 6px;
  color: var(--color-text-muted);
  font-size: 9px;
  text-align: right;
  border-right: 1px solid var(--color-divider-strong);
  vertical-align: top;
  padding-top: 6px;
}
.cell {
  height: 36px;
  padding: 2px 3px;
  border: 1px solid var(--color-divider);
  vertical-align: top;
  transition: background 0.1s;
}
.cell:hover {
  background: var(--color-surface);
}
.cell--today {
  background: var(--color-surface);
}
.cell--drag-over {
  background: var(--color-divider);
  border-left: 2px solid var(--color-text);
}
.day-col--drag-over {
  border-bottom: 2px solid var(--color-text);
  background: var(--color-divider);
}
.calendar-empty {
  font-family: var(--font-serif);
  font-size: 15px;
  color: var(--color-text-muted);
  font-style: italic;
  text-align: center;
  padding: 60px 0;
}
</style>
