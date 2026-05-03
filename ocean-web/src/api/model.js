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
