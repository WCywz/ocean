# Model Management Redesign — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the model management page from a flat CRUD list into a two-layer (model → version) expandable hierarchy with running overview.

**Architecture:** Single-page drill-down. ModelView.vue is the main page composing 3 sub-components (RunningOverview, ModelDialog, VersionDialog). The API layer in model.js is extended with version-related endpoints. Editorial style maintained throughout.

**Tech Stack:** Vue 3 (Composition API, `<script setup>`), Element Plus (dialog/message/confirm only), custom editorial CSS

---

## File Map

| Action | File | Responsibility |
|--------|------|----------------|
| Modify | `ocean-web/src/api/model.js` | Add version + running-overview API functions |
| Create | `ocean-web/src/views/model/ModelDialog.vue` | Create/edit model shell form dialog |
| Create | `ocean-web/src/views/model/VersionDialog.vue` | Create/edit version form dialog |
| Create | `ocean-web/src/views/model/RunningOverview.vue` | Running versions overview bar |
| Modify | `ocean-web/src/views/model/ModelView.vue` | Main page: overview + filter + expandable list |

---

### Task 1: Extend model.js API layer

**Files:**
- Modify: `ocean-web/src/api/model.js`

- [ ] **Step 1: Add version and running-overview API functions**

Add these functions after the existing `toggleModelStatus` export:

```js
/** 获取所有运行中的版本（概览用） */
export function getRunningVersions() {
  return request({ url: '/model/running-versions', method: 'get' })
}

/** 获取模型下的所有版本 */
export function getModelVersions(modelId) {
  return request({ url: `/model/${modelId}/versions`, method: 'get' })
}

/** 新增版本 */
export function addVersion(modelId, data) {
  return request({ url: `/model/${modelId}/version`, method: 'post', data })
}

/** 修改版本 */
export function updateVersion(modelId, versionId, data) {
  return request({ url: `/model/${modelId}/version/${versionId}`, method: 'put', data })
}

/** 删除版本 */
export function deleteVersion(modelId, versionId) {
  return request({ url: `/model/${modelId}/version/${versionId}`, method: 'delete' })
}

/** 启停版本 */
export function toggleVersionStatus(modelId, versionId, status) {
  return request({ url: `/model/${modelId}/version/${versionId}/status`, method: 'put', params: { status } })
}
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/api/model.js
git commit -m "feat: add version and running-overview API functions to model.js"
```

---

### Task 2: Create ModelDialog.vue

**Files:**
- Create: `ocean-web/src/views/model/ModelDialog.vue`

- [ ] **Step 1: Create the model dialog component**

```vue
<template>
  <el-dialog v-model="visible" :title="isEdit ? '编辑模型' : '新增模型'" width="480px" :close-on-click-modal="false" @closed="handleClosed">
    <div style="margin-bottom: 18px;">
      <label class="editorial-form-label">模型名称</label>
      <input v-model="form.modelName" class="editorial-input" placeholder="请输入模型名称" />
    </div>
    <div style="margin-bottom: 18px;">
      <label class="editorial-form-label">模型类型</label>
      <select v-model="form.modelType" class="editorial-select">
        <option value="SST">海表温度 (SST)</option>
        <option value="CHL">叶绿素浓度 (CHL)</option>
        <option value="__custom__">自定义...</option>
      </select>
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
      <button class="editorial-btn" style="padding: 8px 24px; margin-left: 12px;" :disabled="submitting" @click="handleSubmit">确定</button>
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
const submitting = ref(false)
const form = reactive({ modelName: '', modelType: 'SST', customType: '', description: '' })

watch(visible, (v) => {
  if (v) {
    if (props.model) {
      isEdit.value = true
      form.modelName = props.model.modelName
      form.description = props.model.description || ''
      const knownTypes = ['SST', 'CHL']
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

function handleClosed() {
  visible.value = false
}

async function handleSubmit() {
  if (!form.modelName.trim()) { ElMessage.warning('请输入模型名称'); return }
  const modelType = form.modelType === '__custom__' ? form.customType.trim() : form.modelType
  if (!modelType) { ElMessage.warning('请输入模型类型'); return }
  submitting.value = true
  try {
    emit('submit', {
      id: props.model?.id,
      modelName: form.modelName.trim(),
      modelType,
      description: form.description.trim()
    })
  } finally { submitting.value = false }
}
</script>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/model/ModelDialog.vue
git commit -m "feat: add ModelDialog component with custom type support"
```

