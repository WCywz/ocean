import request from '../utils/request'

/** 分页查询观测数据 */
export function getOceanDataPage(params) {
  return request({ url: '/ocean-data/page', method: 'get', params })
}

/** 获取所有去重的经纬度 */
export function getOceanLocations() {
  return request({ url: '/ocean-data/locations', method: 'get' })
}

/** 海表温度时间序列（支持经纬度过滤） */
export function getSstTimeSeries(startDate, endDate, lat, lon) {
  return request({ url: '/ocean-data/sst-timeseries', method: 'get', params: { startDate, endDate, lat, lon } })
}

/** 叶绿素浓度时间序列（支持经纬度过滤） */
export function getChlTimeSeries(startDate, endDate, lat, lon) {
  return request({ url: '/ocean-data/chl-timeseries', method: 'get', params: { startDate, endDate, lat, lon } })
}
