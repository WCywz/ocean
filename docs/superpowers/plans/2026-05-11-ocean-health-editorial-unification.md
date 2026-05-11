# 海洋健康指数 Editorial 风格统一 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 OceanHealthView.vue 的视觉风格统一到 Editorial 设计体系，保留功能布局和交互逻辑。

**Architecture:** 单文件重构 — 重写 `<template>` 和 `<style scoped>`，`<script setup>` 小幅调整（移除废弃的 computed、新增等级文字颜色映射）。复用 editorial.css 全局类，自定义 scoped 样式仅处理左侧色条和信息条。

**Tech Stack:** Vue 3, Element Plus (el-date-picker), editorial.css

---

## File Map

| File | Action | Responsibility |
|------|--------|---------------|
| `ocean-web/src/views/health/OceanHealthView.vue` | **Rewrite template + style** | Editorial 风格统一 |

---

### Task 1: Rewrite Template — Top Section

**Files:**
- Modify: `ocean-web/src/views/health/OceanHealthView.vue:1-82`

- [ ] **Step 1: Replace the banner + toolbar section (lines 1-20) with editorial header**

Replace:
```vue
<!-- summary banner -->
<div :class="['health-banner', `health-banner--${bannerLevel}`]">
  <span class="health-banner__icon" v-html="bannerIcon"></span>
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
```

With:
```vue
<h1 class="editorial-page-title">海洋健康指数</h1>
<p class="editorial-page-subtitle">Ocean Health Index</p>

<div class="health-status-bar" :style="{ borderLeftColor: statusColor }">
  <span class="health-status-bar__level">{{ statusLabel }}</span>
  <span class="health-status-bar__dot">&middot;</span>
  <span class="health-status-bar__desc">{{ bannerText }}</span>
  <span class="health-status-bar__date" @click.stop>
    <el-date-picker
      v-model="forecastDate"
      type="date"
      placeholder="选择日期"
      format="YYYY-MM-DD"
      value-format="YYYY-MM-DD"
      @change="fetchData"
      :teleported="false"
      popper-class="health-date-popper"
    />
  </span>
</div>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/health/OceanHealthView.vue
git commit -m "feat: replace health banner with editorial title and status bar"
```

---

### Task 2: Rewrite Template — Card Grid

**Files:**
- Modify: `ocean-web/src/views/health/OceanHealthView.vue:22-80`

- [ ] **Step 1: Replace the card grid (lines 22-80) with editorial cards**

Replace:
```vue
<!-- zone card grid -->
<div v-loading="loading" class="health-grid">
  <template v-if="assessments.length">
    <div
      v-for="zone in assessments"
      :key="zone.id"
      :class="['health-card', { 'health-card--active': selectedId === zone.id, 'health-card--dimmed': selectedId && selectedId !== zone.id }]"
      @click="selectZone(zone.id)"
    >
      <div class="health-card__label">{{ zone.label }}</div>
      <div class="health-card__body">
        <div class="health-card__badge" :style="{ background: zone.overall.color }">{{ zone.overall.label }}</div>
        <div class="health-card__info">
          <div class="health-card__level">{{ levelText[zone.overall.level] }}</div>
          <div class="health-card__hint">{{ primaryConcern(zone) }}</div>
        </div>
      </div>
      <div class="health-card__tags">
        <span :class="tagClass(zone.sst.level)">SST {{ trendSymbol(zone.sst.trend) }}</span>
        <span :class="tagClass(zone.chl.level)">Chl {{ trendSymbol(zone.chl.trend) }}</span>
        <span :class="tagClass(zone.heatwave.level)">热浪 {{ zone.heatwave.active ? '有' : '无' }}</span>
      </div>

      <!-- expand detail -->
      <div v-if="selectedId === zone.id" class="health-card__detail">
        <p class="health-card__interpretation">{{ buildInterpretation(zone) }}</p>
        <table class="health-card__table">
          <tr>
            <td>SST 当前值</td>
            <td>{{ fmtTemp(zone.sst.value) }}（{{ fmtAnomaly(zone.sst.anomaly) }}）</td>
            <td><span class="level-tag" :style="{ background: zone.sst.color }">{{ zone.sst.label }}</span></td>
          </tr>
          <tr>
            <td>SST 趋势</td>
            <td>{{ trendText(zone.sst.trend) }}</td>
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
  </template>
  <div v-else class="health-empty">暂无数据</div>
</div>
```