---

### Task 3: Create VersionDialog.vue

**Files:**
- Create: `ocean-web/src/views/model/VersionDialog.vue`

- [ ] **Step 1: Create the version dialog component**

```vue
<template>
  <el-dialog v-model="visible" :title="isEdit ? '编辑版本' : `新增版本 — ${modelName}`" width="520px" :close-on-click-modal="false" @closed="handleClosed">
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
      <button class="editorial-btn" style="padding: 8px 24px; margin-left: 12px;" :disabled="submitting" @click="handleSubmit">确定</button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'

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
const submitting = ref(false)
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

function handleClosed() {
  visible.value = false
}

async function handleSubmit() {
  submitting.value = true
  try {
    emit('submit', {
      id: props.version?.id,
      versionLabel: form.versionLabel,
      cronExpression: form.cronExpression.trim(),
      paramsConfig: form.paramsConfig.trim(),
      dataSource: form.dataSource.trim(),
      dataTimeRange: form.dataTimeRange.trim(),
      changeNote: form.changeNote.trim()
    })
  } finally { submitting.value = false }
}
</script>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/model/VersionDialog.vue
git commit -m "feat: add VersionDialog component with data source and change note fields"
```

---

### Task 4: Create RunningOverview.vue

**Files:**
- Create: `ocean-web/src/views/model/RunningOverview.vue`

- [ ] **Step 1: Create the running overview component**

```vue
<template>
  <div v-if="versions.length > 0" style="margin-bottom: 28px;">
    <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px;">
      <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #22c55e;"></span>
      <span style="font-size: 10px; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.1em;">运行概览</span>
      <span style="font-size: 12px; color: #999;">{{ versions.length }} 个版本运行中</span>
    </div>
    <div style="display: flex; flex-wrap: wrap; gap: 10px;">
      <div
        v-for="v in versions"
        :key="v.versionId"
        style="display: flex; align-items: center; gap: 10px; border: 1px solid #e8e8e8; padding: 8px 14px; font-size: 13px;"
      >
        <span style="font-weight: 600; color: var(--color-text);">{{ v.modelName }}</span>
        <span style="background: #e0e7ff; color: #4f46e5; padding: 1px 7px; font-size: 11px; font-weight: 500;">{{ v.versionLabel }}</span>
        <span style="color: #22c55e; font-size: 12px;">运行中</span>
        <a class="editorial-link" style="color: #ef4444;" @click="$emit('stop', v)">停止</a>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  versions: { type: Array, default: () => [] }
})

defineEmits(['stop'])
</script>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/model/RunningOverview.vue
git commit -m "feat: add RunningOverview component for running version quick-stop"
```

---

### Task 5: Rewrite ModelView.vue

**Files:**
- Modify: `ocean-web/src/views/model/ModelView.vue`

- [ ] **Step 1: Rewrite the template**

Replace the entire template section with:

