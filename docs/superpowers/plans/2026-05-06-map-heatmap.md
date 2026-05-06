# Map Heatmap + Overflow Fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace grid-point map rendering with Leaflet.heat heatmap and fix dashboard map overflow.

**Architecture:** Install leaflet.heat plugin, add `buildHeatGradient()` utility to chart-config.js, rewrite `OceanMap.vue` drawGrid() to use `L.heatLayer()` instead of `L.circleMarker`, add CSS overflow constraints to OceanMap and DashboardView.

**Tech Stack:** Vue 3 + Leaflet 1.9 + leaflet.heat + Vite + vitest

---

### Task 1: Install leaflet.heat dependency

**Files:**
- Modify: `ocean-web/package.json`

- [ ] **Step 1: Install the package**

Run: `cd ocean-web && npm install leaflet.heat`

Expected: package.json updated with `"leaflet.heat": "^1.0.0"` (or similar version).

- [ ] **Step 2: Verify installation**

Run: `cd ocean-web && node -e "require('leaflet.heat'); console.log('OK')"` 2>/dev/null || echo "ESM — verify via import in build"

Expected: Either "OK" or "ESM — verify via import in build" (leaflet.heat is a CommonJS module, may not resolve with `require` in ESM project — Vite handles CJS→ESM interop automatically).

- [ ] **Step 3: Commit**

```bash
git add ocean-web/package.json ocean-web/package-lock.json
git commit -m "chore: add leaflet.heat dependency"
```

---

### Task 2: Add buildHeatGradient to chart-config.js

**Files:**
- Modify: `ocean-web/src/utils/chart-config.js`
- Modify: `ocean-web/src/utils/__tests__/chart-config.test.js`

- [ ] **Step 1: Write the failing tests**

Update the top import line in `ocean-web/src/utils/__tests__/chart-config.test.js` to add `buildHeatGradient`:

```js
import { SST_COLORS, CHL_COLORS, OCEAN_CHART_COLORS, buildBaseOption, buildTooltipFormatter, buildSeriesData, SST_MAP_COLORS, CHL_CONC_COLORS, CHL_PROB_COLORS, getMapColor, buildHeatGradient } from '../chart-config'
```

Add a new top-level `describe` block after the closing `})` of `describe('map color configs', ...)`. Add it before the final line of the file:

```js
  describe('buildHeatGradient', () => {
    it('converts SST color ranges to leaflet.heat gradient object', () => {
      const gradient = buildHeatGradient(SST_MAP_COLORS)
      expect(gradient).toBeTypeOf('object')
      expect(Object.keys(gradient)).toHaveLength(5)
      expect(gradient['0']).toBe('#1A5276')
      expect(gradient['1']).toBe('#E74C3C')
    })

    it('normalizes gradient keys between 0 and 1', () => {
      const gradient = buildHeatGradient(SST_MAP_COLORS)
      const keys = Object.keys(gradient).map(Number)
      expect(Math.min(...keys)).toBeGreaterThanOrEqual(0)
      expect(Math.max(...keys)).toBeLessThanOrEqual(1)
    })

    it('handles empty ranges gracefully', () => {
      const gradient = buildHeatGradient([])
      expect(gradient).toEqual({ '0': '#999', '1': '#999' })
    })
  })
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd ocean-web && npx vitest run src/utils/__tests__/chart-config.test.js`

Expected: FAIL — `buildHeatGradient` not exported or test referencing undefined.

- [ ] **Step 3: Implement buildHeatGradient**

Add after `getMapColor` (after line 150) in `ocean-web/src/utils/chart-config.js`:

```js
/**
 * Convert color range config to leaflet.heat gradient object.
 * Keys are 0.0–1.0 normalized positions, values are hex colors.
 */
export function buildHeatGradient(colorRanges) {
  if (!colorRanges.length) {
    return { 0: '#999', 1: '#999' }
  }
  const gradient = {}
  const n = colorRanges.length
  colorRanges.forEach((range, i) => {
    gradient[i / (n - 1)] = range.color
  })
  return gradient
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd ocean-web && npx vitest run src/utils/__tests__/chart-config.test.js`

Expected: All tests pass (existing tests + 3 new tests).

- [ ] **Step 5: Commit**

```bash
git add ocean-web/src/utils/chart-config.js ocean-web/src/utils/__tests__/chart-config.test.js
git commit -m "feat: add buildHeatGradient utility for leaflet.heat gradient config"
```

---

