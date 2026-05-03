# 预报数据地图可视化 — 设计文档

## 概述

将预报数据可视化模块重构为三个子模块，新增交互式地图视图，支持 SST 海表温度和 Chl 叶绿素的动态空间分布分析。

## 导航结构

侧边栏「预报数据可视化」展开为三个子菜单：

- **海表温度预测** (`/app/forecast/sst`) — 地图 + 趋势折线图
- **叶绿素预测** (`/app/forecast/chx`) — 地图 + 趋势折线图
- **历史预报记录** (`/app/forecast/history`) — 分页表格查询（复用现有逻辑）

## 数据背景

实际数据特点（非示例数据）：
- 经度范围：121.33 ~ 125.58
- 纬度范围：26.92 ~ 32.67
- 有效经纬度点数：约 2100 万
- 无观测点名称，lon+lat 作为唯一标识

## 地图瓦片

- 使用 Leaflet + OpenStreetMap 免费瓦片
- 无 API Key 依赖，无调用次数限制

## 数据策略：服务端聚合 + LOD

所有 SQL 查询遵循「先 WHERE 过滤，后 GROUP BY 聚合」原则。

### 多级精度

| 级别 | 精度 | 网格数 | 触发条件 |
|------|------|--------|----------|
| Level 1（默认） | 0.05° | ~9,800 | 初始加载 / 缩小时 |
| Level 2（放大） | 0.01° | 按视口裁剪 | 用户放大时 |

### 数据库索引

```sql
CREATE INDEX idx_filter ON forecast_record (data_type, forecast_date, longitude, latitude);
CREATE INDEX idx_point_trend ON forecast_record (data_type, longitude, latitude, forecast_date);
```

## 前端架构

### 依赖变更

- 新增：`leaflet`、`leaflet-draw`
- 保留：`echarts`、`element-plus`、`chart-config.js`

### 组件树

```
MainLayout.vue（侧边栏子菜单展开）
├── SstMapView.vue
│   ├── FilterBar（日期 + 海域下拉 + 查询/重置）
│   ├── OceanMap.vue（Leaflet + Canvas 网格覆盖层 + 框选 + 图例）
│   └── TrendChart.vue（ECharts 折线图）
├── ChxMapView.vue
│   ├── FilterBar（日期 + 海域下拉 + 浓度/概率切换 + 查询/重置）
│   ├── OceanMap.vue
│   └── TrendChart.vue
└── HistoryView.vue（现有表格逻辑）
```

### OceanMap 共享组件

**Props:** `gridData`（网格数据）, `colorMap`（颜色映射）, `legendLabels`（图例标签）, `loading`
**Events:** `@cellClick` → `{lat, lon}` 触发趋势图加载, `@bboxChange` → 视口/框选变化触发数据刷新

### 地图渲染

- Leaflet 底图加载 OSM 瓦片
- Canvas 覆盖层（L.GridLayer 或自定义 L.Canvas）一次性绘制所有网格矩形
- 右下角 DOM 图例叠加在地图上
- Leaflet.draw 插件支持矩形/多边形框选海域

## 后端 API

### GET /api/forecast/map/grid

地图网格聚合数据。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| dataType | String | 是 | SST / CHL |
| forecastDate | String | 是 | 预报日期 YYYY-MM-DD |
| precision | Double | 否(默认0.05) | 聚合精度（度） |
| minLon | Double | 否 | 可视范围西边界 |
| maxLon | Double | 否 | 可视范围东边界 |
| minLat | Double | 否 | 可视范围南边界 |
| maxLat | Double | 否 | 可视范围北边界 |
| chxMode | String | 否(仅CHL) | concentration / probability |
| threshold | Double | 否(默认3.0) | 概率模式阈值 |

SST/浓度模式的 SQL：

```sql
SELECT ROUND(longitude, ?) AS grid_lon, ROUND(latitude, ?) AS grid_lat, AVG(value) AS value
FROM forecast_record
WHERE data_type = ? AND forecast_date = ?
  AND longitude BETWEEN ? AND ? AND latitude BETWEEN ? AND ?
GROUP BY grid_lon, grid_lat
```

概率模式的 SQL（限定天数范围后计算超阈值比例）：

