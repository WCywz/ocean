# 预报数据地图可视化 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将预报数据可视化模块重构为三个子模块（SST 地图、Chl 地图、历史记录），新增 Leaflet 交互式地图视图，支持服务端网格聚合和动态筛选。

**Architecture:** Leaflet + OSM 瓦片作为地图底图，Canvas 覆盖层渲染聚合网格散点。ECharts 复用现有 chart-config.js 绘制趋势折线图。后端新增三个 API 端点，所有 SQL 遵循「先 WHERE 过滤，后 GROUP BY 聚合」原则。

**Tech Stack:** Vue 3 + Vite + Element Plus + Leaflet + ECharts (前端), Spring Boot 3 + MyBatis-Plus + MySQL (后端)

---

## 文件结构

### 新建
- `ocean-server/src/main/java/com/ocean/dto/MapGridQueryDTO.java` — 地图网格查询参数
- `ocean-server/src/main/java/com/ocean/config/SeaAreaConfig.java` — 预设海域 bbox 配置
- `ocean-web/src/components/OceanMap.vue` — Leaflet 地图共享组件
- `ocean-web/src/components/TrendChart.vue` — ECharts 趋势折线图共享组件
- `ocean-web/src/views/forecast/SstMapView.vue` — SST 地图页
- `ocean-web/src/views/forecast/ChxMapView.vue` — Chl 地图页
- `ocean-web/src/views/forecast/HistoryView.vue` — 历史记录表格页

### 修改
- `ocean-server/src/main/java/com/ocean/mapper/ForecastRecordMapper.java` — 新增聚合查询方法
- `ocean-server/src/main/java/com/ocean/service/ForecastRecordService.java` — 新增接口方法
- `ocean-server/src/main/java/com/ocean/service/impl/ForecastRecordServiceImpl.java` — 新增实现
- `ocean-server/src/main/java/com/ocean/controller/ForecastRecordController.java` — 新增端点
- `ocean-web/src/utils/chart-config.js` — 新增地图颜色常量
- `ocean-web/src/api/forecast.js` — 新增 API 函数
- `ocean-web/src/router/index.js` — 新增路由
- `ocean-web/src/layout/MainLayout.vue` — 侧边栏改为子菜单
- `database/init.sql` — 新增索引

### 可能废弃
- `ocean-web/src/views/forecast/ForecastView.vue` — 拆分为新页面后删除

---

### Task 1: 数据库索引

**Files:**
- Modify: `database/init.sql`

- [ ] **Step 1: 在 init.sql 末尾添加复合索引**

在 `database/init.sql` 末尾添加：

```sql
-- 地图网格聚合查询索引（先过滤后聚合）
CREATE INDEX idx_filter ON forecast_record (data_type, forecast_date, longitude, latitude);

-- 单点位趋势查询索引
CREATE INDEX idx_point_trend ON forecast_record (data_type, longitude, latitude, forecast_date);
```

- [ ] **Step 2: 在生产数据库执行索引**

如果生产库已有数据，手动执行：

```sql
CREATE INDEX IF NOT EXISTS idx_filter ON forecast_record (data_type, forecast_date, longitude, latitude);
CREATE INDEX IF NOT EXISTS idx_point_trend ON forecast_record (data_type, longitude, latitude, forecast_date);
```

- [ ] **Step 3: 提交**

```bash
git add database/init.sql
git commit -m "feat: add composite indexes for map grid aggregation queries"
```

---

### Task 2: MapGridQueryDTO

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/dto/MapGridQueryDTO.java`

- [ ] **Step 1: 创建 DTO**

```java
package com.ocean.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 地图网格聚合查询参数
 */
@Data
public class MapGridQueryDTO {

    /** 数据类型: SST / CHL */
    private String dataType;

    /** 预报日期 */
    private String forecastDate;

    /** 日期范围开始 (Chl 概率模式) */
    private String dateStart;

    /** 日期范围结束 (Chl 概率模式) */
    private String dateEnd;

    /** 聚合精度（度），默认 0.05 */
    private Double precision;

    /** 可视范围西边界 */
    private BigDecimal minLon;

    /** 可视范围东边界 */
    private BigDecimal maxLon;

    /** 可视范围南边界 */
    private BigDecimal minLat;

    /** 可视范围北边界 */
    private BigDecimal maxLat;

    /** Chl 渲染模式: concentration / probability */
    private String chlMode;

    /** 概率模式超阈值，默认 3.0 */
    private Double threshold;
}
```

- [ ] **Step 2: 提交**

```bash
git add ocean-server/src/main/java/com/ocean/dto/MapGridQueryDTO.java
git commit -m "feat: add MapGridQueryDTO for map grid aggregation queries"
```

---

### Task 3: ForecastRecordMapper 新增聚合查询

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/mapper/ForecastRecordMapper.java`

- [ ] **Step 1: 添加网格聚合和点位趋势方法**

在 `ForecastRecordMapper` 接口中添加：

