# Backend Rewrite Spec — 基于新数据库重写后端

> 日期: 2026-05-15 | 数据库: redesign-observation.sql (12 新表已建)

## 一、目标

基于新设计的 12 张数据库表，重写 Spring Boot 后端。要求：
1. **解耦**：每个领域独立 Controller + Service + Mapper，不再把多个领域塞进一个 Controller
2. **功能完整**：覆盖前端所有需要的 API（Dashboard、地图、趋势、告警、健康评估、模型管理、站点管理）
3. **可运行**：插入测试数据，启动服务后前端能正常展示

## 二、新数据库表（已完成）

| 表名 | 用途 | 数据量估算 |
|------|------|-----------|
| observation_data | 观测数据（chl/so/thetao），按月分区 | ~2年热数据 |
| observation_grid_cache | 格点坐标缓存 | ~400行 |
| observation_archive | .nc归档文件索引 | 少量 |
| forecast_grid | 预报网格数据 | 391格点×2变量×365天≈28.5万/年 |
| monitoring_station | 监测站点 | ~20个 |
| model | 模型定义 | ~5个 |
| model_version | 模型版本 | ~10个 |
| alert_rule | 告警规则 | 2条初始 |
| alert_event | 告警事件（区域级） | 按规则+区域产生 |
| alert_station_detail | 告警站点明细 | 按事件产生 |
| health_zone | 健康评估区域 | 4个初始 |
| health_record | 健康评估记录（可留存历史） | 4条/天 |

## 三、API 设计（前端视角 → 后端实现）

### 3.1 预报数据 API → ForecastController (`/api/forecast`)

| Method | Path | 用途 | 对应前端调用 |
|--------|------|------|------------|
| GET | /dashboard | Dashboard 统计数据 | getDashboard() |
| GET | /page | 分页预报数据 | getRecordPage() |
| GET | /map/grid | 地图网格数据 | getMapGrid() |
| GET | /trend/point | 单点位历史趋势 | getPointTrend() |
| GET | /trend/dashboard | Dashboard 趋势（top 5 sparkline） | getDashboardTrend() |
| GET | /sst/trend | SST 趋势 | getSstTrend() |
| GET | /chl/trend | CHL 趋势 | getChlTrend() |
| GET | /chl/probability | CHL 超阈值概率 | 新功能 |
| GET | /sea-areas | 预设海域 | getSeaAreas() |
| GET | /locations | 所有去重经纬度 | getLocations() |

### 3.2 观测数据 API → ObservationController (`/api/observation`)

| Method | Path | 用途 | 对应前端调用 |
|--------|------|------|------------|
| GET | /page | 分页查询 | getOceanDataPage() |
| GET | /locations | 去重经纬度 | getOceanLocations() |
| GET | /sst-timeseries | SST 时间序列 | getSstTimeSeries() |
| GET | /chl-timeseries | CHL 时间序列 | getChlTimeSeries() |
| GET | /chl-by-depth | CHL 按深度分布 | 新功能 |

### 3.3 模型管理 API → ModelController (`/api/model`) + ModelVersionController (`/api/model/{modelId}/version`)

模型和版本严格分离为两个 Controller。

**ModelController:**
| Method | Path | 用途 |
|--------|------|------|
| GET | /page | 分页查询模型 |
| GET | /{id} | 模型详情 |
| POST | / | 新增模型 |
| PUT | /{id} | 修改模型 |
| DELETE | /{id} | 删除模型及所有版本 |
| GET | /running-versions | 运行中版本概览 |

**ModelVersionController:**
| Method | Path | 用途 |
|--------|------|------|
| GET | /{modelId}/versions | 模型的版本列表 |
| POST | /{modelId}/version | 新增版本 |
| PUT | /{modelId}/version/{versionId} | 修改版本 |
| DELETE | /{modelId}/version/{versionId} | 删除版本 |
| PUT | /{modelId}/version/{versionId}/status | 启停版本 |

### 3.4 告警 API → AlertController (`/api/alert`)

| Method | Path | 用途 |
|--------|------|------|
| GET | /rules | 告警规则列表 |
| POST | /rules | 新增告警规则 |
| PUT | /rules/{id} | 修改规则 |
| GET | /events | 告警事件（按日期/区域筛选） |
| GET | /events/{id} | 事件详情 |
| GET | /events/{id}/stations | 事件下超标站点 |
| PUT | /events/{id}/acknowledge | 确认告警 |
| POST | /events/generate | 触发告警生成 |

### 3.5 健康评估 API → HealthController (`/api/health`)

| Method | Path | 用途 |
|--------|------|------|
| GET | /zones | 所有评估区域 |
| GET | /assessment | 按日期查询评估结果 |
| GET | /assessment/{zoneId}/trend | 单区域趋势 |
| GET | /dashboard | Dashboard 健康概览 |

### 3.6 站点 API → StationController (`/api/station`)

| Method | Path | 用途 |
|--------|------|------|
| GET | /page | 分页查询站点 |
| GET | /{id} | 站点详情 |
| GET | /by-zone/{zoneId} | 按健康区域查站点 |
| POST | / | 新增站点 |
| PUT | /{id} | 修改站点 |
| DELETE | /{id} | 删除站点 |

### 3.7 用户 API → UserController (`/api/user`) — 保持现有不变

## 四、前端 API 文件调整

| 文件 | 变化 |
|------|------|
| ocean-web/src/api/forecast.js | /forecast/alerts → /alert/events；/forecast/zone-health → /health/assessment |
| ocean-web/src/api/ocean-data.js | /ocean-data/* → /observation/* |
| ocean-web/src/api/health.js | /forecast/zone-health → /health/assessment |
| ocean-web/src/api/model.js | 基本不变，新增 /version 子路径 |

## 五、解耦原则

- 每个 Controller 最多处理一个领域
- Service 层负责业务逻辑，Controller 只做参数校验和路由
- Mapper 层负责纯 SQL，不混业务判断
- 告警生成、健康评估等定时任务放在 task 包
- 公共配置（海域、阈值等）统一由 config 或对应 Service 管理

## 六、测试数据

需要插入以下测试数据以确保流程跑通：
1. `forecast_grid`: 391个格点 × 2变量 × 7天 ≈ 5474行 SST+CHL 预报数据
2. `monitoring_station`: 6个示例站点
3. `model` + `model_version`: 2个模型，各1-2个版本
4. `health_record`: 当天4个区域的评估记录
5. `alert_event`: 至少1条示例告警
6. `observation_data`: 若干观测数据点
