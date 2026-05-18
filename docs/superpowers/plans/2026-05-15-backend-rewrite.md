# Backend Rewrite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rewrite Spring Boot backend based on 12 redesigned database tables, decoupled by domain with independent controllers/services/mappers per module.

**Architecture:** Layered Spring Boot 3.4.1 + MyBatis-Plus 3.5.7 with strict domain separation. Each domain (Observation, Forecast, Model, ModelVersion, Station, Alert, Health) gets its own Controller → Service → Mapper chain. User module kept as-is. Old entities/mappers/services retained for backward compatibility until verified.

**Tech Stack:** Java 21, Spring Boot 3.4.1, MyBatis-Plus 3.5.7, MySQL 8.x, Lombok

---

## File Structure Map

```
ocean-server/src/main/java/com/ocean/
├── entity/
│   ├── ObservationData.java          (NEW - observation_data)
│   ├── ObservationGridCache.java     (NEW - observation_grid_cache)
│   ├── ForecastGrid.java             (NEW - forecast_grid)
│   ├── MonitoringStation.java        (NEW - monitoring_station)
│   ├── Model.java                    (NEW - model)
│   ├── ModelVersion.java             (NEW - model_version)
│   ├── AlertRule.java                (NEW - alert_rule)
│   ├── AlertEvent.java               (NEW - alert_event)
│   ├── AlertStationDetail.java       (NEW - alert_station_detail)
│   ├── HealthZone.java               (NEW - health_zone)
│   ├── HealthRecord.java             (NEW - health_record)
├── mapper/
│   ├── ObservationDataMapper.java    (NEW)
│   ├── ObservationGridCacheMapper.java (NEW)
│   ├── ForecastGridMapper.java       (NEW)
│   ├── MonitoringStationMapper.java  (NEW)
│   ├── ModelMapper.java              (NEW)
│   ├── ModelVersionMapper.java       (NEW)
│   ├── AlertRuleMapper.java          (NEW)
│   ├── AlertEventMapper.java         (NEW)
│   ├── AlertStationDetailMapper.java (NEW)
│   ├── HealthZoneMapper.java         (NEW)
│   ├── HealthRecordMapper.java       (NEW)
├── dto/
│   ├── ForecastQueryDTO.java         (NEW)
│   ├── MapGridQueryDTO.java          (NEW)
│   ├── StationSaveDTO.java           (NEW)
│   ├── AlertRuleSaveDTO.java         (NEW)
│   ├── ZoneHealthQueryDTO.java       (NEW)
│   ├── HealthAssessmentDTO.java      (NEW)
├── vo/
│   ├── DashboardVO.java              (NEW)
│   ├── ForecastGridVO.java           (NEW)
│   ├── StationVO.java                (NEW)
│   ├── AlertEventVO.java             (NEW)
│   ├── AlertStationDetailVO.java     (NEW)
│   ├── ZoneHealthVO.java             (NEW)
│   ├── ModelVO.java                  (MODIFY - add version stats)
│   ├── ModelVersionVO.java           (MODIFY - updated fields)
├── service/
│   ├── ObservationService.java       (NEW interface)
│   ├── ForecastService.java          (NEW interface)
│   ├── ModelService.java             (NEW interface)
│   ├── ModelVersionService.java      (NEW interface)
│   ├── StationService.java           (NEW interface)
│   ├── AlertService.java             (NEW interface)
│   ├── HealthService.java            (NEW interface)
├── service/impl/
│   ├── ObservationServiceImpl.java   (NEW)
│   ├── ForecastServiceImpl.java      (NEW)
│   ├── ModelServiceImpl.java         (NEW)
│   ├── ModelVersionServiceImpl.java  (NEW)
│   ├── StationServiceImpl.java       (NEW)
│   ├── AlertServiceImpl.java         (NEW)
│   ├── HealthServiceImpl.java        (NEW)
├── controller/
│   ├── ObservationController.java    (NEW - /api/observation)
│   ├── ForecastController.java       (NEW - /api/forecast)
│   ├── ModelController.java          (NEW - /api/model)
│   ├── ModelVersionController.java   (NEW - /api/model/{modelId}/version)
│   ├── StationController.java        (NEW - /api/station)
│   ├── AlertController.java          (NEW - /api/alert)
│   ├── HealthController.java         (NEW - /api/health)
├── task/
│   ├── AlertGenerationTask.java      (NEW - scheduled alert generation)
│   ├── HealthAssessmentTask.java     (NEW - scheduled health assessment)
```

---

## Phase 0: Test Data Preparation

### Task 0: Insert test data into database

**Files:**
- Create: `database/test-data-backend.sql`

- [ ] **Step 1: Write test data SQL**

```sql
-- ============================================================
-- Test data for backend rewrite verification
-- ============================================================

-- 1. Insert models
INSERT INTO model (model_name, model_type, description) VALUES
('SST 海表温度预报模型', 'SST', '基于ROMS+WRF的东海SST预报'),
('CHL 叶绿素浓度预报模型', 'CHL', '基于ROMS+NPCZ的东海CHL预报');

-- 2. Insert model versions
INSERT INTO model_version (model_id, version_label, params_config, cron_expression, data_source, data_time_range, change_note, status) VALUES
(1, 'v1', '{"layers":3,"hidden":64}', '0 6 * * *', 'HYCOM+NCEP GDAS', '2024-01-01 ~ 2025-12-31', '初始版本', 'RUNNING'),
(2, 'v1', '{"layers":3,"hidden":64}', '0 6 * * *', 'HYCOM+NCEP GDAS', '2024-01-01 ~ 2025-12-31', '初始版本', 'RUNNING');

-- 3. Insert forecast grid data (SST, 2026-05-15, ~391 points, 0.25° resolution)
-- East China Sea: lon 121.33-125.58, lat 26.92-32.67
INSERT INTO forecast_grid (model_id, version_id, variable, forecast_date, depth, lat, lon, value, unit)
SELECT 1, 1, 'sst', '2026-05-15', 0,
       ROUND(26.75 + 0.25 * lat_idx, 6),
       ROUND(121.25 + 0.25 * lon_idx, 6),
       ROUND(18.0 + RAND(42+lat_idx*100+lon_idx) * 10.0, 3),
       'degree_C'
FROM (SELECT 0 AS lat_idx UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23) AS lats
CROSS JOIN (SELECT 0 AS lon_idx UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17) AS lons
WHERE ROUND(26.75 + 0.25 * lat_idx, 6) BETWEEN 26.92 AND 32.67
  AND ROUND(121.25 + 0.25 * lon_idx, 6) BETWEEN 121.33 AND 125.58;

-- CHL forecast for same date
INSERT INTO forecast_grid (model_id, version_id, variable, forecast_date, depth, lat, lon, value, unit)
SELECT 2, 2, 'chl', '2026-05-15', 0,
       ROUND(26.75 + 0.25 * lat_idx, 6),
       ROUND(121.25 + 0.25 * lon_idx, 6),
       ROUND(0.5 + RAND(100+lat_idx*100+lon_idx) * 6.0, 3),
       'mg_m3'
FROM (SELECT 0 AS lat_idx UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23) AS lats
CROSS JOIN (SELECT 0 AS lon_idx UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17) AS lons
WHERE ROUND(26.75 + 0.25 * lat_idx, 6) BETWEEN 26.92 AND 32.67
  AND ROUND(121.25 + 0.25 * lon_idx, 6) BETWEEN 121.33 AND 125.58;

-- Also insert for a few more recent days for trends
INSERT INTO forecast_grid (model_id, version_id, variable, forecast_date, depth, lat, lon, value, unit)
SELECT 1, 1, 'sst', DATE_ADD('2026-05-15', INTERVAL -d DAY), 0,
       ROUND(26.75 + 0.25 * FLOOR(RAND(200+d) * 24), 6),
       ROUND(121.25 + 0.25 * FLOOR(RAND(300+d) * 18), 6),
       ROUND(18.0 + RAND(500+d) * 10.0, 3),
       'degree_C'
FROM (SELECT 0 AS d UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) AS days;

INSERT INTO forecast_grid (model_id, version_id, variable, forecast_date, depth, lat, lon, value, unit)
SELECT 2, 2, 'chl', DATE_ADD('2026-05-15', INTERVAL -d DAY), 0,
       ROUND(26.75 + 0.25 * FLOOR(RAND(200+d) * 24), 6),
       ROUND(121.25 + 0.25 * FLOOR(RAND(300+d) * 18), 6),
       ROUND(0.5 + RAND(500+d) * 6.0, 3),
       'mg_m3'
FROM (SELECT 0 AS d UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) AS days;

-- 4. Insert monitoring stations
INSERT INTO monitoring_station (station_name, lat, lon, distance, region, health_zone_id) VALUES
('舟山近岸站', 29.9875, 122.2075, '近岸', '北部', 1),
('宁波近岸站', 29.8625, 121.5725, '近岸', '北部', 1),
('长江口站', 31.2500, 122.0000, '近海', '北部', 1),
('舟山东部站', 29.7500, 123.5000, '远海', '北部', 2),
('温州近岸站', 27.8350, 121.1250, '近岸', '南部', 3),
('钓鱼岛西站', 27.0000, 124.0000, '远海', '南部', 4);

-- 5. Insert observation data samples
INSERT INTO observation_data (variable, obs_time, depth, lat, lon, value) VALUES
('thetao', '2026-05-15', 0, 29.9875, 122.2075, 23.5),
('thetao', '2026-05-14', 0, 29.9875, 122.2075, 23.2),
('thetao', '2026-05-13', 0, 29.9875, 122.2075, 22.8),
('chl', '2026-05-15', 0, 29.9875, 122.2075, 2.1),
('chl', '2026-05-14', 0, 29.9875, 122.2075, 1.9),
('so', '2026-05-15', 0, 29.9875, 122.2075, 32.5),
('thetao', '2026-05-15', 10, 29.9875, 122.2075, 22.0),
('thetao', '2026-05-15', 20, 29.9875, 122.2075, 20.5);

-- 6. Populate grid cache
INSERT INTO observation_grid_cache (lat, lon)
SELECT DISTINCT ROUND(lat, 6), ROUND(lon, 6) FROM forecast_grid;

-- 7. Health records for today
INSERT INTO health_record (zone_id, assess_date, sst_avg, sst_max, sst_anomaly, sst_trend, chl_avg, chl_max, chl_trend, heatwave_active, heatwave_days, sst_grade, chl_grade, heatwave_grade, overall_grade) VALUES
(1, '2026-05-15', 21.5, 23.8, 0.8, 'stable', 2.1, 3.5, 'stable', 0, 0, 'fine', 'fine', 'good', 'fine'),
(2, '2026-05-15', 22.3, 24.5, 1.2, 'rising', 1.5, 2.8, 'stable', 0, 0, 'warn', 'good', 'good', 'warn'),
(3, '2026-05-15', 23.1, 25.0, 0.5, 'stable', 2.8, 4.2, 'rising', 0, 0, 'fine', 'fine', 'good', 'fine'),
(4, '2026-05-15', 23.8, 26.2, 1.8, 'rising', 1.2, 2.0, 'stable', 0, 0, 'warn', 'good', 'good', 'warn');

-- 8. Sample alert event
INSERT INTO alert_event (zone_id, rule_id, variable, source, alert_date, max_value, avg_value, threshold, station_count, severity, status, message) VALUES
(1, 2, 'chl', 'forecast', '2026-05-15', 3.5, 2.1, 5.0, 0, 'warning', 'active', '北部近岸 CHL 接近阈值，关注赤潮风险');
```

