import request from '../utils/request'

/** 获取仪表盘数据 */
export function getDashboard() {
  return request({ url: '/forecast/dashboard', method: 'get' })
}

/** 分页查询预报数据 */
export function getRecordPage(params) {
  return request({ url: '/forecast/page', method: 'get', params })
}

/** 获取所有去重的经纬度及观测点 */
export function getLocations() {
  return request({ url: '/forecast/locations', method: 'get' })
}

/** 获取海表温度趋势 */
export function getSstTrend(lon, lat) {
  return request({ url: '/forecast/sst/trend', method: 'get', params: { lon, lat } })
}

/** 获取叶绿素浓度趋势 */
export function getChlTrend(lon, lat) {
  return request({ url: '/forecast/chl/trend', method: 'get', params: { lon, lat } })
}