```sql
SELECT ROUND(longitude, ?) AS grid_lon, ROUND(latitude, ?) AS grid_lat,
       ROUND(SUM(CASE WHEN value > ? THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) AS probability
FROM forecast_record
WHERE data_type = 'CHL' AND forecast_date BETWEEN ? AND ?
  AND longitude BETWEEN ? AND ? AND latitude BETWEEN ? AND ?
GROUP BY grid_lon, grid_lat
```

### GET /api/forecast/trend/point

单点位历史趋势。参数：`dataType`, `lon`, `lat`, `dateStart`, `dateEnd`。
Step 1 找最近的数据库点位，Step 2 按日期范围查该点位的所有值。

### GET /api/forecast/sea-areas

返回预设海域配置（bbox 列表），供下拉选择和地图快速定位。

## 颜色配置

### SST 海表温度（5 级）

| 范围 (°C) | 颜色 | 色值 |
|-----------|------|------|
| <16 | 深蓝 | #1A5276 |
| 16-20 | 蓝 | #2E86C1 |
| 20-24 | 橙黄 | #F39C12 |
| 24-28 | 橙 | #E67E22 |
| >28 | 红 | #E74C3C |

### CHL 浓度模式（5 级）

| 范围 (mg/m³) | 颜色 | 色值 |
|-------------|------|------|
| <0.5 | 深绿 | #0B5345 |
| 0.5-1.5 | 墨绿 | #148F77 |
| 1.5-3.0 | 青绿 | #1ABC9C |
| 3.0-5.0 | 绿 | #27AE60 |
| >5.0 | 亮绿 | #2ECC71 |

### CHL 概率模式（5 级）

| 范围 (%) | 颜色 | 色值 |
|----------|------|------|
| <20 | 绿 | #27AE60 |
| 20-40 | 黄 | #F1C40F |
| 40-60 | 橙黄 | #F39C12 |
| 60-80 | 橙 | #E67E22 |
| >80 | 红 | #E74C3C |

## 交互逻辑

1. 页面加载 → 请求海域列表 + 默认日期地图网格数据 → 渲染地图 + 趋势图
2. 用户修改日期/海域下拉 → 地图 + 趋势图同步刷新
3. 用户在地图上框选 → 以 bbox 参数重新请求 → 地图 + 趋势图刷新
4. 用户缩放/平移地图 → 按 LOD 精度 + 视口 bbox 重新请求网格数据
5. 点击地图网格 → 请求该点位历史趋势 → 趋势图高亮对应曲线
6. Chl 页面切换浓度/概率模式 → 重新请求数据 → 地图 + 趋势图刷新

## 需要修改的文件

### 新增
- `ocean-web/src/views/forecast/SstMapView.vue`
- `ocean-web/src/views/forecast/ChxMapView.vue`
- `ocean-web/src/views/forecast/HistoryView.vue`（从 ForecastView 拆出）
- `ocean-web/src/components/OceanMap.vue`
- `ocean-web/src/components/MapFilterBar.vue`（可选，SST/Chl 共用）
- `ocean-web/src/components/TrendChart.vue`
- `ocean-server/.../dto/MapGridQueryDTO.java`

### 修改
- `ocean-web/src/router/index.js` — 新路由
- `ocean-web/src/layout/MainLayout.vue` — 子菜单
- `ocean-web/src/api/forecast.js` — 新 API 函数
- `ocean-web/src/utils/chart-config.js` — 地图配色常量
- `ocean-server/.../controller/ForecastRecordController.java` — 新端点
- `ocean-server/.../service/ForecastRecordService.java` + impl — 新方法
- `ocean-server/.../mapper/ForecastRecordMapper.java` — 新查询
- `database/init.sql` — 新索引

### 可能废弃
- `ocean-web/src/views/forecast/ForecastView.vue` — 拆分为三个子页面

## 注意事项

- 21M 数据点的全表扫描不可接受，所有查询必须通过索引过滤后再聚合
- Canvas 渲染优先于 DOM 标记，确保 ~10K 点流畅渲染
- 海域下拉的 bbox 值在后端 `SeaAreaConfig` 中配置，前端不硬编码
