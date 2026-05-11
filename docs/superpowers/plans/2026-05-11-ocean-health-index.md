# 海洋健康指数板块 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增海洋健康指数页面，以 AQI 风格卡片展示各子区域的 SST/Chl/海洋热浪风险等级和文字解读。

**Architecture:** 新增前端页面 `OceanHealthView.vue` + 前端阈值引擎 `health-assessment.js` + 后端接口 `GET /forecast/zone-health`，复用现有 `ForecastRecordController`/`ForecastRecordService`。现有功能零修改。

**Tech Stack:** Vue 3 Composition API, Element Plus, Spring Boot, MyBatis Plus, MySQL

---

### Task 1: 后端 VO —— 分区健康数据返回模型

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/vo/ZoneHealthVO.java`
- Create: `ocean-server/src/main/java/com/ocean/dto/ZoneHealthQueryDTO.java`

- [ ] **Step 1: 创建 ZoneHealthQueryDTO 请求参数 DTO**

```java
package com.ocean.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ZoneHealthQueryDTO {
    private BigDecimal centerLon;
    private BigDecimal centerLat;
    private BigDecimal coastLon;
    private String forecastDate;
}
```

- [ ] **Step 2: 创建 ZoneHealthVO 返回对象**

```java
package com.ocean.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ZoneHealthVO {
    private String zoneName;
    private List<SubZone> zones;

    @Data
    public static class SubZone {
        private String id;
        private String label;
        private SstInfo sst;
        private ChlInfo chl;
        private HeatwaveInfo heatwave;
    }

    @Data
    public static class SstInfo {
        private Double avg;
        private Double max;
        private String trend;
        private Double anomaly;
    }

    @Data
    public static class ChlInfo {
        private Double avg;
        private Double max;
        private String trend;
    }

    @Data
    public static class HeatwaveInfo {
        private Boolean active;
        private Integer days;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add ocean-server/src/main/java/com/ocean/vo/ZoneHealthVO.java ocean-server/src/main/java/com/ocean/dto/ZoneHealthQueryDTO.java
git commit -m "feat: add ZoneHealthVO and ZoneHealthQueryDTO"
```

---

### Task 2: 后端 Mapper —— 新增查询 SQL

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/mapper/ForecastRecordMapper.java`

- [ ] **Step 1: 在 ForecastRecordMapper 接口中新增两个查询方法**

在 `ForecastRecordMapper` 接口末尾（`}` 之前）添加：

```java
    /**
     * 分区健康查询 — SST 统计（均值、极值、趋势方向）
     */
    @Select("<script>" +
        "SELECT AVG(value) AS avgVal, MAX(value) AS maxVal, " +
        "  CASE WHEN AVG(CASE WHEN forecast_date = #{forecastDate} THEN value END) > " +
        "            AVG(CASE WHEN forecast_date &lt; #{forecastDate} THEN value END) " +
        "       THEN 'rising' ELSE 'falling' END AS trend " +
        "FROM forecast_record " +
        "WHERE data_type = 'SST' " +
        "  AND forecast_date &lt;= #{forecastDate} " +
        "  AND longitude &gt;= #{minLon} AND longitude &lt;= #{maxLon} " +
        "  AND latitude &gt;= #{minLat} AND latitude &lt;= #{maxLat} " +
        "</script>")
    Map<String, Object> selectZoneSstStats(@Param("minLon") BigDecimal minLon,
                                           @Param("maxLon") BigDecimal maxLon,
                                           @Param("minLat") BigDecimal minLat,
                                           @Param("maxLat") BigDecimal maxLat,
                                           @Param("forecastDate") String forecastDate);

    /**
     * 分区健康查询 — Chl 统计（均值、极值、趋势方向）
     */
    @Select("<script>" +
        "SELECT AVG(value) AS avgVal, MAX(value) AS maxVal, " +
        "  CASE WHEN AVG(CASE WHEN forecast_date = #{forecastDate} THEN value END) > " +
        "            AVG(CASE WHEN forecast_date &lt; #{forecastDate} THEN value END) " +
        "       THEN 'rising' ELSE 'falling' END AS trend " +
        "FROM forecast_record " +
        "WHERE data_type = 'CHL' " +
        "  AND forecast_date &lt;= #{forecastDate} " +
        "  AND longitude &gt;= #{minLon} AND longitude &lt;= #{maxLon} " +
        "  AND latitude &gt;= #{minLat} AND latitude &lt;= #{maxLat} " +
        "</script>")
    Map<String, Object> selectZoneChlStats(@Param("minLon") BigDecimal minLon,
                                           @Param("maxLon") BigDecimal maxLon,
                                           @Param("minLat") BigDecimal minLat,
                                           @Param("maxLat") BigDecimal maxLat,
                                           @Param("forecastDate") String forecastDate);

    /**
     * 查询 SST 常年同期基准值（过去所有年份同月均值）
     */
    @Select("<script>" +
        "SELECT AVG(value) AS baseline " +
        "FROM forecast_record " +
        "WHERE data_type = 'SST' " +
        "  AND MONTH(forecast_date) = MONTH(#{forecastDate}) " +
        "  AND YEAR(forecast_date) &lt; YEAR(#{forecastDate}) " +
        "  AND longitude &gt;= #{minLon} AND longitude &lt;= #{maxLon} " +
        "  AND latitude &gt;= #{minLat} AND latitude &lt;= #{maxLat} " +
        "</script>")
    Map<String, Object> selectSstBaseline(@Param("minLon") BigDecimal minLon,
                                          @Param("maxLon") BigDecimal maxLon,
                                          @Param("minLat") BigDecimal minLat,
                                          @Param("maxLat") BigDecimal maxLat,
                                          @Param("forecastDate") String forecastDate);

    /**
     * 统计连续高温天数（热浪检测）— SST 高于基准超过 2°C 且连续天数
     */
    @Select("<script>" +
        "SELECT MIN(forecast_date) AS heatStart, COUNT(*) AS heatDays " +
        "FROM forecast_record " +
        "WHERE data_type = 'SST' " +
        "  AND value &gt; #{baseline} + 2 " +
        "  AND forecast_date &lt;= #{forecastDate} " +
        "  AND forecast_date &gt;= DATE_SUB(#{forecastDate}, INTERVAL 30 DAY) " +
        "  AND longitude &gt;= #{minLon} AND longitude &lt;= #{maxLon} " +
        "  AND latitude &gt;= #{minLat} AND latitude &lt;= #{maxLat} " +
        "  AND NOT EXISTS ( " +
        "    SELECT 1 FROM forecast_record fr2 " +
        "    WHERE fr2.data_type = 'SST' " +
        "      AND fr2.forecast_date = DATE_SUB(forecast_record.forecast_date, INTERVAL 1 DAY) " +
        "      AND fr2.value &lt;= #{baseline} + 2 " +
        "      AND fr2.longitude &gt;= #{minLon} AND fr2.longitude &lt;= #{maxLon} " +
        "      AND fr2.latitude &gt;= #{minLat} AND fr2.latitude &lt;= #{maxLat} " +
        "  )" +
        "GROUP BY forecast_date " +
        "ORDER BY heatDays DESC LIMIT 1" +
        "</script>")
    Map<String, Object> selectHeatwaveDays(@Param("minLon") BigDecimal minLon,
                                           @Param("maxLon") BigDecimal maxLon,
                                           @Param("minLat") BigDecimal minLat,
                                           @Param("maxLat") BigDecimal maxLat,
                                           @Param("forecastDate") String forecastDate,
                                           @Param("baseline") Double baseline);
```

- [ ] **Step 2: 提交**

```bash
git add ocean-server/src/main/java/com/ocean/mapper/ForecastRecordMapper.java
git commit -m "feat: add zone health mapper queries for SST/Chl stats and heatwave detection"
```

---

### Task 3: 后端 Service 与 Controller —— 分区计算逻辑

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/service/ForecastRecordService.java`
- Modify: `ocean-server/src/main/java/com/ocean/service/impl/ForecastRecordServiceImpl.java`
- Modify: `ocean-server/src/main/java/com/ocean/controller/ForecastRecordController.java`

- [ ] **Step 1: 在 Service 接口中新增方法签名**

在 `ForecastRecordService` 接口末尾（`}` 之前）添加：

```java
    /** 分区健康指数查询 */
    Map<String, Object> getZoneHealth(com.ocean.dto.ZoneHealthQueryDTO dto);
```

- [ ] **Step 2: 在 ServiceImpl 中实现分区计算逻辑**

在 `ForecastRecordServiceImpl` 末尾（`}` 之前）添加：

```java
    @Override
    public Map<String, Object> getZoneHealth(ZoneHealthQueryDTO dto) {
        BigDecimal centerLon = dto.getCenterLon();
        BigDecimal centerLat = dto.getCenterLat();
        BigDecimal coastLon = dto.getCoastLon();
        String forecastDate = dto.getForecastDate();

        // 方位划分 — 北/南
        String[][] zoneDefs = {
            {"nearshore-north", "近岸北", coastLon, centerLon, centerLat, null},
            {"nearshore-south", "近岸南", coastLon, centerLon, null, centerLat},
            {"transition-north", "过渡带北", centerLon, centerLon.add(new BigDecimal("1.0")), centerLat, null},
            {"transition-south", "过渡带南", centerLon, centerLon.add(new BigDecimal("1.0")), null, centerLat},
            {"offshore-north", "远海北", centerLon.add(new BigDecimal("1.0")), null, centerLat, null},
            {"offshore-south", "远海南", centerLon.add(new BigDecimal("1.0")), null, null, centerLat},
        };

        List<Map<String, Object>> zones = new ArrayList<>();
        for (String[] def : zoneDefs) {
            BigDecimal minLon = new BigDecimal(def[2]);
            BigDecimal maxLon = def[3] != null ? new BigDecimal(def[3]) :
                centerLon.add(new BigDecimal("5.0")); // 远海默认延伸到 5 度
            BigDecimal minLat = def[4] != null ? new BigDecimal(def[4]) :
                centerLat.subtract(new BigDecimal("3.0"));
            BigDecimal maxLat = def[5] != null ? new BigDecimal(def[5]) :
                centerLat.add(new BigDecimal("3.0"));

            // 查询 SST 统计
            Map<String, Object> sstStats = forecastRecordMapper.selectZoneSstStats(
                minLon, maxLon, minLat, maxLat, forecastDate);
            // 查询 Chl 统计
            Map<String, Object> chlStats = forecastRecordMapper.selectZoneChlStats(
                minLon, maxLon, minLat, maxLat, forecastDate);
            // 查询 SST 基准值
            Map<String, Object> baseline = forecastRecordMapper.selectSstBaseline(
                minLon, maxLon, minLat, maxLat, forecastDate);

            Double sstAvg = toDouble(sstStats.get("avgVal"));
            Double sstMax = toDouble(sstStats.get("maxVal"));
            String sstTrend = (String) sstStats.getOrDefault("trend", "stable");
            Double sstBaseline = toDouble(baseline.get("baseline"));
            Double anomaly = (sstAvg != null && sstBaseline != null) ? sstAvg - sstBaseline : null;

            // 热浪检测
            boolean heatActive = false;
            int heatDays = 0;
            if (sstBaseline != null) {
                Map<String, Object> hw = forecastRecordMapper.selectHeatwaveDays(
                    minLon, maxLon, minLat, maxLat, forecastDate, sstBaseline);
                if (hw != null && hw.get("heatDays") != null) {
                    long days = ((Number) hw.get("heatDays")).longValue();
                    heatDays = (int) days;
                    heatActive = heatDays >= 5;
                }
            }

            Double chlAvg = toDouble(chlStats.get("avgVal"));
            Double chlMax = toDouble(chlStats.get("maxVal"));
            String chlTrend = (String) chlStats.getOrDefault("trend", "stable");

            Map<String, Object> zone = new LinkedHashMap<>();
            zone.put("id", def[0]);
            zone.put("label", def[1]);
            zone.put("sst", Map.of("avg", sstAvg != null ? sstAvg : 0,
                                   "max", sstMax != null ? sstMax : 0,
                                   "trend", sstTrend,
                                   "anomaly", anomaly != null ? anomaly : 0));
            zone.put("chl", Map.of("avg", chlAvg != null ? chlAvg : 0,
                                   "max", chlMax != null ? chlMax : 0,
                                   "trend", chlTrend));
            zone.put("heatwave", Map.of("active", heatActive, "days", heatDays));
            zones.add(zone);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("zoneName", "东海");
        result.put("zones", zones);
        return result;
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        return ((Number) val).doubleValue();
    }
```

- [ ] **Step 3: 在 Controller 中新增接口**

在 `ForecastRecordController` 末尾（`}` 之前）添加：

```java
    /**
     * 分区健康指数
     */
    @GetMapping("/zone-health")
    public Result<Map<String, Object>> getZoneHealth(@Validated ZoneHealthQueryDTO dto) {
        Map<String, Object> data = forecastRecordService.getZoneHealth(dto);
        return Result.success(data);
    }
