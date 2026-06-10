# 调度总览 — 拖拽排程

## 目标

将模型版本的 Cron 调度可视化，通过拖拽操作替代手动输入 Cron 表达式，在周视图日历上直观展示所有版本的调度状态。

## 现状

- `ScheduleOverview.vue` 为占位页（"调度总览功能开发中"）
- 调度配置在 `VersionDialog.vue` 中通过 Cron 预设下拉框手动设置，存储在 `model_version.cron_expression`
- Quartz Scheduler 已集成（RAM JobStore），`SchedulerService` 管理每个版本的单个 Quartz Trigger
- `ModelForecastJob` 未加 `@DisallowConcurrentExecution`，存在同版本并发执行风险

## 设计方案

### 页面布局

三栏布局，替换现有占位页：

- **左栏**：ModelLayout 侧边栏（200px，已有，不变）
- **中栏**：周视图日历——7 列（周一至周日）× 24 行（每小时），固定高度内垂直滚动
- **右栏**：版本卡片池（~280px）——可拖拽版本卡片列表，支持搜索和「仅显示运行中」过滤

### 数据模型

#### 新表 `model_schedule`

```sql
CREATE TABLE model_schedule (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  version_id     BIGINT NOT NULL,
  schedule_label VARCHAR(50),
  repetition     VARCHAR(20) NOT NULL,  -- DAILY / WEEKLY / ONCE
  day_of_week    INT,                    -- WEEKLY: 1=周一..7=周日
  schedule_time  TIME NOT NULL,          -- 调度时间 HH:mm
  is_active      TINYINT(1) DEFAULT 1,
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (version_id) REFERENCES model_version(id) ON DELETE CASCADE
);
```

- Cron 表达式由 `(repetition, day_of_week, schedule_time)` 后端实时生成，不存 Cron 字符串
- 一个版本可有多条记录，对应日历上的多个调度块
- `model_version.cron_expression` 字段不再使用（保留不删，向后兼容）

### API

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/model/{modelId}/version/{versionId}/schedule` | 获取某版本的所有调度 |
| `POST` | `/api/model/{modelId}/version/{versionId}/schedule` | 创建调度 |
| `PUT` | `/api/model/schedule/{id}` | 更新调度 |
| `DELETE` | `/api/model/schedule/{id}` | 删除调度 |
| `GET` | `/api/model/schedule/week?startDate=&endDate=` | 按周获取所有版本的调度（日历展示用） |

### 组件结构

```
ScheduleOverview.vue
├── ScheduleCalendar.vue          ← 周日历（中部主区域）
│   └── ScheduleBlock.vue         ← 日历格子中的调度块（点击编辑/删除）
├── VersionCardPool.vue           ← 版本卡片池（右侧栏）
│   └── VersionCard.vue           ← 单张可拖拽版本卡片
└── ScheduleDialog.vue            ← 拖放后弹出（选重复规则）
```

### 交互流程

**创建调度**：右侧卡片 → 拖到日历某天/某时格子 → 松开 → 弹出 ScheduleDialog（选重复规则：每天/每周/仅一次）→ 确认 → 调度块出现在日历上

**编辑调度**：点击日历上的调度块 → 弹出编辑面板（修改时间、重复规则、或删除）

**删除调度**：编辑面板中点击删除，或右键菜单删除

**周切换**：底部「上一周」「下一周」按钮，默认显示当前周

**空状态**：未配置调度时，日历区域显示 editorial 风格提示「暂无调度配置」

### Quartz 调度引擎改造

`SchedulerService` 扩展支持一个版本多个 Trigger：

- `schedule(versionId)`：遍历该版本所有 `is_active=1` 的 `model_schedule` 记录，为每条创建 `CronTrigger`，挂到同一个 `JobDetail`（`forecast-v{versionId}`）
- `unschedule(versionId)`：移除该版本的所有 Trigger
- `reschedule(versionId)`：unschedule → schedule，用于调度变更后整体重建
- `scheduleOne(scheduleId)` / `unscheduleOne(scheduleId)`：单条调度的增删

`ModelForecastJob` 添加 `@DisallowConcurrentExecution`，确保同一版本不会并发执行。

### 设计风格

- editorial 设计体系：Georgia 标题、灰色调、无阴影/无圆角
- 全部颜色使用 CSS 变量，深色模式自动跟随 `[data-theme="dark"]`
- 调度块颜色：灰度阶梯（#2c3e50 / #555 / #777 / #999 / #bbb），按模型自动分配
- 红色仅用于告警状态，不在调度块上使用
- 按钮使用 editorial-btn 等宽字体样式

### 错误处理 & 边界情况

| 场景 | 处理方式 |
|---|---|
| 删除版本时 | 级联删除 `model_schedule` + 清理 Quartz |
| 停止版本时 | 保留 schedule 记录，清理 Quartz Trigger；重启时恢复 |
| 拖拽到已有调度的格子 | 不冲突，同格多调度块垂直堆叠 |
| 拖拽到无效区域 | 松开后卡片回弹，无操作 |
| 日历滚动 | 容器固定高度，内部 overflow-y 滚动 |
| 网络错误 | ElMessage 提示，日历回滚到操作前状态 |
| Quartz 恢复失败 | 日志告警，跳过该条，不影响其他调度 |
| 调度时间冲突 | 不检查——允许多版本同时运行 |

## 不纳入本次

- 拖拽调整调度时间（resize 已存在的调度块）
- 拖拽移动调度到不同时间（move 已存在的调度块）
- 调度执行历史和运行日志（已有 RunMonitor 页面）
- `model_version.cron_expression` 字段清理（保留向后兼容）
- 月视图切换（仅做周视图）
