<template>
  <el-dialog v-model="visible" :title="isEdit ? '编辑版本' : `新增版本 — ${modelName}`" width="520px" :close-on-click-modal="false">
    <div style="margin-bottom: 18px;">
      <label class="editorial-form-label">版本号</label>
      <input class="editorial-input" :value="form.versionLabel" disabled style="color: #999;" />
    </div>
    <div style="margin-bottom: 18px;">
      <label class="editorial-form-label">运行周期 (Cron)</label>
      <input v-model="form.cronExpression" class="editorial-input" placeholder="如: 0 0 6 * * ?" />
    </div>
    <div style="margin-bottom: 18px;">
      <label class="editorial-form-label">参数配置 (JSON)</label>
      <textarea v-model="form.paramsConfig" class="editorial-input" rows="3" placeholder='{"algorithm":"ROMS"}' style="resize: vertical;"></textarea>
    </div>
    <div style="margin-bottom: 18px;">
      <label class="editorial-form-label">数据来源</label>
      <input v-model="form.dataSource" class="editorial-input" placeholder="如: 2025-2026 NOAA OISST" />
    </div>
    <div style="margin-bottom: 18px;">
      <label class="editorial-form-label">数据时间范围</label>
      <input v-model="form.dataTimeRange" class="editorial-input" placeholder="如: 2025-01 ~ 2026-04" />
    </div>
    <div style="margin-bottom: 18px;">
      <label class="editorial-form-label">变更说明</label>
      <textarea v-model="form.changeNote" class="editorial-input" rows="3" placeholder="相对上一版本的变更说明" style="resize: vertical;"></textarea>
    </div>
    <template #footer>
      <button class="editorial-btn-outline" @click="visible = false">取消</button>
      <button class="editorial-btn" style="padding: 8px 24px; margin-left: 12px;" @click="handleSubmit">确定</button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  version: { type: Object, default: null },
  modelName: { type: String, default: '' },
  nextVersionLabel: { type: String, default: 'v1' }
})

const emit = defineEmits(['update:modelValue', 'submit'])

const visible = ref(props.modelValue)
watch(() => props.modelValue, v => { visible.value = v })
watch(visible, v => { emit('update:modelValue', v) })

const isEdit = ref(false)
const form = reactive({
  versionLabel: '',
  cronExpression: '',
  paramsConfig: '',
  dataSource: '',
  dataTimeRange: '',
  changeNote: ''
})

watch(visible, (v) => {
  if (v) {
    if (props.version) {
      isEdit.value = true
      form.versionLabel = props.version.versionLabel
      form.cronExpression = props.version.cronExpression || ''
      form.paramsConfig = props.version.paramsConfig || ''
      form.dataSource = props.version.dataSource || ''
      form.dataTimeRange = props.version.dataTimeRange || ''
      form.changeNote = props.version.changeNote || ''
    } else {
      isEdit.value = false
      form.versionLabel = props.nextVersionLabel
      form.cronExpression = ''
      form.paramsConfig = ''
      form.dataSource = ''
      form.dataTimeRange = ''
      form.changeNote = ''
    }
  }
})

function handleSubmit() {
  emit('submit', {
    id: props.version?.id,
    versionLabel: form.versionLabel,
    cronExpression: form.cronExpression.trim(),
    paramsConfig: form.paramsConfig.trim(),
    dataSource: form.dataSource.trim(),
    dataTimeRange: form.dataTimeRange.trim(),
    changeNote: form.changeNote.trim()
  })
}
</script>