```

- [ ] **Step 4: 提交**

```bash
git add ocean-server/src/main/java/com/ocean/service/ForecastRecordService.java ocean-server/src/main/java/com/ocean/service/impl/ForecastRecordServiceImpl.java ocean-server/src/main/java/com/ocean/controller/ForecastRecordController.java
git commit -m "feat: add /forecast/zone-health endpoint with zone split and stats calculation"
```

---

### Task 4: 前端 API 封装

**Files:**
- Create: `ocean-web/src/api/health.js`

- [ ] **Step 1: 创建 health.js**

```js
import request from '../utils/request'

/** 获取分区健康指数数据 */
export function getZoneHealth(params) {
  return request({ url: '/forecast/zone-health', method: 'get', params })
}
```

- [ ] **Step 2: 提交**

```bash
git add ocean-web/src/api/health.js
git commit -m "feat: add health API layer for zone-health endpoint"
```

---

### Task 5: 前端阈值引擎

**Files:**
- Create: `ocean-web/src/utils/health-assessment.js`

- [ ] **Step 1: 创建 health-assessment.js**

```js
const LEVELS = {
  good: { label: '优', color: '#22c55e' },
  fine: { label: '良', color: '#22c55e' },
  warn: { label: '中', color: '#f59e0b' },
  bad:  { label: '差', color: '#ef4444' }
}

