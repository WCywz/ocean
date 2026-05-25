# 后端架构

## 背景

旧后端将观测、预报、模型、健康、告警逻辑全部耦合在 3 个单体控制器和一个巨大的 `forecast_record` 表中，功能扩展时互相影响。

## 架构决策

### 领域拆分
基于 12 张重新设计的数据库表，将后端拆分为 **7 个独立领域模块**，每个模块有自己的 Controller → Service → Mapper 链：

| 领域 | 控制器 | 核心表 |
|------|--------|--------|
| 观测 | `ObservationController` | `observation_data`（按月分区）, `observation_grid` |
| 预报 | `ForecastController` | `forecast_grid`（391 格点 × 2 变量 × 365 天） |
| 模型 | `ModelController` | `model`, `model_version` |
| 告警 | `AlertController` | `alert_rule`, `alert_event`, `alert_station_detail` |
| 健康 | `HealthController` | `health_zone`, `health_record` |
| 站点 | `StationController` | `monitoring_station` |
| 用户 | `UserController` | 保持原有不变 |

### API 路径重新映射
| 旧路径 | 新路径 |
|--------|--------|
| `/forecast/alerts` | `/api/alert/events` |
| `/forecast/zone-health` | `/api/health/assessment` |
| `/ocean-data/*` | `/api/observation/*` |

### 技术栈
- **Java 21 + Spring Boot 3.4.1**
- **MyBatis-Plus 3.5.7**（所有 Mapper 继承 `BaseMapper<T>`）
- **MySQL** 持久化 + **Redis** 缓存
- **JWT** 认证（jjwt 0.12.6，30 分钟过期）
- **Knife4j 4.5.0** API 文档（`/doc.html`）
- **定时任务：** `task/` 包中的 `@Scheduled` 任务（告警生成、健康评估、数据摄入）

### 分层架构原则
- Controller 层：参数校验 + 路由，不写业务逻辑
- Service 层：业务逻辑，事务管理
- Mapper 层：纯 SQL，不混业务判断
- 每个控制器最多处理一个领域

### forecast_grid 表
大表（每年约 28.5 万行），需要合适的索引和分页策略。
索引：`(variable, forecast_date)`、`(lat, lon)`

### observation_grid 与 forecast_grid 的区别
`observation_grid` 结构类似 `forecast_grid`，但：
- 无 `model_id`/`version_id`（观测数据不来源于模型）
- `forecast_date` 改为 `obs_date`
- 观测无 CHL 概率模式（观测是实测值，非预测）

## 关键文件

- `ocean-server/src/main/java/com/ocean/OceanApplication.java` — 启动类
- `ocean-server/src/main/java/com/ocean/controller/` — 10 个 REST 控制器
- `ocean-server/src/main/java/com/ocean/service/` — 10 个服务接口 + 实现
- `ocean-server/src/main/java/com/ocean/mapper/` — 16 个 MyBatis-Plus Mapper
- `ocean-server/src/main/java/com/ocean/entity/` — 15 个 JPA 实体
- `ocean-server/src/main/java/com/ocean/task/` — 4 个定时任务