```java
/**
 * 网格聚合查询 — SST / CHL 浓度模式
 * 先按 dataType + date + bbox 过滤，再按精度聚合取平均
 */
@Select("<script>" +
    "SELECT ROUND(longitude, #{precision}) AS gridLon, " +
    "       ROUND(latitude, #{precision}) AS gridLat, " +
    "       AVG(value) AS value " +
    "FROM forecast_record " +
    "WHERE data_type = #{dataType} " +
    "  AND forecast_date = #{forecastDate} " +
    "  <if test='minLon != null'> AND longitude &gt;= #{minLon} </if>" +
    "  <if test='maxLon != null'> AND longitude &lt;= #{maxLon} </if>" +
    "  <if test='minLat != null'> AND latitude &gt;= #{minLat} </if>" +
    "  <if test='maxLat != null'> AND latitude &lt;= #{maxLat} </if>" +
    "GROUP BY gridLon, gridLat " +
    "ORDER BY gridLat, gridLon" +
    "</script>")
List<Map<String, Object>> selectAggregatedGrid(MapGridQueryDTO dto);

/**
 * 网格聚合查询 — Chl 概率模式
 * 限定日期范围和海域后，统计每格超阈值比例
 */
@Select("<script>" +
    "SELECT ROUND(longitude, #{precision}) AS gridLon, " +
    "       ROUND(latitude, #{precision}) AS gridLat, " +
    "       ROUND(SUM(CASE WHEN value &gt; #{threshold} THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) AS probability " +
    "FROM forecast_record " +
    "WHERE data_type = 'CHL' " +
    "  AND forecast_date BETWEEN #{dateStart} AND #{dateEnd} " +
    "  <if test='minLon != null'> AND longitude &gt;= #{minLon} </if>" +
    "  <if test='maxLon != null'> AND longitude &lt;= #{maxLon} </if>" +
    "  <if test='minLat != null'> AND latitude &gt;= #{minLat} </if>" +
    "  <if test='maxLat != null'> AND latitude &lt;= #{maxLat} </if>" +
    "GROUP BY gridLon, gridLat " +
    "ORDER BY gridLat, gridLon" +
    "</script>")
List<Map<String, Object>> selectProbabilityGrid(MapGridQueryDTO dto);

/**
 * 单点位趋势 — 查最近点位所有日期的值
 * Step 1: 子查询找最近点位; Step 2: 按点位 + 日期范围查值
 */
@Select("<script>" +
    "SELECT forecast_date AS forecastDate, value " +
    "FROM forecast_record " +
    "WHERE data_type = #{dataType} " +
    "  AND longitude = #{lon} " +
    "  AND latitude = #{lat} " +
    "  <if test='dateStart != null'> AND forecast_date &gt;= #{dateStart} </if>" +
    "  <if test='dateEnd != null'> AND forecast_date &lt;= #{dateEnd} </if>" +
    "ORDER BY forecast_date" +
    "</script>")
List<Map<String, Object>> selectPointTrend(@org.apache.ibatis.annotations.Param("dataType") String dataType,
                                           @org.apache.ibatis.annotations.Param("lon") BigDecimal lon,
                                           @org.apache.ibatis.annotations.Param("lat") BigDecimal lat,
                                           @org.apache.ibatis.annotations.Param("dateStart") String dateStart,
                                           @org.apache.ibatis.annotations.Param("dateEnd") String dateEnd);
```

- [ ] **Step 2: 提交**

```bash
git add ocean-server/src/main/java/com/ocean/mapper/ForecastRecordMapper.java
git commit -m "feat: add mapper methods for grid aggregation and point trend"
```

---

### Task 4: Service 接口和实现

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/service/ForecastRecordService.java`
- Modify: `ocean-server/src/main/java/com/ocean/service/impl/ForecastRecordServiceImpl.java`

- [ ] **Step 1: 在接口中添加方法签名**

在 `ForecastRecordService.java` 中添加：

```java
/** 地图网格聚合数据 */
List<Map<String, Object>> getMapGrid(MapGridQueryDTO dto);

/** 单点位历史趋势 */
List<Map<String, Object>> getPointTrend(String dataType, BigDecimal lon, BigDecimal lat, String dateStart, String dateEnd);

/** 预设海域配置 */
List<Map<String, Object>> getSeaAreas();
```

- [ ] **Step 2: 在实现类中添加实现**

在 `ForecastRecordServiceImpl.java` 中添加：

```java
@Override
public List<Map<String, Object>> getMapGrid(MapGridQueryDTO dto) {
    if (dto.getPrecision() == null) {
        dto.setPrecision(0.05);
    }
    if ("probability".equals(dto.getChlMode())) {
        if (dto.getThreshold() == null) {
            dto.setThreshold(3.0);
        }
        return forecastRecordMapper.selectProbabilityGrid(dto);
    }
    return forecastRecordMapper.selectAggregatedGrid(dto);
}

@Override
public List<Map<String, Object>> getPointTrend(String dataType, BigDecimal lon, BigDecimal lat,
                                                String dateStart, String dateEnd) {
    return forecastRecordMapper.selectPointTrend(dataType, lon, lat, dateStart, dateEnd);
}
```

- [ ] **Step 3: 提交**

```bash
git add ocean-server/src/main/java/com/ocean/service/ForecastRecordService.java ocean-server/src/main/java/com/ocean/service/impl/ForecastRecordServiceImpl.java
git commit -m "feat: add service methods for map grid, point trend, and sea areas"
```

---

### Task 5: SeaAreaConfig 预设海域配置

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/config/SeaAreaConfig.java`

- [ ] **Step 1: 创建配置类**

```java
package com.ocean.config;

import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.*;

/**
 * 预设海域 bbox 配置
 * bbox 顺序: [minLon, minLat, maxLon, maxLat]
 */
@Configuration
public class SeaAreaConfig {

    public List<Map<String, Object>> getSeaAreas() {
        List<Map<String, Object>> areas = new ArrayList<>();

        areas.add(area("全部海域",  121.33, 26.92, 125.58, 32.67));
        areas.add(area("北部海域",  121.33, 30.00, 125.58, 32.67));
        areas.add(area("南部海域",  121.33, 26.92, 125.58, 30.00));
        areas.add(area("近岸海域",  121.33, 26.92, 122.50, 32.67));
        areas.add(area("远海海域",  122.50, 26.92, 125.58, 32.67));

        return areas;
    }

    private Map<String, Object> area(String name, double minLon, double minLat, double maxLon, double maxLat) {
        return Map.of(
            "name", name,
            "minLon", BigDecimal.valueOf(minLon),
            "maxLon", BigDecimal.valueOf(maxLon),
            "minLat", BigDecimal.valueOf(minLat),
            "maxLat", BigDecimal.valueOf(maxLat)
        );
    }
}
```

- [ ] **Step 2: 在 ServiceImpl 中注入并实现 getSeaAreas**

在 `ForecastRecordServiceImpl.java` 中添加：

```java
@Autowired
private SeaAreaConfig seaAreaConfig;

@Override
public List<Map<String, Object>> getSeaAreas() {
    return seaAreaConfig.getSeaAreas();
}
```

- [ ] **Step 3: 提交**

