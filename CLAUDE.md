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

- Local MySQL at `localhost:3306`, database `ocean_forecast`
- Production MySQL: see `deploy/` directory (gitignored) for connection details
- `application.yml` is gitignored (contains secrets). Template at `ocean-server/src/main/resources/application.yml.example`
- `deploy/` is gitignored — contains production JAR, `.pt` model weights, frontend dist
- Root `*.csv` files are gitignored — used by ingest scripts, do not commit
- Scripts require `DB_PASSWORD` environment variable. See `.env.example` for all required env vars.

## Workflow

**全局优先级：临时变更需求 > 本文规则**
- 用户在当前会话中明确给出的临时指令，优先级高于本文所有规则
- 例如：临时要求直接改 master、临时允许某次 push、临时放宽某操作权限

**危险操作定义（需要确认）：**
- 删除文件/目录（`rm`、`git rm`、递归删除）
- 数据库结构变更（DDL：ALTER、DROP、TRUNCATE）和大范围数据修改（无 WHERE 的 UPDATE/DELETE）
- 破坏性 git 操作（`force push`、`hard reset`、`checkout -- .`）
- 除以上外，创建文件、编辑代码、运行测试、构建等操作直接放行

**自主工作模式（"继续执行" / "不用等我"）：**

触发条件：用户明确表示"继续执行"/"不用等我"/"不用确认"等
流程中入到的非危险git直接放行，包括commit，回退版本等等，无需向我过问
触发后流程：
- → 先大 commit（`快照：开始 所有XXX`）
- → 按方案推进所有任务
- → 遇到危险操作需要确认时，等待 3 分钟无响应 → 触发安全快照：
    - 先小 commit 当前状态作为快照
    - 自动放宽操作权限继续执行
    - 操作完成后验证 2-3 轮
    - 发现严重结构错误或数据删除/缺失：
      → 立即 git revert，小 commit 和 revert commit 保留在历史中（记录失败过程）
      → 记录文档（`docs/rollback/YYYY-MM-DD-<简述>.md`），写明：
        - 执行了什么操作
        - 造成了什么影响（文件丢失、数据变化、结构破坏）
        - 涉及哪些模块/文件
      → 跳过该任务，先推进后续任务
    - 没有发现严重错误 → git reset --soft 撤销小 commit（不留下记录），改动保留继续
- → 继续推进任务，遇到危险操作同上
- → 全部完成后大 commit（`完成：所有XXX`）
- → 提供 git diff 和改动摘要，用户回来后决定 push、回退、或丢弃
- 遇到构建失败：重试 2-3 次并诊断，仍失败则跳过，最终未解决留给用户处理

**Git 规范：**
- master 禁止直接开发和 commit，仅通过 feature 分支 merge 引入变更
- 所有开发在 `feature/<功能名>` 分支进行，从 master 拉出
- `git merge --squash` 合并到 master，所有变更压成一个干净 commit，不带入中间 commit
- Commit 用中文，格式：`feat:` / `fix:` / `chore:` / `docs:` / `refactor:` + 简述
- 不自动 push——由用户验收后自己推
