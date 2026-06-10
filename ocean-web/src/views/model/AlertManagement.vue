<template>
  <div>
    <h1 class="editorial-page-title">告警管理</h1>
    <p class="editorial-page-subtitle">Alert Management · 共 {{ total }} 条告警</p>

    <div class="editorial-filter-bar">
      <el-select v-model="query.alertType" placeholder="全部类型" style="width: 140px;">
        <el-option label="全部类型" value="" />
        <el-option label="执行失败" value="EXECUTION_FAILED" />
        <el-option label="连续失败" value="CONSECUTIVE_FAILURES" />
        <el-option label="执行超时" value="EXECUTION_TIMEOUT" />
      </el-select>
      <el-select v-model="query.isRead" placeholder="全部状态" style="width: 100px;">
        <el-option label="全部状态" :value="null" />
        <el-option label="未读" :value="0" />
        <el-option label="已读" :value="1" />
      </el-select>
      <button class="editorial-btn-outline" @click="handleSearch">查询</button>
      <button class="editorial-btn-outline" @click="handleReset">重置</button>
      <span style="flex: 1;"></span>
      <button class="editorial-btn-outline" @click="handleMarkAllRead">全部标记已读</button>
    </div>

    <div v-if="tableData.length === 0 && !loading" style="text-align: center; padding: 60px 0; color: var(--color-text-muted); font-size: 14px;">
      暂无告警记录
    </div>

    <div v-else v-loading="loading" style="min-height: 200px;">
      <div class="alert-table">
        <div class="alert-table__header">
          <span style="flex: 2;">模型版本</span>
          <span style="flex: 1;">类型</span>
          <span style="flex: 2.5;">消息</span>
          <span style="flex: 1.5;">时间</span>
          <span style="flex: 0.5;">状态</span>
          <span style="flex: 1;">操作</span>
        </div>
        <div
          v-for="row in tableData"
          :key="row.id"
          class="alert-table__row"
          :class="{ 'alert-table__row--unread': row.isRead === 0 }"
        >
          <span style="flex: 2;">
            <span style="font-weight: 600; color: var(--color-text);">{{ row.modelName }}</span>
            <span class="ver-badge">{{ row.versionLabel }}</span>
          </span>
          <span style="flex: 1; font-size: 12px; color: var(--color-text-secondary);">{{ row.typeLabel }}</span>
          <span style="flex: 2.5; font-size: 12px; color: var(--color-text-secondary);" :title="row.message">{{ row.message }}</span>
          <span style="flex: 1.5; font-size: 12px; color: var(--color-text-muted);">{{ formatTime(row.createTime) }}</span>
          <span style="flex: 0.5;">
            <span v-if="row.isRead === 0" style="color: #ef4444; font-size: 11px;">未读</span>
            <span v-else style="color: var(--color-text-muted); font-size: 11px;">已读</span>
          </span>
          <span style="flex: 1; display: flex; gap: 8px;">
            <a v-if="row.isRead === 0" class="editorial-link" @click="handleMarkRead(row)">标为已读</a>
            <a v-if="row.runLogId" class="editorial-link" @click="goRunLog(row)">查看日志</a>
          </span>
        </div>
      </div>
    </div>

    <div class="editorial-pagination" v-if="total > 0">
      <span>共 {{ total }} 条</span>
      <el-select v-model="query.pageSize" style="width: 80px;" @change="loadData">
        <el-option :label="'10'" :value="10" />
        <el-option :label="'20'" :value="20" />
        <el-option :label="'50'" :value="50" />
      </el-select>
      <a class="editorial-link" @click="prevPage">&larr;</a>
      <span class="editorial-pagination__page editorial-pagination__page--active">{{ query.pageNum }}</span>
      <a class="editorial-link" @click="nextPage">&rarr;</a>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAlertPage, markAlertRead, markAllAlertsRead } from '../../api/model'
import { ElMessage } from 'element-plus'

const router = useRouter()

const query = reactive({ pageNum: 1, pageSize: 10, alertType: '', isRead: null })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.alertType) params.alertType = query.alertType
    if (query.isRead !== null) params.isRead = query.isRead
    const res = await getAlertPage(params)
    tableData.value = res.data.records || []
    total.value = res.data.total
  } finally { loading.value = false }
}

function handleSearch() { query.pageNum = 1; loadData() }
function handleReset() { query.alertType = ''; query.isRead = null; query.pageNum = 1; loadData() }
function prevPage() { if (query.pageNum > 1) { query.pageNum--; loadData() } }
function nextPage() { query.pageNum++; loadData() }

async function handleMarkRead(row) {
  try {
    await markAlertRead(row.id)
    row.isRead = 1
    ElMessage.success('已标记为已读')
  } catch { /* interceptor handles */ }
}

async function handleMarkAllRead() {
  try {
    await markAllAlertsRead()
    loadData()
    ElMessage.success('全部已标记为已读')
  } catch { /* interceptor handles */ }
}

function goRunLog() {
  router.push('/app/model/monitor')
}

function formatTime(d) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
.alert-table__header,
.alert-table__row {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  gap: 12px;
}

.alert-table__header {
  font-size: 11px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  border-bottom: 1px solid var(--color-divider-strong);
}

.alert-table__row {
  border-bottom: 1px solid var(--color-divider);
  font-size: 13px;
}

.alert-table__row:hover { background: var(--color-surface); }

.alert-table__row--unread {
  border-left: 2px solid #ef4444;
}

.ver-badge {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
  padding: 1px 7px;
  font-size: 11px;
  font-weight: 500;
  margin-left: 8px;
}
</style>
