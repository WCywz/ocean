/**
 * WGS-84 ↔ GCJ-02 coordinate transformation.
 * 高德/腾讯地图使用 GCJ-02 坐标系，数据库存储 WGS-84，前端显示需转换。
 */

const PI = Math.PI
const A = 6378245.0
const EE = 0.00669342162296594323

function isOutOfChina(lng, lat) {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
}

function transformLat(x, y) {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0
  ret += (160.0 * Math.sin(y / 12.0 * PI) + 320.0 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0
  return ret
}

function transformLng(x, y) {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0
  ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0
  return ret
}

function delta(lng, lat) {
  const dlat = transformLat(lng - 105.0, lat - 35.0)
  const dlng = transformLng(lng - 105.0, lat - 35.0)
  const radlat = lat / 180.0 * PI
  let magic = Math.sin(radlat)
  magic = 1 - EE * magic * magic
  const sqrtmagic = Math.sqrt(magic)
  return [
    (dlng * 180.0) / (A / sqrtmagic * Math.cos(radlat) * PI),
    (dlat * 180.0) / ((A * (1 - EE)) / (magic * sqrtmagic) * PI)
  ]
}

/** WGS-84 → GCJ-02 */
export function wgs84ToGcj02(lng, lat) {
  if (isOutOfChina(lng, lat)) return [lng, lat]
  const [dLng, dLat] = delta(lng, lat)
  return [lng + dLng, lat + dLat]
}

/** GCJ-02 → WGS-84 (approximate inverse via delta at the GCJ-02 point) */
export function gcj02ToWgs84(lng, lat) {
  if (isOutOfChina(lng, lat)) return [lng, lat]
  const [gcjLng, gcjLat] = wgs84ToGcj02(lng, lat)
  const dLng = gcjLng - lng
  const dLat = gcjLat - lat
  return [lng - dLng, lat - dLat]
}
