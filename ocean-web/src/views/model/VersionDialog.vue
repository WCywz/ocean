<template>
  <el-dialog v-model="visible" :title="isEdit ? '编辑版本' : `新建版本 — ${modelName}`" width="600px" :close-on-click-modal="false" @opened="onOpened">
    <div style="max-height: 60vh; overflow-y: auto; padding-right: 4px;">

      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">版本号</label>
        <input class="editorial-input" :value="form.versionLabel" disabled style="color: var(--color-text-muted);" />
      </div>

      <!-- ── 运行时配置 ── -->
      <div class="form-section">
        <div class="form-section__title">运行时配置（系统执行需要）</div>

        <div class="form-row">
          <div class="form-col">
            <label class="editorial-form-label">预测目标</label>
            <el-select v-model="runtime.target" style="width: 100%;">
              <el-option label="海表温度 (SST)" value="SST" />
              <el-option label="叶绿素浓度 (CHL)" value="CHL" />
              <el-option label="盐度 (SO)" value="SO" />
            </el-select>
          </div>
          <div class="form-col">
            <label class="editorial-form-label">模型权重文件</label>
            <input v-model="runtime.modelFile" class="editorial-input" placeholder="best_model_sst.pt" />
          </div>
        </div>

        <div class="form-row">
          <div class="form-col form-col--3">
            <label class="editorial-form-label">序列长度（输入天数）</label>
            <input v-model.number="runtime.seqLen" class="editorial-input" type="number" min="1" placeholder="30" />
          </div>
          <div class="form-col form-col--3">
            <label class="editorial-form-label">预测天数</label>
            <input v-model.number="runtime.horizon" class="editorial-input" type="number" min="1" placeholder="7" />
          </div>
          <div class="form-col form-col--3">
            <label class="editorial-form-label">深度层数</label>
            <input v-model.number="runtime.depthLevels" class="editorial-input" type="number" min="0" placeholder="18" />
          </div>
        </div>

        <div>
          <label class="editorial-form-label">归一化文件（每行一个）</label>
          <textarea v-model="runtime.scalerFiles" class="editorial-input" rows="3" placeholder="scaler_chl.pkl&#10;scaler_temp.pkl&#10;scaler_so.pkl" style="resize: vertical;"></textarea>
        </div>
      </div>

      <!-- ── 训练元数据 ── -->
      <div class="form-section">
        <div class="form-section__title">
          训练元数据（科学记录）
          <a class="editorial-link" style="font-weight: 400; margin-left: 12px;" @click="addMeta">+ 添加</a>
        </div>

        <div v-if="trainingMeta.length === 0" style="font-size: 12px; color: var(--color-text-muted); padding: 8px 0;">
          暂无记录，点击「+ 添加」记录训练超参数
        </div>

        <div v-else class="kv-table">
          <div class="kv-table__header">
            <span>参数名</span>
            <span>值</span>
            <span>类型</span>
            <span></span>
          </div>
          <div v-for="(item, i) in trainingMeta" :key="i" class="kv-table__row">
            <input v-model="item.key" class="editorial-input" placeholder="参数名" style="font-size: 12px;" />
            <input v-model="item.value" class="editorial-input" placeholder="值" style="font-size: 12px;" />
            <el-select v-model="item.type" style="font-size: 12px; width: 90px;">
              <el-option label="number" value="number" />
              <el-option label="string" value="string" />
              <el-option label="boolean" value="boolean" />
            </el-select>
            <a class="editorial-link editorial-link--muted" @click="removeMeta(i)">删除</a>
          </div>
        </div>
      </div>

      <!-- ── 数据配置 ── -->
      <div class="form-section">
        <div class="form-section__title">数据配置</div>

        <div class="form-row">
          <div class="form-col">
            <label class="editorial-form-label">数据源</label>
            <el-select v-model="dataSourceSelect" style="width: 100%;" @change="onDataSourceChange">
              <el-option label="observation_grid" value="observation_grid" />
              <el-option label="observation_data" value="observation_data" />
              <el-option label="CSV 文件" value="csv" />
              <el-option label="自定义..." value="__custom__" />
            </el-select>
            <input
              v-if="dataSourceSelect === '__custom__'"
              v-model="form.dataSource"
              class="editorial-input"
              placeholder="输入自定义数据源"
              style="margin-top: 6px;"
            />
          </div>
          <div class="form-col">
            <label class="editorial-form-label">训练数据时间范围</label>
            <input v-model="form.dataTimeRange" class="editorial-input" placeholder="2025-01-01 ~ 2025-06-30" />
          </div>
        </div>
      </div>

      <!-- ── 备注 ── -->
      <div class="form-section">
        <div class="form-section__title">备注</div>
        <div>
          <label class="editorial-form-label">变更说明</label>
          <textarea v-model="form.changeNote" class="editorial-input" rows="3" placeholder="相对上一版本的变更说明" style="resize: vertical;"></textarea>
        </div>
      </div>

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

// Runtime fields
const runtime = reactive({
  target: 'SST',
  modelFile: '',
  seqLen: 30,
  horizon: 7,
  depthLevels: 18,
  scalerFiles: ''
})

// Training metadata KV list
const trainingMeta = ref([])

// Data source dropdown
const dataSourceSelect = ref('observation_grid')

