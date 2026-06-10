import request from '../utils/request'

/** 分页查询模型 */
export function getModelPage(params) {
  return request({ url: '/model/page', method: 'get', params })
}

/** 根据ID查询模型 */
export function getModelById(id) {
  return request({ url: `/model/${id}`, method: 'get' })
}

/** 新增模型 */
export function addModel(data) {
  return request({ url: '/model', method: 'post', data })
}

/** 修改模型 */
export function updateModel(id, data) {
  return request({ url: `/model/${id}`, method: 'put', data })
}

/** 删除模型 */
export function deleteModel(id) {
  return request({ url: `/model/${id}`, method: 'delete' })
}

/** 启停模型 */
export function toggleModelStatus(id, status) {
  return request({ url: `/model/${id}/status`, method: 'put', params: { status } })
}

/** 获取所有运行中的版本（概览用） */
export function getRunningVersions() {
  return request({ url: '/model/running-versions', method: 'get' })
}

/** 获取模型下的所有版本 */
export function getModelVersions(modelId) {
  return request({ url: `/model/${modelId}/version`, method: 'get' })
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

// ==================== 运行日志 ====================

/** 分页查询运行日志 */
export function getRunLogPage(params) {
  return request({ url: '/model/run-log/page', method: 'get', params })
}

/** 查询单条日志详情 */
export function getRunLogById(id) {
  return request({ url: `/model/run-log/${id}`, method: 'get' })
}

/** 今日概览统计 */
export function getTodayOverview() {
  return request({ url: '/model/run-log/today-overview', method: 'get' })
}

/** 最近运行日志（每个版本最新一条） */
export function getRecentRunLogs() {
  return request({ url: '/model/run-log/recent', method: 'get' })
}

/** 版本运行历史 */
export function getRunLogHistory(versionId, days) {
  return request({ url: '/model/run-log/history', method: 'get', params: { versionId, days } })
}

/** 导出CSV */
export function exportRunLogCsv(params) {
  return request({ url: '/model/run-log/export', method: 'get', params, responseType: 'blob' })
}

// ==================== 告警 ====================

/** 未读告警列表 */
export function getUnreadAlerts() {
  return request({ url: '/alert/unread', method: 'get' })
}

/** 最近告警列表 */
export function getRecentAlerts(limit) {
  return request({ url: '/alert/recent', method: 'get', params: { limit } })
}

/** 未读告警数量 */
export function getUnreadAlertCount() {
  return request({ url: '/alert/unread-count', method: 'get' })
}

/** 标记单条已读 */
export function markAlertRead(id) {
  return request({ url: `/alert/${id}/read`, method: 'put' })
}

/** 全部标记已读 */
export function markAllAlertsRead() {
  return request({ url: '/alert/read-all', method: 'put' })
}

/** 分页查询告警 */
export function getAlertPage(params) {
  return request({ url: '/alert/page', method: 'get', params })
}

// ====== 调度管理 ======

/** 获取某版本的所有调度 */
export function getVersionSchedules(modelId, versionId) {
  return request({ url: `/model/${modelId}/version/${versionId}/schedule`, method: 'get' })
}

/** 创建调度 */
export function addSchedule(modelId, versionId, data) {
  return request({ url: `/model/${modelId}/version/${versionId}/schedule`, method: 'post', data })
}

/** 更新调度 */
export function updateSchedule(scheduleId, data) {
  return request({ url: `/model/schedule/${scheduleId}`, method: 'put', data })
}

/** 删除调度 */
export function deleteSchedule(scheduleId) {
  return request({ url: `/model/schedule/${scheduleId}`, method: 'delete' })
}

/** 按周获取调度（日历展示用） */
export function getWeekSchedules(params) {
  return request({ url: '/model/schedule/week', method: 'get', params })
}

/** 获取所有可调度版本 */
export function getAvailableVersions() {
  return request({ url: '/model/schedule/available-versions', method: 'get' })
}

// ====== 系统配置 ======

/** 获取系统日期 */
export function getSystemDate() {
  return request({ url: '/system/date', method: 'get' })
}
