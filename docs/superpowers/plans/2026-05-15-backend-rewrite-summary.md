# 后端重写修改总结

> 日期: 2026-05-15 | 基于 redesign-observation.sql (12张新表)

## 一、数据库

### 新表结构 (database/redesign-observation.sql)
12张表，已全部执行到 ocean_forecast 数据库：

| 表名 | 说明 | 行数 |
|------|------|------|
| observation_data | 观测数据（按月分区） | 8 |
| observation_grid_cache | 观测网格缓存 | 391 |
| forecast_grid | 预报网格数据 | 796 |
| monitoring_station | 监测站点 | 6 |
| model | 预报模型 | 2 |
| model_version | 模型版本 | 2 |
| alert_rule | 告警规则 | 2 |
| alert_event | 告警事件 | 2 (测试生成) |
| alert_station_detail | 告警站点详情 | 0 |
| health_zone | 健康分区 | 4 |
| health_record | 健康记录 | 4 |

### 测试数据 (database/test-data-backend.sql)
- 2个模型（SST海表温度预报、CHL叶绿素浓度预报）
- 2个运行中的模型版本
- 796条预报网格数据（覆盖8天×约100个格点/天）
- 6个监测站点
- 8条观测数据
- 391条网格缓存
- 4条健康记录

## 二、后端代码变更

### 新增文件 (43个)

#### Entity层 (11个) — `ocean-server/src/main/java/com/ocean/entity/`
- `ObservationData.java` — 观测数据
- `ObservationGridCache.java` — 网格缓存
- `ForecastGrid.java` — 预报网格
- `MonitoringStation.java` — 监测站点
- `Model.java` — 模型
- `ModelVersion.java` — 模型版本
- `AlertRule.java` — 告警规则
- `AlertEvent.java` — 告警事件
- `AlertStationDetail.java` — 告警站点详情
- `HealthZone.java` — 健康分区
- `HealthRecord.java` — 健康记录

#### Mapper层 (11个) — `ocean-server/src/main/java/com/ocean/mapper/`
- `ObservationDataMapper.java` — 含4个自定义SQL（SST时序、CHL时序、按深度CHL、去重位置）
- `ObservationGridCacheMapper.java`
- `ForecastGridMapper.java` — 含7个自定义SQL（位置、地图网格、趋势、点趋势、仪表盘趋势、CHL概率、bbox查询）
- `MonitoringStationMapper.java`
- `ModelMapper.java`
- `ModelVersionMapper.java`
- `AlertRuleMapper.java`
- `AlertEventMapper.java`
- `AlertStationDetailMapper.java`
- `HealthZoneMapper.java`
- `HealthRecordMapper.java`

#### DTO层 (2个新增 + 2个修改)
- `AlertRuleSaveDTO.java` (新)
- `StationSaveDTO.java` (新)
- `ForecastQueryDTO.java` (改) — 新增 locationName, forecastDateBegin, forecastDateEnd
- `MapGridQueryDTO.java` (改) — 新增 chlMode, threshold
- `OceanDataQueryDTO.java` (改) — 新增 variable 字段

#### VO层 (5个新增 + 2个修改)
- `DashboardVO.java` (新)
- `StationVO.java` (新)
- `AlertEventVO.java` (新)
- `AlertStationDetailVO.java` (新)
- `ZoneHealthVO.java` (新)
- `ModelVO.java` (改) — 新增 versionCount, runningCount
- `ModelVersionVO.java` (改) — groupId → modelId

#### Service层 (7个接口 + 7个实现)
- `ObservationService` / `ObservationServiceImpl` — 观测数据查询、时序分析
- `ForecastService` / `ForecastServiceImpl` — 仪表盘、地图网格、趋势、概率
- `ModelService` / `ModelServiceImpl` — 模型CRUD + 版本计数
- `ModelVersionService` / `ModelVersionServiceImpl` — 版本CRUD + 启停
- `StationService` / `StationServiceImpl` — 站点CRUD + 分区查询
- `AlertService` / `AlertServiceImpl` — 规则CRUD、事件查询、告警生成（基于规则×分区遍历）
- `HealthService` / `HealthServiceImpl` — 分区健康评估、趋势、仪表盘

#### Controller层 (7个新 + 3个禁用)
新增：
- `ObservationController` — `/api/observation` (5个端点)
- `ForecastController` — `/api/forecast` (10个端点)
- `ModelController` — `/api/model` (5个端点)
- `ModelVersionController` — `/api/model/{modelId}/version` (5个端点)
- `StationController` — `/api/station` (4个端点)
- `AlertController` — `/api/alert` (8个端点)
- `HealthController` — `/api/health` (4个端点)

禁用（注释掉 @RestController 和 @RequestMapping，保留文件）：
- `ForecastRecordController.java`
- `OceanDataController.java`
- `ForecastModelController.java`

#### Config层 (1个新增)
- `MybatisPlusMetaObjectHandler.java` — 自动填充 createTime/updateTime

### 修改文件 (4个前端API文件)

- `ocean-web/src/api/forecast.js` — getAlerts: `/forecast/alerts` → `/alert/events`
- `ocean-web/src/api/ocean-data.js` — 全部 `/ocean-data/` → `/observation/`
- `ocean-web/src/api/health.js` — `/forecast/zone-health` → `/health/assessment`
- `ocean-web/src/api/model.js` — 版本路径更新，适配 ModelVersionController

## 三、API端点测试结果 (20/20 通过)

```
Observation:  page(200)  locations(200)  sst-timeseries(200)  chl-timeseries(200)  chl-by-depth(200)
Forecast:     dashboard(200)  page(200)  sst/trend(200)  chl/trend(200)  locations(200)
              map/grid(200)  sea-areas(200)  trend/point(200)
Model:        page(200)  running-versions(200)  versions(200)
Station:      page(200)  by-zone(200)
Alert:        rules(200)  events(200)  events/{id}(200)  generate(200)
Health:       zones(200)  assessment(200)  dashboard(200)
```

## 四、已修复的问题

1. **create_time 为 null 导致 INSERT 失败** — AlertEvent 等实体的 `@TableField(fill = FieldFill.INSERT)` 在无 MetaObjectHandler 时会插入 null。新增 `MybatisPlusMetaObjectHandler` 统一处理。

2. **model_version 外键错误** — 测试数据中 model_version 的 model_id 指向 1, 2，但实际 model 表 ID 为 5, 6（health_zone 占用了 1-4）。已通过 UPDATE 修复。

3. **旧 Controller 与新 Controller 路径冲突** — 3个旧 Controller 的 @RestController 和 @RequestMapping 已注释禁用。

## 五、架构改进

- **解耦**: 原来3个 Controller 混杂了观测、预报、模型、健康、告警等不同领域逻辑；现在拆分为7个独立 Controller，每个 Controller 一个领域
- **Mapper 自定义SQL**:复杂查询（时序、地图网格、趋势、bbox聚合）使用 `@Select` + 动态 SQL，简单 CRUD 使用 MyBatis-Plus LambdaQueryWrapper
- **告警生成**: AlertServiceImpl.generateAlerts 按规则×分区×bbox数据遍历，支持后续扩展为定时任务

## 六、Git状态

- 提交点: `9a382c1 save: snapshot before backend rewrite based on redesigned database`
- 所有改动未提交，可随时回滚到该提交点