function assessSst(sst) {
  const anomaly = Math.abs(sst.anomaly || 0)
  if (anomaly > 2.5) return 'bad'
  if (anomaly > 1.5) return 'warn'
  if (anomaly > 0.5) return 'fine'
  return 'good'
}

function assessChl(chl) {
  const avg = chl.avg || 0
  if (avg >= 5.0) return 'bad'
  if (avg >= 3.0) return 'warn'
  if (avg >= 2.0) return 'fine'
  return 'good'
}

function assessHeatwave(hw) {
  if (hw.active) return 'bad'
  return 'good'
}

function worstLevel(...levels) {
  const order = ['good', 'fine', 'warn', 'bad']
  let worst = 'good'
  for (const l of levels) {
    if (order.indexOf(l) > order.indexOf(worst)) worst = l
  }
  return worst
}

function sstSummary(sst) {
  const anomaly = sst.anomaly || 0
  const sign = anomaly > 0 ? '+' : ''
  const trend = sst.trend === 'rising' ? '↑' : sst.trend === 'falling' ? '↓' : '→'
  return `SST ${trend} ${sign}${anomaly.toFixed(1)}°C`
}

function chlSummary(chl) {
  const trend = chl.trend === 'rising' ? '上升' : chl.trend === 'falling' ? '下降' : '平稳'
  return `Chl ${chl.avg.toFixed(1)} mg/m³，趋势${trend}`
}

