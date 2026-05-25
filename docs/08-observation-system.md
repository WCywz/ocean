# 观测系统

## 背景

旧的观测数据页面是单一的 `OceanDataView.vue`，将 SST 时序、CHL 时序、深度剖面和数据表全部堆在一个页面中，观察数据增长后难以使用。

## 设计决策

### 与预报系统对齐
观测系统改造为与预报系统完全一致的结构——用户学会一种交互模式即可同时用于预报和观测：
- 三个子页面：温度观测、叶绿素观测、历史数据
- 温度/叶绿素页面使用热力图 + 点击选点展示近 7 天曲线
- 历史页面使用分页表格

### 功能对比

| 功能 | 预报系统 | 观测系统 |
|------|----------|----------|
| SST 地图 | `SstMapView.vue` | `ObsSstView.vue` |
| CHL 地图 | `ChxMapView.vue` | `ObsChlView.vue` |
| 历史数据 | `HistoryView.vue` | `ObsHistoryView.vue` |
| 热力图 | 支持 | 支持 |
| 选点曲线 | 支持 | 支持 |
| CHL 概率模式 | 有（预测概率） | 无（观测是实测值） |

### 导航结构
"观测"变为下拉菜单（与"预报"一致），包含三个子路由：
- `/app/observation/sst` — 温度观测
- `/app/observation/chl` — 叶绿素观测
- `/app/observation/history` — 历史数据

### 数据摄入
两条并行的摄入管道：
- **插值网格管道：** `ingest_daily.py` 读取 `ocean_clean_post_2025.csv` → `observation_grid` 表
- **原始数据管道：** `ingest_raw_daily.py` 读取原始 CSV（`ocean_raw_temp.csv`、`ocean_raw_chl.csv`、`ocean_raw_so.csv`）→ `observation_data` 表

## API

- `GET /api/observation/map/grid` — 观测网格数据（热力图）
- `GET /api/observation/trend/point` — 观测点趋势（近 7 天曲线）
- `GET /api/observation/grid/locations` — 格点位置列表

## 关键文件

- `ocean-web/src/views/observation/ObsSstView.vue`
- `ocean-web/src/views/observation/ObsChlView.vue`
- `ocean-web/src/views/observation/ObsHistoryView.vue`
- `ocean-server/.../controller/ObservationController.java`
- `ocean-server/.../entity/ObservationGrid.java`
- `scripts/ingest_daily.py` — 插值网格日常摄入
- `scripts/ingest_raw_daily.py` — 原始数据日常摄入
