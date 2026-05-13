# Health Alert Threshold Section Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a date-filtered alert threshold section to the Ocean Health page by reusing the existing `AlertPanel` component wrapped in a new `HealthAlertSection` component.

**Architecture:** New `HealthAlertSection.vue` wrapper component combines a summary bar, the reused `AlertPanel`, and drill-down links. Backend adds a `forecastDate` parameter to the existing `/forecast/alerts` endpoint chain (controller → service → mapper).

**Tech Stack:** Vue 3 (Composition API), Spring Boot, MyBatis-Plus, Element Plus

---

### Task 1: Backend Mapper — Rename and add date parameter

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/mapper/ForecastRecordMapper.java:142-153`

- [ ] **Step 1: Replace `selectTodayAlerts` with date-parameterized version**

Replace lines 142-153 in `ForecastRecordMapper.java`:

```java
    /**
     * 阈值告警详情 (SST>28°C 或 CHL>5 mg/m³)，按值降序，最多 20 条
     */
    @Select("SELECT location_name AS locationName, data_type AS dataType, " +
            "       value, forecast_date AS forecastDate, " +
            "       CASE WHEN data_type = 'SST' THEN 28 ELSE 5 END AS threshold " +
            "FROM forecast_record " +
            "WHERE forecast_date = #{forecastDate} " +
            "  AND ((data_type = 'SST' AND value > 28) OR (data_type = 'CHL' AND value > 5)) " +
            "ORDER BY value DESC " +
            "LIMIT 20")
    List<Map<String, Object>> selectAlertsByDate(@Param("forecastDate") String forecastDate);
```

- [ ] **Step 2: Verify the project compiles**

Run: `cd ocean-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/mapper/ForecastRecordMapper.java
git commit -m "refactor: rename selectTodayAlerts to selectAlertsByDate with date parameter"
```

---

### Task 2: Backend Service Interface — Add date parameter

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/service/ForecastRecordService.java:29`

- [ ] **Step 1: Update service interface method signature**

Replace line 29 in `ForecastRecordService.java`:

```java
    /** 阈值告警详情 */
    List<Map<String, Object>> getAlerts(String forecastDate);
```

- [ ] **Step 2: Verify project compiles**

Run: `cd ocean-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/service/ForecastRecordService.java
git commit -m "refactor: rename getTodayAlerts to getAlerts with date parameter in service interface"
```

---

### Task 3: Backend Service Implementation — Update method

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/service/impl/ForecastRecordServiceImpl.java:176-179`

- [ ] **Step 1: Replace service implementation method**

Replace lines 176-179 in `ForecastRecordServiceImpl.java`:

```java
    @Override
    public List<Map<String, Object>> getAlerts(String forecastDate) {
        return forecastRecordMapper.selectAlertsByDate(forecastDate);
    }
```

- [ ] **Step 2: Verify project compiles**

Run: `cd ocean-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/service/impl/ForecastRecordServiceImpl.java
git commit -m "refactor: update getAlerts implementation to use selectAlertsByDate"
```

---

### Task 4: Backend Controller — Add date request parameter

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/controller/ForecastRecordController.java:115-119`

- [ ] **Step 1: Update controller endpoint**

Replace lines 112-119 in `ForecastRecordController.java`:

```java
    /**
     * 阈值告警
     */
    @GetMapping("/alerts")
    public Result<List<Map<String, Object>>> getAlerts(
            @RequestParam String forecastDate) {
        List<Map<String, Object>> data = forecastRecordService.getAlerts(forecastDate);
        return Result.success(data);
    }
```

- [ ] **Step 2: Verify project compiles**

Run: `cd ocean-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Verify the API works**

Start the server and test:
Run: `curl "http://localhost:8080/api/forecast/alerts?forecastDate=2026-01-01"`
Expected: JSON array of alert objects with `locationName`, `dataType`, `value`, `threshold`

- [ ] **Step 4: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/controller/ForecastRecordController.java
git commit -m "feat: add forecastDate parameter to /forecast/alerts endpoint"
```

---

### Task 5: Frontend API — Update getTodayAlerts

**Files:**
- Modify: `ocean-web/src/api/forecast.js:48-51`

- [ ] **Step 1: Add date parameter to API function**

