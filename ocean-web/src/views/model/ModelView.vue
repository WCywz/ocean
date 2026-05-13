<template>
  <div>
    <h1 class="editorial-page-title">预报模型管理</h1>
    <p class="editorial-page-subtitle">Model Management · 共 {{ total }} 条记录</p>

    <div class="page-status-bar" :style="{ borderLeftColor: pageStatusColor }">
      <span class="page-status-bar__level">{{ pageStatusLabel }}</span>
      <span class="page-status-bar__dot">&middot;</span>
      <span class="page-status-bar__desc">{{ pageStatusDesc }}</span>
    </div>

    <div class="editorial-filter-bar">
      <select v-model="query.modelType" class="editorial-select" style="width: 160px;">
        <option value="">全部类型</option>
        <option value="SST">海表温度 (SST)</option>
        <option value="CHL">叶绿素浓度 (CHL)</option>
      </select>
      <input v-model="query.keyword" class="editorial-search" placeholder="模型名称" style="width: 180px;" />
      <button class="editorial-btn-outline" @click="handleSearch">查询</button>
      <button class="editorial-btn-outline" @click="handleReset">重置</button>
      <span style="flex: 1;"></span>
      <button class="editorial-btn-outline" @click="handleAdd">+ 新增模型</button>
    </div>

    <table class="editorial-table" v-loading="loading">
      <thead>
        <tr>
          <td>模型名称</td>
          <td>类型</td>
          <td>运行周期</td>
          <td>状态</td>
          <td>最近运行</td>
          <td style="text-align: right;">操作</td>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in tableData" :key="row.id">
          <td :style="{ borderLeft: '3px solid ' + (row.status === 'RUNNING' ? '#22c55e' : '#ef4444') }">{{ row.modelName }}</td>
          <td><span class="editorial-tag">{{ row.modelType === 'SST' ? 'SST' : 'CHL' }}</span></td>
          <td>{{ row.cronExpression }}</td>
          <td>{{ statusMap[row.status] }}</td>
          <td class="text-muted">{{ row.lastRunTime || '-' }}</td>
          <td style="text-align: right;">
            <a v-if="row.status !== 'RUNNING'" class="editorial-link" @click="handleToggle(row, 'RUNNING')">启动</a>
            <a v-else class="editorial-link" @click="handleToggle(row, 'STOPPED')">停止</a>
            <a class="editorial-link" style="margin-left: 12px;" @click="handleEdit(row)">编辑</a>
            <a class="editorial-link editorial-link--muted" style="margin-left: 12px;" @click="handleDelete(row)">删除</a>
          </td>
        </tr>
      </tbody>
    </table>

    <div class="editorial-pagination">
      <span>共 {{ total }} 条</span>
      <select v-model="query.pageSize" class="editorial-select" style="width: 80px;" @change="loadData">
        <option :value="10">10</option>
        <option :value="20">20</option>
        <option :value="50">50</option>
      </select>
      <a class="editorial-link" @click="prevPage">&larr;</a>
      <span class="editorial-pagination__page editorial-pagination__page--active">{{ query.pageNum }}</span>
      <a class="editorial-link" @click="nextPage">&rarr;</a>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :close-on-click-modal="false">
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">模型名称</label>
        <input v-model="form.modelName" class="editorial-input" placeholder="请输入模型名称" />
      </div>
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">模型类型</label>
        <select v-model="form.modelType" class="editorial-select">
          <option value="SST">海表温度 (SST)</option>
          <option value="CHL">叶绿素浓度 (CHL)</option>
        </select>
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
        <label class="editorial-form-label">模型描述</label>
        <input v-model="form.description" class="editorial-input" placeholder="请输入模型描述" />
      </div>
      <template #footer>
        <button class="editorial-btn-outline" @click="dialogVisible = false">取消</button>
        <button class="editorial-btn" style="padding: 8px 24px; margin-left: 12px;" :disabled="submitLoading" @click="handleSubmit">确定</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getModelPage, addModel, updateModel, deleteModel, toggleModelStatus } from '../../api/model'
import { ElMessage, ElMessageBox } from 'element-plus'

const statusMap = { RUNNING: '运行中', STOPPED: '已停止', ERROR: '异常' }

const query = reactive({ pageNum: 1, pageSize: 10, modelType: '', keyword: '' })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('新增模型')
const isEdit = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const form = reactive({ modelName: '', modelType: 'SST', cronExpression: '', paramsConfig: '', description: '' })

const pageStatusColor = computed(() => {
  const allRunning = tableData.value.length > 0 && tableData.value.every(r => r.status === 'RUNNING')
  if (!tableData.value.length) return '#22c55e'
  return allRunning ? '#22c55e' : '#ef4444'
})
const pageStatusLabel = computed(() => {
  const allRunning = tableData.value.length > 0 && tableData.value.every(r => r.status === 'RUNNING')
  if (!tableData.value.length) return '--'
  return allRunning ? '正常' : '注意'
})
const pageStatusDesc = computed(() => {
  if (!tableData.value.length) return '暂无模型数据'
  const stopped = tableData.value.filter(r => r.status !== 'RUNNING').length
  if (stopped === 0) return '所有模型运行正常'
  return `${stopped} 个模型已停止或异常`
})

onMounted(() => { loadData() })

async function loadData() {
  loading.value = true
  try {
    const res = await getModelPage({ ...query })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.modelType = ''
  query.keyword = ''
  query.pageNum = 1
  loadData()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增模型'
  form.modelName = ''
  form.modelType = 'SST'
  form.cronExpression = ''
  form.paramsConfig = ''
  form.description = ''
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editId.value = row.id
  dialogTitle.value = '编辑模型'
  form.modelName = row.modelName
  form.modelType = row.modelType
  form.cronExpression = row.cronExpression
  form.paramsConfig = row.paramsConfig
  form.description = row.description
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.modelName) { ElMessage.warning('请输入模型名称'); return }
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateModel(editId.value, { ...form })
      ElMessage.success('模型更新成功')
    } else {
      await addModel({ ...form })
      ElMessage.success('模型创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally { submitLoading.value = false }
}

function handleDelete(row) {
  ElMessageBox.confirm(
    `确定要删除模型 "${row.modelName}" 吗？`,
    '删除确认',
    { type: 'warning' }
  ).then(async () => {
    await deleteModel(row.id)
    ElMessage.success('模型已删除')
    loadData()
  }).catch(() => {})
}

async function handleToggle(row, status) {
  await toggleModelStatus(row.id, status)
  ElMessage.success(status === 'RUNNING' ? '模型已启动' : '模型已停止')
  loadData()
}

function prevPage() {
  if (query.pageNum > 1) { query.pageNum--; loadData() }
}
function nextPage() {
  query.pageNum++; loadData()
}
</script>

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

.editorial-table {
  border-collapse: separate;
  border-spacing: 0 6px;
}

.editorial-table :deep(td) {
  text-align: center;
}
</style>