- [ ] **Step 2: Execute test data SQL**

Run: `mysql -u root -pyour_password --default-character-set=utf8mb4 ocean_forecast < database/test-data-backend.sql 2>&1`

Expected: no errors

- [ ] **Step 3: Verify data counts**

```sql
SELECT 'forecast_grid' AS tbl, COUNT(*) FROM forecast_grid
UNION ALL SELECT 'monitoring_station', COUNT(*) FROM monitoring_station
UNION ALL SELECT 'model', COUNT(*) FROM model
UNION ALL SELECT 'model_version', COUNT(*) FROM model_version
UNION ALL SELECT 'health_record', COUNT(*) FROM health_record
UNION ALL SELECT 'alert_event', COUNT(*) FROM alert_event
UNION ALL SELECT 'observation_data', COUNT(*) FROM observation_data;
```

---

## Phase 1: Entity Layer (all entities can be created in parallel)

### Task 1.1: Create ObservationData entity

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/entity/ObservationData.java`

- [ ] **Step 1: Write entity**

```java
package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("observation_data")
public class ObservationData {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String variable;
    private LocalDate obsTime;
    private Double depth;
    private Double lat;
    private Double lon;
    private Double value;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

### Task 1.2: Create ObservationGridCache entity

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/entity/ObservationGridCache.java`

- [ ] **Step 1: Write entity**

```java
package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("observation_grid_cache")
public class ObservationGridCache {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Double lat;
    private Double lon;
}
```

### Task 1.3: Create ForecastGrid entity

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/entity/ForecastGrid.java`

- [ ] **Step 1: Write entity**

```java
package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("forecast_grid")
public class ForecastGrid {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long modelId;
    private Long versionId;
    private String variable;
    private LocalDate forecastDate;
    private Double depth;
    private Double lat;
    private Double lon;
    private Double value;
    private String unit;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

### Task 1.4: Create MonitoringStation entity

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/entity/MonitoringStation.java`

- [ ] **Step 1: Write entity**

```java
package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("monitoring_station")
public class MonitoringStation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String stationName;
    private Double lat;
    private Double lon;
    private String distance;
    private String region;
    private Long healthZoneId;
    private Integer isActive;
    private Integer sortOrder;
}
```

### Task 1.5: Create Model entity

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/entity/Model.java`

- [ ] **Step 1: Write entity**

```java
package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("model")
public class Model {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String modelName;
    private String modelType;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

### Task 1.6: Create ModelVersion entity

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/entity/ModelVersion.java`

- [ ] **Step 1: Write entity**

```java
package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("model_version")
public class ModelVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long modelId;
    private String versionLabel;
    private String paramsConfig;
    private String cronExpression;
    private String dataSource;
    private String dataTimeRange;
    private String changeNote;
    private String status;
    private LocalDateTime lastRunTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

### Task 1.7: Create AlertRule entity

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/entity/AlertRule.java`

- [ ] **Step 1: Write entity**

```java
package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("alert_rule")
public class AlertRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleName;
    private String variable;
    private String source;
    private String operator;
    private Double threshold;
    private String severity;
    private Integer isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

### Task 1.8: Create AlertEvent entity

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/entity/AlertEvent.java`

- [ ] **Step 1: Write entity**

```java
package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("alert_event")
public class AlertEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long zoneId;
    private Long ruleId;
    private String variable;
    private String source;
    private LocalDate alertDate;
    private Double maxValue;
    private Double avgValue;
    private Double threshold;
    private Integer stationCount;
    private String severity;
    private String status;
    private Long ackBy;
    private LocalDateTime ackAt;
    private String message;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

### Task 1.9: Create AlertStationDetail entity

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/entity/AlertStationDetail.java`

- [ ] **Step 1: Write entity**

```java
package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("alert_station_detail")
public class AlertStationDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long alertId;
    private Long stationId;
    private Double actualValue;
    private Double threshold;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

### Task 1.10: Create HealthZone entity

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/entity/HealthZone.java`

- [ ] **Step 1: Write entity**

```java
package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("health_zone")
public class HealthZone {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String zoneName;
    private Double minLon;
    private Double maxLon;
    private Double minLat;
    private Double maxLat;
    private Integer sortOrder;
    private Integer isActive;
}
```

### Task 1.11: Create HealthRecord entity

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/entity/HealthRecord.java`

- [ ] **Step 1: Write entity**

```java
package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_record")
public class HealthRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long zoneId;
    private LocalDate assessDate;
    private Double sstAvg;
    private Double sstMax;
    private Double sstAnomaly;
    private String sstTrend;
    private Double chlAvg;
    private Double chlMax;
    private String chlTrend;
    private Integer heatwaveActive;
    private Integer heatwaveDays;
    private String sstGrade;
    private String chlGrade;
    private String heatwaveGrade;
    private String overallGrade;
    private String suggestions;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

---

## Phase 2: Mapper Layer (all mappers can be created in parallel after entities)

### Task 2.1-2.11: Create all Mappers

Each mapper follows this pattern:

```java
package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.entity.Xxx;

public interface XxxMapper extends BaseMapper<Xxx> {
}
```

Mappers to create:
- `ocean-server/src/main/java/com/ocean/mapper/ObservationDataMapper.java`
- `ocean-server/src/main/java/com/ocean/mapper/ObservationGridCacheMapper.java`
- `ocean-server/src/main/java/com/ocean/mapper/ForecastGridMapper.java`
- `ocean-server/src/main/java/com/ocean/mapper/MonitoringStationMapper.java`
- `ocean-server/src/main/java/com/ocean/mapper/ModelMapper.java`
- `ocean-server/src/main/java/com/ocean/mapper/ModelVersionMapper.java`
- `ocean-server/src/main/java/com/ocean/mapper/AlertRuleMapper.java`
- `ocean-server/src/main/java/com/ocean/mapper/AlertEventMapper.java`
- `ocean-server/src/main/java/com/ocean/mapper/AlertStationDetailMapper.java`
- `ocean-server/src/main/java/com/ocean/mapper/HealthZoneMapper.java`
- `ocean-server/src/main/java/com/ocean/mapper/HealthRecordMapper.java`

---

## Phase 3: DTOs, VOs, and Service Layer (domain by domain)

### Task 3.1: Create ForecastQueryDTO and MapGridQueryDTO

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/dto/ForecastQueryDTO.java`
- Create: `ocean-server/src/main/java/com/ocean/dto/MapGridQueryDTO.java`

```java
package com.ocean.dto;

import lombok.Data;

@Data
public class ForecastQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String dataType;
    private String forecastDate;
    private Double minLon;
    private Double maxLon;
    private Double minLat;
    private Double maxLat;
}
```

```java
package com.ocean.dto;

import lombok.Data;

@Data
public class MapGridQueryDTO {
    private String dataType;
    private String forecastDate;
    private Double precision;
    private Double minLon;
    private Double maxLon;
    private Double minLat;
    private Double maxLat;
}
```

### Task 3.2: Create DashboardVO and other VOs

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/vo/DashboardVO.java`
- Create: `ocean-server/src/main/java/com/ocean/vo/StationVO.java`
- Create: `ocean-server/src/main/java/com/ocean/vo/AlertEventVO.java`
- Create: `ocean-server/src/main/java/com/ocean/vo/AlertStationDetailVO.java`
- Create: `ocean-server/src/main/java/com/ocean/vo/ZoneHealthVO.java`

```java
// DashboardVO.java
package com.ocean.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardVO {
    private Long modelCount;
    private Long runningModelCount;
    private Long todayRecordCount;
    private Long alertCount;
    private List<Map<String, Object>> latestSstData;
    private List<Map<String, Object>> latestChlData;
}
```

```java
// StationVO.java
package com.ocean.vo;

import lombok.Data;

@Data
public class StationVO {
    private Long id;
    private String stationName;
    private Double lat;
    private Double lon;
    private String distance;
    private String region;
    private Long healthZoneId;
    private String zoneName;
    private Integer isActive;
    private Integer sortOrder;
}
```

```java
// AlertEventVO.java
package com.ocean.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AlertEventVO {
    private Long id;
    private Long zoneId;
    private String zoneName;
    private Long ruleId;
    private String ruleName;
    private String variable;
    private String source;
    private LocalDate alertDate;
    private Double maxValue;
    private Double avgValue;
    private Double threshold;
    private Integer stationCount;
    private String severity;
    private String status;
    private String message;
    private LocalDateTime createTime;
}
```

```java
// AlertStationDetailVO.java
package com.ocean.vo;

import lombok.Data;

@Data
public class AlertStationDetailVO {
    private Long id;
    private Long alertId;
    private Long stationId;
    private String stationName;
    private Double lat;
    private Double lon;
    private Double actualValue;
    private Double threshold;
}
```

```java
// ZoneHealthVO.java
package com.ocean.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ZoneHealthVO {
    private List<Map<String, Object>> zones;
    private String overallGrade;
    private String summary;
}
```

### Task 3.3: Create ObservationService and Impl

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/service/ObservationService.java`
- Create: `ocean-server/src/main/java/com/ocean/service/impl/ObservationServiceImpl.java`

```java
// ObservationService.java
package com.ocean.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.dto.OceanDataQueryDTO;
import com.ocean.vo.OceanDataVO;
import java.util.List;
import java.util.Map;

public interface ObservationService {
    IPage<OceanDataVO> getDataPage(OceanDataQueryDTO dto);
    List<Map<String, Object>> getSstTimeSeries(String startDate, String endDate, Double lat, Double lon);
    List<Map<String, Object>> getChlTimeSeries(String startDate, String endDate, Double lat, Double lon);
    List<Map<String, Object>> getChlByDepth();
    List<Map<String, Object>> getDistinctLocations();
}
```

```java
// ObservationServiceImpl.java
package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.dto.OceanDataQueryDTO;
import com.ocean.entity.ObservationData;
import com.ocean.mapper.ObservationDataMapper;
import com.ocean.service.ObservationService;
import com.ocean.vo.OceanDataVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ObservationServiceImpl implements ObservationService {

    @Autowired
    private ObservationDataMapper observationDataMapper;

    @Override
    public IPage<OceanDataVO> getDataPage(OceanDataQueryDTO dto) {
        Page<ObservationData> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ObservationData> wrapper = new LambdaQueryWrapper<>();
        if (dto.getVariable() != null && !dto.getVariable().isEmpty()) {
            wrapper.eq(ObservationData::getVariable, dto.getVariable());
        }
        if (dto.getLon() != null && !dto.getLon().isEmpty()) {
            wrapper.eq(ObservationData::getLon, Double.parseDouble(dto.getLon()));
        }
        if (dto.getLat() != null && !dto.getLat().isEmpty()) {
            wrapper.eq(ObservationData::getLat, Double.parseDouble(dto.getLat()));
        }
        if (dto.getStartDate() != null && !dto.getStartDate().isEmpty()) {
            wrapper.ge(ObservationData::getObsTime, dto.getStartDate());
        }
        if (dto.getEndDate() != null && !dto.getEndDate().isEmpty()) {
            wrapper.le(ObservationData::getObsTime, dto.getEndDate());
        }
        wrapper.orderByDesc(ObservationData::getObsTime).orderByAsc(ObservationData::getDepth);
        IPage<ObservationData> dataPage = observationDataMapper.selectPage(page, wrapper);
        return dataPage.convert(this::toVO);
    }

    @Override
    public List<Map<String, Object>> getSstTimeSeries(String startDate, String endDate, Double lat, Double lon) {
        return observationDataMapper.selectSstTimeSeries(startDate, endDate, lat, lon);
    }

    @Override
    public List<Map<String, Object>> getChlTimeSeries(String startDate, String endDate, Double lat, Double lon) {
        return observationDataMapper.selectChlTimeSeries(startDate, endDate, lat, lon);
    }

    @Override
    public List<Map<String, Object>> getChlByDepth() {
        return observationDataMapper.selectChlByDepth();
    }

    @Override
    public List<Map<String, Object>> getDistinctLocations() {
        return observationDataMapper.selectDistinctLocations();
    }

    private OceanDataVO toVO(ObservationData data) {
        OceanDataVO vo = new OceanDataVO();
        BeanUtils.copyProperties(data, vo);
        return vo;
    }
}
```

### Task 3.4: Create ForecastService and Impl

This is the most complex service. It handles dashboard stats, map grid queries, trends, and CHL probability.

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/service/ForecastService.java`
- Create: `ocean-server/src/main/java/com/ocean/service/impl/ForecastServiceImpl.java`

```java
// ForecastService.java
package com.ocean.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.dto.ForecastQueryDTO;
import com.ocean.dto.MapGridQueryDTO;
import com.ocean.vo.DashboardVO;
import com.ocean.vo.ForecastVO;
import java.util.List;
import java.util.Map;

public interface ForecastService {
    DashboardVO getDashboard();
    IPage<ForecastVO> getRecordPage(ForecastQueryDTO dto);
    List<Map<String, Object>> getSstTrend(Double lon, Double lat);
    List<Map<String, Object>> getChlTrend(Double lon, Double lat);
    List<Map<String, Object>> getLocations();
    List<Map<String, Object>> getMapGrid(MapGridQueryDTO dto);
    List<Map<String, Object>> getPointTrend(String dataType, Double lon, Double lat, String dateStart, String dateEnd);
    List<Map<String, Object>> getDashboardTrend(String dataType, Integer days);
    List<Map<String, Object>> getChlProbability(String forecastDate, String dateEnd, Double threshold);
    List<Map<String, Object>> getSeaAreas();
}
```

```java
// ForecastServiceImpl.java
package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.dto.ForecastQueryDTO;
import com.ocean.dto.MapGridQueryDTO;
import com.ocean.entity.*;
import com.ocean.mapper.*;
import com.ocean.service.ForecastService;
import com.ocean.vo.DashboardVO;
import com.ocean.vo.ForecastVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class ForecastServiceImpl implements ForecastService {

    @Autowired
    private ForecastGridMapper forecastGridMapper;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ModelVersionMapper modelVersionMapper;
    @Autowired
    private AlertEventMapper alertEventMapper;
    @Autowired
    private HealthZoneMapper healthZoneMapper;

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setModelCount(modelMapper.selectCount(null));
        vo.setRunningModelCount(modelVersionMapper.selectCount(
                new LambdaQueryWrapper<ModelVersion>().eq(ModelVersion::getStatus, "RUNNING")));
        vo.setTodayRecordCount(forecastGridMapper.selectCount(
                new LambdaQueryWrapper<ForecastGrid>().eq(ForecastGrid::getForecastDate, LocalDate.now())));
        vo.setAlertCount(alertEventMapper.selectCount(
                new LambdaQueryWrapper<AlertEvent>().eq(AlertEvent::getStatus, "active")));
        vo.setLatestSstData(getLatestGridData("sst", 5));
        vo.setLatestChlData(getLatestGridData("chl", 5));
        return vo;
    }

