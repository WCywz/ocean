# 数据管线状态记录

## 数据流向总览

```
原始 CSVs                      插值 CSV
temp_data.csv ─┐              ocean_clean_multivar.csv
chl_data.csv   ─┤              ocean_clean_post_2025.csv
so_data.csv    ─┤                    │
                │                    ├──→ ingest_daily.py ──→ observation_grid
                │                    │    (每日摄入，表层)
                │                    │
        filter_raw_data.py           ├──→ prepare_forecast_input.py ──→ run_forecast.py
                │                    │    (预报模型输入，所有深度)        ──→ forecast_grid
    ocean_raw_temp.csv               │
    ocean_raw_chl.csv                │
    ocean_raw_so.csv                 │
         │                           │
  ingest_raw_daily.py                │
         │                           │
         ▼                           │
   observation_data                  │
   （原始观测，各自网格）             │
```

## 当前数据状态

### observation_data（原始观测）

| variable | 行数 | 日期范围 | 来源 | 状态 |
|---|---|---|---|---|
| chl | 165 万 | 2025-05-01 ~ 2026-01-02 | 插值（待切换为原始） | 待执行 |
| thetao | 1297 万 | 2025-05-01 ~ 2026-01-02 | 插值（待切换为原始） | 待执行 |
| so | 1940 万 | 2025-01-01 ~ 2026-01-02 | so_data.csv（原始） | 已完成 |

### observation_grid（插值表层）

| variable | 行数 | 日期范围 | 来源 |
|---|---|---|---|
| chl | 15.9 万 | 2025-01-01 ~ 2026-01-02 | 插值（统一网格） |
| thetao | 15.9 万 | 2025-01-01 ~ 2026-01-02 | 插值（统一网格） |

注：grid 不含盐度（确认不需要）。

### forecast_grid（模型预测）

| variable | 行数 | 日期范围 |
|---|---|---|
| chl | 3720 | 2025-12-31 ~ 2026-01-09 |
| sst | 3720 | 2025-12-31 ~ 2026-01-09 |

## 已完成的改造

1. **数据入库拆分为两条链路**：observation_grid ← 插值 CSV，observation_data ← 原始 CSV
2. **预报管线独立数据源**：`ForecastServiceImpl.runForecast()` 不再从 observation_data 读，
   改为通过 `prepare_forecast_input.py` 直接从插值 CSV 生成模型输入，
   避免原始数据网格不匹配导致的 PIVOT NULL 问题
3. so 数据已从 so_data.csv 恢复到 observation_data（2025-01-01 ~ 2026-01-02）
4. salt 已加入 `ingest_raw_daily.py` 的常规摄入流程

## 脚本清单

| 脚本 | 用途 | 调用方式 |
|---|---|---|
| filter_raw_data.py | 从原始 CSV 裁剪 ≥2026-01-01 数据 | 一次性手动 |
| filter_recent_data.py | 从插值 CSV 裁剪 >2025-12-30 数据 | 一次性手动 |
| ingest_daily.py | 每日摄入 observation_grid | Java 调用 |
| ingest_raw_daily.py | 每日摄入 observation_data | Java 调用 |
| ingest_batch.py | 批量摄入 observation_grid（历史） | 手动 |
| ingest_raw_batch.py | 批量摄入 observation_data（历史） | 手动 |
| prepare_forecast_input.py | 从插值 CSV 生成模型输入 | Java 调用 |
| run_forecast.py | 运行模型推理 | Java 调用 |

## 待执行

1. 运行 `filter_raw_data.py` 裁剪原始 temp/chl/so CSV
2. 运行 `ingest_raw_batch.py` 把原始 chl、thetao 批量写入 observation_data（替换现有插值数据）
3. 端到端测试定时任务（SystemDateTask → DataSyncTask）
