# Dashboard Layout Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the dashboard map, restructure into 3-row layout with 2px dividers, and make modules clickable to navigate to corresponding full pages.

**Architecture:** Child components (StatCards, TrendCard, LatestDataTable) emit `navigate` events. DashboardView handles routing via `useRouter`. Each of the three rows has a `border-bottom: 2px solid #e0e0e0` divider. Row 3 uses a 1:2 flex split (alerts left, data tables right).

**Tech Stack:** Vue 3 (Composition API), Vue Router, ECharts, Element Plus

---

### Task 1: Delete DashboardMap.vue

**Files:**
- Delete: `ocean-web/src/views/dashboard/DashboardMap.vue`

- [ ] **Step 1: Delete the file**

```bash
rm ocean-web/src/views/dashboard/DashboardMap.vue
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/dashboard/DashboardMap.vue
git commit -m "feat: remove DashboardMap from dashboard"
```

---

### Task 2: Add navigation to StatCards.vue

**Files:**
- Modify: `ocean-web/src/views/dashboard/StatCards.vue`

- [ ] **Step 1: Update template — wrap in clickable container with nav hint**

Replace the existing template with:

```html
<template>
  <div class="editorial-stats" style="cursor: pointer;" @click="$emit('navigate')">
    <div v-for="card in cards" :key="card.label" class="editorial-stat" :class="{ 'editorial-stat--alert': card.isAlert }">
      <span class="editorial-stat__value">{{ card.value }}</span>
      <span class="editorial-stat__label">{{ card.label }}</span>
    </div>
    <span class="stats-nav-hint">模型管理 →</span>
  </div>
</template>
```

- [ ] **Step 2: Add emit declaration**

Replace the `<script setup>` block with:

```js
<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelCount: { type: Number, default: 0 },
  runningModelCount: { type: Number, default: 0 },
  todayRecordCount: { type: Number, default: 0 },
  alertCount: { type: Number, default: 0 }
})

defineEmits(['navigate'])

const cards = computed(() => [
  { label: '模型总数', value: props.modelCount, isAlert: false },
  { label: '运行中', value: props.runningModelCount, isAlert: false },
  { label: '今日预报', value: props.todayRecordCount, isAlert: false },
  { label: '告警', value: props.alertCount, isAlert: props.alertCount > 0 }
])
</script>
```

- [ ] **Step 3: Update styles — change divider to 2px, add nav hint style**

Replace the `<style scoped>` block with:

```css
<style scoped>
.editorial-stat {
  display: flex;
  align-items: baseline;
}

/* Remove global 1px border — parent DashboardView provides the 2px divider */
.editorial-stats {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.stats-nav-hint {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-left: auto;
  align-self: center;
  white-space: nowrap;
}
</style>
```

- [ ] **Step 5: Commit**

```bash
git add ocean-web/src/views/dashboard/StatCards.vue
git commit -m "feat: add navigation click and hint to StatCards"
```

---

### Task 3: Add navigation to TrendCard.vue

**Files:**
- Modify: `ocean-web/src/views/dashboard/TrendCard.vue`

- [ ] **Step 1: Update template — add nav hint and click handler, remove editorial-section to avoid unwanted bottom border**

Replace the existing template with:

```html
<template>
  <div class="trend-card-wrapper" style="cursor: pointer;" @click="$emit('navigate')">
    <p class="editorial-section-label">Feature · 趋势分析</p>
    <h3 class="editorial-section-heading" style="display: flex; justify-content: space-between; align-items: baseline;">
      <span>{{ title }}</span>
      <span class="trend-nav-hint">{{ dataType === 'SST' ? 'SST 预测' : 'CHL 预测' }} →</span>
    </h3>
    <p class="editorial-narrative">{{ narrativeText }}</p>
    <div v-if="!series.length && !loading" class="editorial-narrative">暂无趋势数据</div>
    <div v-loading="loading" class="chart-wrapper" ref="chartRef"></div>
  </div>
</template>
```

- [ ] **Step 2: Add emit declaration**

Add `defineEmits(['navigate'])` after the props definition:

```js
defineEmits(['navigate'])
```

- [ ] **Step 3: Add nav hint style**

Append to the `<style scoped>` block:

```css
.trend-nav-hint {
  font-size: 11px;
  color: var(--color-text-muted);
  font-weight: 400;
  font-family: var(--font-sans);
  white-space: nowrap;
}

.trend-card-wrapper {
  transition: opacity 0.15s;
}
.trend-card-wrapper:hover {
  opacity: 0.85;
}
```

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/dashboard/TrendCard.vue
git commit -m "feat: add navigation click and hint to TrendCard"
```

---

### Task 4: Add navigation to LatestDataTable.vue

**Files:**
- Modify: `ocean-web/src/views/dashboard/LatestDataTable.vue`

- [ ] **Step 1: Update template — wrap in clickable container with nav hint**

Replace the existing template with:

```html
<template>
  <div style="cursor: pointer;" @click="$emit('navigate')">
    <div style="display: flex; justify-content: space-between; align-items: baseline;">
      <p class="editorial-section-label">数据附录</p>
      <span class="table-nav-hint">观测数据 →</span>
    </div>
    <table class="editorial-table">
      <thead>
        <tr>
          <td>观测点</td>
          <td>{{ valueLabel }}</td>
          <td>预报日期</td>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, idx) in data" :key="idx">
          <td>{{ row.locationName }}</td>
          <td>{{ row.value }} {{ unit }}</td>
          <td class="text-muted">{{ row.forecastDate }}</td>
        </tr>
        <tr v-if="!data.length && !loading">
          <td colspan="3" class="text-muted" style="text-align: center;">暂无数据</td>
        </tr>
      </tbody>
    </table>
    <div v-loading="loading" style="min-height: 120px;" v-if="loading"></div>
  </div>
