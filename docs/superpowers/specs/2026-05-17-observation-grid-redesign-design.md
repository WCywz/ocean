# Observation Grid Redesign Spec — 观测功能对齐预测功能

> 日期: 2026-05-17 | 状态: 已确认

## 一、目标

将观测功能改造为和预测功能完全一致的结构：三个子页面（叶绿素、温度、历史），温度和叶绿素用热力图+点击选点展示近七天曲线。

## 二、数据库

### 2.1 新建 observation_grid 表

和 forecast_grid 结构一致，去掉 model_id/version_id，forecast_date 改为 obs_date。

```sql
CREATE TABLE observation_grid (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    variable VARCHAR(32) NOT NULL COMMENT '变量名: thetao, chl, so',
    obs_date DATE NOT NULL COMMENT '观测日期',
    depth DOUBLE NOT NULL DEFAULT 0 COMMENT '深度(m)',
    lat DECIMAL(10,6) NOT NULL COMMENT '纬度',
    lon DECIMAL(10,6) NOT NULL COMMENT '经度',
    value DOUBLE NOT NULL COMMENT '观测值',
    unit VARCHAR(32) NOT NULL DEFAULT '' COMMENT '单位',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_variable_date (variable, obs_date),
    INDEX idx_lat_lon (lat, lon)
) COMMENT '观测网格数据';
```

## 三、后端

### 3.1 新增文件

| 文件 | 说明 |
|------|------|
| entity/ObservationGrid.java | 映射 observation_grid 表 |
| mapper/ObservationGridMapper.java | Mapper + 自定义查询（selectMapGrid, selectPointTrend, selectTrend） |
| service/ObservationGridService.java | 接口 |
| service/impl/ObservationGridServiceImpl.java | 实现 |

### 3.2 ObservationController 新增端点

| Method | Path | 说明 | 对应预报 |
|--------|------|------|---------|
| GET | /map/grid | 地图网格数据（取第一个点） | /forecast/map/grid |
| GET | /trend/point | 单点位历史趋势 | /forecast/trend/point |
| GET | /grid/locations | 网格去重经纬度 | /forecast/locations |

### 3.3 ObservationGridMapper 自定义查询

| 方法 | 说明 |
|------|------|
| selectMapGrid(variable, date, bbox) | 按变量/日期/边界框查询网格数据 |
| selectPointTrend(variable, lon, lat) | 单点位近7天趋势 |
| selectDistinctLocations() | 去重经纬度 |

## 四、前端

### 4.1 新增三个页面

| 文件 | 说明 | 参考 |
|------|------|------|
| views/observation/ObsSstView.vue | 温度观测：热力图+选点曲线 | SstMapView.vue |
| views/observation/ObsChlView.vue | 叶绿素观测：热力图+选点曲线 | ChxMapView.vue |
| views/observation/ObsHistoryView.vue | 历史观测记录：分页表格 | HistoryView.vue |

三个页面复用 OceanMap.vue 和 TrendChart.vue 组件。

### 4.2 路由

```
/app/observation/sst     → views/observation/ObsSstView.vue
/app/observation/chl     → views/observation/ObsChlView.vue
/app/observation/history → views/observation/ObsHistoryView.vue
```

### 4.3 导航栏

"观测"改为下拉菜单（和"预报"一致），包含三个子项：海表温度观测、叶绿素观测、历史观测记录。

### 4.4 API 文件

ocean-data.js 新增：
- getObsMapGrid(params) → GET /observation/map/grid
- getObsPointTrend(params) → GET /observation/trend/point
- getObsGridLocations() → GET /observation/grid/locations

### 4.5 移除

- 旧 OceanDataView.vue 及其路由 /app/ocean-data

## 五、测试数据

database/test-data-backend.sql 新增 observation_grid 数据，模拟7天 SST+CHL 网格数据。

## 六、和预报的差异点

| 项目 | 预报 | 观测 |
|------|------|------|
| 数据表 | forecast_grid | observation_grid |
| 日期字段 | forecast_date | obs_date |
| 是否有 model_id/version_id | 有 | 无 |
| CHL 概率模式 | 有（多日概率） | 无（观测是实际值） |
| 数据来源 | 模型运行产出 | 手动 SQL 导入 |
| API 前缀 | /api/forecast | /api/observation |