function heatwaveSummary(hw) {
  if (hw.active) return `海洋热浪活跃，已持续 ${hw.days} 天`
  return '无海洋热浪'
}

function buildAdvice(sstLevel, chlLevel, hwLevel) {
  const advices = []
  if (hwLevel === 'bad') {
    advices.push('远海渔业注意水温变化，评估对远洋作业的潜在影响')
  }
  if (sstLevel === 'bad' || sstLevel === 'warn') {
    advices.push('关注未来 3 天 SST 变化趋势')
  }
  if (chlLevel === 'bad' || chlLevel === 'warn') {
    advices.push('赤潮风险升高，建议加强监测')
  }
  if (advices.length === 0) {
    advices.push('各项指标正常，可正常作业')
  }
  return advices
}

export function buildZoneAssessment(zone) {
  const sstLevel = assessSst(zone.sst)
  const chlLevel = assessChl(zone.chl)
  const hwLevel = assessHeatwave(zone.heatwave)
  const overallLevel = worstLevel(sstLevel, chlLevel, hwLevel)

  return {
    id: zone.id,
    label: zone.label,
    overall: {
      level: overallLevel,
      ...LEVELS[overallLevel]
    },
    sst: {
      level: sstLevel,
      ...LEVELS[sstLevel],
      summary: sstSummary(zone.sst),
      value: zone.sst.avg,
      max: zone.sst.max,
      anomaly: zone.sst.anomaly,
      trend: zone.sst.trend
    },
    chl: {
      level: chlLevel,
      ...LEVELS[chlLevel],
      summary: chlSummary(zone.chl),
      value: zone.chl.avg,
      max: zone.chl.max,
      trend: zone.chl.trend
    },
    heatwave: {
      level: hwLevel,
      ...LEVELS[hwLevel],
      summary: heatwaveSummary(zone.heatwave),
      active: zone.heatwave.active,
      days: zone.heatwave.days
    },
    advice: buildAdvice(sstLevel, chlLevel, hwLevel)
  }
}