```vue
<template>
  <div>
    <h1 class="editorial-page-title">预报模型管理</h1>
    <p class="editorial-page-subtitle">Model Management · 共 {{ total }} 个模型<span v-if="runningCount > 0">，{{ runningCount }} 个版本运行中</span></p>

    <div class="page-status-bar" :style="{ borderLeftColor: pageStatusColor }">
      <span class="page-status-bar__level">{{ pageStatusLabel }}</span>
      <span class="page-status-bar__dot">&middot;</span>
      <span class="page-status-bar__desc">{{ pageStatusDesc }}</span>
    </div>

    <RunningOverview :versions="runningVersions" @stop="handleOverviewStop" />

    <div class="editorial-filter-bar">
      <select v-model="query.modelType" class="editorial-select" style="width: 160px;">
        <option value="">全部类型</option>
        <option value="SST">海表温度 (SST)</option>
        <option value="CHL">叶绿素浓度 (CHL)</option>
      </select>
      <input v-model="query.keyword" class="editorial-search" placeholder="模型名称" style="width: 180px;" @keyup.enter="handleSearch" />
      <button class="editorial-btn-outline" @click="handleSearch">查询</button>
      <button class="editorial-btn-outline" @click="handleReset">重置</button>
      <span style="flex: 1;"></span>
      <button class="editorial-btn-outline" @click="handleAddModel">+ 新增模型</button>
    </div>

    <div v-if="tableData.length === 0 && !loading" style="text-align: center; padding: 60px 0; color: var(--color-text-muted); font-size: 14px;">
      暂无模型数据
    </div>

    <div v-else v-loading="loading" style="min-height: 200px;">
      <div
        v-for="row in tableData"
        :key="row.id"
        class="model-card"
        :class="{ 'model-card--expanded': expandedModelId === row.id }"
      >
        <!-- Model Row Header -->
        <div class="model-card__header" :style="{ borderLeftColor: modelStatusBorder(row) }" @click="toggleExpand(row)">
          <span class="model-card__chevron">{{ expandedModelId === row.id ? '▾' : '▸' }}</span>
          <span class="model-card__name">{{ row.modelName }}</span>
          <span class="editorial-tag" style="margin-left: auto; margin-right: 32px; min-width: 48px; text-align: center;">{{ row.modelType }}</span>
          <span style="color: #666; font-size: 12px; margin-right: 32px; min-width: 40px; text-align: center;">{{ row.versionCount ?? 0 }} 个</span>
          <span style="font-size: 12px; margin-right: 32px; min-width: 52px; text-align: center;" :style="{ color: modelStatusColor(row) }">{{ modelStatusText(row) }}</span>
          <span style="display: flex; gap: 12px;" @click.stop>
            <a class="editorial-link" @click="handleAddVersion(row)">+ 版本</a>
            <a class="editorial-link" @click="handleEditModel(row)">编辑</a>
            <a class="editorial-link editorial-link--muted" @click="handleDeleteModel(row)">删除</a>
          </span>
        </div>

        <!-- Model Description -->
        <div class="model-card__desc" v-if="row.description">
          {{ row.description }}
        </div>

        <!-- Version List (expanded) -->
        <div v-if="expandedModelId === row.id" class="model-card__versions">
          <div v-if="versionLoading" style="text-align: center; padding: 12px; color: var(--color-text-muted); font-size: 12px;">加载中...</div>
          <div v-else-if="currentVersions.length === 0" style="text-align: center; padding: 12px; color: var(--color-text-muted); font-size: 12px;">暂无版本</div>
          <div
            v-for="v in currentVersions"
            :key="v.id"
            class="model-card__version-row"
          >
            <span class="version-label">{{ v.versionLabel }}</span>
            <span style="color: #666; font-size: 12px; min-width: 80px;">{{ v.cronExpression }}</span>
            <span style="font-size: 12px; min-width: 44px;" :style="{ color: v.status === 'RUNNING' ? '#22c55e' : '#ef4444' }">{{ statusMap[v.status] }}</span>
            <span class="version-meta" :title="v.dataSource">{{ v.dataSource || '-' }}</span>
            <span style="display: flex; gap: 10px;">
              <a v-if="v.status !== 'RUNNING'" class="editorial-link" style="color: #22c55e;" @click.stop="handleToggleVersion(row, v, 'RUNNING')">启动</a>
              <a v-else class="editorial-link" style="color: #ef4444;" @click.stop="handleToggleVersion(row, v, 'STOPPED')">停止</a>
              <a class="editorial-link" @click.stop="handleEditVersion(row, v)">编辑</a>
              <a class="editorial-link editorial-link--muted" @click.stop="handleDeleteVersion(row, v)">删除</a>
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="editorial-pagination" v-if="total > 0">
      <span>共 {{ total }} 条</span>
      <select v-model="query.pageSize" class="editorial-select" style="width: 80px;" @change="loadModels">
        <option :value="10">10</option>
        <option :value="20">20</option>
        <option :value="50">50</option>
      </select>
      <a class="editorial-link" @click="prevPage">&larr;</a>
      <span class="editorial-pagination__page editorial-pagination__page--active">{{ query.pageNum }}</span>
      <a class="editorial-link" @click="nextPage">&rarr;</a>
    </div>

    <!-- Model Dialog -->
    <ModelDialog v-model="modelDialogVisible" :model="editingModel" @submit="handleModelSubmit" />

    <!-- Version Dialog -->
    <VersionDialog
      v-model="versionDialogVisible"
      :version="editingVersion"
      :model-name="currentModel?.modelName || ''"
      :next-version-label="nextVersionLabel"
      @submit="handleVersionSubmit"
    />
  </div>
</template>
```

