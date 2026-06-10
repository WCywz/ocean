<template>
  <el-dialog v-model="visible" :title="isEdit ? '编辑模型' : '新增模型'" width="480px" :close-on-click-modal="false">
    <div style="margin-bottom: 18px;">
      <label class="editorial-form-label">模型名称</label>
      <input v-model="form.modelName" class="editorial-input" placeholder="请输入模型名称" />
    </div>
    <div style="margin-bottom: 18px;">
      <label class="editorial-form-label">模型类型</label>
      <el-select v-model="form.modelType" style="width: 100%;">
        <el-option label="海表温度 (SST)" value="SST" />
        <el-option label="叶绿素浓度 (CHL)" value="CHL" />
        <el-option label="盐度 (SALINITY)" value="SALINITY" />
        <el-option label="自定义..." value="__custom__" />
      </el-select>
      <input
        v-if="form.modelType === '__custom__'"
        v-model="form.customType"
        class="editorial-input"
        placeholder="输入自定义类型"
        style="margin-top: 8px;"
      />
    </div>
    <div style="margin-bottom: 18px;">
      <label class="editorial-form-label">模型介绍</label>
      <textarea v-model="form.description" class="editorial-input" rows="4" placeholder="模型功能、适用场景、方法论等" style="resize: vertical;"></textarea>
    </div>
    <template #footer>
      <button class="editorial-btn-outline" @click="visible = false">取消</button>
      <button class="editorial-btn" style="padding: 8px 24px; margin-left: 12px;" @click="handleSubmit">确定</button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  model: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'submit'])

const visible = ref(props.modelValue)
watch(() => props.modelValue, v => { visible.value = v })
watch(visible, v => { emit('update:modelValue', v) })

const isEdit = ref(false)
const form = reactive({ modelName: '', modelType: 'SST', customType: '', description: '' })

watch(visible, (v) => {
  if (v) {
    if (props.model) {
      isEdit.value = true
      form.modelName = props.model.modelName
      form.description = props.model.description || ''
      const knownTypes = ['SST', 'CHL', 'SALINITY']
      if (knownTypes.includes(props.model.modelType)) {
        form.modelType = props.model.modelType
        form.customType = ''
      } else {
        form.modelType = '__custom__'
        form.customType = props.model.modelType || ''
      }
    } else {
      isEdit.value = false
      form.modelName = ''
      form.modelType = 'SST'
      form.customType = ''
      form.description = ''
    }
  }
})

function handleSubmit() {
  if (!form.modelName.trim()) { ElMessage.warning('请输入模型名称'); return }
  const modelType = form.modelType === '__custom__' ? form.customType.trim() : form.modelType
  if (!modelType) { ElMessage.warning('请输入模型类型'); return }
  emit('submit', {
    id: props.model?.id,
    modelName: form.modelName.trim(),
    modelType,
    description: form.description.trim()
  })
}
</script>
