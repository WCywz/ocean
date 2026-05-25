# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

海洋环境预报系统 — a full-stack ocean environment monitoring and forecasting platform for the East China Sea. Three subsystems: Vue 3 frontend, Spring Boot backend, PyTorch ML models.

## Commands

**Frontend** (`ocean-web/`):
```bash
npm run dev          # Dev server on :3000, proxies /api → :8080
npm run build        # Production build → dist/
```

**Backend** (`ocean-server/`):
```bash
mvn clean package -DskipTests   # Build JAR
mvn spring-boot:run             # Run (or run OceanApplication.java)
```

**No Docker, no CI.** Deploy is manual: build JAR + frontend, package into tarball, scp to Aliyun ECS, run update script.

## Architecture

```
ocean-web/          Vue 3 + Vite + Element Plus + Leaflet + ECharts + GSAP
ocean-server/       Java 21, Spring Boot 3.4.1, MyBatis-Plus 3.5.7, MySQL, Redis
ocean-model/        Python + PyTorch (forecast models: SST, CHL, salinity)
scripts/            Python data ingestion (reads CSVs → MySQL)
```

**Backend layer**: Controller → Service → ServiceImpl → Mapper (MyBatis-Plus `BaseMapper<T>`, no XML except SysUserMapper). 7 domain controllers: Forecast, Observation, Model, ModelVersion, Alert, Health, Station, plus User and SystemConfig.

**Frontend structure**: `views/<domain>/` pages, `components/` shared (OceanMap.vue, TrendChart.vue), `api/` Axios wrappers, `utils/` (health-assessment.js, chart-config.js, land-mask.js), `store/` Pinia, `router/` Vue Router, `styles/` editorial.css + landing.css.

**Key data tables**: `observation_data` (~15M rows, monthly partitions), `observation_grid` (~280K), `forecast_grid` (~7K), `model` + `model_version` for model registry, `alert_rule` + `alert_event` for alerting, `health_zone` + `health_record` for zone assessments, `monitoring_station` for station metadata.

## Design conventions

- **Editorial design system**: Serif titles (Georgia), monochromatic gray palette, red (`#c0392b`) reserved exclusively for alerts, no shadows/rounded corners. All pages import `editorial.css` globally via `App.vue`.
- **Top navigation bar** (not sidebar). "Forecast" and "Observation" are dropdown menus with SST/CHL/History sub-pages.
- **OceanMap.vue** renders grid data as a custom Canvas bilinear-interpolated heatmap (not `L.heatLayer`). Shared by forecast and observation SST/CHL pages. Clicking a grid point loads its trend chart.
- **Health assessment** thresholds are client-side in `health-assessment.js` (not DB config). 4-level grading: 优/良/中/差, worst-indicator-wins composite.
- **Model management**: Two-level hierarchy (model → versions). VersionDialog captures data source and change notes for scientific reproducibility.

## Environment notes

- Local MySQL: `root/your_password`, `ocean_forecast` database
- Production MySQL: `ocean_forecast/your_password` at your_server_ip
- `application.yml` is gitignored (contains secrets). Template at `ocean-server/src/main/resources/application.yml.example`
- `deploy/` is gitignored — contains production JAR, `.pt` model weights, frontend dist
- Root `*.csv` files are gitignored — used by ingest scripts, do not commit

## Workflow

- **Destructive operations** (file deletion, directory restructuring, database changes): list affected files first, wait for confirmation before executing. Do not mix analysis and execution in the same round.
- **Autopilot mode:** When the user says "继续执行" / "keep going" / "不用等我" / "不用确认", continue executing the agreed-upon plan without asking for confirmations. The user may also pre-authorize at the start: "接下来 2 小时我不在，你继续按方案执行，不用确认". Autopilot does NOT override the framework's hard-gated confirmations (rm -rf outside project, ssh, etc.) — those will still pause, but the session will resume when possible. In autopilot mode, skill-level checkpoints (e.g., executing-plans pausing between steps, writing-plans waiting for approval) are also skipped — self-review each step, fix minor issues silently, log major issues for later and continue. For blocking issues (e.g., build failure): retry 2-3 times with diagnosis between attempts, if still failing skip dependent steps and continue with independent work, revisit later, and if still unfixable leave it for the user.
