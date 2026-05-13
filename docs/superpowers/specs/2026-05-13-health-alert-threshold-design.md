# Health Alert Threshold Section

Add a date-filtered alert threshold section to the Ocean Health page, reusing the existing `AlertPanel` component wrapped in a new health-styled `HealthAlertSection` component.

## Architecture

**New:** `ocean-web/src/views/health/HealthAlertSection.vue` — wrapper component that combines:

```
HealthAlertSection
├── Section header (editorial pattern)
├── Summary bar (SST/CHL alert counts)
├── AlertPanel (reused from dashboard)
└── Drill-down links (SST map / Chl map)
```

**Modified:** `ocean-web/src/views/health/OceanHealthView.vue` — add `<HealthAlertSection>` between the status bar and the health grid, plus `fetchAlerts(forecastDate)` call.

**Backend:** `/forecast/alerts` endpoint — add `@RequestParam forecastDate` parameter, pass through service to mapper, change SQL filter from `CURDATE()` to the parameter.

## Data Flow

1. Page load or date change triggers `fetchAlerts(forecastDate)` in parallel with existing health data fetch
2. API returns alerts array `[{ locationName, dataType, value, threshold }]`
3. `HealthAlertSection` receives `alerts`, `loading`, `forecastDate` as props
4. Summary computed from alerts: SST count and CHL count
5. Drill-down links use `router.push()` to navigate to `/app/forecast/sst` and `/app/forecast/chl`

## Placement

Between the status bar and the "区域健康评估 · 东海" section, full page width.

## Empty State

Show "所选日期无阈值告警" when no alerts for the selected date.

## Styling

Follow the health page editorial pattern:
- Section header: `.editorial-section-label` + `.editorial-section-heading`
- Summary bar: `.health-status-bar` style with accent border-left color (red/orange based on severity)
- Alert list: reuse AlertPanel's `alert-item` border-left pattern (already consistent)
- Drill-down links: text links with `→` arrow

No new CSS variables needed.

## Backend Changes

- **Controller:** `ForecastRecordController.getTodayAlerts()` → add `@RequestParam String forecastDate`
- **Service:** `ForecastRecordService.getTodayAlerts()` → add `String forecastDate` parameter
- **Mapper:** `ForecastRecordMapper.selectTodayAlerts()` → rename to `selectAlertsByDate()`, add `@Param("forecastDate") String forecastDate`, update SQL

## Error Handling

API failure: log error, alerts stays empty, component renders empty state. Consistent with existing `fetchData()` error handling.

## Future

The date parameter may be removed later to always show latest alerts. The prop-based design makes this a one-line call-site change.
