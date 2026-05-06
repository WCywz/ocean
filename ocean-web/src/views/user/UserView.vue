<template>
  <div>
    <h1 class="editorial-page-title">用户管理</h1>
    <p class="editorial-page-subtitle">User Management · 共 {{ total }} 条记录</p>

    <!-- Filter bar -->
    <div class="editorial-filter-bar">
      <input v-model="query.username" class="editorial-search" placeholder="用户名" style="width: 160px;" />
      <select v-model="query.role" class="editorial-select" style="width: 140px;">
        <option value="">全部角色</option>
        <option value="ADMIN">管理员</option>
        <option value="USER">普通用户</option>
      </select>
      <select v-model="query.status" class="editorial-select" style="width: 120px;">
        <option :value="null">全部状态</option>
        <option :value="1">启用</option>
        <option :value="0">禁用</option>
      </select>
      <button class="editorial-btn-outline" @click="handleSearch">查询</button>
      <button class="editorial-btn-outline" @click="handleReset">重置</button>
      <span style="flex: 1;"></span>
      <button class="editorial-btn-outline" @click="handleAdd">+ 新增用户</button>
    </div>

    <!-- Table -->
    <table class="editorial-table" v-loading="loading">
      <thead>
        <tr>
          <td>用户名</td>
          <td>真实姓名</td>
          <td>角色</td>
          <td>状态</td>
          <td>创建时间</td>
          <td style="text-align: right;">操作</td>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in tableData" :key="row.id">
          <td>{{ row.username }}</td>
          <td>{{ row.realName }}</td>
          <td><span class="editorial-tag">{{ row.role === 'ADMIN' ? 'ADMIN' : 'USER' }}</span></td>
          <td>{{ row.status === 1 ? '启用' : '禁用' }}</td>
          <td class="text-muted">{{ row.createTime }}</td>
          <td style="text-align: right;">
            <a class="editorial-link" @click="handleEdit(row)">编辑</a>
            <a class="editorial-link editorial-link--muted" style="margin-left: 12px;" @click="handleDelete(row)">删除</a>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Pagination -->
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

    <!-- Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="480px"
      :close-on-click-modal="false"
    >
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">用户名</label>
        <input v-model="form.username" class="editorial-input" placeholder="请输入用户名" />
      </div>
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">密码</label>
        <input
          v-model="form.password"
          class="editorial-input"
          type="password"
          :placeholder="isEdit ? '留空则不修改密码' : '请输入密码'"
        />
      </div>
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">真实姓名</label>
        <input v-model="form.realName" class="editorial-input" placeholder="请输入真实姓名" />
      </div>
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">角色</label>
        <select v-model="form.role" class="editorial-select">
          <option value="ADMIN">管理员</option>
          <option value="USER">普通用户</option>
        </select>
      </div>
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">状态</label>
        <select v-model="form.status" class="editorial-select">
          <option :value="1">启用</option>
          <option :value="0">禁用</option>
        </select>
      </div>
      <template #footer>
        <button class="editorial-btn-outline" @click="dialogVisible = false">取消</button>
        <button class="editorial-btn" style="padding: 8px 24px; margin-left: 12px;" :disabled="submitLoading" @click="handleSubmit">
          {{ submitLoading ? '...' : '确定' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getUserPage, addUser, updateUser, deleteUser } from '../../api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const query = reactive({ pageNum: 1, pageSize: 10, username: '', role: '', status: null })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isEdit = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const form = reactive({ username: '', password: '', realName: '', role: 'USER', status: 1 })

onMounted(() => { loadData() })

async function loadData() {
  loading.value = true
  try {
    const res = await getUserPage({ ...query })
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
  query.username = ''
  query.role = ''
  query.status = null
  query.pageNum = 1
  loadData()
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  form.username = ''
  form.password = ''
  form.realName = ''
  form.role = 'USER'
  form.status = 1
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editId.value = row.id
  dialogTitle.value = '编辑用户'
  form.username = row.username
  form.password = ''
  form.realName = row.realName
  form.role = row.role
  form.status = row.status
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.username || !form.realName) {
    ElMessage.warning('请填写用户名和真实姓名')
    return
  }
  submitLoading.value = true
  try {
    const data = { ...form }
    if (isEdit.value) {
      if (!data.password) delete data.password
      await updateUser(editId.value, data)
      ElMessage.success('用户更新成功')
    } else {
      if (!data.password) {
        ElMessage.warning('请输入密码')
        submitLoading.value = false
        return
      }
      await addUser(data)
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(
    `确定要删除用户 "${row.username}" 吗？此操作不可恢复。`,
    '删除确认',
    { type: 'warning' }
  ).then(async () => {
    await deleteUser(row.id)
    ElMessage.success('用户已删除')
    loadData()
  }).catch(() => {})
}

function prevPage() {
  if (query.pageNum > 1) {
    query.pageNum--
    loadData()
  }
}
function nextPage() {
  query.pageNum++
  loadData()
}
</script>

<style scoped>
/* uses editorial classes from editorial.css */
</style>
