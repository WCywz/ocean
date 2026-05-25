# 数据库参考导出

导出时间：2026-05-24
数据源：`ocean_forecast` @ localhost (MySQL)

## 导出文件

| 文件 | 内容 |
|------|------|
| `export/schema.sql` | 全部 15 张表的 DDL（建表语句） |
| `export/reference_data.sql` | 12 张小表的全量数据：alert_rule, alert_event, alert_station_detail, health_zone, health_record, model, model_version, monitoring_station, sys_user, system_config, observation_grid_cache, observation_archive |
| `export/forecast_grid_sample.sql` | forecast_grid 抽样 500 行 |
| `export/observation_grid_sample.sql` | observation_grid 抽样 500 行 |
| `export/observation_data_sample.sql` | observation_data 抽样 500 行 |

## 表概览

| 表 | 行数 | 说明 |
|----|------|------|
| observation_data | ~1470 万 | 原始观测数据（按月分区） |
| observation_grid | ~28 万 | 插值后的观测网格 |
| forecast_grid | ~7,400 | 预报网格（391 格点 × 2 变量） |
| observation_grid_cache | 432 | 格点坐标缓存 |
| model | 2 | 模型定义 |
| model_version | 2 | 模型版本 |
| monitoring_station | 6 | 监测站点 |
| health_zone | 6 | 健康评估分区 |
| health_record | 6 | 健康评估记录 |
| alert_rule | 2 | 告警规则 |
| alert_event | 2 | 告警事件 |
| alert_station_detail | 0 | 告警站点详情 |
| sys_user | 2 | 系统用户 |
| system_config | 0 | 系统配置 |
| observation_archive | 0 | 观测归档 |
