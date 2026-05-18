import request from '../utils/request'

/** 获取系统日期 */
export function getSystemDate() {
  return request({ url: '/system/date', method: 'get' })
}
