<template>
  <div class="model-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" size="default">
        <el-form-item label="模型类型">
          <el-select v-model="query.modelType" placeholder="全部" clearable style="width: 150px">
            <el-option label="海表温度 (SST)" value="SST" />
            <el-option label="叶绿素浓度 (CHL)" value="CHL" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="模型名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px;">
      <div style="margin-bottom: 16px;">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增模型
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="modelName" label="模型名称" min-width="180" />
        <el-table-column prop="modelType" label="类型" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="row.modelType === 'SST' ? 'primary' : 'success'" size="small">
              {{ row.modelType === 'SST' ? '海表温度' : '叶绿素浓度' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cronExpression" label="运行周期" width="140" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'RUNNING' ? 'success' : row.status === 'ERROR' ? 'danger' : 'info'"
              size="small"
            >
              {{ statusMap[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastRunTime" label="最近运行时间" min-width="170" />
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 'RUNNING'"
              type="success" link size="small"
              @click="handleToggle(row, 'RUNNING')"
            >启动</el-button>
            <el-button
              v-else
              type="warning" link size="small"
              @click="handleToggle(row, 'STOPPED')"
            >停止</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 16px; text-align: right;">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="模型类型" prop="modelType">
          <el-select v-model="form.modelType" placeholder="请选择模型类型" style="width: 100%">
            <el-option label="海表温度 (SST)" value="SST" />
            <el-option label="叶绿素浓度 (CHL)" value="CHL" />
          </el-select>
        </el-form-item>
        <el-form-item label="运行周期" prop="cronExpression">
          <el-input v-model="form.cronExpression" placeholder="Cron表达式，如: 0 0 6 * * ?" />
        </el-form-item>
        <el-form-item label="参数配置" prop="paramsConfig">
          <el-input
            v-model="form.paramsConfig"
            type="textarea"
            :rows="4"
            placeholder='JSON格式，如: {"algorithm":"ROMS","resolution":"0.1deg"}'
          />
        </el-form-item>
        <el-form-item label="模型描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入模型描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
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
const formRef = ref(null)
const form = reactive({ modelName: '', modelType: 'SST', cronExpression: '', paramsConfig: '', description: '' })

const rules = {
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }]
}

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
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

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
  } finally {
    submitLoading.value = false
  }
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
</script>
