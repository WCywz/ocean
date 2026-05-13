# Health Alert Map Enhancement

Enhance the HealthAlertSection with a split layout: compact alert cards on the left, a small interactive map with pulsed markers on the right. Clicking a card locates the alert on the map.

## Architecture

**Modified:** `ocean-web/src/views/health/HealthAlertSection.vue` — replace the full-width list layout with a left-right split: card list + Leaflet mini-map.

**Modified:** `ocean-server/src/main/java/com/ocean/mapper/ForecastRecordMapper.java` — add `longitude`, `latitude` columns to `selectAlertsByDate` query.

## Layout

```
┌──────────────────────────────────────────────────┐
│ Alerts · 阈值告警                                  │
├──────────────────────┬───────────────────────────┤
│ Summary bar (full w) │                           │
├──────────────────────┤                           │
│ Card 1 ── SST ── red │     Leaflet Mini Map      │
│ Card 2 ── CHL ── org │     (东海, zoom 7)        │
│ Card 3 ── SST ── red │                           │
│ ...                  │   ● pulsed marker          │
│                      │                           │
│ Drill-down links     │                           │
└──────────────────────┴───────────────────────────┘
```

- Left column: ~40% width, compact cards + summary + drill-down links
- Right column: ~60% width, Leaflet map

## Map Behavior

- **Controls:** zoom disabled (`scrollWheelZoom: false, doubleClickZoom: false, zoomControl: false`), drag enabled (`dragging: true`)
- **Center:** `[29.5, 122.5]`, zoom `7` (东海 area)
- **Height:** matches the card list height (~400px or auto)
- **Click card → map:** `map.flyTo([lon, lat], map.getZoom())` to center on the alert location
- **Pulsed marker:** `divIcon` with CSS pulse animation, color matches card border (SST `#c0392b`, Chl `#e67e22`)
- **Pulse size:** `::after` pseudo-element ~3rem radius (smaller than HeroV2View's 10rem)
- **Same location both types:** two markers offset slightly (SST +0.02° lat, CHL -0.02° lat)

## Card Behavior

- Compact design: slimmer padding, shorter height
- Click to select → highlight background + fly map to location
- Selected card gets a subtle background tint
- Click same card again → deselect (map stays at last position)

## Backend Change

Mapper `selectAlertsByDate` query: add `fr.longitude, fr.latitude` to SELECT columns.

## Styling

- Follow existing editorial pattern (`.editorial-section`, `.editorial-section-label`, `.editorial-section-heading`)
- Pulse animation: CSS `@keyframes`, adapted from HeroV2View with smaller scale
- Cards: border-left color coding, compact padding, selected state background
