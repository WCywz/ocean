# Map Optimization: Heatmap + Overflow Fix — Design Spec

**Date:** 2026-05-06
**Status:** Approved
**Scope:** OceanMap component heatmap migration + dashboard map overflow fix

## Summary

Two targeted fixes to the Leaflet-based map visualization:

1. **Heatmap migration** — Replace `L.circleMarker` grid-point rendering in `OceanMap.vue` with `L.heatLayer` (leaflet.heat plugin) for continuous heatmap visualization
2. **Overflow fix** — Prevent map tiles from breaking out of the dashboard flex container

## Change 1: Grid Points → Heatmap

### Current state

`OceanMap.vue` `drawGrid()` renders each data point as an individual `L.circleMarker` with radius 4, grouped by color into `L.layerGroup` instances. The result is a discrete dot map where spacing varies with zoom level.

### Target state

Replace circleMarker rendering with `L.heatLayer` from the leaflet.heat plugin (~5KB). The heatmap interpolates values across the viewport, producing a continuous color surface that looks consistent at any zoom level.

### Implementation

**New dependency:** `leaflet.heat` (npm)

**OceanMap.vue changes:**

- Import `leaflet.heat` at top of script
- In `drawGrid()`: build `[lat, lon, value]` triplets from visible grid data, replace circleMarker loop with `L.heatLayer(data, options).addTo(map)`
- Store heat layer reference (replaces `canvasLayer` ref), remove old layer on re-render
- Heat options: `radius: 15`, `blur: 10`, `maxZoom: 10`, gradient dynamically built from `colorRanges` prop
- Viewport filtering logic (lines 63-66) stays — reduces data passed to heat layer
- Click interaction: use `map.on('click', ...)` event on the heat layer to find nearest data point and emit `cellClick`

**chart-config.js addition:**

New export `buildHeatGradient(colorRanges)` — converts `[{min, max, color}]` to a leaflet.heat gradient object `{0.0: '#...', 0.25: '#...', ...}` keyed by normalized value position.

**Heat layer lifecycle:**

- `drawGrid()` removes previous `heatLayer` before creating new one
- `watch(gridData)` triggers `drawGrid()` via `nextTick` (existing pattern)
- `moveend` still triggers `drawGrid()` + `bboxChange` emit (existing pattern)

### Color mapping

Each page already passes `colorRanges` (SST_MAP_COLORS, CHL_CONC_COLORS, CHL_PROB_COLORS). The gradient is generated from whatever range config is active — no hardcoded gradients.

## Change 2: Dashboard Map Overflow

### Root cause

Two issues compound:
1. `.ocean-map-wrapper` lacks `overflow: hidden`, so Leaflet's absolute-positioned tile layers can render outside the wrapper
2. In `DashboardView.vue`, the flex container around `DashboardMap` + `AlertPanel` has no `min-width: 0`, so the map's intrinsic tile width can force the flex item wider than its `flex: 2` allocation

### Fix

**OceanMap.vue** — Add `overflow: hidden` to `.ocean-map-wrapper`.

**DashboardView.vue** — Add `min-width: 0` to the map's parent `<div style="flex: 2;">`.

These are CSS-only changes with no logic impact.

## Files Changed

| File | Change |
|---|---|
| `ocean-web/package.json` | Add `leaflet.heat` dependency |
| `ocean-web/src/components/OceanMap.vue` | Heatmap rendering + overflow CSS fix |
| `ocean-web/src/utils/chart-config.js` | Add `buildHeatGradient()` |
| `ocean-web/src/views/dashboard/DashboardView.vue` | Add `min-width: 0` to map flex container |

## Test Plan

- Heatmap renders on SST forecast page (SstMapView), color gradient matches SST_MAP_COLORS
- Heatmap renders on CHL forecast page (ChxMapView), both concentration and probability modes
- Heatmap renders on dashboard (DashboardMap), SST/CHL toggle works
- Clicking the heatmap still triggers trend analysis (SstMapView / ChxMapView)
- Clicking the dashboard heatmap still navigates to the forecast page
- Dashboard map does not overflow its container at various window widths
- Zoom/pan updates heatmap to viewport range, bbox selection (leaflet-draw) still works
- Legend overlay still displays correctly on top of heatmap
