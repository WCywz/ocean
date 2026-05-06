# Dashboard Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the dashboard from 3 stat cards + 2 tables into a rich card-grid layout with trend sparklines, compact map, threshold alerts, and polished visual design.

**Architecture:** Backend-first: add alert count to existing DashboardVO, then new endpoints for dashboard trends and alerts. Frontend: extract 5 new child components from DashboardView, each self-contained with props. Reuse `OceanMap.vue` (with height prop), `chart-config.js` color palettes, and existing map grid API.

**Tech Stack:** Vue 3 (Composition API), ECharts, Leaflet, Element Plus, Spring Boot, MyBatis Plus, MySQL

---

### Task 1: Add alert count to DashboardVO and dashboard query

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/vo/DashboardVO.java`
- Modify: `ocean-server/src/main/java/com/ocean/mapper/ForecastRecordMapper.java`
- Modify: `ocean-server/src/main/java/com/ocean/service/impl/ForecastRecordServiceImpl.java`

- [ ] **Step 1: Add `alertCount` field to DashboardVO**

In `DashboardVO.java`, add the new field after `todayRecordCount`:

```java
/** 今日超出阈值的告警记录数 */
private Long alertCount;
```

- [ ] **Step 2: Add `countTodayAlerts` query to ForecastRecordMapper**

In `ForecastRecordMapper.java`, add the new method after `countTodayRecords`:

```java
/**
 * 统计今日超出阈值的告警记录数 (SST>28°C 或 CHL>5 mg/m³)
 */
@Select("SELECT COUNT(*) FROM forecast_record " +
        "WHERE forecast_date = CURDATE() " +
        "AND ((data_type = 'SST' AND value > 28) OR (data_type = 'CHL' AND value > 5))")
Long countTodayAlerts();
```

- [ ] **Step 3: Set alertCount in getDashboard service method**

In `ForecastRecordServiceImpl.java`, inside `getDashboard()`, add after the `todayRecordCount` line:

```java
// 今日告警数
vo.setAlertCount(forecastRecordMapper.countTodayAlerts());
```

- [ ] **Step 4: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/vo/DashboardVO.java \
        ocean-server/src/main/java/com/ocean/mapper/ForecastRecordMapper.java \
        ocean-server/src/main/java/com/ocean/service/impl/ForecastRecordServiceImpl.java
git commit -m "feat: add alertCount to dashboard — count SST>28/CHL>5 records today"
```

---

### Task 2: Add mapper queries for dashboard trend and alerts

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/mapper/ForecastRecordMapper.java`

- [ ] **Step 1: Add `selectDashboardTrend` query**

Append to `ForecastRecordMapper.java`:

```java
/**
 * 仪表盘趋势 — 查 top 5 观测点在最近 N 天的日均值
 */
@Select("SELECT fr.location_name AS locationName, fr.forecast_date AS forecastDate, AVG(fr.value) AS value " +
        "FROM forecast_record fr " +
        "INNER JOIN ( " +
        "  SELECT location_name, COUNT(*) AS cnt " +
        "  FROM forecast_record " +
        "  WHERE data_type = #{dataType} AND forecast_date >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
        "  GROUP BY location_name " +
        "  ORDER BY cnt DESC " +
        "  LIMIT 5 " +
        ") top ON fr.location_name = top.location_name " +
        "WHERE fr.data_type = #{dataType} " +
        "  AND fr.forecast_date >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
        "GROUP BY fr.location_name, fr.forecast_date " +
        "ORDER BY fr.location_name, fr.forecast_date")
List<Map<String, Object>> selectDashboardTrend(@Param("dataType") String dataType,
                                               @Param("days") Integer days);
```

- [ ] **Step 2: Add `selectTodayAlerts` query**

Append to `ForecastRecordMapper.java`:

```java
/**
 * 今日阈值告警详情 (SST>28°C 或 CHL>5 mg/m³)，按值降序，最多 20 条
 */
@Select("SELECT location_name AS locationName, data_type AS dataType, " +
        "       value, forecast_date AS forecastDate, " +
        "       CASE WHEN data_type = 'SST' THEN 28 ELSE 5 END AS threshold " +
        "FROM forecast_record " +
        "WHERE forecast_date = CURDATE() " +
        "  AND ((data_type = 'SST' AND value > 28) OR (data_type = 'CHL' AND value > 5)) " +
        "ORDER BY value DESC " +
        "LIMIT 20")