Replace lines 48-51 in `forecast.js`:

```javascript
/** 获取阈值告警 */
export function getAlerts(forecastDate) {
  return request({ url: '/forecast/alerts', method: 'get', params: { forecastDate } })
}
```

- [ ] **Step 2: Update DashboardView.vue import (breakage fix)**

The DashboardView.vue currently imports `getTodayAlerts`. Update the import:

In `ocean-web/src/views/dashboard/DashboardView.vue`, change the import on line 97:
```javascript
import { getDashboard, getDashboardTrend, getAlerts } from '../../api/forecast'
```

Update the `fetchAlerts` function (line 211-218):
```javascript
async function fetchAlerts() {
  loading.alerts = true
  try {
    const res = await getAlerts('2026-01-01')
    alerts.value = res.data
  } finally {
    loading.alerts = false
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/api/forecast.js ocean-web/src/views/dashboard/DashboardView.vue
git commit -m "feat: add date parameter to getAlerts API, update DashboardView"
```

---

### Task 6: Create HealthAlertSection.vue component

**Files:**
- Create: `ocean-web/src/views/health/HealthAlertSection.vue`

- [ ] **Step 1: Write the component**

```vue
<template>
  <div class="editorial-section">
    <p class="editorial-section-label">Alerts</p>
    <h3 class="editorial-section-heading">阈值告警</h3>

    <div class="health-status-bar" :style="{ borderLeftColor: accentColor }">
      <span class="health-status-bar__level">{{ summaryText }}</span>
    </div>

    <div v-if="!alerts.length && !loading" style="min-height: 120px; display: flex; align-items: center; justify-content: center; color: var(--color-text-muted); font-size: 13px;">
      所选日期无阈值告警
    </div>

    <div v-loading="loading">
      <div
        v-for="(item, idx) in alerts.slice(0, 10)"
        :key="idx"
        class="alert-item"
        :style="{ borderLeftColor: item.dataType === 'SST' ? '#c0392b' : '#e67e22' }"
      >
        <div style="font-size: 13px; font-weight: 600; color: var(--color-text);">{{ item.locationName }}</div>
        <div style="display: flex; align-items: center; gap: 8px; font-size: 12px; color: #666; margin-top: 4px;">
          <span class="editorial-tag" style="font-size: 10px;">{{ item.dataType }}</span>
          <span style="font-weight: 600; color: var(--color-alert);">{{ item.value }}{{ item.dataType === 'SST' ? '°C' : ' mg/m³' }}</span>
          <span style="color: var(--color-text-muted);">阈值 {{ item.threshold }}</span>
        </div>
      </div>
    </div>

    <div class="drilldown-links" v-if="alerts.length">
      <span class="drilldown-label">查看详情：</span>
      <a class="drilldown-link" @click="goSst">海表温度预测地图 →</a>
      <a class="drilldown-link" @click="goChl">叶绿素浓度预测地图 →</a>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const props = defineProps({
  alerts: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const sstCount = computed(() =>
  props.alerts.filter(a => a.dataType === 'SST').length
)

const chlCount = computed(() =>
  props.alerts.filter(a => a.dataType === 'CHL').length
)

const summaryText = computed(() => {
  if (!props.alerts.length) return '暂无告警'
  const parts = []
  if (sstCount.value) parts.push(`${sstCount.value} 个区域超过 SST 阈值`)
  if (chlCount.value) parts.push(`${chlCount.value} 个区域超过 Chl 阈值`)
  return parts.join('，')
})

const accentColor = computed(() => {
  if (!props.alerts.length) return '#22c55e'
  const hasSst = sstCount.value > 0
  const hasChl = chlCount.value > 0
  if (hasSst && hasChl) return '#ef4444'
  if (hasSst) return '#c0392b'
  return '#e67e22'
})

function goSst() { router.push('/app/forecast/sst') }
function goChl() { router.push('/app/forecast/chl') }
</script>

<style scoped>
.health-status-bar {
  display: flex;
  align-items: center;
  border-left: 3px solid;
  padding: 10px 14px;
  background: #fafafa;
  font-size: 13px;
  margin-bottom: 16px;
}

.health-status-bar__level {
  font-family: var(--font-serif);
  font-size: 15px;
  color: var(--color-text);
}

.alert-item {
  padding: 10px 12px;
  margin-bottom: 8px;
  border-left: 3px solid;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  border-right: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.drilldown-links {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
}

.drilldown-label {
  color: var(--color-text-muted);
  margin-right: 16px;
}

.drilldown-link {
  color: var(--color-text);
  cursor: pointer;
  margin-right: 20px;
  text-decoration: none;
  border-bottom: 1px dashed #ccc;
}

.drilldown-link:hover {
  color: var(--color-alert);
  border-bottom-color: var(--color-alert);
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/health/HealthAlertSection.vue
git commit -m "feat: add HealthAlertSection component with summary and drill-down links"
```

