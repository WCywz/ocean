# 海洋健康指数

## 背景

预报数据通过网格和表格呈现，非专业用户难以理解。需要一个类似 AQI（空气质量指数）的直观评估体系，服务于公众科普和决策辅助。

## 设计决策

### 分区规则（算法驱动）
将目标海域划分为 6 个子区域：
- **2 个方位：** 北部 / 南部（以参考点纬度划分）
- **3 个离岸距离带：** 近岸（≤0.5°）、过渡带（0.5°–1.5°）、远海（>1.5°），沿经度方向测量
- 参考点初始配置为东海（122.5°E, 29.5°N），后续可配置化

分区基于算法计算而非手动绘制边界——这使扩展到其他海域只需修改参考坐标。

### 四级评估体系
| 等级 | 颜色 | 含义 |
|------|------|------|
| 优 | `#22c55e` 绿 | 各项指标正常 |
| 良 | `#22c55e` 绿 | 轻微偏离 |
| 中 | `#f59e0b` 橙 | 需关注 |
| 差 | `#ef4444` 红 | 需预警 |

综合等级取各指标的最差值（类比 AQI "首要污染物"原则）。

### 前端驱动的阈值引擎
阈值判断逻辑全部放在前端 `health-assessment.js` 中，而非后端配置表：
- **原因：** 阈值规则相对简单，调整频率高于后端部署周期
- **长期规划：** 未来可迁移至后端配置表
- **核心函数：** `assessSst()`、`assessChl()`、`assessHeatwave()`、`worstLevel()`、`buildZoneAssessment()`、`buildOverallSummary()`

### UI 布局
- **顶部总览横幅：** 自然语言摘要 + 日期
- **3×2 卡片矩阵：** 区域名称、等级标识、一句话结论、子指标标签
- **点击交互：** 选中卡片蓝色高亮，其余变暗，展开详情面板（风险解读 + 指标明细 + 建议）
- **日期选择器：** 切换不同预报日期

### 告警阈值板块
在状态栏和卡片网格之间插入 `HealthAlertSection`：
- 展示当前日期 SST/CHL 超阈值告警
- 复用仪表盘已有的 `AlertPanel` 组件
- 钻取链接导向对应预报地图

## API

- `GET /api/health/assessment` — 区域健康评估
- `GET /api/health/zones` — 区域列表
- 后端按分区计算汇总统计，前端负责评级和解读

## 关键文件

- `ocean-web/src/views/health/OceanHealthView.vue` — 主页面
- `ocean-web/src/views/health/HealthAlertSection.vue` — 告警板块
- `ocean-web/src/utils/health-assessment.js` — 阈值引擎
- `ocean-server/.../controller/HealthController.java` — 后端 API