List<Map<String, Object>> selectTodayAlerts();
```

- [ ] **Step 3: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/mapper/ForecastRecordMapper.java
git commit -m "feat: add mapper queries for dashboard trend and today alerts"
```

---

### Task 3: Add service methods for dashboard trend and alerts

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/service/ForecastRecordService.java`
- Modify: `ocean-server/src/main/java/com/ocean/service/impl/ForecastRecordServiceImpl.java`

- [ ] **Step 1: Add method signatures to service interface**

In `ForecastRecordService.java`, add after `getDashboard()`:

```java
/** 仪表盘趋势数据 — top N 观测点最近天数日均值 */
List<Map<String, Object>> getDashboardTrend(String dataType, Integer days);

/** 今日阈值告警详情 */
List<Map<String, Object>> getTodayAlerts();
```

Also add the missing import at the top if not present (check existing imports for `java.util.List` and `java.util.Map` — they should already be imported).

- [ ] **Step 2: Implement `getDashboardTrend` in service impl**

In `ForecastRecordServiceImpl.java`, add the required import at the top:

```java
import java.util.HashMap;
import java.util.stream.Collectors;
```

Then add the method implementations before the `private ForecastVO toVO` method:

```java
@Override
public List<Map<String, Object>> getDashboardTrend(String dataType, Integer days) {
    if (days == null) days = 7;
    List<Map<String, Object>> rows = forecastRecordMapper.selectDashboardTrend(dataType, days);
    // Group by locationName, build {locationName, dataPoints: [{date, value}]}
    Map<String, List<Map<String, Object>>> grouped = rows.stream()
            .collect(Collectors.groupingBy(
                    row -> (String) row.get("locationName"),
                    Collectors.toList()
            ));
    return grouped.entrySet().stream().map(entry -> {
        Map<String, Object> m = new HashMap<>();
        m.put("locationName", entry.getKey());
        m.put("dataPoints", entry.getValue().stream().map(r -> {
            Map<String, Object> dp = new HashMap<>();
            dp.put("date", r.get("forecastDate").toString());
            dp.put("value", r.get("value"));
            return dp;
        }).collect(Collectors.toList()));
        return m;
    }).collect(Collectors.toList());
}

@Override
public List<Map<String, Object>> getTodayAlerts() {
    return forecastRecordMapper.selectTodayAlerts();
}
```

- [ ] **Step 3: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/service/ForecastRecordService.java \
        ocean-server/src/main/java/com/ocean/service/impl/ForecastRecordServiceImpl.java
git commit -m "feat: add getDashboardTrend and getTodayAlerts service methods"
```

---

### Task 4: Add controller endpoints

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/controller/ForecastRecordController.java`

- [ ] **Step 1: Add `getDashboardTrend` endpoint**

Add before the `getSeaAreas` method:

```java
/**
 * 仪表盘趋势数据
 */
@GetMapping("/trend/dashboard")
public Result<List<Map<String, Object>>> getDashboardTrend(
        @RequestParam(defaultValue = "SST") String dataType,
        @RequestParam(defaultValue = "7") Integer days) {
    List<Map<String, Object>> data = forecastRecordService.getDashboardTrend(dataType, days);
    return Result.success(data);
}
```

- [ ] **Step 2: Add `getTodayAlerts` endpoint**

Add after the `getDashboardTrend` method:

```java
/**
 * 今日阈值告警
 */
@GetMapping("/alerts")
public Result<List<Map<String, Object>>> getTodayAlerts() {
    List<Map<String, Object>> data = forecastRecordService.getTodayAlerts();
    return Result.success(data);
}
```

- [ ] **Step 3: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/controller/ForecastRecordController.java
git commit -m "feat: add /trend/dashboard and /alerts endpoints to forecast controller"
```

---

### Task 5: Add frontend API functions

**Files:**
- Modify: `ocean-web/src/api/forecast.js`

- [ ] **Step 1: Add new API functions**

In `forecast.js`, append after the existing `getSeaAreas` function:

```javascript
/** 获取仪表盘趋势数据 (top 5 观测点 sparkline) */
export function getDashboardTrend(dataType = 'SST', days = 7) {
  return request({ url: '/forecast/trend/dashboard', method: 'get', params: { dataType, days } })
}

/** 获取今日阈值告警 */
export function getTodayAlerts() {
  return request({ url: '/forecast/alerts', method: 'get' })
}
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/api/forecast.js
git commit -m "feat: add getDashboardTrend and getTodayAlerts API functions"
```

---

### Task 6: Create StatCards.vue component

