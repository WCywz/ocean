# 地图可视化

## 背景

预报和观测数据需要在 Leaflet 地图上以直观的方式呈现空间分布。经历了从离散网格点到连续热力图的演进。

## 设计决策

### 从离散点到热力图
**旧方案：** `L.circleMarker` 在 0.25° 分辨率网格上渲染数百个离散圆点，视觉杂乱，空间模式难以辨认。

**新方案：** 自定义 `HeatInterpLayer`（基于 Canvas 的双线性插值热力图层），替代 `leaflet.heat` 插件：
- 从 `[lat, lon, value]` 三元组构建
- 在墨卡托投影上以 3 级分辨率渲染
- 视口过滤：仅可见点传入热力层，提升性能

### 交互模式变更
- **移除：** 点击网格点加载趋势图（热力图无可点击目标，且用户不清楚需要点击）
- **改为：** 挂载时使用默认中心点（122.5°E, 29.8°N）加载趋势数据
- **保留：** 绘制控件、图例叠加、筛选栏、日期选择器、海域选择器、CHL 模式切换

### 颜色渐变
`buildHeatGradient()` 函数（`chart-config.js`）控制颜色映射，参考海洋学标准色阶（蓝→绿→黄→红）。

### 海岸线/国界
使用矢量底图样式：陆地填充 + 国界线，无栅格瓦片图层，保持与 Editorial 设计体系一致的简洁风格。

## 地图组件复用

`OceanMap.vue` 是核心共享组件，被以下页面复用：
- 预报 SST 地图（`SstMapView.vue`）
- 预报 CHL 地图（`ChxMapView.vue`）
- 观测 SST 地图（`ObsSstView.vue`）
- 观测 CHL 地图（`ObsChlView.vue`）
- 健康告警小地图（`HealthAlertSection.vue` 内）

## 关键文件

- `ocean-web/src/components/OceanMap.vue` — 核心地图组件（Canvas 热力层 + 城市标记 + 选点标记）
- `ocean-web/src/utils/chart-config.js` — 颜色渐变配置
- `ocean-web/src/utils/land-mask.js` — 陆地遮罩