    private List<Map<String, Object>> getLatestGridData(String variable, int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        LambdaQueryWrapper<ForecastGrid> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForecastGrid::getVariable, variable)
               .eq(ForecastGrid::getDepth, 0)
               .orderByDesc(ForecastGrid::getForecastDate)
               .last("LIMIT " + limit);
        List<ForecastGrid> list = forecastGridMapper.selectList(wrapper);
        for (ForecastGrid g : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("lat", g.getLat());
            m.put("lon", g.getLon());
            m.put("value", g.getValue());
            m.put("forecastDate", g.getForecastDate().toString());
            result.add(m);
        }
        return result;
    }

    @Override
    public IPage<ForecastVO> getRecordPage(ForecastQueryDTO dto) {
        Page<ForecastGrid> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ForecastGrid> wrapper = new LambdaQueryWrapper<>();
        if (dto.getDataType() != null) wrapper.eq(ForecastGrid::getVariable, dto.getDataType().toLowerCase());
        if (dto.getForecastDate() != null) wrapper.eq(ForecastGrid::getForecastDate, dto.getForecastDate());
        wrapper.orderByDesc(ForecastGrid::getForecastDate);
        IPage<ForecastGrid> dataPage = forecastGridMapper.selectPage(page, wrapper);
        return dataPage.convert(this::toForecastVO);
    }

    private ForecastVO toForecastVO(ForecastGrid g) {
        ForecastVO vo = new ForecastVO();
        vo.setId(g.getId());
        vo.setDataType(g.getVariable().toUpperCase());
        vo.setForecastDate(g.getForecastDate());
        vo.setLongitude(java.math.BigDecimal.valueOf(g.getLon()));
        vo.setLatitude(java.math.BigDecimal.valueOf(g.getLat()));
        vo.setValue(g.getValue());
        vo.setUnit(g.getUnit());
        return vo;
    }

    @Override
    public List<Map<String, Object>> getSstTrend(Double lon, Double lat) {
        return forecastGridMapper.selectTrend("sst", lon, lat);
    }