- [ ] **Step 2: Rewrite the script section**

Replace the entire script section with:

```vue
<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import {
  getModelPage, addModel, updateModel, deleteModel,
  getRunningVersions, getModelVersions,
  addVersion, updateVersion, deleteVersion, toggleVersionStatus
} from '../../api/model'
import { ElMessage, ElMessageBox } from 'element-plus'
import RunningOverview from './RunningOverview.vue'
import ModelDialog from './ModelDialog.vue'
import VersionDialog from './VersionDialog.vue'

const statusMap = { RUNNING: '运行中', STOPPED: '已停止', ERROR: '异常' }

// --- Model list state ---
const query = reactive({ pageNum: 1, pageSize: 10, modelType: '', keyword: '' })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)

// --- Expand state ---
const expandedModelId = ref(null)
const currentVersions = ref([])
const versionLoading = ref(false)

// --- Running overview ---
const runningVersions = ref([])
const runningCount = computed(() => runningVersions.value.length)

// --- Dialogs ---
const modelDialogVisible = ref(false)
const editingModel = ref(null)

const versionDialogVisible = ref(false)
const editingVersion = ref(null)
const currentModel = ref(null)
const nextVersionLabel = computed(() => {
  if (!currentVersions.value.length) return 'v1'
  const nums = currentVersions.value
    .map(v => parseInt(v.versionLabel.replace('v', '')))
    .filter(n => !isNaN(n))
  return nums.length ? `v${Math.max(...nums) + 1}` : 'v1'
})

// --- Status bar ---
const pageStatusColor = computed(() => {
  if (!tableData.value.length) return '#22c55e'
  const allRunning = tableData.value.every(r => r._runningCount > 0)
  return allRunning ? '#22c55e' : '#e67e22'
})
const pageStatusLabel = computed(() => {
  if (!tableData.value.length) return '--'
  const allRunning = tableData.value.every(r => r._runningCount > 0)
  return allRunning ? '正常' : '注意'
})
const pageStatusDesc = computed(() => {
  if (!tableData.value.length) return '暂无模型数据'
  return runningCount.value > 0 ? `${runningCount.value} 个版本运行中` : '无运行中的版本'
})

// --- Model status helpers ---
function modelStatusBorder(row) {
  return row._runningCount > 0 ? '#22c55e' : '#ef4444'
}
function modelStatusColor(row) {
  return row._runningCount > 0 ? '#22c55e' : '#ef4444'
}
function modelStatusText(row) {
  if (!row.versionCount) return '无版本'
  if (row._runningCount === 0) return '已停止'
  if (row._runningCount === row.versionCount) return '全部运行'
  return '部分运行'
}

// --- Init ---
onMounted(() => {
  loadModels()
  loadRunningOverview()
})

// --- Model list ---
async function loadModels() {
  loading.value = true
  try {
    const res = await getModelPage({ ...query })
    const records = res.data.records || []
    // Attach placeholder _runningCount (updated on expand or via overview)
    records.forEach(r => { r._runningCount = 0 })
    // Cross-ref with running overview if loaded
    for (const r of records) {
      r._runningCount = runningVersions.value.filter(v => v.modelId === r.id).length
    }
    tableData.value = records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadRunningOverview() {
  try {
    const res = await getRunningVersions()
    runningVersions.value = res.data || []
    // Update _runningCount on currently loaded models
    for (const r of tableData.value) {
      r._runningCount = runningVersions.value.filter(v => v.modelId === r.id).length
    }
  } catch {
    runningVersions.value = []
  }
}

function handleSearch() {
  query.pageNum = 1
  loadModels()
}

function handleReset() {
  query.modelType = ''
  query.keyword = ''
  query.pageNum = 1
  loadModels()
}

function prevPage() {
  if (query.pageNum > 1) { query.pageNum--; loadModels() }
}
function nextPage() {
  query.pageNum++; loadModels()
}

// --- Expand / collapse ---
async function toggleExpand(row) {
  if (expandedModelId.value === row.id) {
    expandedModelId.value = null
    currentVersions.value = []
    return
  }
  expandedModelId.value = row.id
  versionLoading.value = true
  try {
    const res = await getModelVersions(row.id)
    currentVersions.value = res.data || []
  } catch {
    currentVersions.value = []
  } finally {
    versionLoading.value = false
  }
}

// --- Model CRUD ---
function handleAddModel() {
  editingModel.value = null
  modelDialogVisible.value = true
}

function handleEditModel(row) {
  editingModel.value = { ...row }
  modelDialogVisible.value = true
}

async function handleModelSubmit(data) {
  try {
    if (data.id) {
      await updateModel(data.id, data)
      ElMessage.success('模型更新成功')
    } else {
      await addModel(data)
      ElMessage.success('模型创建成功')
    }
    modelDialogVisible.value = false
    loadModels()
  } catch { /* error handled by interceptor */ }
}

async function handleDeleteModel(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除模型 "${row.modelName}" 及其所有版本吗？`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteModel(row.id)
    if (expandedModelId.value === row.id) {
      expandedModelId.value = null
      currentVersions.value = []
    }
    ElMessage.success('模型已删除')
    loadModels()
    loadRunningOverview()
  } catch { /* cancelled */ }
}