With:
```vue
<p class="editorial-section-label">区域健康评估 &middot; 东海</p>

<div v-loading="loading" class="health-grid">
  <template v-if="assessments.length">
    <div
      v-for="zone in assessments"
      :key="zone.id"
      :class="['health-card', { 'health-card--active': selectedId === zone.id, 'health-card--dimmed': selectedId && selectedId !== zone.id }]"
      :style="{ borderLeftColor: zone.overall.color }"
      @click="selectZone(zone.id)"
    >
      <div class="health-card__label">{{ zone.label }}</div>
      <div class="health-card__body">
        <span class="health-card__level" :class="{ 'health-card__level--warn': zone.overall.level === 'warn', 'health-card__level--bad': zone.overall.level === 'bad' }">{{ levelText[zone.overall.level] }}</span>
        <span class="health-card__hint">&ensp;&middot;&ensp;{{ primaryConcern(zone) }}</span>
      </div>
      <div class="health-card__tags">
        <span>SST {{ trendSymbol(zone.sst.trend) }}</span>
        <span>Chl {{ trendSymbol(zone.chl.trend) }}</span>
        <span>热浪 {{ zone.heatwave.active ? '有' : '无' }}</span>
      </div>

      <div v-if="selectedId === zone.id" class="health-card__detail" :style="{ borderColor: zone.overall.color }">
        <span class="editorial-section-label">Detail &middot; {{ zone.label }}</span>
        <p class="health-card__interpretation">{{ buildInterpretation(zone) }}</p>
        <table class="editorial-table health-detail-table">
          <thead>
            <tr>
              <td>指标</td>
              <td>当前值</td>
              <td>等级</td>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>SST</td>
              <td>{{ fmtTemp(zone.sst.value) }}（{{ fmtAnomaly(zone.sst.anomaly) }}）</td>
              <td><span class="level-badge" :style="{ background: zone.sst.color }">{{ zone.sst.label }}</span></td>
            </tr>
            <tr>
              <td>SST 趋势</td>
              <td>{{ trendText(zone.sst.trend) }}</td>
              <td class="text-muted">{{ zone.sst.level === 'bad' || zone.sst.level === 'warn' ? '关注' : '正常' }}</td>
            </tr>
            <tr>
              <td>Chl 浓度</td>
              <td>{{ zone.chl.value.toFixed(1) }} mg/m³</td>
              <td><span class="level-badge" :style="{ background: zone.chl.color }">{{ zone.chl.label }}</span></td>
            </tr>
            <tr>
              <td>海洋热浪</td>
              <td>{{ zone.heatwave.active ? '已持续 ' + zone.heatwave.days + ' 天' : '未见异常' }}</td>
              <td><span class="level-badge" :style="{ background: zone.heatwave.color }">{{ zone.heatwave.label }}</span></td>
            </tr>
          </tbody>
        </table>
        <div class="health-card__advice">
          <strong>建议：</strong>
          <ul>
            <li v-for="(item, i) in zone.advice" :key="i">{{ item }}</li>
          </ul>
        </div>
      </div>
    </div>
  </template>
  <div v-else class="health-empty">暂无数据</div>
</div>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/health/OceanHealthView.vue
git commit -m "feat: restyle health cards with editorial left-border and serif typography"
```

---

### Task 3: Update Script — Cleanup Computed Properties

**Files:**
- Modify: `ocean-web/src/views/health/OceanHealthView.vue:84-191`

- [ ] **Step 1: Remove `bannerIcon` and `bannerLevel` computed; add `statusColor` and `statusLabel`**

In `<script setup>`, remove these lines:
```js
// Remove:
const bannerLevel = computed(() => {
  if (!assessments.value.length) return 'fine'
  const order = ['good', 'fine', 'warn', 'bad']
  return assessments.value.reduce((worst, a) => {
    return order.indexOf(a.overall.level) > order.indexOf(worst) ? a.overall.level : worst
  }, 'good')
})

const bannerIcon = computed(() => {
  const icons = { good: '&#9989;', fine: '&#9989;', warn: '&#9888;&#65039;', bad: '&#128308;' }
  return icons[bannerLevel.value]
})
```

Add these computed properties after `bannerText`:
```js
const statusColor = computed(() => {
  if (!assessments.value.length) return '#22c55e'
  const order = ['good', 'fine', 'warn', 'bad']
  const worst = assessments.value.reduce((w, a) => {
    return order.indexOf(a.overall.level) > order.indexOf(w) ? a.overall.level : w
  }, 'good')
  const colors = { good: '#22c55e', fine: '#22c55e', warn: '#f59e0b', bad: '#ef4444' }
  return colors[worst]
})

const statusLabel = computed(() => {
  if (!assessments.value.length) return '--'
  const order = ['good', 'fine', 'warn', 'bad']
  const worst = assessments.value.reduce((w, a) => {
    return order.indexOf(a.overall.level) > order.indexOf(w) ? a.overall.level : w
  }, 'good')
  return levelText[worst]
})
```

