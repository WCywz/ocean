import request from '../utils/request'

/** 获取分区健康指数数据（旧版，保留兼容） */
export function getZoneHealth(params) {
  return request({ url: '/health/assessment', method: 'get', params })
}

/** 获取分区健康指数数据 V2（三层：回顾 + 当前 + 预报） */
export function getZoneHealthV2(params) {
  return request({ url: '/health/assessment-v2', method: 'get', params })
}

/** 获取告警地图数据（站点 + 热点） */
export function getAlertMap(params) {
  return request({ url: '/health/alert-map', method: 'get', params })
}
