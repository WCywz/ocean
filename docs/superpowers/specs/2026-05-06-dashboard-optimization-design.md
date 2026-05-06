# Dashboard Optimization — Design Document

**Date:** 2026-05-06
**Status:** Approved
**Scope:** Frontend dashboard redesign + backend API additions

## Goal

Transform the current bare dashboard (3 stat cards + 2 data tables) into a rich, card-grid dashboard that serves both system administrators and ocean researchers with data insights, trend sparklines, a compact map, and threshold alerts.

## Layout

Card grid with collapsible sections, 4-row structure:

```
Row 1: Stat Cards (4 across) — Models | Running | Today's Records | Alerts
Row 2: Trend Charts (2 columns) — SST sparkline | CHL sparkline
Row 3: Map + Alerts (2 columns) — Compact map | Threshold violations
Row 4: Data Tables (2 columns) — Latest SST table | Latest CHL table (existing)
```

Each card has a header with title, optional filter chips, and a fullscreen expand button. Cards are independent — a user can expand the map card without affecting the trend charts.

## Component Architecture

### DashboardView.vue (orchestrator)
Fetches all dashboard data in parallel on mount. Passes props to child components. Handles loading/error states at the page level with skeleton placeholders per card.

### New Components

**StatCards.vue**
- Props: `modelCount`, `runningModelCount`, `todayRecordCount`, `alertCount`
- Displays 4 stat cards in a flex row with icon, value, and label
- Alert count card uses red accent color and shows `0` in green when no alerts

**TrendCard.vue** (reusable, used twice)
- Props: `dataType` (SST/CHL), `series` (array of {locationName, dataPoints}), `loading`
- Renders ECharts multi-line sparkline at ~280px height
- Uses `chart-config.js` color palettes (warm for SST, teal for CHL)
- Top-right time-range chip: "Last 7 days" (default)
- Shows legend inline with few series; suppresses if 1 series
- Empty state: "暂无趋势数据" placeholder text

**DashboardMap.vue**
- Props: `gridData`, `colorMap`, `legendLabels`, `activeType` (SST/CHL), `loading`
- Wraps `OceanMap.vue` at 300px height with toggle between SST/CHL modes
- Default bbox: full sea area extent (121.33°E–125.58°E, 26.92°N–32.67°N)
- Click on grid cell navigates to full forecast map page (`/app/forecast/sst` or `/chl`)
- SST/CHL toggle in card header

**AlertPanel.vue**
- Props: `alerts` (array of {locationName, dataType, value, forecastDate, threshold})
- Lists threshold violations sorted by severity (SST > 30 as critical-red, others as warning)
- Each row: location name, data type badge, value, threshold, timestamp
- Empty state: "今日无阈值告警" with green checkmark icon
- Max 10 visible; "查看全部" link to expand

**LatestDataTable.vue**
- Props: `data` (array), `dataType` (SST/CHL), `loading`
- Preserves existing table logic from current DashboardView
- Max 10 rows; links to full history page for more

### Files Changed

| Action | File |
|--------|------|
| New | `ocean-web/src/views/dashboard/StatCards.vue` |
| New | `ocean-web/src/views/dashboard/TrendCard.vue` |
| New | `ocean-web/src/views/dashboard/DashboardMap.vue` |
| New | `ocean-web/src/views/dashboard/AlertPanel.vue` |
| New | `ocean-web/src/views/dashboard/LatestDataTable.vue` |
| Modify | `ocean-web/src/views/dashboard/DashboardView.vue` |
| Modify | `ocean-web/src/api/forecast.js` |
| Modify | `ocean-server/.../vo/DashboardVO.java` |
| Modify | `ocean-server/.../controller/ForecastRecordController.java` |
| Modify | `ocean-server/.../service/ForecastRecordService.java` |
| Modify | `ocean-server/.../service/impl/ForecastRecordServiceImpl.java` |
| Modify | `ocean-server/.../mapper/ForecastRecordMapper.java` |

### Files NOT Changed
- Router, layout, store
- `OceanMap.vue`, `TrendChart.vue` (reused as-is or wrapped)
- `chart-config.js` (color palettes reused)
- `database/init.sql`
- Existing map/trend endpoints

## Backend API

### Modified: GET /forecast/dashboard
Adds `alertCount` field to `DashboardVO`:
```java
private Long alertCount;
```
Queried via:
```sql
SELECT COUNT(*) FROM forecast_record
WHERE forecast_date = CURDATE()
  AND ((data_type = 'SST' AND value > 28) OR (data_type = 'CHL' AND value > 5))
```

### New: GET /forecast/trend/dashboard
Dashboard trend data for sparkline charts.

| Param | Type | Default | Desc |
|-------|------|---------|------|
| dataType | String | SST | SST / CHL |
| days | Integer | 7 | Number of days to look back |

Returns: `[{locationName, dataPoints: [{date, value}]}]`

SQL: finds top 5 locations by latest-value count for the given data type, then queries their daily averages over the date range.

### New: GET /forecast/alerts
Threshold violation records for today.

| Param | Type | Default | Desc |
|-------|------|---------|------|
| dataType | String | (both) | Optional filter: SST / CHL |

Returns: `[{locationName, dataType, value, forecastDate, threshold}]`

SQL:
```sql
SELECT location_name, data_type, value, forecast_date, forecast_time,
       CASE WHEN data_type = 'SST' THEN 28 ELSE 5 END AS threshold
FROM forecast_record
WHERE forecast_date = CURDATE()
  AND ((data_type = 'SST' AND value > 28) OR (data_type = 'CHL' AND value > 5))
ORDER BY value DESC
LIMIT 20
```

Thresholds are hardcoded (SST > 28°C, CHL > 5 mg/m³), matching existing map color config breakpoints.

## Reuse Strategy

- `TrendCard.vue` imports colors from `chart-config.js` (SST_COLORS, CHL_COLORS) — no new color definitions
- `DashboardMap.vue` wraps `OceanMap.vue` — passes grid data + color map as props, listens to existing events
- Map grid data endpoint (`GET /forecast/map/grid`) reused unchanged
- Redis cache on `/forecast/dashboard` is updated with `alertCount` field

## Loading & Error States

- Page-level skeleton: each card shows an animated placeholder while data loads
- Individual cards handle partial failures — if alerts endpoint fails, alert card shows "加载失败" with retry button, but other cards display normally
- All dashboard data requests fire in parallel (`Promise.all`)
- Empty states: 0 alerts = green checkmark, 0 trend data = placeholder text, 0 table rows = "暂无数据"

## Edge Cases

| Case | Behavior |
|------|----------|
| No alerts today | AlertPanel shows green "今日无阈值告警" empty state |
| Alert count = 0 | Stat card shows green `0` value, not red |
| Under 5 locations | TrendCard shows whatever is available, minimum 1 |
| Single series | TrendCard hides legend (no toggle needed) |
| Map grid API error | DashboardMap card shows error state with retry |
| Redis cache stale | alertCount included in same cache key, cache TTL unchanged |
