import { feature } from 'topojson-client'
import landTopo from 'world-atlas/land-50m.json'

let _multiPolygon = null
let _bboxes = null

function getLandMultiPolygon() {
  if (_multiPolygon) return _multiPolygon
  const geojson = feature(landTopo, landTopo.objects.land)
  const features = geojson.features || [geojson]
  _multiPolygon = features[0].geometry
  return _multiPolygon
}

function getBBoxes() {
  if (_bboxes) return _bboxes
  const mp = getLandMultiPolygon()
  _bboxes = mp.coordinates.map(polygon => {
    const ring = polygon[0]
    let minLng = Infinity, maxLng = -Infinity
    let minLat = Infinity, maxLat = -Infinity
    for (const [lng, lat] of ring) {
      if (lng < minLng) minLng = lng
      if (lng > maxLng) maxLng = lng
      if (lat < minLat) minLat = lat
      if (lat > maxLat) maxLat = lat
    }
    return { minLng, maxLng, minLat, maxLat }
  })
  return _bboxes
}

function pointInPolygon(x, y, rings) {
  let inside = false
  for (const ring of rings) {
    for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
      const xi = ring[i][0], yi = ring[i][1]
      const xj = ring[j][0], yj = ring[j][1]
      if ((yi > y) !== (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
        inside = !inside
      }
    }
  }
  return inside
}

export function isPointInLand(lng, lat) {
  const mp = getLandMultiPolygon()
  const bboxes = getBBoxes()
  for (let i = 0; i < mp.coordinates.length; i++) {
    const bb = bboxes[i]
    if (lng < bb.minLng || lng > bb.maxLng || lat < bb.minLat || lat > bb.maxLat) continue
    if (pointInPolygon(lng, lat, mp.coordinates[i])) return true
  }
  return false
}