**Files:**
- Create: `ocean-web/src/views/dashboard/StatCards.vue`

- [ ] **Step 1: Write the component**

Create `ocean-web/src/views/dashboard/StatCards.vue`:

```vue
<template>
  <el-row :gutter="20" class="stat-row">
    <el-col :span="6" v-for="card in cards" :key="card.label">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" :style="{ background: card.bg }">
            <el-icon :size="28" :color="card.color"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value" :style="{ color: card.valueColor || '#1a3a5c' }">
              {{ card.value }}
            </div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { computed } from 'vue'
import { Setting, VideoPlay, TrendCharts, Warning } from '@element-plus/icons-vue'

const props = defineProps({
  modelCount: { type: Number, default: 0 },
  runningModelCount: { type: Number, default: 0 },
  todayRecordCount: { type: Number, default: 0 },
  alertCount: { type: Number, default: 0 }
})

const cards = computed(() => [
  {
    label: '模型总数',
    value: props.modelCount,
    icon: Setting,
    bg: '#e6f7ff',
    color: '#1890ff',
    valueColor: '#1a3a5c'
  },
  {
    label: '运行中模型',
    value: props.runningModelCount,
    icon: VideoPlay,
    bg: '#f6ffed',
    color: '#52c41a',
    valueColor: '#1a3a5c'
  },
  {
    label: '今日预报记录',
    value: props.todayRecordCount,
    icon: TrendCharts,
    bg: '#fff7e6',
    color: '#fa8c16',
    valueColor: '#1a3a5c'
  },
  {
    label: '阈值告警',
    value: props.alertCount,
    icon: Warning,
    bg: props.alertCount > 0 ? '#fff2f0' : '#f6ffed',
    color: props.alertCount > 0 ? '#e74c3c' : '#52c41a',
    valueColor: props.alertCount > 0 ? '#e74c3c' : '#52c41a'
  }
])
</script>

<style scoped>
.stat-row { margin-bottom: 20px; }
.stat-card { cursor: pointer; transition: transform 0.2s; }
.stat-card:hover { transform: translateY(-2px); }
.stat-content { display: flex; align-items: center; gap: 14px; }
.stat-icon {
  width: 52px; height: 52px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.stat-value { font-size: 26px; font-weight: 700; }
.stat-label { color: #8899aa; font-size: 13px; margin-top: 2px; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/dashboard/StatCards.vue
git commit -m "feat: add StatCards dashboard component with dynamic alert styling"
```

---

### Task 7: Create TrendCard.vue component

**Files:**
- Create: `ocean-web/src/views/dashboard/TrendCard.vue`

- [ ] **Step 1: Write the component**

Create `ocean-web/src/views/dashboard/TrendCard.vue`:

```vue
<template>
  <el-card shadow="hover" class="trend-card">
    <template #header>
      <div class="card-header">
        <span class="card-title">{{ title }}</span>
        <el-tag size="small" type="info">最近 7 天</el-tag>
      </div>
    </template>
    <div v-if="!series.length && !loading" class="empty-state">暂无趋势数据</div>
    <div v-loading="loading" class="chart-wrapper" ref="chartRef"></div>
  </el-card>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { SST_COLORS, CHL_COLORS, buildBaseOption, buildSeriesData } from '../../utils/chart-config'

const props = defineProps({
  title: { type: String, default: '' },
  dataType: { type: String, default: 'SST' },
  series: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const chartRef = ref(null)
let chart = null

function renderChart() {
  if (!chart || !props.series.length) return
  const colors = props.dataType === 'SST' ? SST_COLORS : CHL_COLORS
  const unit = props.dataType === 'SST' ? '°C' : 'mg/m³'
  const xAxisData = props.series[0]?.dataPoints?.map(d => d.date) || []
  const legendData = props.series.map(s => s.locationName)
  const seriesMap = {}
  props.series.forEach(s => {
    seriesMap[s.locationName] = s.dataPoints.map(d => d.value)
  })

  const base = buildBaseOption({
    legendData,
    xAxisData,
    yAxisName: props.dataType === 'SST' ? '温度 (°C)' : '浓度 (mg/m³)',
    yAxisUnit: unit
  })

  const chartSeries = buildSeriesData(seriesMap, colors, { area: true, markLine: false })

  chart.setOption({ ...base, series: chartSeries }, true)
}

function handleResize() {
  chart?.resize()
}

watch(() => props.series, () => nextTick(() => renderChart()), { deep: true })

onMounted(() => {
  nextTick(() => {
    chart = echarts.init(chartRef.value)
    window.addEventListener('resize', handleResize)
    renderChart()
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.trend-card { height: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: 600; color: #1a3a5c; }
.chart-wrapper { width: 100%; height: 280px; }
.empty-state {
  height: 280px; display: flex; align-items: center; justify-content: center;
  color: #bbb; font-size: 14px;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/dashboard/TrendCard.vue
git commit -m "feat: add TrendCard dashboard component with ECharts sparklines"
```

