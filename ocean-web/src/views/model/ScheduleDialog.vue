<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑调度' : '新建调度'"
    width="420px"
    :close-on-click-modal="false"
    append-to-body
  >
    <div class="schedule-dialog-body">
      <el-form label-position="top" size="default">
        <el-form-item v-if="!isEdit" label="版本">
          <div class="editorial-tag">{{ versionLabel }}</div>
        </el-form-item>
        <el-form-item label="调度标签">
          <input class="editorial-input" v-model="form.scheduleLabel" placeholder="可选标签" />
        </el-form-item>
        <el-form-item label="时间">
          <el-time-picker
            v-model="form.scheduleTime"
            format="HH:mm"
            value-format="HH:mm"
            placeholder="选择时间"
            :teleported="false"
          />
        </el-form-item>
        <el-form-item label="重复规则">
          <el-select v-model="form.repetition" style="width: 100%;">
            <el-option label="每天" value="DAILY" />
            <el-option label="每周" value="WEEKLY" />
            <el-option v-if="!hideOnce" :label="'仅一次（' + displayDate + '）'" value="ONCE" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.repetition === 'WEEKLY'" label="星期">
          <el-select v-model="form.dayOfWeek" style="width: 100%;">
            <el-option :label="'周一'" :value="1" />
            <el-option :label="'周二'" :value="2" />
            <el-option :label="'周三'" :value="3" />
            <el-option :label="'周四'" :value="4" />
            <el-option :label="'周五'" :value="5" />
            <el-option :label="'周六'" :value="6" />
            <el-option :label="'周日'" :value="7" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <div class="schedule-dialog-footer">
        <button v-if="isEdit" class="editorial-link" style="color: var(--color-alert);" @click="handleDelete">删除调度</button>
        <div style="flex:1;"></div>
        <button class="editorial-btn-outline" @click="visible = false">取消</button>
        <button class="editorial-btn" style="padding-left:24px;padding-right:24px;" @click="handleConfirm">确认</button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  versionLabel: { type: String, default: '' },
  schedule: { type: Object, default: null },
  date: { type: String, default: '' },
  hideOnce: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'submit', 'delete'])

const visible = ref(false)

watch(() => props.modelValue, (v) => {
  visible.value = v
  if (v) {
    if (props.schedule) {
      form.scheduleLabel = props.schedule.scheduleLabel || ''
      form.repetition = props.schedule.repetition || 'DAILY'
      form.dayOfWeek = props.schedule.dayOfWeek || 1
      const t = props.schedule.scheduleTime
      form.scheduleTime = typeof t === 'string' ? t.substring(0, 5) : '06:00'
    } else {
      form.scheduleLabel = ''
      form.repetition = props.hideOnce ? 'DAILY' : 'DAILY'
      form.dayOfWeek = props.date ? dateToDayOfWeek(props.date) : 1
      form.scheduleTime = '06:00'
    }
  }
})
watch(visible, (v) => { if (!v) emit('update:modelValue', false) })

const form = reactive({
  scheduleLabel: '',
  repetition: 'DAILY',
  dayOfWeek: 1,
  scheduleTime: '06:00'
})

const isEdit = computed(() => !!props.schedule)
const displayDate = computed(() => props.date || '')

function dateToDayOfWeek(dateStr) {
  const d = new Date(dateStr)
  const dow = d.getDay()
  return dow === 0 ? 7 : dow
}

function handleConfirm() {
  emit('submit', {
    scheduleLabel: form.scheduleLabel,
    repetition: form.repetition,
    dayOfWeek: form.repetition === 'WEEKLY' ? form.dayOfWeek : null,
    scheduleTime: form.scheduleTime,
    scheduleDate: form.repetition === 'ONCE' ? props.date : null
  })
  visible.value = false
}

function handleDelete() {
  emit('delete', props.schedule.id)
  visible.value = false
}
</script>

<style scoped>
.schedule-dialog-body {
  padding: 8px 0;
}
.schedule-dialog-footer {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