---

### Task 7: Integrate HealthAlertSection into OceanHealthView

**Files:**
- Modify: `ocean-web/src/views/health/OceanHealthView.vue`

- [ ] **Step 1: Add import for HealthAlertSection and API**

In the `<script setup>` section, add the import after line 96:
```javascript
import HealthAlertSection from './HealthAlertSection.vue'
import { getAlerts } from '../../api/forecast'
```

- [ ] **Step 2: Add alerts ref and fetchAlerts function**

Add after line 103:
```javascript
const alerts = ref([])
const alertsLoading = ref(false)
```

Add after the `fetchData` function definition (before `onMounted`):
```javascript
async function fetchAlerts() {
  alertsLoading.value = true
  try {
    const res = await getAlerts(forecastDate.value)
    alerts.value = res.data || []
  } catch (e) {
    console.error('Failed to fetch alerts', e)
  } finally {
    alertsLoading.value = false
  }
}
```

- [ ] **Step 3: Add HealthAlertSection to template**

Insert between the status bar (`</div>` closing the `.health-status-bar`) and the `区域健康评估` section label (line 24):

```html
    <HealthAlertSection :alerts="alerts" :loading="alertsLoading" />
```

The result should look like:

```html
    </div>  <!-- closing health-status-bar -->

    <HealthAlertSection :alerts="alerts" :loading="alertsLoading" />

    <p class="editorial-section-label">区域健康评估 &middot; 东海</p>
```

- [ ] **Step 4: Update fetchData to also fetch alerts**

In the existing `fetchData` function, add the call:
```javascript
async function fetchData() {
  loading.value = true
  try {
    const res = await getZoneHealth({
      centerLon: 122.5,
      centerLat: 29.5,
      coastLon: 121.5,
      forecastDate: forecastDate.value
    })
    rawData.value = res.data
    assessments.value = (res.data && res.data.zones || []).map(buildZoneAssessment)
    selectedIds.value = new Set(assessments.value.map(z => z.id))
    fetchAlerts()  // fire-and-forget in try block so errors don't block zone health
  } catch (e) {
    console.error('Failed to fetch zone health data', e)
  } finally {
    loading.value = false
  }
}
```

- [ ] **Step 5: Review full file to verify integration is correct**

Read the modified file and confirm:
- `<HealthAlertSection>` is between the status bar div close and the health section label
- Imports are present
- `fetchAlerts` is called from `fetchData`
- The component receives `:alerts` and `:loading` props

- [ ] **Step 6: Commit**

```bash
git add ocean-web/src/views/health/OceanHealthView.vue
git commit -m "feat: integrate HealthAlertSection into OceanHealthView"
```

---

### Task 8: Final Verification

- [ ] **Step 1: Verify backend compiles**

Run: `cd ocean-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Verify frontend type-checks (if available)**

Run: `cd ocean-web && npx vue-tsc --noEmit 2>&1 || true`
Note: May show pre-existing issues — only flag new errors matching the changed files.

- [ ] **Step 3: Manual test checklist**

1. Navigate to `/app/ocean-health`
2. Confirm the alert threshold section appears between the status bar and the zone health cards
3. Verify the summary line shows correct count ("X 个区域超过 SST 阈值，Y 个区域超过 Chl 阈值")
4. Verify the alert list shows location name, data type, value, and threshold
5. Change the date — verify alerts update
6. Select a date with no alerts — verify "所选日期无阈值告警" empty state
7. Click drill-down links — verify navigation to SST/Chl forecast maps
