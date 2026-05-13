# Model Management Redesign — Design Spec

**Date:** 2026-05-13
**Status:** Draft
**Scope:** Frontend redesign of model management page (ModelView.vue)

## Problem

The current model management is a flat CRUD list with no concept of versions. In reality, a model can have multiple versions (v1, v2, v3) each with different training data and algorithms. Additionally, the current UI lacks structured description fields for both the model purpose and version-specific details.

## Data Model

Two-layer hierarchy:

```
Model (parent)
├── name, type (SST/CHL/custom), description
├── createdAt
│
└── Version (child)
    ├── versionLabel (v1, v2, ...)
    ├── cronExpression
    ├── paramsConfig (JSON)
    ├── dataSource (training data origin)
    ├── dataTimeRange (data temporal coverage)
    ├── changeNote (what changed from previous version)
    ├── status (RUNNING/STOPPED/ERROR)
    └── lastRunTime
```

## Page Layout

Single-page drill-down. Editorial style (native tables, custom CSS).

```
┌──────────────────────────────────────────────┐
│  Page Title + Subtitle                        │
│  Status Bar  (all-good / attention)           │
├──────────────────────────────────────────────┤
│  Running Overview                            │
│  SST东海 v3 RUNNING [Stop]  CHL v1 RUNNING   │
├──────────────────────────────────────────────┤
│  Filter Bar (type select + keyword + actions) │
├──────────────────────────────────────────────┤
│  Model List                                  │
│  ▾ SST东海  SST  3 versions  部分运行         │
│    model description text...                  │
│    ┌─────────────────────────────────────┐   │
│    │ v3  06:00  RUNNING  dataSource ...   │   │
│    │ v2  06:00  STOPPED  dataSource ...   │   │
│    │ v1  06:00  STOPPED  dataSource ...   │   │
│    └─────────────────────────────────────┘   │
│  ▸ CHL     CHL  2 versions  已停止            │
│    model description text...                  │
├──────────────────────────────────────────────┤
│  Pagination                                  │
└──────────────────────────────────────────────┘
```

## Component Tree

- ModelView.vue (main page)
  - PageStatusBar (status indicator)
  - RunningOverview (list of running versions, quick stop)
  - ModelFilterBar (type select with custom option, keyword, search/reset, + new model)
  - ModelTable (expandable model list)
    - ModelRow (model info, description, expand/collapse toggle)
      - VersionRow (version info, inline start/stop, edit, delete)
  - Pagination
  - ModelDialog (create/edit model shell)
  - VersionDialog (create/edit version)

## Dialogs

### Model Dialog (create/edit model shell)

| Field | Type | Notes |
|-------|------|-------|
| modelName | text input | Required |
| modelType | select + custom input | Predefined: SST, CHL. "Custom" option reveals a text input |
| description | textarea | Model purpose, use cases, methodology |

### Version Dialog (create/edit version)

| Field | Type | Notes |
|-------|------|-------|
| versionLabel | auto-generated | v1, v2, ... based on next version number |
| cronExpression | text input | e.g., `0 0 6 * * ?` |
| paramsConfig | textarea | JSON |
| dataSource | text input | e.g., "2025-2026 NOAA OISST" |
| dataTimeRange | text input | e.g., "2025-01 ~ 2026-04" |
| changeNote | textarea | What changed from previous version |

## Interaction Rules

### Expand/Collapse
- Click model row to toggle version list visibility
- Only one model expanded at a time (accordion behavior)

### Start/Stop Logic
- **Model level — Stop**: stops ALL versions of the model
- **Model level — Start**: starts the latest version (by version number), leaves older versions untouched
- **Version level**: independent start/stop for each version
- **Running Overview**: each running version has a quick stop button

### Create Flow
1. User creates a model shell (name + type + description)
2. User expands the model and clicks "+ Version" to add versions

### Delete
- Model-level delete: delete model and all its versions (with confirmation)
- Version-level delete: delete single version (prevent if it's the only running version of the model? TBD)

## API Implications

New endpoints needed (exact design TBD during implementation):
- `GET /api/model/versions?modelId=` — list versions for a model
- `POST /api/model/{id}/version` — add version
- `PUT /api/model/{id}/version/{versionId}` — update version
- `DELETE /api/model/{id}/version/{versionId}` — delete version
- `PUT /api/model/{id}/version/{versionId}/status` — toggle version status

Additionally, the model type enum may need to support custom string values instead of just SST/CHL.

## Non-goals

- Backend database schema changes (deferred, frontend first)
- Actual cron job execution logic
- Batch operations on multiple models/versions
- Version comparison/diff view