---

### Task 8: Create AlertPanel.vue component

**Files:**
- Create: `ocean-web/src/views/dashboard/AlertPanel.vue`

- [ ] **Step 1: Write the component**

Create `ocean-web/src/views/dashboard/AlertPanel.vue`:

```vue
<template>
  <el-card shadow="hover" class="alert-panel">
    <template #header>
      <div class="card-header">
        <span class="card-title">阈值告警</span>
        <el-tag v-if="alerts.length" type="danger" size="small">{{ alerts.length }} 条</el-tag>
        <el-tag v-else type="success" size="small">正常</el-tag>
      </div>
    </template>
    <div v-if="!alerts.length && !loading" class="empty-state">
      <el-icon :size="36" color="#52c41a"><CircleCheck /></el-icon>
      <span style="margin-top: 8px; color: #999;">今日无阈值告警</span>
    </div>
    <div v-loading="loading" class="alert-list">
      <div
        v-for="(item, idx) in alerts.slice(0, 10)"
        :key="idx"
        class="alert-item"
        :class="item.value > (item.dataType === 'SST' ? 30 : 10) ? 'critical' : 'warning'"
      >
        <div class="alert-location">{{ item.locationName }}</div>
        <div class="alert-meta">
          <el-tag :type="item.dataType === 'SST' ? 'danger' : ''" size="small" effect="plain">
            {{ item.dataType }}
          </el-tag>
          <span class="alert-value">{{ item.value }}{{ item.dataType === 'SST' ? '°C' : ' mg/m³' }}</span>
          <span class="alert-threshold">阈值 {{ item.threshold }}</span>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { CircleCheck } from '@element-plus/icons-vue'

defineProps({
  alerts: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})
</script>

<style scoped>
.alert-panel { height: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: 600; color: #1a3a5c; }
.alert-list { min-height: 200px; }
.alert-item {
  padding: 10px 12px; border-radius: 6px; margin-bottom: 8px;
  border-left: 3px solid; display: flex; justify-content: space-between; align-items: center;
}
.alert-item.warning { background: #fffbe6; border-color: #fa8c16; }
.alert-item.critical { background: #fff2f0; border-color: #e74c3c; }
.alert-location { font-size: 13px; font-weight: 600; color: #333; }
.alert-meta { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #666; }
.alert-value { font-weight: 600; color: #e74c3c; }
.alert-threshold { color: #999; }
.empty-state {
  min-height: 200px; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/dashboard/AlertPanel.vue
git commit -m "feat: add AlertPanel dashboard component for threshold violations"
```

---

### Task 9: Create DashboardMap.vue + add height prop to OceanMap.vue

**Files:**
- Modify: `ocean-web/src/components/OceanMap.vue`
- Create: `ocean-web/src/views/dashboard/DashboardMap.vue`

- [ ] **Step 1: Add `height` prop to OceanMap.vue**

In `OceanMap.vue`, add the `height` prop to the existing `defineProps` block (add inside the props object):

```javascript
height: { type: String, default: '450px' },
```

Then change the template line for the container to bind the height:

```html
<div v-loading="loading" class="ocean-map-container" ref="mapContainer" :style="{ height: height }"></div>
```

Remove `height: 450px;` from `.ocean-map-container` in the scoped styles.

- [ ] **Step 2: Write DashboardMap.vue**

Create `ocean-web/src/views/dashboard/DashboardMap.vue`:

```vue
<template>
  <el-card shadow="hover" class="dashboard-map-card">
    <template #header>
      <div class="card-header">
        <span class="card-title">数据覆盖分布</span>
        <el-radio-group v-model="activeType" size="small" @change="$emit('typeChange', activeType)">
          <el-radio-button value="SST">SST</el-radio-button>
          <el-radio-button value="CHL">CHL</el-radio-button>
        </el-radio-group>
      </div>
    </template>
    <OceanMap
      :gridData="gridData"
      :colorRanges="colorRanges"
      :legendLabels="legendLabels"
      :legendTitle="legendTitle"
      :loading="loading"
      :height="height"
      :center="center"
      :zoom="zoom"
      @cellClick="(pos) => $emit('cellClick', pos)"
    />
  </el-card>
</template>

<script setup>
import OceanMap from '../../components/OceanMap.vue'

defineProps({
  gridData: { type: Array, default: () => [] },
  colorRanges: { type: Array, default: () => [] },
  legendLabels: { type: Array, default: () => [] },
  legendTitle: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  activeType: { type: String, default: 'SST' }
})

defineEmits(['typeChange', 'cellClick'])

const height = '300px'
const center = [29.8, 123.5]
const zoom = 7
</script>

<style scoped>
.dashboard-map-card { height: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: 600; color: #1a3a5c; }
</style>
```

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/components/OceanMap.vue \
        ocean-web/src/views/dashboard/DashboardMap.vue
git commit -m "feat: add height prop to OceanMap, create DashboardMap wrapper component"
```

---

### Task 10: Create LatestDataTable.vue component

**Files:**
- Create: `ocean-web/src/views/dashboard/LatestDataTable.vue`

- [ ] **Step 1: Write the component**

Create `ocean-web/src/views/dashboard/LatestDataTable.vue`:

```vue
<template>
  <el-card shadow="hover" class="data-table-card">
    <template #header>
      <span class="card-title">{{ title }}</span>
    </template>
    <el-table :data="data" size="small" stripe v-loading="loading" empty-text="暂无数据" max-height="280">
      <el-table-column prop="locationName" label="观测点" />
      <el-table-column prop="value" :label="valueLabel">
        <template #default="{ row }">{{ row.value }} {{ unit }}</template>
      </el-table-column>
      <el-table-column prop="forecastDate" label="预报日期" />
    </el-table>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  data: { type: Array, default: () => [] },
  dataType: { type: String, default: 'SST' },
  loading: { type: Boolean, default: false }
})

const unit = computed(() => props.dataType === 'SST' ? '°C' : 'mg/m³')
const valueLabel = computed(() => props.dataType === 'SST' ? '温度值' : '浓度值')
</script>

<style scoped>
.data-table-card { height: 100%; }
.card-title { font-weight: 600; color: #1a3a5c; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/dashboard/LatestDataTable.vue
git commit -m "feat: add LatestDataTable dashboard component"
```

---

### Task 11: Rewrite DashboardView.vue as orchestrator

**Files:**
- Modify: `ocean-web/src/views/dashboard/DashboardView.vue`

- [ ] **Step 1: Rewrite DashboardView.vue**

Replace the entire content of `ocean-web/src/views/dashboard/DashboardView.vue`:

```vue
<template>
  <div class="dashboard">
    <h2 class="page-title">系统仪表盘</h2>

    <StatCards
      :modelCount="data.modelCount"
      :runningModelCount="data.runningModelCount"
      :todayRecordCount="data.todayRecordCount"
      :alertCount="data.alertCount"
    />

    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="12">
        <TrendCard
          title="海表温度趋势 (SST)"
          dataType="SST"
          :series="sstTrend"
          :loading="loading.trendSst"
        />
      </el-col>
      <el-col :span="12">
        <TrendCard
          title="叶绿素浓度趋势 (CHL)"
          dataType="CHL"
          :series="chlTrend"
          :loading="loading.trendChl"
        />
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="16">
        <DashboardMap
          :gridData="mapGridData"
          :colorRanges="mapColorRanges"
          :legendLabels="mapLegendLabels"
          :legendTitle="mapLegendTitle"
          :loading="loading.map"
          :activeType="mapType"
          @typeChange="onMapTypeChange"
          @cellClick="onMapCellClick"
        />
      </el-col>
      <el-col :span="8">
        <AlertPanel :alerts="alerts" :loading="loading.alerts" />
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <LatestDataTable
          title="最新海表温度 (SST)"
          dataType="SST"
          :data="data.latestSstData"
          :loading="loading.dashboard"
        />
      </el-col>
      <el-col :span="12">
        <LatestDataTable
          title="最新叶绿素浓度 (CHL)"
          dataType="CHL"
          :data="data.latestChlData"
          :loading="loading.dashboard"
        />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getDashboard, getDashboardTrend, getTodayAlerts, getMapGrid } from '../../api/forecast'
import { SST_MAP_COLORS, CHL_CONC_COLORS } from '../../utils/chart-config'
import StatCards from './StatCards.vue'
import TrendCard from './TrendCard.vue'
import DashboardMap from './DashboardMap.vue'
import AlertPanel from './AlertPanel.vue'
import LatestDataTable from './LatestDataTable.vue'