- [ ] **Step 2: Remove `tagClass` function (no longer needed)**

Remove:
```js
function tagClass(level) {
  return `health-tag health-tag--${level}`
}
```

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/views/health/OceanHealthView.vue
git commit -m "feat: replace banner computed props with status bar helpers"
```

---

### Task 4: Rewrite Styles — Editorial Scoped CSS

**Files:**
- Modify: `ocean-web/src/views/health/OceanHealthView.vue:194-398`

- [ ] **Step 1: Replace the entire `<style scoped>` block (lines 194-398)**

Replace:
```css
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

.health-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px;
  color: #999;
  font-size: 14px;
}

@media (max-width: 800px) {
  .health-grid {
    grid-template-columns: 1fr;
  }
}
</style>
```

With:
```css
<style scoped>
/* ---- status bar ---- */
.health-status-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  border-left: 3px solid;
  padding: 10px 14px;
  background: #fafafa;
  font-size: 13px;
  margin-bottom: 28px;
}

.health-status-bar__level {
  font-family: var(--font-serif);
  font-size: 15px;
  color: var(--color-text);
}

.health-status-bar__dot {
  color: var(--color-text-muted);
}

.health-status-bar__desc {
  color: #666;
  flex: 1;
}

.health-status-bar__date {
  font-size: 11px;
  color: var(--color-text-muted);
  cursor: pointer;
  white-space: nowrap;
}

.health-status-bar__date :deep(.el-input__wrapper) {
  box-shadow: none;
  padding: 0;
  background: transparent;
  border-bottom: 1px dashed #ccc;
  border-radius: 0;
}

/* ---- grid ---- */
.health-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

/* ---- card ---- */
.health-card {
  background: #fff;
  padding: 14px 16px;
  border-top: 1px solid #f0f0f0;
  border-right: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  border-left: 3px solid;
  cursor: pointer;
  transition: opacity 0.2s;
}

.health-card--dimmed {
  opacity: 0.45;
}

.health-card__label {
  font-size: 11px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 4px;
}

.health-card__body {
  display: flex;
  align-items: baseline;
  margin-bottom: 8px;
}

.health-card__level {
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 400;
  color: var(--color-text);
}

.health-card__level--warn {
  color: #92400e;
}

.health-card__level--bad {
  color: var(--color-alert);
}

.health-card__hint {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.health-card__tags {
  display: flex;
  gap: 14px;
  padding-top: 8px;
  border-top: 1px solid #f5f5f5;
  font-size: 11px;
  color: var(--color-text-muted);
}

/* ---- detail panel ---- */
.health-card__detail {
  margin-top: 14px;
  padding: 16px 20px;
  border: 2px solid;
}

.health-card__interpretation {
  font-size: 13px;
  color: #555;
  line-height: 1.8;
  margin: 6px 0 14px 0;
}

.health-detail-table {
  margin-bottom: 12px;
}

.level-badge {
  color: #fff;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 11px;
}

.health-card__advice {
  background: var(--color-surface);
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

.health-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px;
  color: var(--color-text-muted);
  font-size: 14px;
}

.health-date-popper {
  font-family: var(--font-sans);
}

@media (max-width: 800px) {
  .health-grid {
    grid-template-columns: 1fr;
  }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/health/OceanHealthView.vue
git commit -m "style: replace health custom styles with editorial scoped CSS"
```

---

### Task 5: Verification

**Files:** None (read-only check)

- [ ] **Step 1: Build the frontend**

Run: `cd ocean-web && npm run build`
Expected: Build succeeds with no errors.

- [ ] **Step 2: Start dev server and visual check**

Run: `cd ocean-web && npm run dev`
Navigate to `http://localhost:3000/app/ocean-health`
Verify:
- Page title is serif "海洋健康指数" with italic subtitle
- Status bar has left color border + summary text + date
- Cards have left color border, serif level text, no shadow, no round badge
- Clicking a card expands detail with color-matched border
- Other cards dim on selection
- Clicking same card collapses detail

- [ ] **Step 3: Commit if any fixes**

```bash
git add -A
git commit -m "chore: final editorial unification cleanup"
```