    @Override
    public List<Map<String, Object>> getChlTrend(Double lon, Double lat) {
        return forecastGridMapper.selectTrend("chl", lon, lat);
    }

    @Override
    public List<Map<String, Object>> getLocations() {
        return forecastGridMapper.selectDistinctLocations();
    }

    @Override
    public List<Map<String, Object>> getMapGrid(MapGridQueryDTO dto) {
        return forecastGridMapper.selectMapGrid(
                dto.getDataType() != null ? dto.getDataType().toLowerCase() : "sst",
                dto.getForecastDate(),
                dto.getMinLon(), dto.getMaxLon(),
                dto.getMinLat(), dto.getMaxLat());
    }

    @Override
    public List<Map<String, Object>> getPointTrend(String dataType, Double lon, Double lat, String dateStart, String dateEnd) {
        return forecastGridMapper.selectPointTrend(dataType.toLowerCase(), lon, lat, dateStart, dateEnd);
    }

    @Override
    public List<Map<String, Object>> getDashboardTrend(String dataType, Integer days) {
        return forecastGridMapper.selectDashboardTrend(dataType.toLowerCase(), days);
    }

    @Override
    public List<Map<String, Object>> getChlProbability(String dateStart, String dateEnd, Double threshold) {
        return forecastGridMapper.selectChlProbability(dateStart, dateEnd, threshold != null ? threshold : 5.0);
    }

    @Override
    public List<Map<String, Object>> getSeaAreas() {
        List<Map<String, Object>> areas = new ArrayList<>();
        List<HealthZone> zones = healthZoneMapper.selectList(
                new LambdaQueryWrapper<HealthZone>().eq(HealthZone::getIsActive, 1));
        for (HealthZone zone : zones) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", zone.getZoneName());
            m.put("minLon", zone.getMinLon());
            m.put("maxLon", zone.getMaxLon());
            m.put("minLat", zone.getMinLat());
            m.put("maxLat", zone.getMaxLat());
            areas.add(m);
        }
        return areas;
    }
}
```

### Task 3.5: Create ModelService + ModelVersionService and Impls

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/service/ModelService.java`
- Create: `ocean-server/src/main/java/com/ocean/service/impl/ModelServiceImpl.java`
- Create: `ocean-server/src/main/java/com/ocean/service/ModelVersionService.java`
- Create: `ocean-server/src/main/java/com/ocean/service/impl/ModelVersionServiceImpl.java`

```java
// ModelService.java
package com.ocean.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.dto.ModelSaveDTO;
import com.ocean.vo.ModelVO;
import com.ocean.vo.RunningVersionVO;
import java.util.List;

public interface ModelService {
    IPage<ModelVO> getModelPage(Integer pageNum, Integer pageSize, String modelType, String keyword);
    ModelVO getModelById(Long id);
    void addModel(ModelSaveDTO dto);
    void updateModel(ModelSaveDTO dto);
    void deleteModel(Long id);
    List<RunningVersionVO> getRunningVersions();
}
```

```java
// ModelServiceImpl.java
package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.common.BusinessException;
import com.ocean.dto.ModelSaveDTO;
import com.ocean.entity.Model;
import com.ocean.entity.ModelVersion;
import com.ocean.mapper.ModelMapper;
import com.ocean.mapper.ModelVersionMapper;
import com.ocean.service.ModelService;
import com.ocean.vo.ModelVO;
import com.ocean.vo.RunningVersionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ModelServiceImpl implements ModelService {

    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ModelVersionMapper modelVersionMapper;

    @Override
    public IPage<ModelVO> getModelPage(Integer pageNum, Integer pageSize, String modelType, String keyword) {
        Page<Model> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        if (modelType != null && !modelType.isEmpty()) wrapper.eq(Model::getModelType, modelType);
        if (keyword != null && !keyword.isEmpty()) wrapper.like(Model::getModelName, keyword);
        wrapper.orderByDesc(Model::getCreateTime);
        IPage<Model> modelPage = modelMapper.selectPage(page, wrapper);
        return modelPage.convert(this::toVO);
    }

    @Override
    public ModelVO getModelById(Long id) {
        Model model = modelMapper.selectById(id);
        if (model == null) throw new BusinessException("模型不存在");
        return toVO(model);
    }

    @Override
    public void addModel(ModelSaveDTO dto) {
        Model model = new Model();
        BeanUtils.copyProperties(dto, model);
        modelMapper.insert(model);
    }

    @Override
    public void updateModel(ModelSaveDTO dto) {
        Model model = modelMapper.selectById(dto.getId());
        if (model == null) throw new BusinessException("模型不存在");
        BeanUtils.copyProperties(dto, model);
        modelMapper.updateById(model);
    }

    @Override
    public void deleteModel(Long id) {
        Model model = modelMapper.selectById(id);
        if (model == null) throw new BusinessException("模型不存在");
        LambdaQueryWrapper<ModelVersion> runningWrapper = new LambdaQueryWrapper<>();
        runningWrapper.eq(ModelVersion::getModelId, id).eq(ModelVersion::getStatus, "RUNNING");
        if (modelVersionMapper.selectCount(runningWrapper) > 0) {
            throw new BusinessException("模型下存在运行中的版本，请先停止后再删除");
        }
        modelVersionMapper.delete(new LambdaQueryWrapper<ModelVersion>().eq(ModelVersion::getModelId, id));
        modelMapper.deleteById(id);
    }

    @Override
    public List<RunningVersionVO> getRunningVersions() {
        LambdaQueryWrapper<ModelVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelVersion::getStatus, "RUNNING");
        List<ModelVersion> runningVersions = modelVersionMapper.selectList(wrapper);
        List<RunningVersionVO> result = new ArrayList<>();
        for (ModelVersion mv : runningVersions) {
            RunningVersionVO vo = new RunningVersionVO();
            vo.setVersionId(mv.getId());
            vo.setModelId(mv.getModelId());
            vo.setVersionLabel(mv.getVersionLabel());
            Model model = modelMapper.selectById(mv.getModelId());
            vo.setModelName(model != null ? model.getModelName() : "");
            result.add(vo);
        }
        return result;
    }

    private ModelVO toVO(Model model) {
        ModelVO vo = new ModelVO();
        BeanUtils.copyProperties(model, vo);
        LambdaQueryWrapper<ModelVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelVersion::getModelId, model.getId());
        vo.setVersionCount(modelVersionMapper.selectCount(wrapper));
        wrapper.eq(ModelVersion::getStatus, "RUNNING");
        vo.setRunningCount(modelVersionMapper.selectCount(wrapper));
        return vo;
    }
}
```

```java
// ModelVersionService.java
package com.ocean.service;

import com.ocean.dto.ModelVersionSaveDTO;
import com.ocean.vo.ModelVersionVO;
import java.util.List;

public interface ModelVersionService {
    List<ModelVersionVO> getVersionsByModelId(Long modelId);
    void addVersion(Long modelId, ModelVersionSaveDTO dto);
    void updateVersion(Long modelId, Long versionId, ModelVersionSaveDTO dto);
    void deleteVersion(Long modelId, Long versionId);
    void toggleStatus(Long modelId, Long versionId, String status);
}
```

```java
// ModelVersionServiceImpl.java
package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.common.BusinessException;
import com.ocean.dto.ModelVersionSaveDTO;
import com.ocean.entity.Model;
import com.ocean.entity.ModelVersion;
import com.ocean.mapper.ModelMapper;
import com.ocean.mapper.ModelVersionMapper;
import com.ocean.service.ModelVersionService;
import com.ocean.vo.ModelVersionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModelVersionServiceImpl implements ModelVersionService {

    @Autowired
    private ModelVersionMapper modelVersionMapper;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<ModelVersionVO> getVersionsByModelId(Long modelId) {
        LambdaQueryWrapper<ModelVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelVersion::getModelId, modelId).orderByDesc(ModelVersion::getCreateTime);
        return modelVersionMapper.selectList(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public void addVersion(Long modelId, ModelVersionSaveDTO dto) {
        Model model = modelMapper.selectById(modelId);
        if (model == null) throw new BusinessException("模型不存在");
        ModelVersion mv = new ModelVersion();
        mv.setModelId(modelId);
        mv.setVersionLabel(dto.getVersionLabel());
        mv.setParamsConfig(dto.getParamsConfig());
        mv.setCronExpression(dto.getCronExpression());
        mv.setDataSource(dto.getDataSource());
        mv.setDataTimeRange(dto.getDataTimeRange());
        mv.setChangeNote(dto.getChangeNote());
        mv.setStatus("STOPPED");
        modelVersionMapper.insert(mv);
    }

    @Override
    public void updateVersion(Long modelId, Long versionId, ModelVersionSaveDTO dto) {
        ModelVersion mv = modelVersionMapper.selectById(versionId);
        if (mv == null || !mv.getModelId().equals(modelId)) throw new BusinessException("版本不存在");
        mv.setVersionLabel(dto.getVersionLabel());
        mv.setCronExpression(dto.getCronExpression());
        mv.setParamsConfig(dto.getParamsConfig());
        mv.setDataSource(dto.getDataSource());
        mv.setDataTimeRange(dto.getDataTimeRange());
        mv.setChangeNote(dto.getChangeNote());
        modelVersionMapper.updateById(mv);
    }

    @Override
    public void deleteVersion(Long modelId, Long versionId) {
        ModelVersion mv = modelVersionMapper.selectById(versionId);
        if (mv == null || !mv.getModelId().equals(modelId)) throw new BusinessException("版本不存在");
        if ("RUNNING".equals(mv.getStatus())) throw new BusinessException("运行中的版本无法删除");
        modelVersionMapper.deleteById(versionId);
    }

    @Override
    public void toggleStatus(Long modelId, Long versionId, String status) {
        ModelVersion mv = modelVersionMapper.selectById(versionId);
        if (mv == null || !mv.getModelId().equals(modelId)) throw new BusinessException("版本不存在");
        mv.setStatus(status);
        if ("RUNNING".equals(status)) mv.setLastRunTime(LocalDateTime.now());
        modelVersionMapper.updateById(mv);
    }

    private ModelVersionVO toVO(ModelVersion mv) {
        ModelVersionVO vo = new ModelVersionVO();
        BeanUtils.copyProperties(mv, vo);
        return vo;
    }
}
```

