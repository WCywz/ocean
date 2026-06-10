<template>
  <div>
    <h1 class="editorial-page-title">运行状态监控</h1>
    <p class="editorial-page-subtitle">Run Monitor · 模型执行状态与历史记录</p>

    <!-- Today Overview -->
    <div class="overview-bar">
      <div class="overview-bar__item">
        <span class="overview-bar__num">{{ overview.total }}</span>
        <span class="overview-bar__label">总运行</span>
      </div>
      <div class="overview-bar__item">
        <span class="overview-bar__num" style="color: #22c55e;">{{ overview.success }}</span>
        <span class="overview-bar__label">成功</span>
      </div>
      <div class="overview-bar__item">
        <span class="overview-bar__num" style="color: #ef4444;">{{ overview.failed }}</span>
        <span class="overview-bar__label">失败</span>
      </div>
      <div class="overview-bar__item">
        <span class="overview-bar__num" style="color: #e67e22;">{{ overview.running }}</span>
        <span class="overview-bar__label">运行中</span>
      </div>
      <span style="flex: 1;"></span>
      <button class="editorial-btn-outline" @click="refreshAll">刷新</button>
    </div>

    <!-- Status Table -->
    <div v-if="recentLogs.length === 0 && !loading" style="text-align: center; padding: 60px 0; color: var(--color-text-muted); font-size: 14px;">
      暂无运行记录
    </div>

    <div v-else v-loading="loading" style="min-height: 200px;">
      <div class="monitor-table">
        <div class="monitor-table__header">
          <span style="flex: 2;">模型版本</span>
          <span style="flex: 2;">最近运行时间</span>
          <span style="flex: 1;">耗时</span>
          <span style="flex: 1;">状态</span>
          <span style="flex: 1.5;">操作</span>
        </div>
        <div
          v-for="log in recentLogs"
          :key="log.id"
          class="monitor-table__row"
        >
          <span style="flex: 2;">
            <span style="font-weight: 600; color: var(--color-text);">{{ log.modelName }}</span>
            <span class="version-badge">{{ log.versionLabel }}</span>
          </span>
          <span style="flex: 2; font-size: 12px; color: var(--color-text-secondary);">
            {{ formatTime(log.startTime) }}
          </span>
          <span style="flex: 1; font-size: 12px; color: var(--color-text-secondary);">
            {{ log.status === 'RUNNING' ? '-' : formatDuration(log.durationMs) }}
          </span>
          <span style="flex: 1;">
            <span :class="statusClass(log.status)">{{ statusMap[log.status] }}</span>
          </span>
          <span style="flex: 1.5; display: flex; gap: 10px;">
            <a class="editorial-link" @click="openDetail(log)">日志</a>
            <a class="editorial-link" @click="openHistory(log)">历史</a>
            <a class="editorial-link" @click="exportSingle(log)">CSV</a>
          </span>
        </div>
      </div>
    </div>

    <!-- Log Detail Dialog -->
    <el-dialog v-model="detailVisible" title="运行日志" width="640px" :close-on-click-modal="false">
      <div v-if="detailLog">
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-item__label">模型</span>
            <span class="detail-item__value">{{ detailLog.modelName }} {{ detailLog.versionLabel }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">状态</span>
            <span :class="statusClass(detailLog.status)" style="font-weight: 600;">{{ statusMap[detailLog.status] }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">开始时间</span>
            <span class="detail-item__value">{{ formatTime(detailLog.startTime) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">结束时间</span>
            <span class="detail-item__value">{{ formatTime(detailLog.endTime) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">耗时</span>
            <span class="detail-item__value">{{ formatDuration(detailLog.durationMs) }}</span>
          </div>
          <div class="detail-item" v-if="detailLog.outputSummary">
            <span class="detail-item__label">输出概要</span>
            <span class="detail-item__value">{{ detailLog.outputSummary }}</span>
          </div>
        </div>
        <div v-if="detailLog.errorMessage" class="error-banner">
          {{ detailLog.errorMessage }}
        </div>
        <div v-if="detailLog.logText" style="margin-top: 14px;">
          <div class="log-section-title">执行日志</div>
          <pre class="log-text">{{ detailLog.logText }}</pre>
        </div>
      </div>
      <template #footer>
        <button class="editorial-btn-outline" @click="detailVisible = false">关闭</button>
      </template>
    </el-dialog>

    <!-- History Dialog -->
    <el-dialog v-model="historyVisible" :title="`运行历史 — ${historyTarget?.modelName || ''} ${historyTarget?.versionLabel || ''}`" width="680px" :close-on-click-modal="false">
      <div v-if="historyLoading" style="text-align: center; padding: 20px; color: var(--color-text-muted);">加载中...</div>
      <div v-else-if="historyList.length === 0" style="text-align: center; padding: 20px; color: var(--color-text-muted);">暂无历史记录</div>
      <table v-else class="history-table">
        <thead>
          <tr>
            <th>日期</th>
            <th>时间</th>
            <th>耗时</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="h in historyList" :key="h.id">
            <td>{{ formatDate(h.startTime) }}</td>
            <td>{{ formatTimeOnly(h.startTime) }}</td>
            <td>{{ formatDuration(h.durationMs) }}</td>
            <td :class="statusClass(h.status)">{{ statusMap[h.status] }}</td>
            <td><a class="editorial-link" @click="openDetail(h); historyVisible = false">查看</a></td>
          </tr>
        </tbody>
      </table>
      <template #footer>
        <a class="editorial-link" style="margin-right: auto;" @click="exportHistory">导出 CSV</a>
        <button class="editorial-btn-outline" @click="historyVisible = false">关闭</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  getTodayOverview, getRecentRunLogs, getRunLogById,
  getRunLogHistory, exportRunLogCsv
} from '../../api/model'

const statusMap = { SUCCESS: '成功', FAILED: '失败', RUNNING: '运行中' }

function statusClass(s) {
  return s === 'SUCCESS' ? 'status-success' : s === 'FAILED' ? 'status-fail' : 'status-running'
}

// Overview
const overview = reactive({ total: 0, success: 0, failed: 0, running: 0 })
const recentLogs = ref([])
const loading = ref(false)

// Log detail dialog
const detailVisible = ref(false)
const detailLog = ref(null)

// History dialog
const historyVisible = ref(false)
const historyTarget = ref(null)
const historyList = ref([])
const historyLoading = ref(false)

onMounted(() => refreshAll())

async function refreshAll() {
  loading.value = true
  try {
    const [ov, rec] = await Promise.all([getTodayOverview(), getRecentRunLogs()])
    Object.assign(overview, ov.data)
    recentLogs.value = rec.data || []
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}

async function openDetail(log) {
  try {
    const res = await getRunLogById(log.id)
    detailLog.value = res.data
    detailVisible.value = true
  } catch { /* handled */ }
}

async function openHistory(log) {
  historyTarget.value = log
  historyVisible.value = true
  historyLoading.value = true
  try {
    const res = await getRunLogHistory(log.versionId, 7)
    historyList.value = res.data || []
  } catch { historyList.value = [] }
  finally { historyLoading.value = false }
}

async function exportSingle(log) {
  try {
    const token = localStorage.getItem('token')
    const res = await fetch(`/api/model/run-log/export?versionId=${log.versionId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (!res.ok) throw new Error('导出失败')
    const blob = await res.blob()
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = 'run_log_export.csv'
    a.click()
    URL.revokeObjectURL(a.href)
  } catch { /* handled */ }
}

function exportHistory() {
  if (!historyTarget.value) return
  exportSingle(historyTarget.value)
}

function formatTime(d) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 19)
}
function formatTimeOnly(d) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(11, 19)
}
function formatDate(d) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 10)
}
function formatDuration(ms) {
  if (!ms || ms === 0) return '-'
  const seconds = Math.floor(ms / 1000)
  if (seconds < 60) return seconds + '秒'
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  return minutes + '分' + secs + '秒'
}
</script>

<style scoped>
.error-banner {
  margin-top: 14px;
  padding: 10px 12px;
  background: #fef2f2;
  border-left: 3px solid #ef4444;
  font-size: 12px;
  color: #b91c1c;
}

.overview-bar {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 16px 18px;
  background: var(--color-surface);
  border: 1px solid var(--color-divider-strong);
  margin-bottom: 24px;
}

.overview-bar__num {
  font-family: var(--font-serif);
  font-size: 28px;
  line-height: 1;
  display: block;
}

.overview-bar__label {
  font-size: 11px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-top: 4px;
}

.monitor-table__header,
.monitor-table__row {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  gap: 12px;
}

.monitor-table__header {
  font-size: 11px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  border-bottom: 1px solid var(--color-divider-strong);
}

.monitor-table__row {
  border-bottom: 1px solid var(--color-divider);
  font-size: 13px;
}

.monitor-table__row:hover {
  background: var(--color-surface);
}

.version-badge {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
  padding: 1px 7px;
  font-size: 11px;
  font-weight: 500;
  margin-left: 8px;
}

.status-success { color: #22c55e; }
.status-fail { color: #ef4444; }
.status-running { color: #e67e22; }

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 24px;
}

.detail-item__label {
  font-size: 11px;
  color: var(--color-text-muted);
  display: block;
}

.detail-item__value {
  font-size: 13px;
  color: var(--color-text);
}

.log-section-title {
  font-size: 11px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 8px;
}

.log-text {
  font-size: 12px;
  line-height: 1.7;
  color: var(--color-text-secondary);
  background: var(--color-surface);
  padding: 12px 14px;
  max-height: 240px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.history-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.history-table th,
.history-table td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid var(--color-divider);
}

.history-table th {
  font-size: 11px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 600;
}
</style>

<style>
[data-theme="dark"] .error-banner {
  background: #2d1517;
  color: #f87171;
}
</style>