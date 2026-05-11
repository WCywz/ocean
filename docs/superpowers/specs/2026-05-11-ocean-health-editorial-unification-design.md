# 海洋健康指数 Editorial 风格统一 设计文档

## 概述

将海洋健康指数页面 (`OceanHealthView.vue`) 的视觉风格统一到项目的 Editorial 设计体系中，与 Dashboard、OceanData、SST Map 等页面保持一致。保留现有 3x2 卡片网格的功能布局和交互逻辑，仅改变视觉呈现。

## 核心原则

- **保留功能、替换样式**：卡片网格布局、点击展开详情、日期切换等交互行为不变
- **颜色服务于等级信息**：只在左侧色条和等级文字上使用颜色，其余使用 Editorial 的灰阶体系
- **复用 Editorial CSS 变量**：使用 `editorial.css` 已定义的 `--color-*`、`--font-*` 等变量

## 视觉变更清单

### 顶部区域

- **标题**：使用 `editorial-page-title`（Georgia 衬线体，28px）+ `editorial-page-subtitle`（斜体副标题 "Ocean Health Index"）
- **综合摘要**：取消彩色背景横幅，改为淡色信息条：
  - 左侧 3px 色条（绿/橙/红，反映综合等级）
  - 背景 `#fafafa`
  - 衬线体等级文字（"优良"/"良好"/"中等"/"较差"）
  - 摘要描述文字 + 右对齐日期文字，日期作为 `<el-date-picker>` 的触发入口（inline text 样式，点击弹出日期选择器）

### 卡片网格

每张卡片变更：

| 元素 | 现状 | 改为 |
|------|------|------|
| 卡片容器 | 白色背景 + 2px 透明边框 + 阴影 | 白色背景 + 1px `#f0f0f0` 实线边框（上/右/下）+ 无阴影 |
| 左侧等级指示 | 无 | 3px 色条（绿/橙/红），颜色反映综合等级 |
| 区域名称 | 12px #888 常规 | 11px `var(--color-text-muted)` 大写加字距 |
| 等级文字 | 15px 粗体 | `editorial-section-heading` 衬线体 20px，差/中等用对应颜色 |
| 一句话结论 | 12px #666 | 12px `var(--color-text-secondary)` |
| 圆形徽标 | 48px 圆形彩色背景 + 白色文字 | **移除** |
| 指标标签 | 彩色背景圆角标签 | 11px 灰色文字，用分割线与等级区分 |

### 展开详情面板

- 边框颜色：**跟随选中区域的综合等级颜色**（红/橙/绿）
- 边框宽度：2px
- 内部结构不变：解读文字 → 指标明细表 → 建议列表
- 详情表格使用 `editorial-table` 样式
- 等级标签使用 `--color-alert` 或对应颜色文字

### 选中/未选中状态

- 选中卡片：边框（上/右/下）保持正常，左侧色条不变
- 未选中卡片：`opacity: 0.45`，视觉退后
- 取消选中：再次点击同一卡片收起详情

## 等级颜色映射

| 等级 | 色条颜色 | 文字颜色（差/中时） |
|------|---------|-------------------|
| 优良 | `#22c55e` | 默认（`var(--color-text)`） |
| 良好 | `#22c55e` | 默认 |
| 中等 | `#f59e0b` | `#92400e` |
| 较差 | `#ef4444` | `#c0392b` |

## 技术实现

### 修改文件

| 文件 | 改动 |
|------|------|
| `ocean-web/src/views/health/OceanHealthView.vue` | 重写 template 和 style，script 逻辑基本不变 |

### 不变文件

- `ocean-web/src/utils/health-assessment.js` — 阈值引擎不变
- `ocean-web/src/api/health.js` — API 封装不变
- `ocean-web/src/router/index.js` — 路由不变
- `ocean-server/` — 后端零改动

### 样式实现方式

- 复用 `editorial.css` 全局类：`editorial-page-title`、`editorial-page-subtitle`、`editorial-section-label`、`editorial-section-heading`、`editorial-table`
- 自定义 scoped 样式：卡片左侧色条、信息条、展开面板边框颜色（通过动态 class 或 inline style 绑定）
- 颜色计算：在 `buildZoneAssessment()` 中已返回 `zone.overall.color`，直接复用于左侧色条和详情面板边框

## 交互行为

保持不变：
- 默认加载当天预报数据
- 日期选择器触发重新请求
- 点击卡片展开/折叠详情
- 同一时间只有一个展开面板