```bash
git add ocean-server/src/main/java/com/ocean/config/SeaAreaConfig.java ocean-server/src/main/java/com/ocean/service/impl/ForecastRecordServiceImpl.java
git commit -m "feat: add SeaAreaConfig with predefined bounding boxes"
```

---

### Task 6: Controller 新增端点

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/controller/ForecastRecordController.java`

- [ ] **Step 1: 添加三个新端点**

在 `ForecastRecordController.java` 中添加：

```java
/**
 * 获取地图网格聚合数据
 */
@GetMapping("/map/grid")
public Result<List<Map<String, Object>>> getMapGrid(@Validated MapGridQueryDTO dto) {
    List<Map<String, Object>> data = forecastRecordService.getMapGrid(dto);
    return Result.success(data);
}

/**
 * 获取单点位历史趋势
 */
@GetMapping("/trend/point")
public Result<List<Map<String, Object>>> getPointTrend(
        @RequestParam String dataType,
        @RequestParam BigDecimal lon,
        @RequestParam BigDecimal lat,
        @RequestParam(required = false) String dateStart,
        @RequestParam(required = false) String dateEnd) {
    List<Map<String, Object>> data = forecastRecordService.getPointTrend(dataType, lon, lat, dateStart, dateEnd);
    return Result.success(data);
}

/**
 * 获取预设海域配置
 */
@GetMapping("/sea-areas")
public Result<List<Map<String, Object>>> getSeaAreas() {
    List<Map<String, Object>> data = forecastRecordService.getSeaAreas();
    return Result.success(data);
}
```

确保类顶部导入 `MapGridQueryDTO`：

```java
import com.ocean.dto.MapGridQueryDTO;
```

- [ ] **Step 2: 提交**

```bash
git add ocean-server/src/main/java/com/ocean/controller/ForecastRecordController.java
git commit -m "feat: add /map/grid, /trend/point, /sea-areas endpoints"
```

---

### Task 7: 前端安装 Leaflet 依赖

**Files:**
- Modify: `ocean-web/package.json`

- [ ] **Step 1: 安装 leaflet 和 leaflet-draw**

```bash
cd ocean-web && npm install leaflet leaflet-draw
```

- [ ] **Step 2: 验证安装**

Run: `cd ocean-web && node -e "require('leaflet'); console.log('Leaflet OK')"`
Expected: `Leaflet OK`

- [ ] **Step 3: 提交**

```bash
git add ocean-web/package.json ocean-web/package-lock.json
git commit -m "chore: add leaflet and leaflet-draw dependencies"
```

---

### Task 8: chart-config.js 新增地图颜色配置

**Files:**
- Modify: `ocean-web/src/utils/chart-config.js`
- Modify: `ocean-web/src/utils/__tests__/chart-config.test.js`

- [ ] **Step 1: 编写测试**

在 `chart-config.test.js` 末尾添加：

```js
import { SST_MAP_COLORS, CHL_CONC_COLORS, CHL_PROB_COLORS, getMapColor } from '../chart-config'

