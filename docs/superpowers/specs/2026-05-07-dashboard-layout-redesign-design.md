# Dashboard Layout Redesign — Design Document

**Date:** 2026-05-07
**Status:** Approved
**Scope:** Frontend dashboard layout restructure, map removal, module navigation

## Goal

Remove the map from the dashboard, restructure modules into a three-row full-width layout with strong visual separation (2px dividers), and make modules clickable to navigate to their corresponding full pages.

## Layout

Three rows stacked vertically, each separated by `2px solid #e0e0e0` divider:

```
┌──────────────────────────────────────────────┐
│ Page Title + Subtitle                         │
├──────────────────────────────────────────────┤
│ ROW 1: StatCards (4 across)    → /app/model  │
├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
│ ROW 2: Trend Cards (2 columns)               │
│   SST trend → /app/forecast/sst              │
│   CHL trend → /app/forecast/chl              │
├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
│ ROW 3: Alerts (L 1/3) + Data Tables (R 2/3)  │
│   Alerts: no navigation                       │
│   Tables: SST + CHL stacked → /app/ocean-data │
└──────────────────────────────────────────────┘
```

## Navigation Mapping

| Module | Target Route | 
|--------|-------------|
| StatCards (entire row) | `/app/model` |
| SST TrendCard | `/app/forecast/sst` |
| CHL TrendCard | `/app/forecast/chl` |
| AlertPanel | _(none for now)_ |
| LatestDataTable (both) | `/app/ocean-data` |

Each clickable area shows a subtle `→` hint on the right side and uses `cursor: pointer`.

## Component Changes

### StatCards.vue
- Emit `navigate` event on row click
- Add `cursor: pointer` to container
- Add `→ 模型管理` hint text on the right
- Change divider from 1px `#f0f0f0` to 2px `#e0e0e0`

### TrendCard.vue
- Emit `navigate` event on card click
- Add `cursor: pointer` to card wrapper
- Add navigation hint text in header area (e.g., `SST 预测 →`)

### AlertPanel.vue
- No navigation changes

### LatestDataTable.vue
- Emit `navigate` event on card click  
- Add `cursor: pointer` to container
- Add navigation hint text (e.g., `观测数据 →`)

### DashboardView.vue
- Remove DashboardMap import and usage
- Remove all map-related state: `mapGridData`, `mapType`, `mapColorRanges`, `mapLegendLabels`, `mapLegendTitle`
- Remove `loading.map`
- Remove `fetchMapData()`, `onMapTypeChange()`, `onMapCellClick()`
- Add `useRouter` import and route navigation handlers for each row/module
- Restructure template: three rows with 2px dividers, Row 3 with 1:2 column ratio
- Remove `map` loading state from `onMounted`

### DashboardMap.vue
- **Delete entire file**

## Files Changed

| Action | File |
|--------|------|
| Modify | `ocean-web/src/views/dashboard/DashboardView.vue` |
| Modify | `ocean-web/src/views/dashboard/StatCards.vue` |
| Modify | `ocean-web/src/views/dashboard/TrendCard.vue` |
| Modify | `ocean-web/src/views/dashboard/AlertPanel.vue` |
| Modify | `ocean-web/src/views/dashboard/LatestDataTable.vue` |
| Delete | `ocean-web/src/views/dashboard/DashboardMap.vue` |

## Files NOT Changed
- Backend (no API changes needed)
- Router
- Layout (MainLayout.vue)
- Store
- `OceanMap.vue` (still used by forecast pages)
- `chart-config.js`
- `editorial.css`

## Visual Separation

Replace the current mix of `1px solid var(--color-divider)` (#f0f0f0) and inline `gap` spacing with a consistent `2px solid #e0e0e0` divider between each of the three rows. Each row has `padding-bottom` and `margin-bottom` for proper spacing around the divider.

## Edge Cases

| Case | Behavior |
|------|----------|
| No alerts | AlertPanel shows "今日无阈值告警" empty state, row still renders |
| No trend data | TrendCard shows "暂无趋势数据" placeholder |
| No table data | LatestDataTable shows "暂无数据" row |
| Non-admin user | StatCards row still clickable but router guard redirects to dashboard |