export function buildOverallSummary(assessments) {
  const badOnes = assessments.filter(a => a.overall.level === 'bad' || a.overall.level === 'warn')
  if (badOnes.length === 0) {
    return '当前海洋状况总体良好，所有子区域各项指标均在正常范围内。'
  }
  const names = badOnes.map(a => a.label).join('、')
  const issues = badOnes.map(a => {
    const parts = []
    if (a.sst.level === 'bad' || a.sst.level === 'warn') parts.push('SST 异常')
    if (a.heatwave.level === 'bad') parts.push('海洋热浪')
    if (a.chl.level === 'bad' || a.chl.level === 'warn') parts.push('赤潮风险')
    return parts.join('/')
  }).join('；')
  return `${names}区域需关注：${issues}。`
}
```

- [ ] **Step 2: 提交**

```bash
git add ocean-web/src/utils/health-assessment.js
git commit -m "feat: add frontend health assessment engine with thresholds and advice"
```

---

### Task 6: 前端页面组件

**Files:**
- Create: `ocean-web/src/views/health/OceanHealthView.vue`

- [ ] **Step 1: 创建 OceanHealthView.vue**

```vue
<template>
  <div class="ocean-health">
    <!-- top summary banner -->
    <div class="health-banner" :class="`health-banner--${bannerLevel}`">
      <span class="health-banner__icon">{{ bannerIcon }}</span>
      <span class="health-banner__text">{{ bannerText }}</span>
      <span class="health-banner__date">基于 {{ forecastDate }} 预报数据</span>
    </div>

    <!-- date picker -->
    <div class="health-toolbar">
      <el-date-picker
        v-model="forecastDate"
        type="date"
        placeholder="选择日期"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        @change="fetchData"
      />
    </div>

    <!-- zone card grid -->
    <div v-loading="loading" class="health-grid">
      <div
        v-for="zone in assessments"
        :key="zone.id"
        class="health-card"
        :class="{
          'health-card--active': selectedId === zone.id,
          'health-card--dimmed': selectedId && selectedId !== zone.id
        }"
        @click="selectZone(zone.id)"
      >
        <div class="health-card__label">{{ zone.label }}</div>
        <div class="health-card__body">
          <div
            class="health-card__badge"
            :style="{ background: zone.overall.color }"
          >{{ zone.overall.label }}</div>
          <div class="health-card__info">
            <div class="health-card__level">{{ levelText[zone.overall.level] }}</div>
            <div class="health-card__hint">{{ primaryConcern(zone) }}</div>
          </div>
        </div>
        <div class="health-card__tags">
          <span :class="tagClass(zone.sst.level)">SST {{ zone.sst.trend === 'rising' ? '↑' : zone.sst.trend === 'falling' ? '↓' : '→' }}</span>
          <span :class="tagClass(zone.chl.level)">Chl {{ zone.chl.trend === 'rising' ? '↑' : zone.chl.trend === 'falling' ? '↓' : '→' }}</span>
          <span :class="tagClass(zone.heatwave.level)">热浪 {{ zone.heatwave.active ? '有' : '无' }}</span>
        </div>

        <!-- expand detail -->
        <div v-if="selectedId === zone.id" class="health-card__detail">
          <p class="health-card__interpretation">{{ buildInterpretation(zone) }}</p>
          <table class="health-card__table">
            <tr>
              <td>SST 当前值</td>
              <td>{{ zone.sst.value.toFixed(1) }}°C（{{ zone.sst.anomaly > 0 ? '+' : '' }}{{ zone.sst.anomaly.toFixed(1) }}°C）</td>
              <td><span class="level-tag" :style="{ background: zone.sst.color }">{{ zone.sst.label }}</span></td>
            </tr>
            <tr>
              <td>SST 趋势</td>
              <td>{{ zone.sst.trend === 'rising' ? '上升' : zone.sst.trend === 'falling' ? '下降' : '平稳' }}</td>
              <td><span class="level-tag" :style="{ background: zone.sst.level === 'bad' || zone.sst.level === 'warn' ? '#ef4444' : '#22c55e' }">{{ zone.sst.level === 'bad' || zone.sst.level === 'warn' ? '关注' : '正常' }}</span></td>
            </tr>
            <tr>
              <td>Chl 浓度</td>
              <td>{{ zone.chl.value.toFixed(1) }} mg/m³</td>
              <td><span class="level-tag" :style="{ background: zone.chl.color }">{{ zone.chl.label }}</span></td>
            </tr>
            <tr>
              <td>海洋热浪</td>
              <td>{{ zone.heatwave.active ? '已持续 ' + zone.heatwave.days + ' 天' : '未见异常' }}</td>
              <td><span class="level-tag" :style="{ background: zone.heatwave.color }">{{ zone.heatwave.label }}</span></td>
            </tr>
          </table>
          <div class="health-card__advice">
            <strong>建议：</strong>
            <ul>
              <li v-for="(item, i) in zone.advice" :key="i">{{ item }}</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getZoneHealth } from '../../api/health'
