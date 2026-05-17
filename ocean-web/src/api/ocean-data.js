import request from '../utils/request'

/** 分页查询观测数据 */
export function getOceanDataPage(params) {
  return request({ url: '/observation/page', method: 'get', params })
}

/** 获取所有去重的经纬度 */
export function getOceanLocations() {
  return request({ url: '/observation/locations', method: 'get' })
}

/** 海表温度时间序列（支持经纬度过滤） */
export function getSstTimeSeries(startDate, endDate, lat, lon) {
  return request({ url: '/observation/sst-timeseries', method: 'get', params: { startDate, endDate, lat, lon } })
}

/** 叶绿素浓度时间序列（支持经纬度过滤） */
export function getChlTimeSeries(startDate, endDate, lat, lon) {
  return request({ url: '/observation/chl-timeseries', method: 'get', params: { startDate, endDate, lat, lon } })
}

// ---- observation_grid (网格观测数据) ----

/** 观测网格地图数据 */
export function getObsMapGrid(params) {
  return request({ url: '/observation/map/grid', method: 'get', params })
}

/** 观测单点位趋势 */
export function getObsPointTrend(params) {
  return request({ url: '/observation/trend/point', method: 'get', params })
}

/** 观测网格去重经纬度 */
export function getObsGridLocations() {
  return request({ url: '/observation/grid/locations', method: 'get' })
}
