# Grid Map → Heatmap Migration

**Date**: 2026-05-13
**Status**: Approved

## Summary

Replace discrete `L.circleMarker` grid-point rendering in `OceanMap.vue` with `L.heatLayer` for continuous heatmap visualization. Remove click-to-trend interaction; trend chart loads with a default point instead.

## Context

- `leaflet.heat` already installed in `ocean-web/package.json`
- `buildHeatGradient()` already exported from `chart-config.js`
- `OceanMap.vue` is used by `SstMapView.vue` and `ChxMapView.vue`

## Files Changed

| File | Change |
|---|---|
| `ocean-web/src/components/OceanMap.vue` | Replace circleMarker with L.heatLayer; remove cellClick emit |
| `ocean-web/src/views/forecast/SstMapView.vue` | Remove click handler; load default trend on mount |
| `ocean-web/src/views/forecast/ChxMapView.vue` | Remove click handler; load default trend on mount |

## OceanMap.vue

### drawGrid() rewrite

Replace the circleMarker + layerGroup loop (current lines 71-96) with:

```
const data = visible.map(p => [p.lat, p.lon, p.value])
const gradient = buildHeatGradient(props.colorRanges)
const heat = L.heatLayer(data, {
  radius: 15,
  blur: 10,
  maxZoom: 10,
  gradient
})
heat.addTo(map)
```

- Store heat layer reference (replaces `canvasLayer`), remove old layer before creating new one
- Viewport filtering (current lines 63-66) stays — only visible points go to heatLayer
- Watch, moveend, draw controls, and bboxChange emit unchanged

### Remove click

- Delete the `marker.on('click', ...)` block — heatmap has no click targets
- Remove `cellClick` from `defineEmits`

## SstMapView.vue / ChxMapView.vue

- Remove `@cell-click` binding on `<OceanMap>`
- Remove `onMapCellClick` function and `selectedPoint` variable
- In `onMounted`, after `fetchGridData()`, call `fetchTrendData(123.5, 29.8)` with the default center point
- Remove the `selectedPoint` display span from template (lines 40-42 in SstMapView, 44-47 in ChxMapView)

## What Does NOT Change

- `chart-config.js` — `buildHeatGradient` already exists
- `package.json` — `leaflet.heat` already installed
- Leaflet tile layer, draw controls, zoom/pan behavior
- Legend overlay rendering
- Filter bar, date picker, sea area selector, CHL mode toggle
- Backend API — same endpoints, same data format
- `TrendChart` component — unchanged

## Test Plan

- SST heatmap renders with `SST_MAP_COLORS` gradient
- CHL heatmap renders in both concentration and probability modes
- Legend overlay displays correctly on top of heatmap
- Zoom/pan updates heatmap to viewport range
- Draw rectangle/polygon selection still works and triggers bbox filtering
- Trend chart loads automatically on page mount with default center point data
- Filter bar search/reset still works