const data = ref({
  modelCount: 0,
  runningModelCount: 0,
  todayRecordCount: 0,
  alertCount: 0,
  latestSstData: [],
  latestChlData: []
})

const sstTrend = ref([])
const chlTrend = ref([])
const alerts = ref([])
const mapGridData = ref([])
const mapType = ref('SST')

const loading = reactive({
  dashboard: false,
  trendSst: false,
  trendChl: false,
  alerts: false,
  map: false
})

const mapColorRanges = ref(SST_MAP_COLORS)
const mapLegendLabels = ref(SST_MAP_COLORS.map(r => r.label))
const mapLegendTitle = ref('SST 温度 (°C)')

async function fetchDashboard() {
  loading.dashboard = true
  try {
    const res = await getDashboard()
    data.value = res.data
  } finally {
    loading.dashboard = false
  }
}

async function fetchTrend(dataType) {
  const key = dataType === 'SST' ? 'trendSst' : 'trendChl'
  loading[key] = true
  try {
    const res = await getDashboardTrend(dataType, 7)
    if (dataType === 'SST') sstTrend.value = res.data
    else chlTrend.value = res.data
  } finally {
    loading[key] = false
  }
}

async function fetchAlerts() {
  loading.alerts = true
  try {
    const res = await getTodayAlerts()
    alerts.value = res.data
  } finally {
    loading.alerts = false
  }
}

async function fetchMapData() {
  loading.map = true
  try {
    const colorRanges = mapType.value === 'SST' ? SST_MAP_COLORS : CHL_CONC_COLORS
    const res = await getMapGrid({
      dataType: mapType.value,
      forecastDate: new Date().toISOString().slice(0, 10),
      precision: 0.05,
      minLon: 121.33, maxLon: 125.58,
      minLat: 26.92, maxLat: 32.67
    })
    mapGridData.value = (res.data || []).map(r => ({ lat: r.gridLat, lon: r.gridLon, value: r.value }))
    mapColorRanges.value = colorRanges
    mapLegendLabels.value = colorRanges.map(r => r.label)
    mapLegendTitle.value = mapType.value === 'SST' ? 'SST 温度 (°C)' : 'CHL 浓度 (mg/m³)'
  } finally {
    loading.map = false
  }
}

function onMapTypeChange(type) {
  mapType.value = type
  fetchMapData()
}

function onMapCellClick(pos) {
  // Navigate to full forecast map page
  const route = mapType.value === 'SST' ? '/app/forecast/sst' : '/app/forecast/chl'
  window.location.hash = '#' + route
}

onMounted(() => {
  fetchDashboard()
  fetchTrend('SST')
  fetchTrend('CHL')
  fetchAlerts()
  fetchMapData()
})
</script>

<style scoped>
.page-title {
  margin-bottom: 20px;
  color: #1a3a5c;
  font-size: 20px;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/dashboard/DashboardView.vue
git commit -m "feat: rewrite DashboardView as card-grid orchestrator with trend, map, and alert sections"
```

---

### Task 12: Verify integration

- [ ] **Step 1: Start backend and verify new endpoints work**

```bash
# Start the backend server, then test endpoints:
curl http://localhost:8080/api/forecast/dashboard | head
curl "http://localhost:8080/api/forecast/trend/dashboard?dataType=SST&days=7" | head
curl http://localhost:8080/api/forecast/alerts | head
```

Expected: All three return JSON with `code: 200` and data payloads.

- [ ] **Step 2: Start frontend and verify dashboard renders**

```bash
cd ocean-web && npm run dev
```

Navigate to `http://localhost:5173/app/dashboard`.

Expected: 4 stat cards across the top, 2 trend charts below, map + alerts side by side, and 2 data tables at the bottom. All sections load without errors.

- [ ] **Step 3: Test error states**

Stop the backend temporarily, refresh the dashboard page.
Expected: Each card shows its loading skeleton briefly, then handles failure gracefully (no page crash).

- [ ] **Step 4: Test edge cases**

| Test | Expected |
|------|----------|
| Zero alerts | AlertPanel shows green "今日无阈值告警" with success tag |
| Zero trend data | TrendCard shows "暂无趋势数据" placeholder |
| Map type toggle (SST → CHL) | Map reloads with green color scheme |
| Click map grid cell | Navigates to /app/forecast/sst or /chl |
| Resize browser window | Charts resize, map reflows |
