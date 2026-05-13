# Grid Map → Heatmap Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace discrete grid-point circle markers with continuous L.heatLayer heatmap in OceanMap.vue, and switch trend loading from click-driven to default-point.

**Architecture:** Rewrite `drawGrid()` in OceanMap.vue to use `L.heatLayer` instead of `L.circleMarker`/`L.layerGroup`. Remove `cellClick` emit. Update both SstMapView and ChxMapView to load trend with default center point on mount instead of on click.

**Tech Stack:** Vue 3 + Leaflet + leaflet.heat 0.2.0

---

### Task 1: OceanMap.vue — Heatmap rendering

**Files:**
- Modify: `ocean-web/src/components/OceanMap.vue`

- [ ] **Step 1: Import leaflet.heat and buildHeatGradient**

Change the imports at the top of `<script setup>`:

```js
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import L from 'leaflet'
import 'leaflet-draw'
import 'leaflet.heat'
import { buildHeatGradient } from '../utils/chart-config'
```

Remove `getMapColor` from the import (no longer used).

- [ ] **Step 2: Remove cellClick from emits and replace canvasLayer ref**

Change `defineEmits` line:
```js
const emit = defineEmits(['bboxChange'])
```

Replace `let canvasLayer = null` with:
```js
let heatLayer = null
```

- [ ] **Step 3: Rewrite drawGrid() function**

Replace the entire `drawGrid()` function (lines 47-98) with:

```js
function drawGrid() {
  if (!map || !props.gridData.length) return

  if (heatLayer) {
    map.removeLayer(heatLayer)
  }

  const bounds = map.getBounds()
  const pad = 0.01
  const visible = props.gridData.filter(p =>
    p.lat >= bounds.getSouth() - pad &&
    p.lat <= bounds.getNorth() + pad &&
    p.lon >= bounds.getWest() - pad &&
    p.lon <= bounds.getEast() + pad
  )

  if (!visible.length) return

  const data = visible.map(p => [p.lat, p.lon, p.value])
  heatLayer = L.heatLayer(data, {
    radius: 15,
    blur: 10,
    maxZoom: 10,
    gradient: buildHeatGradient(props.colorRanges)
  }).addTo(map)
}
```

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/components/OceanMap.vue
git commit -m "feat: replace circleMarker grid with L.heatLayer in OceanMap"
```

---

### Task 2: SstMapView.vue — Default trend on mount

**Files:**
- Modify: `ocean-web/src/views/forecast/SstMapView.vue`

- [ ] **Step 1: Remove click binding and selectedPoint display from template**

Remove `@cell-click="onMapCellClick"` from the `<OceanMap>` tag (line 37). The tag becomes:
```html
<OceanMap
  :grid-data="gridData"
  :color-ranges="SST_MAP_COLORS"
  :legend-labels="legendLabels"
  legend-title="温度 (°C)"
  :loading="mapLoading"
  @bbox-change="onBboxChange"
/>
```

Remove the `selectedPoint` display span (lines 40-42):
```html
<span v-if="selectedPoint" style="font-size: 13px; color: var(--color-text-muted);">
  当前选中: ({{ selectedPoint.lon.toFixed(2) }}, {{ selectedPoint.lat.toFixed(2) }})
</span>
```

- [ ] **Step 2: Remove selectedPoint ref and onMapCellClick**

Remove line 71:
```js
const selectedPoint = ref(null)
```

Remove the `onMapCellClick` function (lines 132-134):
```js
function onMapCellClick({ lat, lon }) {
  fetchTrendData(lon, lat)
}
```

- [ ] **Step 3: Add default trend call in onMounted**

Change `onMounted` to call `fetchTrendData` with the default center point after grid data loads:

```js
onMounted(async () => {
  filterDate.value = '2026-01-01'
  await loadSeaAreas()
  await fetchGridData()
  fetchTrendData(123.5, 29.8)
})
```

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/forecast/SstMapView.vue
git commit -m "feat: load SST trend with default point on mount"
```

---

### Task 3: ChxMapView.vue — Default trend on mount

**Files:**
- Modify: `ocean-web/src/views/forecast/ChxMapView.vue`

- [ ] **Step 1: Remove click binding and selectedPoint display from template**

Remove `@cell-click="onMapCellClick"` from the `<OceanMap>` tag. The tag becomes:
```html
<OceanMap
  :grid-data="gridData"
  :color-ranges="currentColorRanges"
  :legend-labels="currentLegendLabels"
  :legend-title="chlMode === 'concentration' ? '浓度 (mg/m³)' : '概率 (%)'"
  :loading="mapLoading"
  @bbox-change="onBboxChange"
/>
```

Remove the `selectedPoint` display span (lines 44-47):
```html
<span v-if="selectedPoint" style="font-size: 13px; color: var(--color-text-muted);">选中: ({{ selectedPoint.lon.toFixed(2) }}, {{ selectedPoint.lat.toFixed(2) }})</span>
```

- [ ] **Step 2: Remove selectedPoint ref and onMapCellClick**

Remove line 78:
```js
const selectedPoint = ref(null)
```

Remove the `onMapCellClick` function (line 160):
```js
function onMapCellClick({ lat, lon }) { fetchTrendData(lon, lat) }
```

- [ ] **Step 3: Add default trend call in onMounted**

Change `onMounted` to call `fetchTrendData` with the default center point after grid data loads:

```js
onMounted(async () => {
  filterDate.value = '2026-01-01'
  await loadSeaAreas()
  await fetchGridData()
  fetchTrendData(123.5, 29.8)
})
```

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/forecast/ChxMapView.vue
git commit -m "feat: load CHL trend with default point on mount"
```
