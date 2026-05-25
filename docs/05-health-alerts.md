# 健康告警系统

## 背景

海洋健康页面需要展示超阈值告警信息，并提供空间可视化能力。最初是纯文字列表，后来增加了互动地图。

## 设计决策

### 告警数据结构
后端查询返回每条告警的：
- 数据类型（SST / CHL）
- 实测值、阈值
- **经纬度**（用于地图定位）
- 所属区域、站点信息

### 分割布局
`HealthAlertSection.vue` 采用左右分栏：
- **左侧（40%）：** 紧凑告警卡片 + 摘要统计栏
- **右侧（60%）：** Leaflet 小地图，禁用缩放（`scrollWheelZoom: false`、`zoomControl: false`），保留拖拽平移

### 交互设计
- **点击卡片 → 地图定位：** `map.flyTo([lon, lat], zoom)` 飞到告警位置，放置脉冲标记
- **脉冲动画：** CSS `@keyframes alert-pulse`，`divIcon` 自定义标记，尺寸约 3rem（远小于英雄页 10rem 动画）
- **颜色编码：** SST 告警 = `#c0392b` 红，CHL 告警 = `#e67e22` 橙，与 Editorial 体系一致
- **同位置多告警：** 标记轻微偏移（SST 纬度 +0.02°，CHL 纬度 -0.02°）
- **取消选中：** 再次点击同一卡片取消选中，地图保持在最后位置

### 日期过滤
告警与预报日期绑定。后端 `/api/forecast/alerts` 接受 `forecastDate` 参数（原为 `CURDATE()` 硬编码），与健康页面的日期选择器联动。

### 组件复用
健康页面的 `HealthAlertSection` 复用了仪表盘的 `AlertPanel` 组件：
- 仪表盘显示当日告警
- 健康页面显示选定预报日期的告警
- 复用降低维护成本

## 关键文件

- `ocean-web/src/views/health/HealthAlertSection.vue` — 告警板块（卡片列表 + Leaflet 小地图）
- `ocean-web/src/views/dashboard/AlertPanel.vue` — 被复用的告警面板组件
- `ocean-server/.../controller/AlertController.java` — 告警 API