### Task 3.6: Create StationService and Impl

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/service/StationService.java`
- Create: `ocean-server/src/main/java/com/ocean/service/impl/StationServiceImpl.java`

```java
// StationService.java
package com.ocean.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.dto.StationSaveDTO;
import com.ocean.vo.StationVO;
import java.util.List;

public interface StationService {
    IPage<StationVO> getStationPage(Integer pageNum, Integer pageSize, String distance, String region);
    StationVO getStationById(Long id);
    List<StationVO> getStationsByZoneId(Long zoneId);
    void addStation(StationSaveDTO dto);
    void updateStation(StationSaveDTO dto);
    void deleteStation(Long id);
}
```

```java
// StationServiceImpl.java
package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.common.BusinessException;
import com.ocean.dto.StationSaveDTO;
import com.ocean.entity.HealthZone;
import com.ocean.entity.MonitoringStation;
import com.ocean.mapper.HealthZoneMapper;
import com.ocean.mapper.MonitoringStationMapper;
import com.ocean.service.StationService;
import com.ocean.vo.StationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StationServiceImpl implements StationService {

    @Autowired
    private MonitoringStationMapper stationMapper;
    @Autowired
    private HealthZoneMapper healthZoneMapper;

    @Override
    public IPage<StationVO> getStationPage(Integer pageNum, Integer pageSize, String distance, String region) {
        Page<MonitoringStation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MonitoringStation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MonitoringStation::getIsActive, 1);
        if (distance != null) wrapper.eq(MonitoringStation::getDistance, distance);
        if (region != null) wrapper.eq(MonitoringStation::getRegion, region);
        wrapper.orderByAsc(MonitoringStation::getSortOrder);
        IPage<MonitoringStation> result = stationMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    @Override
    public StationVO getStationById(Long id) {
        MonitoringStation station = stationMapper.selectById(id);
        if (station == null) throw new BusinessException("站点不存在");
        return toVO(station);
    }

    @Override
    public List<StationVO> getStationsByZoneId(Long zoneId) {
        LambdaQueryWrapper<MonitoringStation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MonitoringStation::getHealthZoneId, zoneId).eq(MonitoringStation::getIsActive, 1);
        List<MonitoringStation> stations = stationMapper.selectList(wrapper);
        List<StationVO> result = new ArrayList<>();
        for (MonitoringStation s : stations) result.add(toVO(s));
        return result;
    }

    @Override
    public void addStation(StationSaveDTO dto) {
        MonitoringStation station = new MonitoringStation();
        BeanUtils.copyProperties(dto, station);
        station.setIsActive(1);
        stationMapper.insert(station);
    }

    @Override
    public void updateStation(StationSaveDTO dto) {
        MonitoringStation station = stationMapper.selectById(dto.getId());
        if (station == null) throw new BusinessException("站点不存在");
        BeanUtils.copyProperties(dto, station);
        stationMapper.updateById(station);
    }

    @Override
    public void deleteStation(Long id) {
        if (stationMapper.selectById(id) == null) throw new BusinessException("站点不存在");
        stationMapper.deleteById(id);
    }

    private StationVO toVO(MonitoringStation station) {
        StationVO vo = new StationVO();
        BeanUtils.copyProperties(station, vo);
        if (station.getHealthZoneId() != null) {
            HealthZone zone = healthZoneMapper.selectById(station.getHealthZoneId());
            vo.setZoneName(zone != null ? zone.getZoneName() : "");
        }
        return vo;
    }
}
```

### Task 3.7: Create AlertService and Impl

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/service/AlertService.java`
- Create: `ocean-server/src/main/java/com/ocean/service/impl/AlertServiceImpl.java`

```java
// AlertService.java
package com.ocean.service;

import com.ocean.dto.AlertRuleSaveDTO;
import com.ocean.entity.AlertRule;
import com.ocean.vo.AlertEventVO;
import com.ocean.vo.AlertStationDetailVO;
import java.time.LocalDate;
import java.util.List;

public interface AlertService {
    List<AlertRule> getRules();
    void addRule(AlertRuleSaveDTO dto);
    void updateRule(Long id, AlertRuleSaveDTO dto);
    List<AlertEventVO> getEvents(LocalDate alertDate, Long zoneId, String status);
    AlertEventVO getEventById(Long id);
    List<AlertStationDetailVO> getEventStations(Long eventId);
    void acknowledgeEvent(Long id, Long userId);
    int generateAlerts(LocalDate alertDate);
}
```

```java
// AlertServiceImpl.java
package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.common.BusinessException;
import com.ocean.dto.AlertRuleSaveDTO;
import com.ocean.entity.*;
import com.ocean.mapper.*;
import com.ocean.service.AlertService;
import com.ocean.vo.AlertEventVO;
import com.ocean.vo.AlertStationDetailVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlertServiceImpl implements AlertService {

    @Autowired private AlertRuleMapper alertRuleMapper;
    @Autowired private AlertEventMapper alertEventMapper;
    @Autowired private AlertStationDetailMapper alertStationDetailMapper;
    @Autowired private ForecastGridMapper forecastGridMapper;
    @Autowired private HealthZoneMapper healthZoneMapper;
    @Autowired private MonitoringStationMapper stationMapper;

    @Override
    public List<AlertRule> getRules() {
        return alertRuleMapper.selectList(new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getIsActive, 1));
    }

    @Override
    public void addRule(AlertRuleSaveDTO dto) {
        AlertRule rule = new AlertRule();
        BeanUtils.copyProperties(dto, rule);
        rule.setIsActive(1);
        alertRuleMapper.insert(rule);
    }

    @Override
    public void updateRule(Long id, AlertRuleSaveDTO dto) {
        AlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null) throw new BusinessException("规则不存在");
        BeanUtils.copyProperties(dto, rule);
        alertRuleMapper.updateById(rule);
    }

    @Override
    public List<AlertEventVO> getEvents(LocalDate alertDate, Long zoneId, String status) {
        LambdaQueryWrapper<AlertEvent> wrapper = new LambdaQueryWrapper<>();
        if (alertDate != null) wrapper.eq(AlertEvent::getAlertDate, alertDate);
        if (zoneId != null) wrapper.eq(AlertEvent::getZoneId, zoneId);
        if (status != null) wrapper.eq(AlertEvent::getStatus, status);
        wrapper.orderByDesc(AlertEvent::getSeverity);
        List<AlertEvent> events = alertEventMapper.selectList(wrapper);
        return events.stream().map(this::toEventVO).collect(Collectors.toList());
    }

    @Override
    public AlertEventVO getEventById(Long id) {
        AlertEvent event = alertEventMapper.selectById(id);
        if (event == null) throw new BusinessException("告警事件不存在");
        return toEventVO(event);
    }

    @Override
    public List<AlertStationDetailVO> getEventStations(Long eventId) {
        LambdaQueryWrapper<AlertStationDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertStationDetail::getAlertId, eventId);
        List<AlertStationDetail> details = alertStationDetailMapper.selectList(wrapper);
        List<AlertStationDetailVO> result = new ArrayList<>();
        for (AlertStationDetail d : details) {
            AlertStationDetailVO vo = new AlertStationDetailVO();
            BeanUtils.copyProperties(d, vo);
            MonitoringStation station = stationMapper.selectById(d.getStationId());
            if (station != null) {
                vo.setStationName(station.getStationName());
                vo.setLat(station.getLat());
                vo.setLon(station.getLon());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public void acknowledgeEvent(Long id, Long userId) {
        AlertEvent event = alertEventMapper.selectById(id);
        if (event == null) throw new BusinessException("告警事件不存在");
        event.setStatus("acknowledged");
        event.setAckBy(userId);
        event.setAckAt(LocalDateTime.now());
        alertEventMapper.updateById(event);
    }

    @Override
    @Transactional
    public int generateAlerts(LocalDate alertDate) {
        int count = 0;
        List<AlertRule> rules = getRules();
        List<HealthZone> zones = healthZoneMapper.selectList(
                new LambdaQueryWrapper<HealthZone>().eq(HealthZone::getIsActive, 1));

        for (AlertRule rule : rules) {
            for (HealthZone zone : zones) {
                // Aggregate forecast data within zone bbox
                List<Map<String, Object>> gridData = forecastGridMapper.selectByBbox(
                        rule.getVariable(), alertDate.toString(), 0.0,
                        zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());

                if (gridData.isEmpty()) continue;

                double maxVal = 0, sumVal = 0;
                int exceedCount = 0;
                for (Map<String, Object> row : gridData) {
                    double val = ((Number) row.get("value")).doubleValue();
                    if (val > maxVal) maxVal = val;
                    sumVal += val;
                    if (val > rule.getThreshold()) exceedCount++;
                }
                double avgVal = sumVal / gridData.size();

                AlertEvent event = new AlertEvent();
                event.setZoneId(zone.getId());
                event.setRuleId(rule.getId());
                event.setVariable(rule.getVariable());
                event.setSource(rule.getSource());
                event.setAlertDate(alertDate);
                event.setMaxValue(maxVal);
                event.setAvgValue(Math.round(avgVal * 100.0) / 100.0);
                event.setThreshold(rule.getThreshold());
                event.setStationCount(exceedCount);
                event.setSeverity(rule.getSeverity());
                event.setStatus("active");
                event.setMessage(zone.getZoneName() + " " + rule.getVariable().toUpperCase() + " 超标，最高 " + maxVal);

                alertEventMapper.insert(event);
                count++;
            }
        }
        return count;
    }

    private AlertEventVO toEventVO(AlertEvent event) {
        AlertEventVO vo = new AlertEventVO();
        BeanUtils.copyProperties(event, vo);
        HealthZone zone = healthZoneMapper.selectById(event.getZoneId());
        if (zone != null) vo.setZoneName(zone.getZoneName());
        if (event.getRuleId() != null) {
            AlertRule rule = alertRuleMapper.selectById(event.getRuleId());
            if (rule != null) vo.setRuleName(rule.getRuleName());
        }
        return vo;
    }
}
```

