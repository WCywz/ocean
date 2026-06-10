<template>
  <div>
    <div style="margin-bottom: 24px;">
      <router-link to="/app/model" class="editorial-link">&larr; 返回模型列表</router-link>
    </div>

    <h1 class="editorial-page-title">{{ model.modelName || '加载中...' }}</h1>
    <p class="editorial-page-subtitle">
      类型：{{ model.modelType }} &ensp;|&ensp; 版本：{{ versions.length }} 个
      <span v-if="runningCount > 0">（{{ runningCount }} 个运行中）</span>
    </p>

    <div v-if="runningVersions.length > 0" style="margin-bottom: 24px;">
      <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 10px;">
        <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #22c55e;"></span>
        <span style="font-size: 10px; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.1em;">运行概览</span>
        <span style="font-size: 12px; color: var(--color-text-muted);">{{ runningCount }} 个版本运行中</span>
      </div>
      <div style="display: flex; flex-wrap: wrap; gap: 10px;">
        <div
          v-for="v in runningVersions"
          :key="v.id"
          style="display: flex; align-items: center; gap: 10px; border: 1px solid var(--color-border-light); padding: 8px 14px; font-size: 13px;"
        >
          <span style="font-weight: 600; color: var(--color-text);">{{ model.modelName }}</span>
          <span style="background: var(--color-border-light); color: var(--color-text-secondary); padding: 1px 7px; font-size: 11px; font-weight: 500;">{{ v.versionLabel }}</span>
          <span style="color: #22c55e; font-size: 12px;">运行中</span>
          <a class="editorial-link" style="color: #ef4444;" @click="handleStopVersion(v)">停止</a>
        </div>
      </div>
    </div>

    <div style="margin-bottom: 24px;">
      <button class="editorial-btn-outline" @click="showEditModel = !showEditModel">
        {{ showEditModel ? '收起编辑' : '编辑模型信息' }}
      </button>
    </div>

    <div v-if="showEditModel" style="margin-bottom: 28px; padding: 20px; border: 1px solid var(--color-divider-strong); background: var(--color-surface);">
      <div style="margin-bottom: 14px;">
        <label class="editorial-form-label">模型名称</label>
        <input v-model="editForm.modelName" class="editorial-input" />
      </div>
      <div style="margin-bottom: 14px;">
        <label class="editorial-form-label">模型类型</label>
        <el-select v-model="editForm.modelType" style="width: 100%;">
          <el-option label="海表温度 (SST)" value="SST" />
          <el-option label="叶绿素浓度 (CHL)" value="CHL" />
          <el-option label="盐度 (SALINITY)" value="SALINITY" />
        </el-select>
      </div>
      <div style="margin-bottom: 14px;">
        <label class="editorial-form-label">模型介绍</label>
        <textarea v-model="editForm.description" class="editorial-input" rows="3" style="resize: vertical;"></textarea>
      </div>
      <button class="editorial-btn-outline" @click="handleUpdateModel">保存</button>
    </div>

    <div class="editorial-filter-bar">
      <span style="font-size: 12px; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.08em; font-weight: 600;">版本列表</span>
      <span style="flex: 1;"></span>
      <button class="editorial-btn-outline" @click="handleAddVersion">+ 新建版本</button>
    </div>

    <div v-if="versions.length === 0 && !versionLoading" style="text-align: center; padding: 40px 0; color: var(--color-text-muted); font-size: 13px;">暂无版本</div>

    <div v-else v-loading="versionLoading" style="min-height: 120px;">
      <div v-for="v in versions" :key="v.id" class="version-row">
        <span class="version-label">{{ v.versionLabel }}</span>
        <span style="color: var(--color-text-secondary); font-size: 12px; min-width: 80px;">{{ v.cronExpression }}</span>
        <span style="font-size: 12px; min-width: 44px;" :style="{ color: v.status === 'RUNNING' ? '#22c55e' : '#ef4444' }">{{ statusMap[v.status] }}</span>
        <span class="version-ds" :title="v.dataSource">{{ v.dataSource || '-' }}</span>
        <span style="display: flex; gap: 10px;">
          <a v-if="v.status !== 'RUNNING'" class="editorial-link" style="color: #22c55e;" @click="handleToggleVersion(v, 'RUNNING')">启动</a>
          <a v-else class="editorial-link" style="color: #ef4444;" @click="handleToggleVersion(v, 'STOPPED')">停止</a>
          <a class="editorial-link" @click="handleEditVersion(v)">编辑</a>
          <a class="editorial-link editorial-link--muted" @click="handleDeleteVersion(v)">删除</a>
        </span>
      </div>
    </div>

    <VersionDialog
      v-model="versionDialogVisible"
      :version="editingVersion"
      :model-name="model.modelName || ''"
      :next-version-label="nextVersionLabel"
      @submit="handleVersionSubmit"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getModelById, updateModel, getModelVersions, addVersion, updateVersion, deleteVersion, toggleVersionStatus } from '../../api/model'
