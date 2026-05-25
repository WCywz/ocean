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
          <span style="color: var(--color-text-secondary); font-size: 12px; margin-right: 32px; min-width: 40px; text-align: center;">{{ row.versionCount ?? 0 }} 个</span>
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
            <span style="color: var(--color-text-secondary); font-size: 12px; min-width: 80px;">{{ v.cronExpression }}</span>
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

    <ModelDialog v-model="modelDialogVisible" :model="editingModel" @submit="handleModelSubmit" />

    <VersionDialog
      v-model="versionDialogVisible"
      :version="editingVersion"
      :model-name="currentModel?.modelName || ''"
      :next-version-label="nextVersionLabel"
      @submit="handleVersionSubmit"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
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
    records.forEach(r => { r._runningCount = 0 })
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
  try {
    await toggleVersionStatus(model.id, version.id, status)
    ElMessage.success(status === 'RUNNING' ? '版本已启动' : '版本已停止')
    const res = await getModelVersions(model.id)
    currentVersions.value = res.data || []
    loadModels()
    loadRunningOverview()
  } catch { /* error handled by interceptor */ }
}

async function handleOverviewStop(v) {
  try {
    await toggleVersionStatus(v.modelId, v.versionId, 'STOPPED')
    ElMessage.success('版本已停止')
    loadRunningOverview()
    if (expandedModelId.value === v.modelId) {
      const res = await getModelVersions(v.modelId)
      currentVersions.value = res.data || []
    }
  } catch { /* error handled by interceptor */ }
}
</script>

<style scoped>
.page-status-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  border-left: 3px solid;
  padding: 10px 14px;
  background: var(--color-surface);
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
  color: var(--color-text-secondary);
  flex: 1;
}

.model-card {
  border: 1px solid var(--color-divider-strong);
  background: var(--color-bg);
  overflow: hidden;
  margin-bottom: 6px;
}

.model-card__header {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  border-left: 3px solid var(--color-border);
  cursor: pointer;
  user-select: none;
}

.model-card__header:hover {
  background: var(--color-surface);
}

.model-card__chevron {
  font-size: 15px;
  color: var(--color-text-secondary);
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
  color: var(--color-text-muted);
  line-height: 1.7;
}

.model-card__versions {
  border-top: 1px solid var(--color-divider);
}

.model-card__version-row {
  display: flex;
  align-items: center;
  padding: 8px 14px 8px 42px;
  font-size: 13px;
  border-bottom: 1px solid var(--color-divider);
}

.model-card__version-row:last-child {
  border-bottom: none;
}

.version-label {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
  padding: 1px 7px;
  font-size: 11px;
  font-weight: 500;
  margin-right: 12px;
  min-width: 28px;
  text-align: center;
}

.version-meta {
  color: var(--color-text-muted);
  font-size: 12px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0 16px;
}
</style>