### Task 3: Rewrite OceanMap.vue with heatmap rendering

**Files:**
- Modify: `ocean-web/src/components/OceanMap.vue`

- [ ] **Step 1: Add leaflet.heat import**

In the `<script setup>` block, add after `import L from 'leaflet'` (line 22):

```js
import 'leaflet.heat'
```

This patches `L.heatLayer()` onto the Leaflet namespace via side-effect import.

- [ ] **Step 2: Replace ref and drawGrid implementation**

Replace `let canvasLayer = null` (line 41) with:

```js
let heatLayer = null
```

Replace the entire `drawGrid` function (lines 47-98) with:

```js
function drawGrid() {
  if (!map || !props.gridData.length) return

  if (heatLayer) {
    map.removeLayer(heatLayer)
    heatLayer = null
  }

  const bounds = map.getBounds()
  const south = bounds.getSouth()
  const north = bounds.getNorth()
  const west = bounds.getWest()
  const east = bounds.getEast()

  const visible = props.gridData.filter(p =>
    p.lat >= south && p.lat <= north && p.lon >= west && p.lon <= east
  )

  if (!visible.length) return

  const heatData = visible.map(p => [p.lat, p.lon, p.value ?? 0])

  heatLayer = L.heatLayer(heatData, {
    radius: 15,
    blur: 10,
    maxZoom: 10,
    gradient: buildHeatGradient(props.colorRanges)
  })

  heatLayer.on('click', (e) => {
    const clickLat = e.latlng.lat
    const clickLng = e.latlng.lng
    let nearest = null
    let minDist = Infinity
    for (const p of props.gridData) {
      const dlat = p.lat - clickLat
      const dlng = p.lon - clickLng
      const dist = dlat * dlat + dlng * dlng
      if (dist < minDist) {
        minDist = dist
        nearest = p
      }
    }
    if (nearest) {
      emit('cellClick', { lat: nearest.lat, lon: nearest.lon, value: nearest.value })
    }
  })

  heatLayer.addTo(map)
}
```

- [ ] **Step 3: Add buildHeatGradient import**

Update the import from chart-config (line 24):

```js
import { getMapColor, buildHeatGradient } from '../utils/chart-config'
```

Note: `getMapColor` is no longer used by drawGrid. Remove it from the import:

```js
import { buildHeatGradient } from '../utils/chart-config'
```

- [ ] **Step 4: Note on legendColors**

The `legendColors` computed (line 45) is still used by the legend template at line 13 (`:style="{ background: legendColors[idx] }"`). Keep it as-is — no change needed.

- [ ] **Step 5: Fix CSS overflow**

In the `<style scoped>` block, modify `.ocean-map-wrapper`:

```css
.ocean-map-wrapper {
  position: relative;
  width: 100%;
  overflow: hidden;
}
```

- [ ] **Step 6: Verify build**

Run: `cd ocean-web && npx vite build`

Expected: Build succeeds with no errors.

- [ ] **Step 7: Commit**

```bash
git add ocean-web/src/components/OceanMap.vue
git commit -m "feat: replace circleMarker grid with leaflet.heat heatmap layer"
```

---

### Task 4: Fix DashboardView.vue flex overflow

**Files:**
- Modify: `ocean-web/src/views/dashboard/DashboardView.vue`

- [ ] **Step 1: Add min-width to map flex container**

On line 36, change:

```html
<div style="flex: 2;">
```

To:

```html
<div style="flex: 2; min-width: 0;">
```

- [ ] **Step 2: Verify build**

Run: `cd ocean-web && npx vite build`

Expected: Build succeeds.

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/views/dashboard/DashboardView.vue
git commit -m "fix: prevent dashboard map overflow with min-width: 0 on flex container"
```

---

### Task 5: Integration smoke test

**Files:**
- Verify: all map-using pages build and render

- [ ] **Step 1: Run all tests**

Run: `cd ocean-web && npx vitest run`

Expected: All tests pass.

- [ ] **Step 2: Full build**

Run: `cd ocean-web && npx vite build`

Expected: Build succeeds with no warnings.

- [ ] **Step 3: Verify OceanMap is imported correctly in all consumers**

Run: `cd ocean-web && npx grep -r "OceanMap" src/ --files-with-matches`

Expected: `src/components/OceanMap.vue`, `src/views/dashboard/DashboardMap.vue`, `src/views/forecast/SstMapView.vue`, `src/views/forecast/ChxMapView.vue`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: integration smoke test — all tests pass, build succeeds"
```