import { buildZoneAssessment, buildOverallSummary } from '../../utils/health-assessment'

const forecastDate = ref('2026-01-01')
const loading = ref(false)
const rawData = ref(null)
const selectedId = ref(null)

const assessments = ref([])

const levelText = { good: '优良', fine: '良好', warn: '中等', bad: '较差' }

const bannerLevel = computed(() => {
  if (!assessments.value.length) return 'fine'
  const worst = assessments.value.reduce((w, a) => {
    const order = ['good', 'fine', 'warn', 'bad']
    return order.indexOf(a.overall.level) > order.indexOf(w) ? a.overall.level : w
  }, 'good')
  return worst
})

const bannerIcon = computed(() => {
  const icons = { good: '&#9989;', fine: '&#9989;', warn: '&#9888;&#65039;', bad: '&#128308;' }
  return icons[bannerLevel.value]
})

const bannerText = computed(() => {
  if (!rawData.value) return '加载中...'
  return buildOverallSummary(assessments.value)
})

function tagClass(level) {
  return `health-tag health-tag--${level}`
}

function primaryConcern(zone) {
  if (zone.heatwave.active) return '海洋热浪活跃'
  if (zone.sst.level === 'bad' || zone.sst.level === 'warn') return `SST 偏高 ${zone.sst.anomaly.toFixed(1)}°C`
  if (zone.chl.level === 'bad' || zone.chl.level === 'warn') return `Chl ${zone.chl.value.toFixed(1)} mg/m³`
  return '各项正常'
}

function buildInterpretation(zone) {
  const parts = []
  parts.push(`${zone.label}海域`)
  if (zone.sst.anomaly) {
    const sign = zone.sst.anomaly > 0 ? '偏高' : '偏低'
    parts.push(`SST 较常年同期${sign} ${Math.abs(zone.sst.anomaly).toFixed(1)}°C`)
  }
  if (zone.heatwave.active) {
    parts.push(`海洋热浪持续活跃，已维持 ${zone.heatwave.days} 天`)
  }
  if (zone.chl.level === 'bad' || zone.chl.level === 'warn') {
    parts.push(`叶绿素浓度偏高，赤潮风险需关注`)
  } else {
    parts.push(`叶绿素浓度正常，暂无赤潮风险`)
  }
  return parts.join('，') + '。'
}

function selectZone(id) {
  selectedId.value = selectedId.value === id ? null : id
}