// --- Version CRUD ---
function handleAddVersion(row) {
  currentModel.value = row
  editingVersion.value = null
  // Ensure versions are loaded
  if (expandedModelId.value !== row.id) {
    toggleExpand(row).then(() => {
      versionDialogVisible.value = true
    })
    return
  }
  versionDialogVisible.value = true
}

function handleEditVersion(model, version) {
  currentModel.value = model
  editingVersion.value = { ...version }
  versionDialogVisible.value = true
}

async function handleVersionSubmit(data) {
  const modelId = currentModel.value.id
  try {
    if (data.id) {
      await updateVersion(modelId, data.id, data)
      ElMessage.success('版本更新成功')
    } else {
      await addVersion(modelId, data)
      ElMessage.success('版本创建成功')
    }
    versionDialogVisible.value = false
    // Refresh versions for expanded model
    if (expandedModelId.value === modelId) {
      const res = await getModelVersions(modelId)
      currentVersions.value = res.data || []
    }
    loadModels()
    loadRunningOverview()
  } catch { /* error handled by interceptor */ }
}

async function handleDeleteVersion(model, version) {
  try {
    await ElMessageBox.confirm(
      `确定要删除 "${model.modelName}" 的版本 "${version.versionLabel}" 吗？`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteVersion(model.id, version.id)
    ElMessage.success('版本已删除')
    const res = await getModelVersions(model.id)
    currentVersions.value = res.data || []
    loadModels()
    loadRunningOverview()
  } catch { /* cancelled */ }
}

async function handleToggleVersion(model, version, status) {
  await toggleVersionStatus(model.id, version.id, status)
  ElMessage.success(status === 'RUNNING' ? '版本已启动' : '版本已停止')
  // Refresh versions
  const res = await getModelVersions(model.id)
  currentVersions.value = res.data || []
  loadRunningOverview()
}

// --- Overview stop ---
async function handleOverviewStop(v) {
  try {
    await toggleVersionStatus(v.modelId, v.versionId, 'STOPPED')
    ElMessage.success('版本已停止')
    loadRunningOverview()
    // Also refresh expanded version list if applicable
    if (expandedModelId.value === v.modelId) {
      const res = await getModelVersions(v.modelId)
      currentVersions.value = res.data || []
    }
  } catch { /* error handled by interceptor */ }
}
</script>
```

- [ ] **Step 3: Replace the style section**

Replace the entire `<style scoped>` section with:

```css
<style scoped>
.page-status-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  border-left: 3px solid;
  padding: 10px 14px;
  background: #fafafa;
  font-size: 13px;
  margin-bottom: 24px;
}