import { ElMessage, ElMessageBox } from 'element-plus'
import VersionDialog from './VersionDialog.vue'

const route = useRoute()
const router = useRouter()
const modelId = computed(() => Number(route.params.id))

const statusMap = { RUNNING: '运行中', STOPPED: '已停止', ERROR: '异常' }

const model = ref({})
const versions = ref([])
const versionLoading = ref(false)
const showEditModel = ref(false)

const editForm = reactive({ modelName: '', modelType: 'SST', description: '' })

const versionDialogVisible = ref(false)
const editingVersion = ref(null)

const runningVersions = computed(() => versions.value.filter(v => v.status === 'RUNNING'))
const runningCount = computed(() => runningVersions.value.length)

const nextVersionLabel = computed(() => {
  if (!versions.value.length) return 'v1'
  const nums = versions.value.map(v => parseInt(v.versionLabel.replace('v', ''))).filter(n => !isNaN(n))
  return nums.length ? `v${Math.max(...nums) + 1}` : 'v1'
})

onMounted(() => loadAll())
watch(modelId, () => loadAll())

let requestId = 0

async function loadAll() {
  const currentId = ++requestId
  try {
    const res = await getModelById(modelId.value)
    if (currentId !== requestId) return
    model.value = res.data
    editForm.modelName = res.data.modelName
    editForm.modelType = res.data.modelType
    editForm.description = res.data.description || ''
  } catch {
    if (currentId !== requestId) return
    router.push('/app/model')
  }
  await loadVersions(currentId)
}

async function loadVersions() {
  versionLoading.value = true
  try {
    const res = await getModelVersions(modelId.value)
    versions.value = res.data || []
  } catch { versions.value = [] }
  finally { versionLoading.value = false }
}

async function handleUpdateModel() {
  try {
    await updateModel(modelId.value, {
      modelName: editForm.modelName.trim(),
      modelType: editForm.modelType,
      description: editForm.description.trim()
    })
    model.value.modelName = editForm.modelName.trim()
    model.value.modelType = editForm.modelType
    model.value.description = editForm.description.trim()
    ElMessage.success('模型信息已更新')
  } catch { /* interceptor handles */ }
}

function handleAddVersion() { editingVersion.value = null; versionDialogVisible.value = true }

function handleEditVersion(v) { editingVersion.value = { ...v }; versionDialogVisible.value = true }

async function handleVersionSubmit(data) {
  try {
    if (data.id) { await updateVersion(modelId.value, data.id, data); ElMessage.success('版本更新成功') }
    else { await addVersion(modelId.value, data); ElMessage.success('版本创建成功') }
    versionDialogVisible.value = false
    loadVersions()
  } catch { /* interceptor handles */ }
}

async function handleDeleteVersion(v) {
  try {
    await ElMessageBox.confirm(`确定要删除版本 "${v.versionLabel}" 吗？`, '删除确认', { type: 'warning' })
    await deleteVersion(modelId.value, v.id)
    ElMessage.success('版本已删除')
    loadVersions()
  } catch { /* cancelled */ }
}

async function handleToggleVersion(v, status) {
  try {
    await toggleVersionStatus(modelId.value, v.id, status)
    ElMessage.success(status === 'RUNNING' ? '版本已启动' : '版本已停止')
    loadVersions()
  } catch { /* interceptor handles */ }
}

async function handleStopVersion(v) {
  try {
    await toggleVersionStatus(modelId.value, v.id, 'STOPPED')
    ElMessage.success('版本已停止')
    loadVersions()
  } catch { /* interceptor handles */ }
}
</script>

<style scoped>
.version-row {
  display: flex;
  align-items: center;
  padding: 8px 0;
  font-size: 13px;
  border-bottom: 1px solid var(--color-divider);
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

.version-ds {
  color: var(--color-text-muted);
  font-size: 12px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0 16px;
}
</style>
