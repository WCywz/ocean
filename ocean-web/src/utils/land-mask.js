import { feature } from 'topojson-client'
import landTopo from 'world-atlas/land-50m.json'
import { wgs84ToGcj02 } from './coord-transform'

let _geojson = null

function convertCoords(coords) {
  if (typeof coords[0] === 'number') {
    const [lng, lat] = wgs84ToGcj02(coords[0], coords[1])
    return [lng, lat]
  }
  return coords.map(convertCoords)
}

export function getLandGeoJSON() {
  if (_geojson) return _geojson

  const geojson = feature(landTopo, landTopo.objects.land)
  const features = geojson.features ? geojson.features : [geojson]

  _geojson = {
    type: 'FeatureCollection',
    features: features.map(f => ({
      ...f,
      geometry: {
        ...f.geometry,
        coordinates: convertCoords(f.geometry.coordinates)
      }
    }))
  }
  return _geojson
}
