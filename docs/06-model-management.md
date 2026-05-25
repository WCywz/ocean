# 模型管理

## 背景

旧版模型管理是扁平的 CRUD 列表，没有版本概念。实际上每个模型有多个版本（不同训练数据、参数、时间范围），需要支持版本化管理。

## 设计决策

### 二层层级模型
- **模型（父级）：** 名称、类型（SST/CHL/自定义）、描述、创建时间
- **版本（子级）：** 版本标签（v1, v2, ...）、cron 表达式、参数配置（JSON）、数据源、数据时间范围、变更说明、状态（RUNNING/STOPPED/ERROR）、上次运行时间

### 展开交互
- **手风琴模式：** 一次只展开一个模型，点击模型行切换版本的显示/隐藏
- **按需加载：** 展开时才通过 API 获取版本列表
- **运行概览面板：** 页面顶部显示所有正在运行的版本及快速停止按钮（无需展开模型即可看到运行状态）

### 操作语义
| 操作 | 级别 | 行为 |
|------|------|------|
| 启动 | 模型级 | 仅启动最新版本 |
| 停止 | 模型级 | 停止该模型下所有版本 |
| 启动/停止 | 版本级 | 独立控制单个版本 |

### 创建流程
先创建模型外壳（名称 + 类型 + 描述），再展开模型添加版本（配置 + 数据源 + 时间范围）。

### 模型类型
支持预定义类型（SST、CHL）以及自定义输入，以兼容未来的新模型类型。

### 版本追溯
版本对话框中记录了数据源和变更说明字段——这对科学研究的可复现性很重要。

## API

- `ModelController`：模型 CRUD + 运行中版本概览
- `ModelVersionController`：版本 CRUD（嵌套在模型下 `/{modelId}/versions`）+ 独立启停端点

## 关键文件

- `ocean-web/src/views/model/ModelView.vue` — 主页面（可展开列表）
- `ocean-web/src/views/model/ModelDialog.vue` — 模型创建/编辑对话框
- `ocean-web/src/views/model/VersionDialog.vue` — 版本创建/编辑对话框
- `ocean-web/src/views/model/RunningOverview.vue` — 运行概览面板
- `ocean-server/.../controller/ModelController.java`
- `ocean-server/.../controller/ModelVersionController.java`