### Task 3.8: Create HealthService and Impl

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/service/HealthService.java`
- Create: `ocean-server/src/main/java/com/ocean/service/impl/HealthServiceImpl.java`

```java
// HealthService.java
package com.ocean.service;

import com.ocean.entity.HealthZone;
import com.ocean.vo.ZoneHealthVO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface HealthService {
    List<HealthZone> getZones();
    ZoneHealthVO getAssessment(LocalDate date);
    List<Map<String, Object>> getZoneTrend(Long zoneId, LocalDate startDate, LocalDate endDate);
    Map<String, Object> getDashboard();
}
```

```java
// HealthServiceImpl.java
package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.entity.HealthRecord;
import com.ocean.entity.HealthZone;
import com.ocean.mapper.HealthRecordMapper;
import com.ocean.mapper.HealthZoneMapper;
import com.ocean.service.HealthService;
import com.ocean.vo.ZoneHealthVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class HealthServiceImpl implements HealthService {

    @Autowired private HealthZoneMapper healthZoneMapper;
    @Autowired private HealthRecordMapper healthRecordMapper;

    @Override
    public List<HealthZone> getZones() {
        return healthZoneMapper.selectList(
                new LambdaQueryWrapper<HealthZone>().eq(HealthZone::getIsActive, 1).orderByAsc(HealthZone::getSortOrder));
    }

    @Override
    public ZoneHealthVO getAssessment(LocalDate date) {
        if (date == null) date = LocalDate.now();
        LocalDate assessDate = date;
        List<HealthZone> zones = getZones();
        List<Map<String, Object>> zoneData = new ArrayList<>();

        for (HealthZone zone : zones) {
            LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HealthRecord::getZoneId, zone.getId()).eq(HealthRecord::getAssessDate, assessDate);
            HealthRecord record = healthRecordMapper.selectOne(wrapper);

            Map<String, Object> z = new HashMap<>();
            z.put("id", zone.getId());
            z.put("label", zone.getZoneName());
            if (record != null) {
                Map<String, Object> sst = new HashMap<>();
                sst.put("avg", record.getSstAvg());
                sst.put("max", record.getSstMax());
                sst.put("anomaly", record.getSstAnomaly());
                sst.put("trend", record.getSstTrend());
                z.put("sst", sst);

                Map<String, Object> chl = new HashMap<>();
                chl.put("avg", record.getChlAvg());
                chl.put("max", record.getChlMax());
                chl.put("trend", record.getChlTrend());
                z.put("chl", chl);

                Map<String, Object> hw = new HashMap<>();
                hw.put("active", record.getHeatwaveActive() == 1);
                hw.put("days", record.getHeatwaveDays());
                z.put("heatwave", hw);

                z.put("overallGrade", record.getOverallGrade());
            } else {
                z.put("sst", emptyMetric());
                z.put("chl", emptyMetric());
                z.put("heatwave", Map.of("active", false, "days", 0));
                z.put("overallGrade", "good");
            }
            zoneData.add(z);
        }

        ZoneHealthVO vo = new ZoneHealthVO();
        vo.setZones(zoneData);
        return vo;
    }

    private Map<String, Object> emptyMetric() {
        Map<String, Object> m = new HashMap<>();
        m.put("avg", 0d);
        m.put("max", 0d);
        m.put("anomaly", 0d);
        m.put("trend", "stable");
        return m;
    }

    @Override
    public List<Map<String, Object>> getZoneTrend(Long zoneId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecord::getZoneId, zoneId)
               .between(HealthRecord::getAssessDate, startDate, endDate)
               .orderByAsc(HealthRecord::getAssessDate);
        List<HealthRecord> records = healthRecordMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (HealthRecord r : records) {
            Map<String, Object> m = new HashMap<>();
            m.put("assessDate", r.getAssessDate().toString());
            m.put("sstAvg", r.getSstAvg());
            m.put("sstAnomaly", r.getSstAnomaly());
            m.put("chlAvg", r.getChlAvg());
            m.put("heatwaveActive", r.getHeatwaveActive());
            m.put("heatwaveDays", r.getHeatwaveDays());
            m.put("overallGrade", r.getOverallGrade());
            result.add(m);
        }
        return result;
    }

    @Override
    public Map<String, Object> getDashboard() {
        List<HealthZone> zones = getZones();
        LocalDate today = LocalDate.now();
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> zoneHealth = new ArrayList<>();

        for (HealthZone zone : zones) {
            LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HealthRecord::getZoneId, zone.getId()).eq(HealthRecord::getAssessDate, today);
            HealthRecord record = healthRecordMapper.selectOne(wrapper);
            Map<String, Object> z = new HashMap<>();
            z.put("id", zone.getId());
            z.put("name", zone.getZoneName());
            z.put("grade", record != null ? record.getOverallGrade() : "good");
            zoneHealth.add(z);
        }
        result.put("zones", zoneHealth);
        return result;
    }
}
```

---

## Phase 4: Controller Layer (one per domain)

### Task 4.1: Create ObservationController

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/controller/ObservationController.java`

```java
package com.ocean.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.common.Result;
import com.ocean.dto.OceanDataQueryDTO;
import com.ocean.service.ObservationService;
import com.ocean.vo.OceanDataVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/observation")
public class ObservationController {

    @Autowired
    private ObservationService observationService;

    @GetMapping("/page")
    public Result<IPage<OceanDataVO>> getDataPage(OceanDataQueryDTO dto) {
        return Result.success(observationService.getDataPage(dto));
    }

    @GetMapping("/locations")
    public Result<List<Map<String, Object>>> getLocations() {
        return Result.success(observationService.getDistinctLocations());
    }

    @GetMapping("/sst-timeseries")
    public Result<List<Map<String, Object>>> getSstTimeSeries(
            @RequestParam String startDate, @RequestParam String endDate,
            @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lon) {
        return Result.success(observationService.getSstTimeSeries(startDate, endDate, lat, lon));
    }

    @GetMapping("/chl-timeseries")
    public Result<List<Map<String, Object>>> getChlTimeSeries(
            @RequestParam String startDate, @RequestParam String endDate,
            @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lon) {
        return Result.success(observationService.getChlTimeSeries(startDate, endDate, lat, lon));
    }

    @GetMapping("/chl-by-depth")
    public Result<List<Map<String, Object>>> getChlByDepth() {
        return Result.success(observationService.getChlByDepth());
    }
}
```

### Task 4.2: Create ForecastController

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/controller/ForecastController.java`

```java
package com.ocean.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.common.Result;
import com.ocean.dto.ForecastQueryDTO;
import com.ocean.dto.MapGridQueryDTO;
import com.ocean.service.ForecastService;
import com.ocean.vo.DashboardVO;
import com.ocean.vo.ForecastVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    @Autowired
    private ForecastService forecastService;

    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard() {
        return Result.success(forecastService.getDashboard());
    }

    @GetMapping("/page")
    public Result<IPage<ForecastVO>> getRecordPage(ForecastQueryDTO dto) {
        return Result.success(forecastService.getRecordPage(dto));
    }

    @GetMapping("/sst/trend")
    public Result<List<Map<String, Object>>> getSstTrend(
            @RequestParam(required = false) Double lon, @RequestParam(required = false) Double lat) {
        return Result.success(forecastService.getSstTrend(lon, lat));
    }

    @GetMapping("/chl/trend")
    public Result<List<Map<String, Object>>> getChlTrend(
            @RequestParam(required = false) Double lon, @RequestParam(required = false) Double lat) {
        return Result.success(forecastService.getChlTrend(lon, lat));
    }

    @GetMapping("/locations")
    public Result<List<Map<String, Object>>> getLocations() {
        return Result.success(forecastService.getLocations());
    }

    @GetMapping("/map/grid")
    public Result<List<Map<String, Object>>> getMapGrid(MapGridQueryDTO dto) {
        return Result.success(forecastService.getMapGrid(dto));
    }

    @GetMapping("/trend/point")
    public Result<List<Map<String, Object>>> getPointTrend(
            @RequestParam String dataType, @RequestParam Double lon, @RequestParam Double lat,
            @RequestParam(required = false) String dateStart, @RequestParam(required = false) String dateEnd) {
        return Result.success(forecastService.getPointTrend(dataType, lon, lat, dateStart, dateEnd));
    }

    @GetMapping("/trend/dashboard")
    public Result<List<Map<String, Object>>> getDashboardTrend(
            @RequestParam(defaultValue = "SST") String dataType, @RequestParam(defaultValue = "7") Integer days) {
        return Result.success(forecastService.getDashboardTrend(dataType, days));
    }

    @GetMapping("/chl/probability")
    public Result<List<Map<String, Object>>> getChlProbability(
            @RequestParam String dateStart, @RequestParam(required = false) String dateEnd,
            @RequestParam(required = false) Double threshold) {
        return Result.success(forecastService.getChlProbability(dateStart, dateEnd, threshold));
    }

    @GetMapping("/sea-areas")
    public Result<List<Map<String, Object>>> getSeaAreas() {
        return Result.success(forecastService.getSeaAreas());
    }
}
```

### Task 4.3: Create ModelController

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/controller/ModelController.java`

