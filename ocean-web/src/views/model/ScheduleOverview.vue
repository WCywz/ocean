<template>
  <div class="schedule-overview">
    <div class="schedule-main">
      <h1 class="editorial-page-title">调度总览</h1>
      <p class="editorial-page-subtitle">Schedule Overview · {{ weekRangeText }} · 共 {{ schedules.length }} 个调度</p>
      <ScheduleCalendar
        :week-start="weekStart"
        :schedules="schedules"
        :color-map="colorMap"
        @cell-drop="onCellDrop"
        @header-drop="onHeaderDrop"
        @block-click="onBlockClick"
        @prev-week="onWeekChange"
        @next-week="onWeekChange"
      />
    </div>
    <VersionCardPool
      :versions="availableVersions"
      @drag-start="onDragStart"
      @drag-end="onDragEnd"
    />
    <ScheduleDialog
      v-model="dialogVisible"
      :version-label="dialogVersionLabel"
      :schedule="dialogSchedule"
      :date="dialogDate"
      :hide-once="dialogMode === 'header'"
      @submit="handleSubmit"
      @delete="handleDeleteSchedule"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ScheduleCalendar from './ScheduleCalendar.vue'
import VersionCardPool from './VersionCardPool.vue'
import ScheduleDialog from './ScheduleDialog.vue'
import {
  getWeekSchedules,
  getAvailableVersions,
  addSchedule,
  updateSchedule,
  deleteSchedule,
  getSystemDate
} from '../../api/model'

// --- Week state ---
const weekStart = ref('')

async function fetchSystemDate() {
  try {
    const res = await getSystemDate()
    return res.data || null
  } catch (e) {
    return null
  }
}

function getMonday(d) {
  const date = new Date(d)
  const day = date.getDay()
  const diff = date.getDate() - day + (day === 0 ? -6 : 1)
  date.setDate(diff)
  return formatDate(date)
}

function formatDate(d) {
  return d.getFullYear() + '-' +
    String(d.getMonth() + 1).padStart(2, '0') + '-' +
    String(d.getDate()).padStart(2, '0')
}

function getWeekEnd() {
  const d = new Date(weekStart.value)
  d.setDate(d.getDate() + 6)
  return formatDate(d)
}

const weekRangeText = computed(() => {
  return weekStart.value + ' — ' + getWeekEnd()
})

// --- Data ---
const schedules = ref([])
const availableVersions = ref([])
const colorMap = ref({})

async function loadSchedules() {
  try {
    const res = await getWeekSchedules({
      startDate: weekStart.value,
      endDate: getWeekEnd()
    })
    schedules.value = res.data || []
    buildColorMap()
  } catch (e) {
    ElMessage.error('加载调度数据失败')
  }
}

async function loadVersions() {
  try {
    const res = await getAvailableVersions()
    availableVersions.value = res.data || []
  } catch (e) {
    ElMessage.error('加载版本列表失败')
  }
}

function buildColorMap() {
  const models = [...new Set(schedules.value.map(s => s.modelName).filter(Boolean))]
  const colors = ['#2c3e50', '#555', '#777', '#999', '#bbb']
  const map = {}
  models.forEach((m, i) => { map[m] = i % colors.length })
  colorMap.value = map
}

// --- Dialog state ---
const dialogVisible = ref(false)
const dialogVersion = ref(null)
const dialogSchedule = ref(null)
const dialogDate = ref('')
const dialogMode = ref('default')

const dialogVersionLabel = computed(() => {
  const v = dialogVersion.value
  if (!v) return ''
  return (v.modelName || '') + ' ' + (v.versionLabel || '')
})

// --- Event handlers ---
function onWeekChange(newStart) {
  weekStart.value = newStart
  loadSchedules()
}

// 拖到时间格 → 直接创建 ONCE 调度
async function onCellDrop({ version, date, hour }) {
  const timeStr = String(hour).padStart(2, '0') + ':00'
  try {
    await addSchedule(version.modelId, version.versionId, {
      repetition: 'ONCE',
      scheduleTime: timeStr,
      scheduleDate: date
    })
    ElMessage.success(`已创建一次性调度: ${date} ${timeStr}`)
    loadSchedules()
  } catch (e) {
    ElMessage.error('调度创建失败')
  }
}

// 拖到日期表头 → 弹窗设置 DAILY / WEEKLY
function onHeaderDrop({ version, date }) {
  dialogVersion.value = version
  dialogSchedule.value = null
  dialogDate.value = date
  dialogMode.value = 'header'
  dialogVisible.value = true
}

function onBlockClick(schedule) {
  dialogVersion.value = {
    modelId: schedule.modelId,
    versionId: schedule.versionId,
    modelName: schedule.modelName,
    versionLabel: schedule.versionLabel
  }
  dialogSchedule.value = schedule
  dialogDate.value = schedule.scheduleDate || ''
  dialogMode.value = 'default'
  dialogVisible.value = true
}

async function handleSubmit(formData) {
  const v = dialogVersion.value
  if (!v) return
  try {
    if (dialogSchedule.value) {
      await updateSchedule(dialogSchedule.value.id, formData)
      ElMessage.success('调度更新成功')
    } else {
      await addSchedule(v.modelId, v.versionId, formData)
      ElMessage.success('调度创建成功')
    }
    dialogVisible.value = false
    loadSchedules()
  } catch (e) {
    ElMessage.error(dialogSchedule.value ? '调度更新失败' : '调度创建失败')
  }
}

async function handleDeleteSchedule(scheduleId) {
  try {
    await deleteSchedule(scheduleId)
    ElMessage.success('调度已删除')
    loadSchedules()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

function onDragStart() {}
function onDragEnd() {}

// --- Init ---
onMounted(async () => {
  const sysDate = await fetchSystemDate()
  weekStart.value = getMonday(sysDate ? new Date(sysDate) : new Date())
  loadSchedules()
  loadVersions()
})
</script>

<style scoped>
.schedule-overview {
  display: flex;
  height: calc(100vh - 64px);
}
.schedule-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.schedule-main .editorial-page-title {
  padding: 20px 16px 0 16px;
}
.schedule-main .editorial-page-subtitle {
  padding: 0 16px;
  margin-bottom: 20px;
}
.schedule-main > :last-child {
  flex: 1;
  min-height: 0;
}
.schedule-overview > :nth-child(2) {
  width: 280px;
  min-width: 280px;
  border-left: 1px solid var(--color-divider-strong);
  background: var(--color-surface);
}
</style>