describe('map color configs', () => {
  it('SST_MAP_COLORS has 5 temperature ranges', () => {
    expect(SST_MAP_COLORS).toHaveLength(5)
    expect(SST_MAP_COLORS[0]).toEqual({ min: -Infinity, max: 16, color: '#1A5276', label: '<16°C' })
    expect(SST_MAP_COLORS[4]).toEqual({ min: 28, max: Infinity, color: '#E74C3C', label: '>28°C' })
  })

  it('CHL_CONC_COLORS has 5 concentration ranges', () => {
    expect(CHL_CONC_COLORS).toHaveLength(5)
    expect(CHL_CONC_COLORS[0].color).toBe('#0B5345')
    expect(CHL_CONC_COLORS[4].color).toBe('#2ECC71')
  })

  it('CHL_PROB_COLORS has 5 probability ranges', () => {
    expect(CHL_PROB_COLORS).toHaveLength(5)
    expect(CHL_PROB_COLORS[0]).toEqual({ min: -Infinity, max: 20, color: '#27AE60', label: '<20%' })
  })

  it('getMapColor returns correct color for value', () => {
    expect(getMapColor(15, SST_MAP_COLORS)).toBe('#1A5276')
    expect(getMapColor(22, SST_MAP_COLORS)).toBe('#F39C12')
    expect(getMapColor(30, SST_MAP_COLORS)).toBe('#E74C3C')
  })

  it('getMapColor returns fallback for undefined value', () => {
    expect(getMapColor(null, SST_MAP_COLORS)).toBe('#999')
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd ocean-web && npx vitest run src/utils/__tests__/chart-config.test.js`
Expected: FAIL — `SST_MAP_COLORS not exported from chart-config.js`

- [ ] **Step 3: 在 chart-config.js 中添加地图颜色常量**

在 `chart-config.js` 末尾添加：

```js
// ---- Map color configs ----

export const SST_MAP_COLORS = [
  { min: -Infinity, max: 16,  color: '#1A5276', label: '<16°C' },
  { min: 16,       max: 20,  color: '#2E86C1', label: '16-20°C' },
  { min: 20,       max: 24,  color: '#F39C12', label: '20-24°C' },
  { min: 24,       max: 28,  color: '#E67E22', label: '24-28°C' },
  { min: 28,       max: Infinity, color: '#E74C3C', label: '>28°C' }
]

export const CHL_CONC_COLORS = [
  { min: -Infinity, max: 0.5,  color: '#0B5345', label: '<0.5 mg/m³' },
  { min: 0.5,      max: 1.5,  color: '#148F77', label: '0.5-1.5' },
  { min: 1.5,      max: 3.0,  color: '#1ABC9C', label: '1.5-3.0' },
  { min: 3.0,      max: 5.0,  color: '#27AE60', label: '3.0-5.0' },
  { min: 5.0,      max: Infinity, color: '#2ECC71', label: '>5.0' }
]

export const CHL_PROB_COLORS = [
  { min: -Infinity, max: 20,  color: '#27AE60', label: '<20%' },
  { min: 20,       max: 40,  color: '#F1C40F', label: '20-40%' },
  { min: 40,       max: 60,  color: '#F39C12', label: '40-60%' },
  { min: 60,       max: 80,  color: '#E67E22', label: '60-80%' },
  { min: 80,       max: Infinity, color: '#E74C3C', label: '>80%' }
]

/**
 * Get the color for a value from a color range config array.
 */
export function getMapColor(value, colorRanges) {
  if (value == null) return '#999'
  for (const range of colorRanges) {
    if (value > range.min && value <= range.max) return range.color
    if (value <= range.min && range.min === -Infinity) return range.color
  }
  return '#999'
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd ocean-web && npx vitest run src/utils/__tests__/chart-config.test.js`
Expected: PASS (all tests, including new ones)

- [ ] **Step 5: 提交**

```bash
git add ocean-web/src/utils/chart-config.js ocean-web/src/utils/__tests__/chart-config.test.js
git commit -m "feat: add map color configs and getMapColor utility for Leaflet rendering"
```

---

### Task 9: forecast.js 新增 API 函数

**Files:**
- Modify: `ocean-web/src/api/forecast.js`

- [ ] **Step 1: 添加新的 API 函数**

在 `forecast.js` 末尾添加：

```js
/** 获取地图网格聚合数据 */
export function getMapGrid(params) {
  return request({ url: '/forecast/map/grid', method: 'get', params })
}

/** 获取单点位历史趋势 */
export function getPointTrend(params) {
  return request({ url: '/forecast/trend/point', method: 'get', params })
}

/** 获取预设海域配置 */
export function getSeaAreas() {
  return request({ url: '/forecast/sea-areas', method: 'get' })
}
```

- [ ] **Step 2: 提交**

```bash
git add ocean-web/src/api/forecast.js
git commit -m "feat: add map grid, point trend, and sea areas API functions"
```

---

### Task 10: TrendChart 共享组件

**Files:**
- Create: `ocean-web/src/components/TrendChart.vue`

- [ ] **Step 1: 创建 TrendChart.vue**

```vue
<template>
  <div v-loading="loading" class="trend-chart-container" ref="chartRef">
    <div v-if="empty" class="trend-chart-empty">点击地图上的网格以查看趋势</div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { buildBaseOption, buildTooltipFormatter } from '../utils/chart-config'

const props = defineProps({
  seriesData: { type: Array, default: () => [] },
  xAxisData: { type: Array, default: () => [] },
  yAxisName: { type: String, default: '' },
  yAxisUnit: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  colors: { type: Array, default: () => ['#3498DB'] }
})

const chartRef = ref(null)
const empty = ref(true)
let chart = null

function render() {
  if (!chart) return
  if (!props.seriesData.length || !props.xAxisData.length) {
    empty.value = true
    chart.clear()
    return
  }
  empty.value = false

  const legendData = props.seriesData.map(s => s.name)
  const base = buildBaseOption({
    legendData,
    xAxisData: props.xAxisData,
    yAxisName: props.yAxisName,
    yAxisUnit: props.yAxisUnit
  })
  base.tooltip.formatter = buildTooltipFormatter(props.yAxisUnit)

  const series = props.seriesData.map((s) => ({
    name: s.name,
    type: 'line',
    smooth: true,
    symbol: 'circle',
    symbolSize: 4,
    lineStyle: { width: 2 },
    data: s.data
  }))

  chart.setOption({ ...base, series, color: props.colors }, true)
}

watch(() => [props.seriesData, props.xAxisData, props.loading], () => {
  nextTick(() => render())
}, { deep: true })

onMounted(() => {
  nextTick(() => {
    chart = echarts.init(chartRef.value)
    render()
    window.addEventListener('resize', () => chart?.resize())
  })
})

onUnmounted(() => {
  chart?.dispose()
})
</script>

<style scoped>
.trend-chart-container {
  width: 100%;
  height: 300px;
}
.trend-chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  font-size: 14px;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add ocean-web/src/components/TrendChart.vue
git commit -m "feat: add TrendChart shared component for ECharts trend line charts"
```

---

### Task 11: OceanMap 共享组件

**Files:**
- Create: `ocean-web/src/components/OceanMap.vue`

- [ ] **Step 1: 创建 OceanMap.vue**

```vue
<template>
  <div class="ocean-map-wrapper">
    <div v-loading="loading" class="ocean-map-container" ref="mapContainer"></div>

    <!-- Legend overlay -->
    <div class="map-legend" v-if="legendLabels.length">
      <div class="legend-title">{{ legendTitle }}</div>
      <div
        v-for="(item, idx) in legendLabels"
        :key="idx"
        class="legend-item"
      >
        <span class="legend-dot" :style="{ background: legendColors[idx] }"></span>
        <span class="legend-label">{{ item }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import 'leaflet-draw'
import 'leaflet-draw/dist/leaflet.draw.css'
import { getMapColor } from '../utils/chart-config'

const props = defineProps({
  gridData: { type: Array, default: () => [] },
  colorRanges: { type: Array, default: () => [] },
  legendLabels: { type: Array, default: () => [] },
  legendTitle: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  center: { type: Array, default: () => [29.8, 123.5] },
  zoom: { type: Number, default: 7 }
})

const emit = defineEmits(['cellClick', 'bboxChange'])

const mapContainer = ref(null)
let map = null
let canvasLayer = null
let drawControl = null
let drawnItems = null

const legendColors = computed(() => props.colorRanges.map(r => r.color))

function buildCanvasLayer() {
  return L.canvas({ padding: 0.5 })
}

function drawGrid() {
  if (!map || !props.gridData.length) return

  // Remove old canvas and create fresh one
  if (canvasLayer) {
    map.removeLayer(canvasLayer)
  }

  canvasLayer = L.layerGroup()

  const bounds = map.getBounds()
  const south = bounds.getSouth()
  const north = bounds.getNorth()
  const west = bounds.getWest()
  const east = bounds.getEast()

  // Filter points within current viewport (with padding)
  const visible = props.gridData.filter(p =>
    p.lat >= south && p.lat <= north && p.lon >= west && p.lon <= east
  )

  if (!visible.length) return

  // Group by value color
  const byColor = {}
  visible.forEach(p => {
    const color = getMapColor(p.value, props.colorRanges)
    if (!byColor[color]) byColor[color] = []
    byColor[color].push(p)
  })

  Object.entries(byColor).forEach(([color, points]) => {
    const circles = points.map(p => {
      const latlng = L.latLng(p.lat, p.lon)
      return L.circleMarker(latlng, {
        radius: 4,
        fillColor: color,
        color: color,
        weight: 1,
        opacity: 0.8,
        fillOpacity: 0.7
      })
    })
    const group = L.layerGroup(circles)
    circles.forEach(c => {
      c.on('click', () => {
        const { lat, lng } = c.getLatLng()
        emit('cellClick', { lat, lon: lng, value: points.find(
          p => p.lat === lat && p.lon === lng
        )?.value })
      })
    })
    canvasLayer.addLayer(group)
  })

  canvasLayer.addTo(map)
}

function onMoveEnd() {
  drawGrid()
  const bounds = map.getBounds()
  emit('bboxChange', {
    north: bounds.getNorth(),
    south: bounds.getSouth(),
    east: bounds.getEast(),
    west: bounds.getWest()
  })
}

function onDrawCreated(e) {
  drawnItems.addLayer(e.layer)
  const bounds = e.layer.getBounds()
  emit('bboxChange', {
    north: bounds.getNorth(),
    south: bounds.getSouth(),
    east: bounds.getEast(),
    west: bounds.getWest()
  })
}

function onDrawDeleted() {
  const bounds = map.getBounds()
  emit('bboxChange', {
    north: bounds.getNorth(),
    south: bounds.getSouth(),
    east: bounds.getEast(),
    west: bounds.getWest()
  })
}

function initDraw() {
  drawnItems = new L.FeatureGroup()
  map.addLayer(drawnItems)

  drawControl = new L.Control.Draw({
    draw: {
      polygon: { allowIntersection: false },
      rectangle: {},
      circle: false,
      circlemarker: false,
      marker: false,
      polyline: false
    },
    edit: { featureGroup: drawnItems }
  })
  map.addControl(drawControl)

  map.on(L.Draw.Event.CREATED, onDrawCreated)
  map.on(L.Draw.Event.DELETED, onDrawDeleted)
}

onMounted(() => {
  nextTick(() => {
    map = L.map(mapContainer.value, {
      preferCanvas: true,
      center: props.center,
      zoom: props.zoom
    })

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 18
    }).addTo(map)

    map.on('moveend', onMoveEnd)
    initDraw()
    drawGrid()
  })
})

watch(() => props.gridData, () => {
  nextTick(() => drawGrid())
}, { deep: true })

onUnmounted(() => {
  map?.remove()
})
</script>

<style scoped>
.ocean-map-wrapper {
  position: relative;
  width: 100%;
}
.ocean-map-container {
  width: 100%;
  height: 450px;
  border-radius: 8px;
  overflow: hidden;
}
.map-legend {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  padding: 10px 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  font-size: 12px;
  z-index: 1000;
}
.legend-title {
  font-weight: 600;
  margin-bottom: 6px;
  color: #1a3a5c;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.legend-label {
  color: #555;
  white-space: nowrap;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add ocean-web/src/components/OceanMap.vue
git commit -m "feat: add OceanMap shared component with Leaflet, canvas overlay, and draw controls"
```

---

### Task 12: 路由和侧边栏更新

**Files:**
- Modify: `ocean-web/src/router/index.js`
- Modify: `ocean-web/src/layout/MainLayout.vue`

- [ ] **Step 1: 更新路由**

在 `router/index.js` 中，将 forecast 路由替换为三个子路由：

```js
{
  path: 'forecast/sst',
  name: 'ForecastSst',
  component: () => import('../views/forecast/SstMapView.vue'),
  meta: { title: '海表温度预测' }
},
{
  path: 'forecast/chl',
  name: 'ForecastChl',
  component: () => import('../views/forecast/ChxMapView.vue'),
  meta: { title: '叶绿素预测' }
},
{
  path: 'forecast/history',
  name: 'ForecastHistory',
  component: () => import('../views/forecast/HistoryView.vue'),
  meta: { title: '历史预报记录' }
}
```

删除原来的：
```js
{
  path: 'forecast',
  name: 'Forecast',
  component: () => import('../views/forecast/ForecastView.vue'),
  meta: { title: '预报数据可视化' }
}
```

- [ ] **Step 2: 更新侧边栏为 el-sub-menu**

在 `MainLayout.vue` 中，将原来的 `el-menu-item index="/app/forecast"` 替换为：

```vue
<el-sub-menu index="/app/forecast">
  <template #title>
    <el-icon><TrendCharts /></el-icon>
    <span>预报数据可视化</span>
  </template>
  <el-menu-item index="/app/forecast/sst">
    <template #title>海表温度预测</template>
  </el-menu-item>
  <el-menu-item index="/app/forecast/chl">
    <template #title>叶绿素预测</template>
  </el-menu-item>
  <el-menu-item index="/app/forecast/history">
    <template #title>历史预报记录</template>
  </el-menu-item>
</el-sub-menu>
```

- [ ] **Step 3: 更新 activeMenu 计算属性以匹配子路由**

在 `MainLayout.vue` 的 script 中修改 `activeMenu`：

```js
const activeMenu = computed(() => {
  const path = route.path
  // 匹配子路由时高亮父菜单
  if (path.startsWith('/app/forecast')) return '/app/forecast'
  return path
})
```

- [ ] **Step 4: 提交**

```bash
git add ocean-web/src/router/index.js ocean-web/src/layout/MainLayout.vue
git commit -m "feat: update router and sidebar with forecast sub-menu structure"
```

---

### Task 13: SstMapView 页面

**Files:**
- Create: `ocean-web/src/views/forecast/SstMapView.vue`

- [ ] **Step 1: 创建 SstMapView.vue**

```vue
<template>
  <div class="sst-page">
    <h2 class="page-title">海表温度预测</h2>

    <!-- Filter bar -->
    <el-card shadow="hover" class="filter-card">
      <div class="filter-bar">
        <span class="filter-label">数据筛选</span>
        <el-date-picker
          v-model="filterDate"
          type="date"
          placeholder="选择预报日期"
          value-format="YYYY-MM-DD"
          style="width: 180px"
        />
        <el-select v-model="seaArea" placeholder="海域筛选" style="width: 160px" @change="onSeaAreaChange">
          <el-option
            v-for="area in seaAreas"
            :key="area.name"
            :label="area.name"
            :value="area"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <span class="filter-hint">也可在地图上拖拽框选海域</span>
      </div>
    </el-card>

    <!-- Map -->
    <el-card shadow="hover" class="map-card">
      <OceanMap
        :grid-data="gridData"
        :color-ranges="SST_MAP_COLORS"
        :legend-labels="legendLabels"
        legend-title="温度 (°C)"
        :loading="mapLoading"
        @cell-click="onMapCellClick"
        @bbox-change="onBboxChange"
      />
    </el-card>

    <!-- Trend chart -->
    <el-card shadow="hover" class="trend-card">
      <template #header>
        <div class="trend-header">
          <span class="trend-title">温度变化趋势</span>
          <span v-if="selectedPoint" class="trend-subtitle">
            当前选中: ({{ selectedPoint.lon.toFixed(2) }}, {{ selectedPoint.lat.toFixed(2) }})
          </span>
        </div>
      </template>
      <TrendChart
        :series-data="trendSeries"
        :x-axis-data="trendDates"
        y-axis-name="温度 (°C)"
        y-axis-unit="°C"
        :loading="trendLoading"
        :colors="SST_COLORS"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import OceanMap from '../../components/OceanMap.vue'
import TrendChart from '../../components/TrendChart.vue'
import { getMapGrid, getPointTrend, getSeaAreas } from '../../api/forecast'
import { SST_MAP_COLORS, SST_COLORS } from '../../utils/chart-config'

const filterDate = ref('')
const seaArea = ref(null)
const seaAreas = ref([])
const gridData = ref([])
const mapLoading = ref(false)
const trendSeries = ref([])
const trendDates = ref([])
const trendLoading = ref(false)
const selectedPoint = ref(null)
const customBbox = ref(null)

const legendLabels = ['<16°C', '16-20°C', '20-24°C', '24-28°C', '>28°C']

function todayStr() {
  const d = new Date()
  return d.toISOString().slice(0, 10)
}

function buildBboxParams() {
  const params = {}
  if (customBbox.value) {
    params.minLon = customBbox.value.west
    params.maxLon = customBbox.value.east
    params.minLat = customBbox.value.south
    params.maxLat = customBbox.value.north
  } else if (seaArea.value) {
    params.minLon = seaArea.value.minLon
    params.maxLon = seaArea.value.maxLon
    params.minLat = seaArea.value.minLat
    params.maxLat = seaArea.value.maxLat
  }
  return params
}

async function fetchGridData() {
  mapLoading.value = true
  try {
    const params = {
      dataType: 'SST',
      forecastDate: filterDate.value || todayStr(),
      precision: 0.05,
      ...buildBboxParams()
    }
    const res = await getMapGrid(params)
    gridData.value = (res.data || []).map(r => ({
      lon: r.gridLon != null ? Number(r.gridLon) : Number(r.longitude),
      lat: r.gridLat != null ? Number(r.gridLat) : Number(r.latitude),
      value: r.value
    }))
  } finally {
    mapLoading.value = false
  }
}

async function fetchTrendData(lon, lat) {
  trendLoading.value = true
  try {
    const res = await getPointTrend({ dataType: 'SST', lon, lat })
    const points = res.data || []
    trendDates.value = points.map(p => p.forecastDate)
    trendSeries.value = [{
      name: `(${Number(lon).toFixed(2)}, ${Number(lat).toFixed(2)})`,
      data: points.map(p => p.value)
    }]
    selectedPoint.value = { lon: Number(lon), lat: Number(lat) }
  } finally {
    trendLoading.value = false
  }
}

function onMapCellClick({ lat, lon }) {
  fetchTrendData(lon, lat)
}

function onBboxChange(bbox) {
  customBbox.value = bbox
}

async function loadSeaAreas() {
  try {
    const res = await getSeaAreas()
    seaAreas.value = res.data || []
  } catch (e) { /* empty */ }
}

async function handleSearch() {
  await fetchGridData()
}

function handleReset() {
  filterDate.value = ''
  seaArea.value = null
  customBbox.value = null
  fetchGridData()
}

function onSeaAreaChange() {
  customBbox.value = null
}

onMounted(async () => {
  filterDate.value = todayStr()
  await loadSeaAreas()
  await fetchGridData()
})
</script>

<style scoped>
.sst-page { padding: 0; }
.page-title { margin-bottom: 20px; color: #1a3a5c; font-size: 22px; }
.filter-card { margin-bottom: 16px; }
.filter-bar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.filter-label { font-weight: 600; color: #1a3a5c; }
.filter-hint { font-size: 12px; color: #999; margin-left: auto; }
.map-card { margin-bottom: 16px; }
.trend-card { margin-bottom: 16px; }
.trend-header { display: flex; align-items: center; justify-content: space-between; }
.trend-title { font-weight: 600; color: #1a3a5c; font-size: 16px; }
.trend-subtitle { font-size: 13px; color: #409EFF; }
</style>
```

- [ ] **Step 2: 提交**

```bash
git add ocean-web/src/views/forecast/SstMapView.vue
git commit -m "feat: add SstMapView page with Leaflet map, filters, and trend chart"
```

---

### Task 14: ChxMapView 页面

**Files:**
- Create: `ocean-web/src/views/forecast/ChxMapView.vue`

- [ ] **Step 1: 创建 ChxMapView.vue**

```vue
<template>
  <div class="chl-page">
    <h2 class="page-title">叶绿素预测</h2>

    <!-- Filter bar -->
    <el-card shadow="hover" class="filter-card">
      <div class="filter-bar">
        <span class="filter-label">数据筛选</span>

        <!-- Mode toggle -->
        <div class="mode-toggle">
          <el-radio-group v-model="chlMode" @change="onModeChange" size="small">
            <el-radio-button value="concentration">浓度值</el-radio-button>
            <el-radio-button value="probability">超阈值概率</el-radio-button>
          </el-radio-group>
        </div>

        <el-date-picker
          v-model="filterDate"
          type="date"
          placeholder="选择预报日期"
          value-format="YYYY-MM-DD"
          style="width: 180px"
          v-if="chlMode === 'concentration'"
        />

        <template v-if="chlMode === 'probability'">
          <el-input-number v-model="probDays" :min="1" :max="90" style="width: 140px" />
          <span style="color: #666; font-size: 13px;">天</span>
          <el-input-number v-model="threshold" :min="0.1" :step="0.5" :precision="1" style="width: 140px" />
          <span style="color: #666; font-size: 13px;">阈值 mg/m³</span>
        </template>

        <el-select v-model="seaArea" placeholder="海域筛选" style="width: 160px" @change="onSeaAreaChange">
          <el-option v-for="area in seaAreas" :key="area.name" :label="area.name" :value="area" />
        </el-select>

        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <span class="filter-hint">也可在地图上拖拽框选海域</span>
      </div>
    </el-card>

    <!-- Map -->
    <el-card shadow="hover" class="map-card">
      <OceanMap
        :grid-data="gridData"
        :color-ranges="currentColorRanges"
        :legend-labels="currentLegendLabels"
        :legend-title="chlMode === 'concentration' ? '浓度 (mg/m³)' : '概率 (%)'"
        :loading="mapLoading"
        @cell-click="onMapCellClick"
        @bbox-change="onBboxChange"
      />
    </el-card>

    <!-- Trend chart -->
    <el-card shadow="hover" class="trend-card">
      <template #header>
        <div class="trend-header">
          <span class="trend-title">
            {{ chlMode === 'concentration' ? '叶绿素浓度变化趋势' : '超阈值概率历史' }}
          </span>
          <span v-if="selectedPoint" class="trend-subtitle">
            选中: ({{ selectedPoint.lon.toFixed(2) }}, {{ selectedPoint.lat.toFixed(2) }})
          </span>
        </div>
      </template>
      <TrendChart
        :series-data="trendSeries"
        :x-axis-data="trendDates"
        :y-axis-name="chlMode === 'concentration' ? '浓度 (mg/m³)' : '值'"
        :y-axis-unit="chlMode === 'concentration' ? 'mg/m³' : ''"
        :loading="trendLoading"
        :colors="CHL_COLORS"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import OceanMap from '../../components/OceanMap.vue'
import TrendChart from '../../components/TrendChart.vue'
import { getMapGrid, getPointTrend, getSeaAreas } from '../../api/forecast'
import { CHL_CONC_COLORS, CHL_PROB_COLORS, CHL_COLORS } from '../../utils/chart-config'

const chlMode = ref('concentration')
const filterDate = ref('')
const probDays = ref(7)
const threshold = ref(3.0)
const seaArea = ref(null)
const seaAreas = ref([])
const gridData = ref([])
const mapLoading = ref(false)
const trendSeries = ref([])
const trendDates = ref([])
const trendLoading = ref(false)
const selectedPoint = ref(null)
const customBbox = ref(null)

const currentColorRanges = computed(() =>
  chlMode.value === 'concentration' ? CHL_CONC_COLORS : CHL_PROB_COLORS
)

const currentLegendLabels = computed(() =>
  chlMode.value === 'concentration'
    ? ['<0.5', '0.5-1.5', '1.5-3.0', '3.0-5.0', '>5.0 mg/m³']
    : ['<20%', '20-40%', '40-60%', '60-80%', '>80%']
)

function todayStr() {
  return new Date().toISOString().slice(0, 10)
}

function pastDate(days) {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return d.toISOString().slice(0, 10)
}

function buildBboxParams() {
  const params = {}
  if (customBbox.value) {
    params.minLon = customBbox.value.west
    params.maxLon = customBbox.value.east
    params.minLat = customBbox.value.south
    params.maxLat = customBbox.value.north
  } else if (seaArea.value) {
    params.minLon = seaArea.value.minLon
    params.maxLon = seaArea.value.maxLon
    params.minLat = seaArea.value.minLat
    params.maxLat = seaArea.value.maxLat
  }
  return params
}

async function fetchGridData() {
  mapLoading.value = true
  try {
    const params = {
      dataType: 'CHL',
      precision: 0.05,
      chlMode: chlMode.value,
      ...buildBboxParams()
    }
    if (chlMode.value === 'concentration') {
      params.forecastDate = filterDate.value || todayStr()
    } else {
      params.dateStart = pastDate(probDays.value)
      params.dateEnd = todayStr()
      params.threshold = threshold.value
    }
    const res = await getMapGrid(params)
    gridData.value = (res.data || []).map(r => ({
      lon: r.gridLon != null ? Number(r.gridLon) : Number(r.longitude),
      lat: r.gridLat != null ? Number(r.gridLat) : Number(r.latitude),
      value: r.value != null ? r.value : r.probability
    }))
  } finally {
    mapLoading.value = false
  }
}

async function fetchTrendData(lon, lat) {
  trendLoading.value = true
  try {
    const res = await getPointTrend({ dataType: 'CHL', lon, lat })
    const points = res.data || []
    trendDates.value = points.map(p => p.forecastDate)
    trendSeries.value = [{
      name: `(${Number(lon).toFixed(2)}, ${Number(lat).toFixed(2)})`,
      data: points.map(p => p.value)
    }]
    selectedPoint.value = { lon: Number(lon), lat: Number(lat) }
  } finally {
    trendLoading.value = false
  }
}

function onMapCellClick({ lat, lon }) { fetchTrendData(lon, lat) }
function onBboxChange(bbox) { customBbox.value = bbox }

async function loadSeaAreas() {
  try {
    const res = await getSeaAreas()
    seaAreas.value = res.data || []
  } catch (e) { /* empty */ }
}

function onModeChange() {
  customBbox.value = null
  if (chlMode.value === 'concentration') {
    filterDate.value = todayStr()
  }
  fetchGridData()
}

async function handleSearch() { await fetchGridData() }

function handleReset() {
  filterDate.value = todayStr()
  probDays.value = 7
  threshold.value = 3.0
  seaArea.value = null
  customBbox.value = null
  fetchGridData()
}

function onSeaAreaChange() { customBbox.value = null }

onMounted(async () => {
  filterDate.value = todayStr()
  await loadSeaAreas()
  await fetchGridData()
})
</script>

<style scoped>
.chl-page { padding: 0; }
.page-title { margin-bottom: 20px; color: #1a3a5c; font-size: 22px; }
.filter-card { margin-bottom: 16px; }
.filter-bar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.filter-label { font-weight: 600; color: #1a3a5c; }
.filter-hint { font-size: 12px; color: #999; margin-left: auto; }
.mode-toggle { margin-right: 4px; }
.map-card { margin-bottom: 16px; }
.trend-card { margin-bottom: 16px; }
.trend-header { display: flex; align-items: center; justify-content: space-between; }
.trend-title { font-weight: 600; color: #1a3a5c; font-size: 16px; }
.trend-subtitle { font-size: 13px; color: #409EFF; }
</style>
```

- [ ] **Step 2: 提交**

```bash
git add ocean-web/src/views/forecast/ChxMapView.vue
git commit -m "feat: add ChxMapView page with concentration/probability mode toggle"
```

---

### Task 15: HistoryView 页面（从 ForecastView 拆出）

**Files:**
- Create: `ocean-web/src/views/forecast/HistoryView.vue`

- [ ] **Step 1: 从 ForecastView.vue 提取表格部分创建 HistoryView.vue**

```vue
<template>
  <div class="history-page">
    <h2 class="page-title">历史预报记录</h2>

    <el-card shadow="hover">
      <el-form :inline="true" :model="tableQuery" size="default" style="margin-bottom: 16px;">
        <el-form-item label="数据类型">
          <el-select v-model="tableQuery.dataType" placeholder="全部" clearable style="width: 150px">
            <el-option label="海表温度" value="SST" />
            <el-option label="叶绿素浓度" value="CHL" />
          </el-select>
        </el-form-item>
        <el-form-item label="预报日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="tableLoading" stripe border size="small">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="modelName" label="模型名称" min-width="180" />
        <el-table-column prop="dataType" label="数据类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.dataType === 'SST' ? 'primary' : 'success'" size="small">
              {{ row.dataType === 'SST' ? '海表温度' : '叶绿素浓度' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="forecastDate" label="预报日期" width="120" align="center" />
        <el-table-column prop="value" label="数值" width="100" align="center" />
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column prop="longitude" label="经度" width="110" align="center" />
        <el-table-column prop="latitude" label="纬度" width="110" align="center" />
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
      </el-table>

      <div style="margin-top: 16px; text-align: right;">
        <el-pagination
          v-model:current-page="tableQuery.pageNum"
          v-model:page-size="tableQuery.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="tableTotal"
          layout="total, sizes, prev, pager, next"
          @size-change="loadTableData"
          @current-change="loadTableData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRecordPage } from '../../api/forecast'

const tableQuery = ref({ pageNum: 1, pageSize: 10, dataType: '', locationName: '' })
const dateRange = ref([])
const tableData = ref([])
const tableTotal = ref(0)
const tableLoading = ref(false)

async function loadTableData() {
  tableLoading.value = true
  try {
    const params = { ...tableQuery.value }
    if (dateRange.value && dateRange.value.length === 2) {
      params.forecastDateBegin = dateRange.value[0]
      params.forecastDateEnd = dateRange.value[1]
    }
    const res = await getRecordPage(params)
    tableData.value = res.data.records
    tableTotal.value = res.data.total
  } finally { tableLoading.value = false }
}

function handleSearch() {
  tableQuery.value.pageNum = 1
  loadTableData()
}

function handleReset() {
  tableQuery.value.dataType = ''
  tableQuery.value.pageNum = 1
  dateRange.value = []
  loadTableData()
}

onMounted(() => { loadTableData() })
</script>

<style scoped>
.page-title { margin-bottom: 20px; color: #1a3a5c; font-size: 22px; }
</style>
```

- [ ] **Step 2: 提交**

```bash
git add ocean-web/src/views/forecast/HistoryView.vue
git commit -m "feat: extract HistoryView from ForecastView as standalone page"
```

---

### Task 16: 删除旧 ForecastView.vue

**Files:**
- Delete: `ocean-web/src/views/forecast/ForecastView.vue`

- [ ] **Step 1: 删除旧文件**

```bash
rm ocean-web/src/views/forecast/ForecastView.vue
```

- [ ] **Step 2: 提交**

```bash
git add ocean-web/src/views/forecast/ForecastView.vue
git commit -m "refactor: remove deprecated ForecastView, replaced by SstMapView, ChxMapView, HistoryView"
```

---

### Task 17: 端到端验证

- [ ] **Step 1: 确认后端编译通过**

Run: `cd ocean-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 确认前端构建通过**

Run: `cd ocean-web && npx vite build`
Expected: Build successful, no errors

- [ ] **Step 3: 确认前端测试通过**

Run: `cd ocean-web && npx vitest run`
Expected: All tests pass

- [ ] **Step 4: 启动后端并测试新端点**

Run backend, then:
```bash
curl -s http://localhost:8080/api/forecast/sea-areas | head -c 200
```
Expected: JSON response with sea area array

- [ ] **Step 5: 启动前端并验证页面**

Run: `cd ocean-web && npx vite`
- Navigate to `http://localhost:5173`
- Login, click sidebar "预报数据可视化" → expand sub-menu
- Click "海表温度预测" → map loads with OSM tiles and grid points
- Select date, sea area → click query → map refreshes
- Click grid point → trend chart loads below
- Repeat for "叶绿素预测" page, test mode toggle

- [ ] **Step 6: 提交最终验证**

```bash
git add -A
git commit -m "chore: final verification — all builds and tests pass"
```