async function fetchData() {
  loading.value = true
  selectedId.value = null
  try {
    const res = await getZoneHealth({
      centerLon: 122.5,
      centerLat: 29.5,
      coastLon: 121.5,
      forecastDate: forecastDate.value
    })
    rawData.value = res.data
    assessments.value = (res.data.zones || []).map(buildZoneAssessment)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.ocean-health {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.health-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 14px;
}

.health-banner--good,
.health-banner--fine {
  background: #dcfce7;
  color: #166534;
}

.health-banner--warn {
  background: #fef3c7;
  color: #92400e;
}

.health-banner--bad {
  background: #fee2e2;
  color: #991b1b;
}

.health-banner__date {
  margin-left: auto;
  font-size: 12px;
  opacity: 0.7;
}

.health-toolbar {
  margin-bottom: 18px;
}

.health-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.health-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: opacity 0.2s, border-color 0.2s, box-shadow 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.health-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.health-card--active {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59,130,246,0.15);
}

.health-card--dimmed {
  opacity: 0.45;
}

.health-card__label {
  font-size: 12px;
  color: #888;
  margin-bottom: 8px;
}

.health-card__body {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.health-card__badge {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.health-card__info {
  flex: 1;
}

.health-card__level {
  font-weight: 600;
  font-size: 15px;
}

.health-card__hint {
  font-size: 12px;
  color: #666;
  margin-top: 2px;
}

.health-card__tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.health-tag {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.health-tag--good,
.health-tag--fine {
  background: #dcfce7;
  color: #166534;
}

.health-tag--warn {
  background: #fef3c7;
  color: #92400e;
}

.health-tag--bad {
  background: #fee2e2;
  color: #991b1b;
}

.health-card__detail {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.health-card__interpretation {
  font-size: 13px;
  color: #333;
  margin: 0 0 10px;
  line-height: 1.6;
}

.health-card__table {
  width: 100%;
  font-size: 12px;
  border-collapse: collapse;
  margin-bottom: 10px;
}

.health-card__table td {
  padding: 5px 0;
  border-bottom: 1px solid #f5f5f5;
}

.level-tag {
  color: #fff;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 11px;
}

.health-card__advice {
  background: #f9fafb;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 12px;
}

.health-card__advice ul {
  margin: 4px 0 0;
  padding-left: 16px;
}

.health-card__advice li {
  margin-bottom: 4px;
  color: #555;
}

@media (max-width: 800px) {
  .health-grid {
    grid-template-columns: 1fr;
  }
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add ocean-web/src/views/health/OceanHealthView.vue
git commit -m "feat: add OceanHealthView page with zone cards, expand detail, and date picker"
```

---

### Task 7: 路由与导航

**Files:**
- Modify: `ocean-web/src/router/index.js`
- Modify: `ocean-web/src/layout/MainLayout.vue`

- [ ] **Step 1: 在 router/index.js 添加路由**

在 `children` 数组的 `ocean-data` 路由之后添加：

```js
      {
        path: 'ocean-health',
        name: 'OceanHealth',
        component: () => import('../views/health/OceanHealthView.vue'),
        meta: { title: '海洋健康指数' }
      }
```

- [ ] **Step 2: 在 MainLayout.vue 导航栏添加入口**

在"观测"导航项之后添加：

```html
        <router-link
          to="/app/ocean-health"
          class="editorial-nav__item"
          :class="{ 'editorial-nav__item--active': isActive('/app/ocean-health') }"
        >健康</router-link>
```

- [ ] **Step 3: 提交**

```bash
git add ocean-web/src/router/index.js ocean-web/src/layout/MainLayout.vue
git commit -m "feat: add /app/ocean-health route and nav entry"
```

---

### Task 8: 验证测试

**Files:** 无新建文件

- [ ] **Step 1: 启动后端确认接口可用**

```bash
# 启动 ocean-server 后测试接口
curl "http://localhost:8080/api/forecast/zone-health?centerLon=122.5&centerLat=29.5&coastLon=121.5&forecastDate=2026-01-01"
```

期望：返回 `{"code":200,"data":{"zoneName":"东海","zones":[...长度为6的数组...]}}`，每个 zone 含 `sst`/`chl`/`heatwave` 字段。

- [ ] **Step 2: 启动前端确认页面渲染**

```bash
cd ocean-web && npm run dev
```

打开浏览器访问 `http://localhost:5173/app/ocean-health`，确认：
- 页面正常加载，日期选择器默认 2026-01-01
- 6 张卡片渲染，各有等级和标签
- 点击卡片，其他变暗，详情展开
- 再次点击收起

- [ ] **Step 3: 提交**（如有修复）

```bash
git add . && git commit -m "fix: verification fixes"
```

---

### Task 9: Spec 自检

- [ ] **Spec coverage check:** 逐条对照 spec 确认全部实现
  - `/app/ocean-health` 路由：Task 7
  - OceanHealthView.vue 页面：Task 6
  - 健康指数 API：Task 1-3
  - 阈值引擎：Task 5
  - 3×2 卡片矩阵，等级/标签/解读：Task 6
  - 点击展开详情 + 其他变暗：Task 6
  - 分区规则(方位 × 离岸距离)：Task 3
  - 等级体系(优/良/中/差)：Task 5
  - 现有功能无影响：确认无现有文件修改（仅 router/nav 新增条目）