```java
package com.ocean.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.common.Result;
import com.ocean.dto.ModelSaveDTO;
import com.ocean.service.ModelService;
import com.ocean.vo.ModelVO;
import com.ocean.vo.RunningVersionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/model")
public class ModelController {

    @Autowired
    private ModelService modelService;

    @GetMapping("/page")
    public Result<IPage<ModelVO>> getModelPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String keyword) {
        return Result.success(modelService.getModelPage(pageNum, pageSize, modelType, keyword));
    }

    @GetMapping("/{id}")
    public Result<ModelVO> getModelById(@PathVariable Long id) {
        return Result.success(modelService.getModelById(id));
    }

    @PostMapping
    public Result<?> addModel(@Validated @RequestBody ModelSaveDTO dto) {
        modelService.addModel(dto);
        return Result.success("模型创建成功");
    }

    @PutMapping("/{id}")
    public Result<?> updateModel(@PathVariable Long id, @Validated @RequestBody ModelSaveDTO dto) {
        dto.setId(id);
        modelService.updateModel(dto);
        return Result.success("模型更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteModel(@PathVariable Long id) {
        modelService.deleteModel(id);
        return Result.success("模型删除成功");
    }

    @GetMapping("/running-versions")
    public Result<List<RunningVersionVO>> getRunningVersions() {
        return Result.success(modelService.getRunningVersions());
    }
}
```

### Task 4.4: Create ModelVersionController

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/controller/ModelVersionController.java`

```java
package com.ocean.controller;

import com.ocean.common.Result;
import com.ocean.dto.ModelVersionSaveDTO;
import com.ocean.service.ModelVersionService;
import com.ocean.vo.ModelVersionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/model/{modelId}/version")
public class ModelVersionController {

    @Autowired
    private ModelVersionService modelVersionService;

    @GetMapping
    public Result<List<ModelVersionVO>> getVersions(@PathVariable Long modelId) {
        return Result.success(modelVersionService.getVersionsByModelId(modelId));
    }

    @PostMapping
    public Result<?> addVersion(@PathVariable Long modelId, @RequestBody ModelVersionSaveDTO dto) {
        modelVersionService.addVersion(modelId, dto);
        return Result.success("版本创建成功");
    }

    @PutMapping("/{versionId}")
    public Result<?> updateVersion(@PathVariable Long modelId, @PathVariable Long versionId,
                                   @RequestBody ModelVersionSaveDTO dto) {
        dto.setId(versionId);
        modelVersionService.updateVersion(modelId, versionId, dto);
        return Result.success("版本更新成功");
    }

    @DeleteMapping("/{versionId}")
    public Result<?> deleteVersion(@PathVariable Long modelId, @PathVariable Long versionId) {
        modelVersionService.deleteVersion(modelId, versionId);
        return Result.success("版本删除成功");
    }

    @PutMapping("/{versionId}/status")
    public Result<?> toggleStatus(@PathVariable Long modelId, @PathVariable Long versionId,
                                  @RequestParam String status) {
        modelVersionService.toggleStatus(modelId, versionId, status);
        String msg = "RUNNING".equals(status) ? "版本已启动" : "版本已停止";
        return Result.success(msg);
    }
}
```

### Task 4.5: Create StationController

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/controller/StationController.java`

```java
package com.ocean.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.common.Result;
import com.ocean.dto.StationSaveDTO;
import com.ocean.service.StationService;
import com.ocean.vo.StationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/station")
public class StationController {

    @Autowired
    private StationService stationService;

    @GetMapping("/page")
    public Result<IPage<StationVO>> getPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String distance,
            @RequestParam(required = false) String region) {
        return Result.success(stationService.getStationPage(pageNum, pageSize, distance, region));
    }

    @GetMapping("/{id}")
    public Result<StationVO> getById(@PathVariable Long id) {
        return Result.success(stationService.getStationById(id));
    }

    @GetMapping("/by-zone/{zoneId}")
    public Result<List<StationVO>> getByZone(@PathVariable Long zoneId) {
        return Result.success(stationService.getStationsByZoneId(zoneId));
    }

    @PostMapping
    public Result<?> add(@RequestBody StationSaveDTO dto) {
        stationService.addStation(dto);
        return Result.success("站点创建成功");
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody StationSaveDTO dto) {
        dto.setId(id);
        stationService.updateStation(dto);
        return Result.success("站点更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        stationService.deleteStation(id);
        return Result.success("站点删除成功");
    }
}
```

### Task 4.6: Create AlertController

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/controller/AlertController.java`

```java
package com.ocean.controller;

import com.ocean.common.Result;
import com.ocean.dto.AlertRuleSaveDTO;
import com.ocean.entity.AlertRule;
import com.ocean.service.AlertService;
import com.ocean.vo.AlertEventVO;
import com.ocean.vo.AlertStationDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/alert")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @GetMapping("/rules")
    public Result<List<AlertRule>> getRules() {
        return Result.success(alertService.getRules());
    }

    @PostMapping("/rules")
    public Result<?> addRule(@RequestBody AlertRuleSaveDTO dto) {
        alertService.addRule(dto);
        return Result.success("规则创建成功");
    }

    @PutMapping("/rules/{id}")
    public Result<?> updateRule(@PathVariable Long id, @RequestBody AlertRuleSaveDTO dto) {
        alertService.updateRule(id, dto);
        return Result.success("规则更新成功");
    }

    @GetMapping("/events")
    public Result<List<AlertEventVO>> getEvents(
            @RequestParam(required = false) String alertDate,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(defaultValue = "active") String status) {
        LocalDate date = alertDate != null ? LocalDate.parse(alertDate) : null;
        return Result.success(alertService.getEvents(date, zoneId, status));
    }

    @GetMapping("/events/{id}")
    public Result<AlertEventVO> getEvent(@PathVariable Long id) {
        return Result.success(alertService.getEventById(id));
    }

    @GetMapping("/events/{id}/stations")
    public Result<List<AlertStationDetailVO>> getEventStations(@PathVariable Long id) {
        return Result.success(alertService.getEventStations(id));
    }

    @PutMapping("/events/{id}/acknowledge")
    public Result<?> acknowledge(@PathVariable Long id, @RequestParam(required = false, defaultValue = "1") Long userId) {
        alertService.acknowledgeEvent(id, userId);
        return Result.success("已确认");
    }

    @PostMapping("/events/generate")
    public Result<?> generateAlerts(@RequestParam String alertDate) {
        int count = alertService.generateAlerts(LocalDate.parse(alertDate));
        return Result.success("生成告警 " + count + " 条");
    }
}
```

### Task 4.7: Create HealthController

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/controller/HealthController.java`

```java
package com.ocean.controller;

import com.ocean.common.Result;
import com.ocean.entity.HealthZone;
import com.ocean.service.HealthService;
import com.ocean.vo.ZoneHealthVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private HealthService healthService;

    @GetMapping("/zones")
    public Result<List<HealthZone>> getZones() {
        return Result.success(healthService.getZones());
    }

    @GetMapping("/assessment")
    public Result<ZoneHealthVO> getAssessment(@RequestParam(required = false) String forecastDate) {
        LocalDate date = forecastDate != null ? LocalDate.parse(forecastDate) : LocalDate.now();
        return Result.success(healthService.getAssessment(date));
    }

    @GetMapping("/assessment/{zoneId}/trend")
    public Result<List<Map<String, Object>>> getZoneTrend(
            @PathVariable Long zoneId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return Result.success(healthService.getZoneTrend(zoneId, LocalDate.parse(startDate), LocalDate.parse(endDate)));
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard() {
        return Result.success(healthService.getDashboard());
    }
}
```

---

## Phase 5: ForecastGridMapper with custom queries

