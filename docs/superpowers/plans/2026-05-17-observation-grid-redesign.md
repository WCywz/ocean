# Observation Grid Redesign Implementation Plan

> **Date:** 2026-05-17

## Task 0: Database

- [ ] Create `observation_grid` table (DDL)
- [ ] Insert test data (7 days SST + CHL grid data)
- [ ] Update `database/test-data-backend.sql`

## Task 1: Backend — Entity & Mapper

- [ ] Create `entity/ObservationGrid.java`
- [ ] Create `mapper/ObservationGridMapper.java` (with selectMapGrid, selectPointTrend, selectDistinctLocations)

## Task 2: Backend — Service

- [ ] Create `service/ObservationGridService.java` (interface)
- [ ] Create `service/impl/ObservationGridServiceImpl.java`

## Task 3: Backend — Controller

- [ ] Update `controller/ObservationController.java` — add `/map/grid`, `/trend/point`, `/grid/locations`

## Task 4: Frontend — API

- [ ] Update `api/ocean-data.js` — add `getObsMapGrid`, `getObsPointTrend`, `getObsGridLocations`

## Task 5: Frontend — Views

- [ ] Create `views/observation/ObsSstView.vue` (copy SstMapView, adapt to observation API)
- [ ] Create `views/observation/ObsChlView.vue` (copy ChxMapView, remove probability mode, adapt to observation API)
- [ ] Create `views/observation/ObsHistoryView.vue` (copy HistoryView, adapt to observation API)

## Task 6: Frontend — Router & Navigation

- [ ] Update `router/index.js` — replace `/ocean-data` with three observation sub-routes
- [ ] Update `layout/MainLayout.vue` — change "观测" to dropdown

## Task 7: Build & Verify

- [ ] `mvn compile` — Java compiles
- [ ] `npx vite build` — frontend builds
