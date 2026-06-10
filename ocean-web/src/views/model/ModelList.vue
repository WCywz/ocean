<template>
  <div>
    <h1 class="editorial-page-title">模型管理</h1>
    <p class="editorial-page-subtitle">Model Management · 共 {{ total }} 个模型</p>

    <div class="editorial-filter-bar">
      <el-select v-model="query.modelType" placeholder="全部类型" style="width: 160px;">
        <el-option label="全部类型" value="" />
        <el-option label="海表温度 (SST)" value="SST" />
        <el-option label="叶绿素浓度 (CHL)" value="CHL" />
        <el-option label="盐度 (SALINITY)" value="SALINITY" />
      </el-select>
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
        @click="goDetail(row)"
        :style="{ borderLeftColor: row._runningCount > 0 ? '#22c55e' : '#ccc' }"
      >
        <div class="model-card__header">
          <div class="model-card__info">
            <span class="model-card__name">{{ row.modelName }}</span>
            <span class="model-card__meta">
              类型：{{ row.modelType }} &ensp;|&ensp; 版本：{{ row.versionCount ?? 0 }} 个<span v-if="row._runningCount > 0">（{{ row._runningCount }} 个运行中）</span>
            </span>
          </div>
          <span style="display: flex; gap: 12px;" @click.stop>
            <a class="editorial-link" @click="goDetail(row)">管理版本</a>
            <a class="editorial-link" @click="handleEditModel(row)">编辑</a>
            <a class="editorial-link editorial-link--muted" @click="handleDeleteModel(row)">删除</a>
          </span>
        </div>
        <div class="model-card__desc" v-if="row.description">{{ row.description }}</div>
        <div class="model-card__time">创建时间：{{ formatDate(row.createTime) }}</div>
      </div>
    </div>

    <div class="editorial-pagination" v-if="total > 0">
      <span>共 {{ total }} 条</span>
      <el-select v-model="query.pageSize" style="width: 80px;" @change="loadModels">
        <el-option :label="'10'" :value="10" />
        <el-option :label="'20'" :value="20" />
        <el-option :label="'50'" :value="50" />
      </el-select>
      <a class="editorial-link" @click="prevPage">&larr;</a>
      <span class="editorial-pagination__page editorial-pagination__page--active">{{ query.pageNum }}</span>
      <a class="editorial-link" @click="nextPage">&rarr;</a>
    </div>

    <ModelDialog v-model="modelDialogVisible" :model="editingModel" @submit="handleModelSubmit" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getModelPage, addModel, updateModel, deleteModel, getRunningVersions } from '../../api/model'
import { ElMessage, ElMessageBox } from 'element-plus'
import ModelDialog from './ModelDialog.vue'

const router = useRouter()

const query = reactive({ pageNum: 1, pageSize: 10, modelType: '', keyword: '' })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const runningVersions = ref([])

const modelDialogVisible = ref(false)
const editingModel = ref(null)

onMounted(async () => {
  await loadRunning()
  await loadModels()
})

async function loadModels() {
  loading.value = true
  try {
    const res = await getModelPage({ ...query })
    const records = res.data.records || []
    records.forEach(r => { r._runningCount = runningVersions.value.filter(v => v.modelId === r.id).length })
    tableData.value = records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function loadRunning() {
  try {
    const res = await getRunningVersions()
    runningVersions.value = res.data || []
    for (const r of tableData.value) {
      r._runningCount = runningVersions.value.filter(v => v.modelId === r.id).length
    }
  } catch { runningVersions.value = [] }
}

function handleSearch() { query.pageNum = 1; loadModels() }
function handleReset() { query.modelType = ''; query.keyword = ''; query.pageNum = 1; loadModels() }
const totalPages = computed(() => Math.ceil(total.value / query.pageSize))

function prevPage() { if (query.pageNum > 1) { query.pageNum--; loadModels() } }
function nextPage() { if (query.pageNum < totalPages.value) { query.pageNum++; loadModels() } }

function goDetail(row) { router.push(`/app/model/${row.id}`) }

function handleAddModel() { editingModel.value = null; modelDialogVisible.value = true }

function handleEditModel(row) { editingModel.value = { ...row }; modelDialogVisible.value = true }

async function handleModelSubmit(data) {
  try {
    if (data.id) { await updateModel(data.id, data); ElMessage.success('模型更新成功') }
    else { await addModel(data); ElMessage.success('模型创建成功') }
    modelDialogVisible.value = false
    loadModels()
  } catch { /* interceptor handles */ }
}

async function handleDeleteModel(row) {
  try {
    await ElMessageBox.confirm(`确定要删除模型 "${row.modelName}" 及其所有版本吗？`, '删除确认', { type: 'warning' })
    await deleteModel(row.id)
    ElMessage.success('模型已删除')
    loadModels()
    loadRunning()
  } catch { /* cancelled */ }
}

function formatDate(d) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 10)
}
</script>

<style scoped>
.model-card {
  border: 1px solid var(--color-divider-strong);
  background: var(--color-bg);
  overflow: hidden;
  margin-bottom: 6px;
  cursor: pointer;
  border-left: 3px solid var(--color-border);
}

.model-card:hover { background: var(--color-surface); }

.model-card__header {
  display: flex;
  align-items: center;
  padding: 10px 14px;
}

.model-card__info { flex: 1; min-width: 0; }

.model-card__name {
  font-weight: 600;
  color: var(--color-text);
  font-size: 14px;
  display: block;
}

.model-card__meta {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 2px;
  display: block;
}

.model-card__desc {
  padding: 0 14px 6px 14px;
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.model-card__time {
  padding: 0 14px 10px 14px;
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