.page-status-bar__level {
  font-family: var(--font-serif);
  font-size: 15px;
  color: var(--color-text);
}

.page-status-bar__dot {
  color: var(--color-text-muted);
}

.page-status-bar__desc {
  color: #666;
  flex: 1;
}

/* Model card */
.model-card {
  border: 1px solid #ebebeb;
  background: #fff;
  overflow: hidden;
  margin-bottom: 6px;
}

.model-card__header {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  border-left: 3px solid #ccc;
  cursor: pointer;
  user-select: none;
}

.model-card__header:hover {
  background: #fafafa;
}

.model-card__chevron {
  font-size: 15px;
  color: #666;
  margin-right: 10px;
  font-weight: 600;
}

.model-card__name {
  font-weight: 600;
  color: var(--color-text);
  flex: 1;
  font-size: 14px;
}

.model-card__desc {
  padding: 0 14px 10px 42px;
  font-size: 12px;
  color: #999;
  line-height: 1.7;
}

.model-card__versions {
  border-top: 1px solid #f0f0f0;
}

.model-card__version-row {
  display: flex;
  align-items: center;
  padding: 8px 14px 8px 42px;
  font-size: 13px;
  border-bottom: 1px solid #fafafa;
}

.model-card__version-row:last-child {
  border-bottom: none;
}

.version-label {
  background: #e0e7ff;
  color: #4f46e5;
  padding: 1px 7px;
  font-size: 11px;
  font-weight: 500;
  margin-right: 12px;
  min-width: 28px;
  text-align: center;
}

.version-meta {
  color: #999;
  font-size: 12px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0 16px;
}
</style>
```

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/model/ModelView.vue
git commit -m "feat: rewrite ModelView with two-layer expandable model/version hierarchy"
```

---

### Task 6: Verification

- [ ] **Step 1: Start the dev server**

```bash
cd ocean-web && npm run dev
```

- [ ] **Step 2: Verify the page renders**

Navigate to `http://localhost:5173/app/model` (or appropriate dev URL) and check:
- Page title and subtitle display
- Status bar shows correctly
- Running overview shows running versions (or is absent if none)
- Filter bar works (type select, keyword search)
- Model cards render in the list
- Clicking a model card expands/collapses version list
- "+ 新增模型" opens ModelDialog
- "+ 版本" opens VersionDialog (auto-generates next version label)
- Edit/delete buttons work on both model and version rows
- Running overview stop button works

- [ ] **Step 3: Verify empty states**

- Delete all models → page shows "暂无模型数据"
- Model with no versions → expand shows "暂无版本"
- No running versions → running overview section hidden

- [ ] **Step 4: Commit any fixes if needed**