function parseRuntimeFromJson(json) {
  try {
    const obj = typeof json === 'string' ? JSON.parse(json) : json
    return obj?.runtime || {}
  } catch { return {} }
}

function parseTrainingFromJson(json) {
  try {
    const obj = typeof json === 'string' ? JSON.parse(json) : json
    const t = obj?.training || {}
    return Object.entries(t).map(([key, value]) => {
      let type = 'string'
      if (typeof value === 'number') type = 'number'
      else if (typeof value === 'boolean') type = 'boolean'
      return { key, value: String(value), type }
    })
  } catch { return [] }
}

function detectDataSourceSelect(ds) {
  if (!ds) return 'observation_grid'
  const known = ['observation_grid', 'observation_data', 'csv']
  return known.includes(ds) ? ds : '__custom__'
}

function resetForm() {
  runtime.target = 'SST'
  runtime.modelFile = ''
  runtime.seqLen = 30
  runtime.horizon = 7
  runtime.depthLevels = 18
  runtime.scalerFiles = ''
  trainingMeta.value = []
  dataSourceSelect.value = 'observation_grid'
  form.versionLabel = ''
  form.cronExpression = ''
  form.paramsConfig = ''
  form.dataSource = ''
  form.dataTimeRange = ''
  form.changeNote = ''
}

function onOpened() {
}

watch(visible, (v) => {
  if (v) {
    const vv = props.version
    if (vv) {
      isEdit.value = true
      form.versionLabel = vv.versionLabel
      form.cronExpression = vv.cronExpression || ''
      form.dataSource = vv.dataSource || ''
      form.dataTimeRange = vv.dataTimeRange || ''
      form.changeNote = vv.changeNote || ''

      const rt = parseRuntimeFromJson(vv.paramsConfig)
      runtime.target = rt.target || 'SST'
      runtime.modelFile = rt.modelFile || ''
      runtime.seqLen = rt.seqLen || 30
      runtime.horizon = rt.horizon || 7
      runtime.depthLevels = rt.depthLevels || 18
      runtime.scalerFiles = rt.scalerFiles || ''

      trainingMeta.value = parseTrainingFromJson(vv.paramsConfig)

      dataSourceSelect.value = detectDataSourceSelect(vv.dataSource)
      if (dataSourceSelect.value === '__custom__') {
        form.dataSource = vv.dataSource || ''
      }
    } else {
      isEdit.value = false
      resetForm()
      form.versionLabel = props.nextVersionLabel
    }
  }
})

function onDataSourceChange() {
  if (dataSourceSelect.value !== '__custom__') {
    form.dataSource = dataSourceSelect.value
  } else {
    form.dataSource = ''
  }
}

function addMeta() {
  trainingMeta.value.push({ key: '', value: '', type: 'number' })
}

function removeMeta(i) {
  trainingMeta.value.splice(i, 1)
}

function serializeParamsConfig() {
  const trainingObj = {}
  for (const item of trainingMeta.value) {
    if (!item.key.trim()) continue
    let val = item.value.trim()
    if (item.type === 'number') {
      val = isNaN(Number(val)) ? val : Number(val)
    } else if (item.type === 'boolean') {
      val = val === 'true'
    }
    trainingObj[item.key.trim()] = val
  }

  return JSON.stringify({
    runtime: {
      target: runtime.target,
      modelFile: runtime.modelFile.trim(),
      seqLen: runtime.seqLen,
      horizon: runtime.horizon,
      depthLevels: runtime.depthLevels,
      scalerFiles: runtime.scalerFiles.trim()
    },
    training: trainingObj
  })
}

function handleSubmit() {
  if (dataSourceSelect.value !== '__custom__') {
    form.dataSource = dataSourceSelect.value
  }

  emit('submit', {
    id: props.version?.id,
    versionLabel: form.versionLabel,
    cronExpression: form.cronExpression.trim(),
    paramsConfig: serializeParamsConfig(),
    dataSource: form.dataSource.trim(),
    dataTimeRange: form.dataTimeRange.trim(),
    changeNote: form.changeNote.trim()
  })
}
</script>

<style scoped>
.form-section {
  border-top: 1px solid var(--color-divider);
  padding-top: 16px;
  margin-bottom: 18px;
}

.form-section__title {
  font-size: 12px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 600;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.form-row {
  display: flex;
  gap: 14px;
  margin-bottom: 14px;
}

.form-col {
  flex: 1;
  min-width: 0;
}

.form-col--3 {
  flex: 0 0 calc((100% - 28px) / 3);
}

.kv-table {
  font-size: 12px;
}

.kv-table__header {
  display: flex;
  gap: 8px;
  padding: 4px 0;
  color: var(--color-text-muted);
  font-weight: 500;
  border-bottom: 1px solid var(--color-divider);
  margin-bottom: 4px;
}

.kv-table__header span:nth-child(1) { flex: 2; }
.kv-table__header span:nth-child(2) { flex: 2; }
.kv-table__header span:nth-child(3) { width: 90px; }
.kv-table__header span:nth-child(4) { width: 40px; }

.kv-table__row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 4px;
}

.kv-table__row .editorial-input { flex: 2; min-width: 0; }
.kv-table__row .el-select { flex-shrink: 0; }
</style>