### Task 5.1: Create ForecastGridMapper with @Select queries

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/mapper/ForecastGridMapper.java`

This mapper is the most critical as it handles all the map/trend/probability queries.

```java
package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.entity.ForecastGrid;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface ForecastGridMapper extends BaseMapper<ForecastGrid> {

    @Select("SELECT DISTINCT lat, lon FROM forecast_grid ORDER BY lat, lon")
    List<Map<String, Object>> selectDistinctLocations();

    @Select("<script>" +
            "SELECT lat AS gridLat, lon AS gridLon, value " +
            "FROM forecast_grid " +
            "WHERE variable = #{variable} AND forecast_date = #{forecastDate} AND depth = 0 " +
            "<if test='minLon != null'> AND lon &gt;= #{minLon} </if>" +
            "<if test='maxLon != null'> AND lon &lt;= #{maxLon} </if>" +
            "<if test='minLat != null'> AND lat &gt;= #{minLat} </if>" +
            "<if test='maxLat != null'> AND lat &lt;= #{maxLat} </if>" +
            "ORDER BY lat, lon" +
            "</script>")
    List<Map<String, Object>> selectMapGrid(@Param("variable") String variable,
                                             @Param("forecastDate") String forecastDate,
                                             @Param("minLon") Double minLon,
                                             @Param("maxLon") Double maxLon,
                                             @Param("minLat") Double minLat,
                                             @Param("maxLat") Double maxLat);

    @Select("<script>" +
            "SELECT forecast_date AS forecastDate, value " +
            "FROM forecast_grid " +
            "WHERE variable = #{dataType} AND depth = 0 " +
            "<if test='lon != null'> AND lon = #{lon} </if>" +
            "<if test='lat != null'> AND lat = #{lat} </if>" +
            "ORDER BY forecast_date ASC" +
            "</script>")
    List<Map<String, Object>> selectTrend(@Param("dataType") String dataType,
                                           @Param("lon") Double lon,
                                           @Param("lat") Double lat);

    @Select("<script>" +
            "SELECT forecast_date AS forecastDate, value " +
            "FROM forecast_grid " +
            "WHERE variable = #{dataType} AND depth = 0 " +
            "AND lat = #{lat} AND lon = #{lon} " +
            "<if test='dateStart != null'> AND forecast_date &gt;= #{dateStart} </if>" +
            "<if test='dateEnd != null'> AND forecast_date &lt;= #{dateEnd} </if>" +
            "ORDER BY forecast_date ASC" +
            "</script>")
    List<Map<String, Object>> selectPointTrend(@Param("dataType") String dataType,
                                                @Param("lon") Double lon,
                                                @Param("lat") Double lat,
                                                @Param("dateStart") String dateStart,
                                                @Param("dateEnd") String dateEnd);

    @Select("SELECT f.lat, f.lon, AVG(f.value) AS avg_value, f.forecast_date AS forecastDate " +
            "FROM forecast_grid f WHERE f.variable = #{dataType} AND f.depth = 0 " +
            "AND f.forecast_date >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY f.lat, f.lon ORDER BY AVG(f.value) DESC LIMIT 5")
    List<Map<String, Object>> selectDashboardTrend(@Param("dataType") String dataType,
                                                    @Param("days") Integer days);

    @Select("SELECT lat, lon, " +
            "SUM(CASE WHEN value > #{threshold} THEN 1 ELSE 0 END) / COUNT(*) AS probability " +
            "FROM forecast_grid " +
            "WHERE variable = 'chl' AND depth = 0 " +
            "AND forecast_date BETWEEN #{dateStart} AND #{dateEnd} " +
            "GROUP BY lat, lon")
    List<Map<String, Object>> selectChlProbability(@Param("dateStart") String dateStart,
                                                    @Param("dateEnd") String dateEnd,
                                                    @Param("threshold") Double threshold);

    @Select("<script>" +
            "SELECT lat, lon, value " +
            "FROM forecast_grid " +
            "WHERE variable = #{variable} AND forecast_date = #{forecastDate} AND depth = #{depth} " +
            "AND lat BETWEEN #{minLat} AND #{maxLat} " +
            "AND lon BETWEEN #{minLon} AND #{maxLon}" +
            "</script>")
    List<Map<String, Object>> selectByBbox(@Param("variable") String variable,
                                            @Param("forecastDate") String forecastDate,
                                            @Param("depth") Double depth,
                                            @Param("minLon") Double minLon,
                                            @Param("maxLon") Double maxLon,
                                            @Param("minLat") Double minLat,
                                            @Param("maxLat") Double maxLat);
}
```

---

## Phase 6: ObservationDataMapper with custom queries

### Task 6.1: Rewrite ObservationDataMapper for new table

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/mapper/ObservationDataMapper.java` (rewrite)

```java
package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.entity.ObservationData;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface ObservationDataMapper extends BaseMapper<ObservationData> {

    @Select("<script>" +
            "SELECT obs_time AS time, lat, lon, AVG(value) AS value FROM observation_data " +
            "WHERE variable = 'thetao' AND obs_time BETWEEN #{startDate} AND #{endDate} " +
            "<if test='lat != null'> AND lat = #{lat} </if>" +
            "<if test='lon != null'> AND lon = #{lon} </if>" +
            "GROUP BY obs_time, lat, lon ORDER BY obs_time ASC" +
            "</script>")
    List<Map<String, Object>> selectSstTimeSeries(@Param("startDate") String startDate,
                                                   @Param("endDate") String endDate,
                                                   @Param("lat") Double lat,
                                                   @Param("lon") Double lon);

    @Select("<script>" +
            "SELECT obs_time AS time, lat, lon, AVG(value) AS value FROM observation_data " +
            "WHERE variable = 'chl' AND obs_time BETWEEN #{startDate} AND #{endDate} " +
            "<if test='lat != null'> AND lat = #{lat} </if>" +
            "<if test='lon != null'> AND lon = #{lon} </if>" +
            "GROUP BY obs_time, lat, lon ORDER BY obs_time ASC" +
            "</script>")
    List<Map<String, Object>> selectChlTimeSeries(@Param("startDate") String startDate,
                                                   @Param("endDate") String endDate,
                                                   @Param("lat") Double lat,
                                                   @Param("lon") Double lon);

    @Select("SELECT depth, AVG(value) AS avg_value, MIN(value) AS min_value, MAX(value) AS max_value " +
            "FROM observation_data WHERE variable = 'chl' GROUP BY depth ORDER BY depth ASC")
    List<Map<String, Object>> selectChlByDepth();

    @Select("SELECT DISTINCT lat, lon FROM observation_grid_cache ORDER BY lat, lon")
    List<Map<String, Object>> selectDistinctLocations();
}
```

---

## Phase 7: DTOs for new modules

### Task 7.1: Create DTOs

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/dto/StationSaveDTO.java`
- Create: `ocean-server/src/main/java/com/ocean/dto/AlertRuleSaveDTO.java`

```java
package com.ocean.dto;

import lombok.Data;

@Data
public class StationSaveDTO {
    private Long id;
    private String stationName;
    private Double lat;
    private Double lon;
    private String distance;
    private String region;
    private Long healthZoneId;
    private Integer sortOrder;
}
```

```java
package com.ocean.dto;

import lombok.Data;

@Data
public class AlertRuleSaveDTO {
    private String ruleName;
    private String variable;
    private String source;
    private String operator;
    private Double threshold;
    private String severity;
}
```

---

## Phase 8: Retrofit existing VO classes

### Task 8.1: Update ModelVO and ModelVersionVO

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/vo/ModelVO.java`
- Modify: `ocean-server/src/main/java/com/ocean/vo/ModelVersionVO.java`

The existing ModelVO needs a `versionCount` and `runningCount` fields (if not already there). ModelVersionVO needs updated fields matching the new ModelVersion entity.

```java
// ModelVO.java - add fields if missing:
// private Long versionCount;
// private Long runningCount;
```

```java
// ModelVersionVO.java - update fields:
// private Long id;
// private Long modelId;
// private String versionLabel;
// private String paramsConfig;
// private String cronExpression;
// private String dataSource;
// private String dataTimeRange;
// private String changeNote;
// private String status;
// private LocalDateTime lastRunTime;
// private LocalDateTime createTime;
// private LocalDateTime updateTime;
```

---

## Phase 9: Update frontend API files

### Task 9.1: Update forecast.js

**Files:**
- Modify: `ocean-web/src/api/forecast.js`

Change alert and health endpoints:
```js
// Change:
// getAlerts → /alert/events
// getZoneHealth → /health/assessment

export function getAlerts(forecastDate) {
  return request({ url: '/alert/events', method: 'get', params: { alertDate: forecastDate, status: 'active' } })
}
```

### Task 9.2: Update ocean-data.js

**Files:**
- Modify: `ocean-web/src/api/ocean-data.js`

Change paths from `/ocean-data/` to `/observation/`.

### Task 9.3: Update health.js

**Files:**
- Modify: `ocean-web/src/api/health.js`

Change path from `/forecast/zone-health` to `/health/assessment`.

---

## Phase 10: Build & Verify

### Task 10.1: Build the project

Run: `cd ocean-server && mvn compile -q 2>&1`

Expected: BUILD SUCCESS

### Task 10.2: Start server and test endpoints

Run: `cd ocean-server && mvn spring-boot:run` (in background), then test with curl:

```bash
curl http://localhost:8080/api/forecast/dashboard
curl "http://localhost:8080/api/forecast/map/grid?dataType=SST&forecastDate=2026-05-15"
curl "http://localhost:8080/api/health/assessment?forecastDate=2026-05-15"
curl "http://localhost:8080/api/alert/events"
curl "http://localhost:8080/api/model/page"
curl "http://localhost:8080/api/station/page"
```

### Task 10.3: Verify with frontend build

Run: `cd ocean-web && npx vite build 2>&1`

Expected: no errors

---

## Execution Order

1. **Phase 0** (Test Data) - insert data first so services have data to query
2. **Phase 1** (Entities) - all 11 entities can be created in parallel
3. **Phase 2** (Mappers) - all 11 mappers in parallel (after entities)
4. **Phase 3** (Services) - domain by domain, can parallelize Observation + Forecast + Station + Alert + Health
5. **Phase 4** (Controllers) - after services, all can be created in parallel
6. **Phase 5** (ForecastGridMapper queries) - critical custom queries
7. **Phase 6** (ObservationDataMapper rewrite) - rewrite existing mapper
8. **Phase 7** (DTOs) - new DTOs
9. **Phase 8** (VO updates) - retrofit existing VOs
10. **Phase 9** (Frontend API) - update frontend
11. **Phase 10** (Verify) - build, test, fix