</template>
```

- [ ] **Step 2: Add emit declaration**

Replace the `<script setup>` block with:

```js
<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  data: { type: Array, default: () => [] },
  dataType: { type: String, default: 'SST' },
  loading: { type: Boolean, default: false }
})

defineEmits(['navigate'])

const unit = computed(() => props.dataType === 'SST' ? '°C' : 'mg/m³')
const valueLabel = computed(() => props.dataType === 'SST' ? '温度值' : '浓度值')
</script>
```

- [ ] **Step 3: Add nav hint style**

Replace the `<style scoped>` block with:

```css
<style scoped>
.table-nav-hint {
  font-size: 11px;
  color: var(--color-text-muted);
  white-space: nowrap;
}
</style>
```

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/dashboard/LatestDataTable.vue
git commit -m "feat: add navigation click and hint to LatestDataTable"
```

---

### Task 5: Remove map code and restructure DashboardView.vue

**Files:**
- Modify: `ocean-web/src/views/dashboard/DashboardView.vue`

- [ ] **Step 1: Replace template — three rows with 2px dividers, Row 3 with 1:2 split**

Replace the `<template>` block with:

```html
<template>
  <div>
    <h1 class="editorial-page-title">系统仪表盘</h1>
    <p class="editorial-page-subtitle">System Dashboard · {{ todayStr }}</p>

    <!-- Row 1: Stat Cards -->
    <div class="dashboard-row" @click="goModel">
      <StatCards
        :modelCount="data.modelCount"
        :runningModelCount="data.runningModelCount"
        :todayRecordCount="data.todayRecordCount"
        :alertCount="data.alertCount"
        @navigate="goModel"
      />
    </div>

    <!-- Row 2: Trend Charts -->
    <div class="dashboard-row" style="display: flex; gap: 40px;">
      <div style="flex: 1;">
        <TrendCard
          title="海表温度 SST"
          dataType="SST"
          :series="sstTrend"
          :loading="loading.trendSst"
          @navigate="goSst"
        />
      </div>
      <div style="flex: 1;">
        <TrendCard
          title="叶绿素浓度 CHL"
          dataType="CHL"
          :series="chlTrend"
          :loading="loading.trendChl"
          @navigate="goChl"
        />
      </div>
    </div>

    <!-- Row 3: Alerts + Data Tables -->
    <div class="dashboard-row dashboard-row--last" style="display: flex; gap: 40px;">
      <div style="flex: 1;">
        <AlertPanel :alerts="alerts" :loading="loading.alerts" />
      </div>
      <div style="flex: 2; display: flex; flex-direction: column; gap: 24px;">
        <LatestDataTable
          title="最新海表温度 (SST)"
          dataType="SST"
          :data="data.latestSstData"
          :loading="loading.dashboard"
          @navigate="goOceanData"
        />
        <LatestDataTable
          title="最新叶绿素浓度 (CHL)"
          dataType="CHL"
          :data="data.latestChlData"
          :loading="loading.dashboard"
          @navigate="goOceanData"
        />
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Replace script — remove map code, add router and nav handlers**

Replace the `<script setup>` block with:

```js
<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboard, getDashboardTrend, getTodayAlerts } from '../../api/forecast'
import StatCards from './StatCards.vue'
import TrendCard from './TrendCard.vue'
import AlertPanel from './AlertPanel.vue'
import LatestDataTable from './LatestDataTable.vue'

const router = useRouter()

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

const loading = reactive({
  dashboard: false,
  trendSst: false,
  trendChl: false,
  alerts: false
})

const todayStr = computed(() => {
  const d = new Date()
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
  return `${months[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`
})

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

function goModel() { router.push('/app/model') }
function goSst() { router.push('/app/forecast/sst') }
function goChl() { router.push('/app/forecast/chl') }
function goOceanData() { router.push('/app/ocean-data') }

onMounted(() => {
  fetchDashboard()
  fetchTrend('SST')
  fetchTrend('CHL')
  fetchAlerts()
})
</script>
```

- [ ] **Step 3: Replace styles — add 2px divider for rows**

Replace the `<style scoped>` block with:

```css
<style scoped>
.dashboard-row {
  padding-bottom: 28px;
  margin-bottom: 32px;
  border-bottom: 2px solid #e0e0e0;
}
.dashboard-row--last {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}
</style>
```

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/dashboard/DashboardView.vue
git commit -m "feat: restructure dashboard to 3-row layout with navigation"
```

---

### Task 6: Verify build and check for regressions

- [ ] **Step 1: Verify frontend builds successfully**

```bash
cd ocean-web && npm run build
```

Expected: Build completes without errors.

- [ ] **Step 2: Start dev server and visually verify**

```bash
cd ocean-web && npm run dev
```

Open the dashboard page and verify:
- Map is gone
- Three rows display with 2px dividers
- StatCards row shows `模型管理 →` hint
- SST/CHL TrendCards show `SST 预测 →` / `CHL 预测 →` hints
- Data tables show `观测数据 →` hint
- AlertPanel shows normally without click hint
- Row 3 has alerts on left (1/3) and tables stacked on right (2/3)
- Hovering over clickable areas shows pointer cursor
