import request from '../utils/request'

/** 获取分区健康指数数据 */
export function getZoneHealth(params) {
  return request({ url: '/health/assessment', method: 'get', params })
}
