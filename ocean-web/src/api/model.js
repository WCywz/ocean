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
