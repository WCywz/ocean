# Editorial Style UI Redesign — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the dark-sidebar Element Plus style with an editorial/magazine-inspired design: pure white, serif headings, top nav bar, no cards/shadows/colored icon blocks.

**Architecture:** Global CSS tokens in a shared stylesheet (`editorial.css`), top nav replaces sidebar in MainLayout, each page/view restyled individually by replacing `el-card` wrappers with plain divs and dividers, tables converted from `el-table` to native `<table>` with minimal styling.

**Tech Stack:** Vue 3, Element Plus (reduced usage), ECharts, Leaflet, Vite

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `ocean-web/src/styles/editorial.css` | **Create** | Global design tokens: typography, colors, spacing, utility classes |
| `ocean-web/src/App.vue` | **Modify** | Import editorial.css, update global reset |
| `ocean-web/src/layout/MainLayout.vue` | **Rewrite** | Top nav bar replacing sidebar |
| `ocean-web/src/views/login/LoginView.vue` | **Modify** | Editorial restyle |
| `ocean-web/src/views/register/RegisterView.vue` | **Modify** | Editorial restyle |
| `ocean-web/src/views/home/HomeView.vue` | **Modify** | Editorial restyle landing page |
| `ocean-web/src/views/dashboard/DashboardView.vue` | **Modify** | Remove card wrappers, section dividers |
| `ocean-web/src/views/dashboard/StatCards.vue` | **Rewrite** | Serif numbers, no card backgrounds |
| `ocean-web/src/views/dashboard/TrendCard.vue` | **Modify** | Remove el-card, add narrative text |
| `ocean-web/src/views/dashboard/DashboardMap.vue` | **Modify** | Remove el-card wrapper |
| `ocean-web/src/views/dashboard/AlertPanel.vue` | **Modify** | Remove el-card, editorial alert list |
| `ocean-web/src/views/dashboard/LatestDataTable.vue` | **Modify** | Native table, minimal styling |
| `ocean-web/src/views/user/UserView.vue` | **Modify** | Native table, custom pagination, editorial form |
| `ocean-web/src/views/model/ModelView.vue` | **Modify** | Native table, custom pagination, editorial form |
| `ocean-web/src/views/ocean/OceanDataView.vue` | **Modify** | Remove card wrappers, native table |
| `ocean-web/src/views/forecast/SstMapView.vue` | **Modify** | Remove card wrappers |
| `ocean-web/src/views/forecast/ChxMapView.vue` | **Modify** | Remove card wrappers |
| `ocean-web/src/views/forecast/HistoryView.vue` | **Modify** | Remove card wrapper, native table |

---

### Task 1: Global Design Tokens (editorial.css)

**Files:**
- Create: `ocean-web/src/styles/editorial.css`

- [ ] **Step 1: Write editorial.css with all design tokens**

```css
/* ===== Editorial Design Tokens ===== */

/* --- Typography --- */
:root {
  --font-serif: Georgia, 'Times New Roman', serif;
  --font-sans: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', sans-serif;
  --font-mono: 'SF Mono', 'Fira Code', 'Consolas', monospace;

  --color-bg: #ffffff;
  --color-surface: #fafafa;
  --color-text: #2c3e50;
  --color-text-secondary: #aaa;
  --color-text-muted: #bbb;
  --color-divider: #f0f0f0;
  --color-divider-strong: #eee;
  --color-border: #d0d0d0;
  --color-border-light: #e8e8e8;
  --color-alert: #c0392b;
}

/* --- Global resets --- */
body {
  font-family: var(--font-sans);
  color: var(--color-text);
  background: var(--color-bg);
  -webkit-font-smoothing: antialiased;
}

/* --- Editorial nav bar --- */
.editorial-nav {
  display: flex;
  align-items: center;
  height: 52px;
  padding: 0 36px;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-divider-strong);
  gap: 32px;
}
.editorial-nav__brand {
  font-family: var(--font-serif);
  font-size: 16px;
  color: var(--color-text);
  letter-spacing: 0.02em;
  text-decoration: none;
  cursor: pointer;
  white-space: nowrap;
}
.editorial-nav__items {
  display: flex;
  gap: 2px;
}
.editorial-nav__item {
  font-size: 11px;
  color: var(--color-text-muted);
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 5px 12px;
  cursor: pointer;
  border-radius: 4px;
  text-decoration: none;
  transition: color 0.15s;
  white-space: nowrap;
}
.editorial-nav__item:hover {
  color: var(--color-text);
}
.editorial-nav__item--active {
  color: var(--color-text);
  font-weight: 600;
}
.editorial-nav__spacer {
  flex: 1;
}
.editorial-nav__user {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--color-text-muted);
  white-space: nowrap;
}

/* --- Editorial page titles --- */
.editorial-page-title {
  font-family: var(--font-serif);
  font-size: 28px;
  font-weight: 400;
  color: var(--color-text);
  margin: 0 0 4px 0;
  letter-spacing: -0.01em;
}
.editorial-page-subtitle {
  font-size: 13px;
  color: var(--color-text-muted);
  font-style: italic;
  margin: 0 0 36px 0;
}

/* --- Editorial stats row --- */
.editorial-stats {
  display: flex;
  gap: 48px;
  padding-bottom: 32px;
  margin-bottom: 36px;
  border-bottom: 1px solid var(--color-divider);
}
.editorial-stat__value {
  font-family: var(--font-serif);
  font-size: 42px;
  color: var(--color-text);
  line-height: 1;
}
.editorial-stat__label {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-left: 8px;
}
.editorial-stat--alert .editorial-stat__value,
.editorial-stat--alert .editorial-stat__label {
  color: var(--color-alert);
}

/* --- Editorial section label --- */
.editorial-section-label {
  font-size: 10px;
  color: var(--color-text-muted);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  margin-bottom: 10px;
}

/* --- Editorial section divider --- */
.editorial-section {
  padding-bottom: 32px;
  margin-bottom: 36px;
  border-bottom: 1px solid var(--color-divider);
}

/* --- Editorial section heading --- */
.editorial-section-heading {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 400;
  color: var(--color-text);
  margin: 0 0 8px 0;
}

/* --- Editorial narrative text --- */
.editorial-narrative {
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.8;
  margin: 0 0 14px 0;
}

/* --- Editorial table --- */
.editorial-table {
  width: 100%;
  font-size: 13px;
  border-collapse: collapse;
}
.editorial-table thead td {
  color: var(--color-text-muted);
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  padding: 10px 0;
  border-bottom: 1px solid var(--color-divider-strong);
  font-weight: 400;
}
.editorial-table tbody td {
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
  color: var(--color-text);
}
.editorial-table tbody tr:last-child td {
  border-bottom: none;
}
.editorial-table .text-muted {
  color: var(--color-text-muted);
}

/* --- Editorial input (underline style) --- */
.editorial-input {
  width: 100%;
  border: none;
  border-bottom: 1px solid #e0e0e0;
  padding: 10px 0;
  font-size: 14px;
  color: var(--color-text);
  outline: none;
  font-family: var(--font-sans);
  background: transparent;
  transition: border-color 0.2s;
}
.editorial-input:focus {
  border-bottom-color: var(--color-text);
}
.editorial-input::placeholder {
  color: var(--color-text-muted);
}

/* --- Editorial select (underline style) --- */
.editorial-select {
  width: 100%;
  border: none;
  border-bottom: 1px solid #e0e0e0;
  padding: 10px 0;
  font-size: 14px;
  color: var(--color-text);
  outline: none;
  font-family: var(--font-sans);
  background: transparent;
  cursor: pointer;
}

/* --- Editorial button: primary (solid dark) --- */
.editorial-btn {
  padding: 10px 0;
  background: var(--color-text);
  color: #fff;
  border: none;
  font-family: var(--font-mono);
  font-size: 13px;
  letter-spacing: 0.2em;
  cursor: pointer;
  text-align: center;
  transition: opacity 0.2s;
}
.editorial-btn:hover {
  opacity: 0.85;
}
.editorial-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* --- Editorial button: outline --- */
.editorial-btn-outline {
  padding: 7px 18px;
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text);
  font-size: 12px;
  letter-spacing: 0.04em;
  cursor: pointer;
  font-family: var(--font-sans);
  transition: border-color 0.2s;
}
.editorial-btn-outline:hover {
  border-color: var(--color-text);
}

/* --- Editorial text link --- */
.editorial-link {
  color: var(--color-text-muted);
  text-decoration: none;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.15s;
}
.editorial-link:hover {
  color: var(--color-text);
}
.editorial-link--muted {
  color: #ddd;
}

/* --- Editorial form label --- */
.editorial-form-label {
  font-size: 11px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: 4px;
  display: block;
}

/* --- Editorial pagination --- */
.editorial-pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 16px;
}
.editorial-pagination__page {
  cursor: pointer;
  padding: 2px 6px;
}
.editorial-pagination__page--active {
  color: var(--color-text);
  border-bottom: 2px solid var(--color-text);
}

/* --- Editorial tag (monospace text) --- */
.editorial-tag {
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--color-text);
}

/* --- Editorial search input --- */
.editorial-search {
  padding: 7px 12px;
  border: 1px solid var(--color-border-light);
  font-size: 12px;
  color: var(--color-text);
  outline: none;
  background: transparent;
  font-family: var(--font-sans);
  width: 180px;
}
.editorial-search:focus {
  border-color: var(--color-border);
}
.editorial-search::placeholder {
  color: var(--color-text-muted);
}

/* --- Editorial filter bar --- */
.editorial-filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

/* --- Editorarial content area --- */
.editorial-content {
  padding: 36px 40px;
}

/* --- Remove Element Plus card shadows globally --- */
.el-card {
  box-shadow: none !important;
  border: 1px solid var(--color-divider-strong) !important;
  border-radius: 0 !important;
}
```

- [ ] **Step 2: Verify the file was created**

Run: `ls ocean-web/src/styles/editorial.css`

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/styles/editorial.css
git commit -m "feat: add editorial design tokens (editorial.css)"
```

---

### Task 2: Update App.vue Global Styles

**Files:**
- Modify: `ocean-web/src/App.vue`

- [ ] **Step 1: Import editorial.css and update global font**

Replace the `<style>` block in `App.vue`:

```vue
<style>
@import './styles/editorial.css';

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
html, body, #app {
  height: 100%;
  font-family: var(--font-sans);
  background: var(--color-bg);
  color: var(--color-text);
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/App.vue
git commit -m "feat: import editorial.css and update global styles in App.vue"
```

---

### Task 3: Rewrite MainLayout — Top Nav Bar

**Files:**
- Modify: `ocean-web/src/layout/MainLayout.vue`

- [ ] **Step 1: Replace sidebar layout with top nav and forecast dropdown**

Replace entire file:

```vue
<template>
  <div class="app-shell">
    <!-- Top navigation bar -->
    <nav class="editorial-nav">
      <router-link to="/app/dashboard" class="editorial-nav__brand">
        海洋预报系统
      </router-link>

      <div class="editorial-nav__items">
        <router-link
          to="/app/dashboard"
          class="editorial-nav__item"
          :class="{ 'editorial-nav__item--active': isActive('/app/dashboard') }"
        >仪表盘</router-link>

        <!-- Forecast dropdown -->
        <div
          class="editorial-nav__item"
          :class="{ 'editorial-nav__item--active': isActive('/app/forecast') }"
          @mouseenter="showForecastMenu = true"
          @mouseleave="showForecastMenu = false"
          style="position: relative;"
        >
          预报
          <div
            v-show="showForecastMenu"
            class="forecast-dropdown"
            @mouseenter="showForecastMenu = true"
            @mouseleave="showForecastMenu = false"
          >
            <router-link to="/app/forecast/sst" class="forecast-dropdown__item">海表温度预测</router-link>
            <router-link to="/app/forecast/chl" class="forecast-dropdown__item">叶绿素预测</router-link>
            <router-link to="/app/forecast/history" class="forecast-dropdown__item">历史预报记录</router-link>
          </div>
        </div>

        <router-link
          to="/app/ocean-data"
          class="editorial-nav__item"
          :class="{ 'editorial-nav__item--active': isActive('/app/ocean-data') }"
        >观测</router-link>

        <template v-if="isAdmin">
          <router-link
            to="/app/model"
            class="editorial-nav__item"
            :class="{ 'editorial-nav__item--active': isActive('/app/model') }"
          >模型</router-link>
          <router-link
            to="/app/user"
            class="editorial-nav__item"
            :class="{ 'editorial-nav__item--active': isActive('/app/user') }"
          >用户</router-link>
        </template>
      </div>

      <span class="editorial-nav__spacer"></span>

      <span v-if="isAdmin" class="editorial-tag" style="margin-right: 12px;">ADMIN</span>
      <span class="editorial-nav__user">{{ userInfo?.realName || userInfo?.username }}</span>
      <a class="editorial-link" style="margin-left: 16px;" @click="handleLogout">退出</a>
    </nav>

    <!-- Content area -->
    <main class="editorial-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const showForecastMenu = ref(false)
const userInfo = computed(() => userStore.userInfo)
const isAdmin = computed(() => userStore.isAdmin())

function isActive(basePath) {
  return route.path.startsWith(basePath)
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    .then(() => {
      userStore.logout()
      router.push('/')
    })
    .catch(() => {})
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.forecast-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  background: var(--color-bg);
  border: 1px solid var(--color-divider-strong);
  min-width: 140px;
  z-index: 200;
  padding: 8px 0;
}
.forecast-dropdown__item {
  display: block;
  padding: 8px 16px;
  font-size: 13px;
  color: var(--color-text);
  text-decoration: none;
  text-transform: none;
  letter-spacing: 0;
  transition: background 0.15s;
}
.forecast-dropdown__item:hover {
  background: var(--color-surface);
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/layout/MainLayout.vue
git commit -m "feat: replace sidebar with editorial top nav bar"
```

---

### Task 4: Restyle Login Page

**Files:**
- Modify: `ocean-web/src/views/login/LoginView.vue`

- [ ] **Step 1: Replace template with editorial layout**

Replace the `<template>` block:

```vue
<template>
  <div class="login-page">
    <div class="login-form">
      <div style="text-align: center; margin-bottom: 40px;">
        <h1 class="editorial-page-title" style="font-size: 28px; text-align: center;">海洋环境预报系统</h1>
        <p style="font-family: var(--font-serif); font-size: 13px; color: var(--color-text-muted); font-style: italic; margin: 6px 0 0 0;">
          Ocean Forecast System
        </p>
      </div>

      <div style="width: 320px;">
        <div style="margin-bottom: 24px;">
          <input
            v-model="form.username"
            class="editorial-input"
            placeholder="用户名"
            @keyup.enter="handleLogin"
          />
        </div>
        <div style="margin-bottom: 28px;">
          <input
            v-model="form.password"
            class="editorial-input"
            type="password"
            placeholder="密码"
            @keyup.enter="handleLogin"
          />
        </div>
        <button
          class="editorial-btn"
          style="width: 100%; padding: 12px 0;"
          :disabled="loading"
          @click="handleLogin"
        >
          {{ loading ? '...' : '登  录' }}
        </button>
        <div style="text-align: center; margin-top: 20px;">
          <span style="color: var(--color-text-muted); font-size: 12px;">还没有账号？</span>
          <a class="editorial-link" style="color: var(--color-text);" @click="$router.push('/register')">注册</a>
        </div>
        <div style="text-align: center; margin-top: 12px;">
          <a class="editorial-link" @click="$router.push('/')">返回首页</a>
        </div>
      </div>

      <div style="margin-top: 28px; font-size: 11px; color: var(--color-text-muted); text-align: center;">
        管理员: admin / admin123 &nbsp;·&nbsp; 用户: user / user123
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Remove el-form ref from script**

In `<script setup>`, remove `formRef` and `rules`. Keep the rest of the script logic intact but simplify `handleLogin`:

```js
async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await login(form)
    userStore.setToken(res.data.token)
    userStore.setUserInfo({
      userId: res.data.userId,
      username: res.data.username,
      realName: res.data.realName,
      role: res.data.role
    })
    ElMessage.success('登录成功')
    router.push('/app/dashboard')
  } catch (e) {
    // error handled in interceptor
  } finally {
    loading.value = false
  }
}
```

- [ ] **Step 3: Replace `<style scoped>` block**

```css
<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
}
.login-form {
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>
```

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/login/LoginView.vue
git commit -m "feat: restyle login page with editorial design"
```

---

### Task 5: Restyle Register Page

**Files:**
- Modify: `ocean-web/src/views/register/RegisterView.vue`

- [ ] **Step 1: Replace template with editorial layout**

```vue
<template>
  <div class="register-page">
    <div class="register-form">
      <div style="text-align: center; margin-bottom: 36px;">
        <h1 class="editorial-page-title" style="font-size: 24px; text-align: center;">创建账号</h1>
        <p style="font-family: var(--font-serif); font-size: 13px; color: var(--color-text-muted); font-style: italic; margin: 4px 0 0 0;">
          加入海洋环境预报系统
        </p>
      </div>

      <div style="width: 320px;">
        <div style="margin-bottom: 20px;">
          <input v-model="form.username" class="editorial-input" placeholder="用户名" />
        </div>
        <div style="margin-bottom: 20px;">
          <input v-model="form.realName" class="editorial-input" placeholder="真实姓名" />
        </div>
        <div style="margin-bottom: 20px;">
          <input v-model="form.password" class="editorial-input" type="password" placeholder="密码" />
        </div>
        <div style="margin-bottom: 28px;">
          <input v-model="form.confirmPassword" class="editorial-input" type="password" placeholder="再次输入密码" />
        </div>
        <button
          class="editorial-btn"
          style="width: 100%; padding: 12px 0;"
          :disabled="loading"
          @click="handleRegister"
        >
          {{ loading ? '...' : '注  册' }}
        </button>
        <div style="text-align: center; margin-top: 20px;">
          <span style="color: var(--color-text-muted); font-size: 12px;">已有账号？</span>
          <a class="editorial-link" style="color: var(--color-text);" @click="$router.push('/login')">去登录</a>
        </div>
        <div style="text-align: center; margin-top: 12px;">
          <a class="editorial-link" @click="$router.push('/')">返回首页</a>
        </div>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Simplify script — remove formRef, rules, replace handleRegister**

```js
async function handleRegister() {
  if (!form.username || !form.realName || !form.password || !form.confirmPassword) {
    ElMessage.warning('请填写所有字段')
    return
  }
  if (form.username.length < 3 || form.username.length > 20) {
    ElMessage.warning('用户名长度在 3 到 20 个字符')
    return
  }
  if (form.password.length < 6 || form.password.length > 20) {
    ElMessage.warning('密码长度在 6 到 20 个字符')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await register({
      username: form.username,
      realName: form.realName,
      password: form.password,
      role: 'USER',
      status: 1
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    // error handled in interceptor
  } finally {
    loading.value = false
  }
}
```

- [ ] **Step 3: Replace style block**

```css
<style scoped>
.register-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
}
.register-form {
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>
```

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/register/RegisterView.vue
git commit -m "feat: restyle register page with editorial design"
```

---

### Task 6: Restyle Home Landing Page

**Files:**
- Modify: `ocean-web/src/views/home/HomeView.vue`

- [ ] **Step 1: Replace template with editorial landing page**

```vue
<template>
  <div class="home-page">
    <!-- Nav -->
    <nav class="editorial-nav">
      <span class="editorial-nav__brand">海洋环境预报系统</span>
      <span class="editorial-nav__spacer"></span>
      <a class="editorial-link" style="margin-right: 20px;" @click="$router.push('/login')">登录</a>
      <a class="editorial-link" @click="$router.push('/register')">注册</a>
    </nav>

    <!-- Hero -->
    <section class="hero">
      <div class="hero-inner">
        <h1 style="font-family: var(--font-serif); font-size: 36px; font-weight: 400; color: var(--color-text); margin: 0 0 8px 0; letter-spacing: -0.02em;">
          海洋环境预报系统
        </h1>
        <p style="font-family: var(--font-serif); font-size: 15px; color: var(--color-text-muted); font-style: italic; margin: 0 0 32px 0;">
          Ocean Environment Forecast System
        </p>
        <p style="font-size: 15px; color: var(--color-text-secondary); line-height: 1.8; max-width: 560px; margin: 0 auto 36px auto;">
          实时监测海表温度与叶绿素浓度变化，为海洋科学研究与环境保护提供精准预报数据
        </p>
        <div style="display: flex; gap: 16px; justify-content: center;">
          <a class="editorial-btn" style="padding: 12px 32px; display: inline-block; text-decoration: none;" @click="$router.push('/register')">
            立即注册
          </a>
          <a class="editorial-btn-outline" style="padding: 11px 31px;" @click="$router.push('/login')">
            已有账号？去登录
          </a>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section style="padding: 72px 48px; border-top: 1px solid var(--color-divider);">
      <div style="text-align: center; margin-bottom: 48px;">
        <p class="editorial-section-label">Features</p>
        <h2 style="font-family: var(--font-serif); font-size: 24px; font-weight: 400; color: var(--color-text); margin: 0;">
          系统功能
        </h2>
      </div>
      <div style="display: flex; gap: 64px; max-width: 860px; margin: 0 auto;">
        <div style="flex: 1;">
          <h3 style="font-family: var(--font-serif); font-size: 16px; font-weight: 400; color: var(--color-text); margin: 0 0 8px 0;">数据可视化</h3>
          <p style="font-size: 13px; color: var(--color-text-secondary); line-height: 1.7; margin: 0;">海表温度、叶绿素浓度实时图表展示，多观测点趋势对比分析</p>
        </div>
        <div style="flex: 1;">
          <h3 style="font-family: var(--font-serif); font-size: 16px; font-weight: 400; color: var(--color-text); margin: 0 0 8px 0;">模型管理</h3>
          <p style="font-size: 13px; color: var(--color-text-secondary); line-height: 1.7; margin: 0;">集成多种海洋预报模型，支持参数配置、启停控制与状态监控</p>
        </div>
        <div style="flex: 1;">
          <h3 style="font-family: var(--font-serif); font-size: 16px; font-weight: 400; color: var(--color-text); margin: 0 0 8px 0;">预报记录</h3>
          <p style="font-size: 13px; color: var(--color-text-secondary); line-height: 1.7; margin: 0;">历史预报数据查询与导出，支持按日期、观测点多维度检索</p>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer style="text-align: center; padding: 28px; color: var(--color-text-muted); font-size: 12px; border-top: 1px solid var(--color-divider);">
      Ocean Environment Forecast System &copy; 2026
    </footer>
  </div>
</template>
```

- [ ] **Step 2: Remove the old style block entirely, replace with:**

```css
<style scoped>
.home-page {
  min-height: 100vh;
}
.hero {
  padding: 100px 48px 80px;
}
.hero-inner {
  max-width: 640px;
  margin: 0 auto;
  text-align: center;
}
</style>
```

- [ ] **Step 3: Remove unused Element Plus icon imports from script**

The `<script setup>` block is empty — no changes needed.

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/home/HomeView.vue
git commit -m "feat: restyle home landing page with editorial design"
```

---

### Task 7: Rewrite StatCards Component

**Files:**
- Modify: `ocean-web/src/views/dashboard/StatCards.vue`

- [ ] **Step 1: Replace entire file**

```vue
<template>
  <div class="editorial-stats">
    <div v-for="card in cards" :key="card.label" class="editorial-stat" :class="{ 'editorial-stat--alert': card.isAlert }">
      <span class="editorial-stat__value">{{ card.value }}</span>
      <span class="editorial-stat__label">{{ card.label }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelCount: { type: Number, default: 0 },
  runningModelCount: { type: Number, default: 0 },
  todayRecordCount: { type: Number, default: 0 },
  alertCount: { type: Number, default: 0 }
})

const cards = computed(() => [
  { label: '模型总数', value: props.modelCount, isAlert: false },
  { label: '运行中', value: props.runningModelCount, isAlert: false },
  { label: '今日预报', value: props.todayRecordCount, isAlert: false },
  { label: '告警', value: props.alertCount, isAlert: props.alertCount > 0 }
])
</script>

<style scoped>
.editorial-stat {
  display: flex;
  align-items: baseline;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/dashboard/StatCards.vue
git commit -m "feat: rewrite StatCards with editorial serif numbers"
```

---

### Task 8: Restyle TrendCard — Remove el-card, Add Narrative

**Files:**
- Modify: `ocean-web/src/views/dashboard/TrendCard.vue`

- [ ] **Step 1: Replace template**

```vue
<template>
  <div class="editorial-section">
    <p class="editorial-section-label">Feature · 趋势分析</p>
    <h3 class="editorial-section-heading">{{ title }}</h3>
    <p class="editorial-narrative">{{ narrativeText }}</p>
    <div v-if="!series.length && !loading" class="editorial-narrative">暂无趋势数据</div>
    <div v-loading="loading" class="chart-wrapper" ref="chartRef"></div>
  </div>
</template>
```

- [ ] **Step 2: Add narrativeText computed in script**

Add after the `props` block:

```js
const narrativeText = computed(() => {
  if (!props.series.length) return ''
  if (props.dataType === 'SST') {
    const vals = props.series[0]?.dataPoints?.map(d => d.value) || []
    if (!vals.length) return ''
    const avg = (vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(1)
    const trend = vals[vals.length - 1] > vals[0] ? '上升' : '下降'
    return `过去 ${vals.length} 天东海海域海表温度呈${trend}趋势，平均温度 ${avg}°C。`
  }
  const vals = props.series[0]?.dataPoints?.map(d => d.value) || []
  if (!vals.length) return ''
  const avg = (vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(1)
  return `近海叶绿素浓度维持正常水平，平均 ${avg} mg/m³，无异常藻华预警信号。`
})
```

Import `computed` if not already: add to the existing import from `'vue'`.

- [ ] **Step 3: Replace style block**

```css
<style scoped>
.chart-wrapper {
  width: 100%;
  height: 280px;
}
</style>
```

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/dashboard/TrendCard.vue
git commit -m "feat: restyle TrendCard with editorial narrative and no el-card"
```

---

### Task 9: Restyle DashboardMap — Remove el-card Wrapper

**Files:**
- Modify: `ocean-web/src/views/dashboard/DashboardMap.vue`

- [ ] **Step 1: Replace el-card with plain div**

Replace the entire template:

```vue
<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px;">
      <h3 class="editorial-section-heading" style="margin: 0;">数据覆盖分布</h3>
      <el-radio-group :model-value="activeType" size="small" @change="$emit('typeChange', $event)">
        <el-radio-button value="SST">SST</el-radio-button>
        <el-radio-button value="CHL">CHL</el-radio-button>
      </el-radio-group>
    </div>
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
  </div>
</template>
```

Replace style block:

```css
<style scoped>
/* uses editorial-section-heading from editorial.css */
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/dashboard/DashboardMap.vue
git commit -m "feat: remove el-card wrapper from DashboardMap"
```

---

### Task 10: Restyle AlertPanel — Remove el-card

**Files:**
- Modify: `ocean-web/src/views/dashboard/AlertPanel.vue`

- [ ] **Step 1: Replace template — remove el-card, use plain div**

```vue
<template>
  <div class="editorial-section">
    <p class="editorial-section-label">Alerts</p>
    <h3 class="editorial-section-heading">阈值告警</h3>
    <div v-if="!alerts.length && !loading" style="min-height: 200px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: var(--color-text-muted); font-size: 13px;">
      今日无阈值告警
    </div>
    <div v-loading="loading">
      <div
        v-for="(item, idx) in alerts.slice(0, 10)"
        :key="idx"
        class="alert-item"
        :style="{ borderLeftColor: item.value > (item.dataType === 'SST' ? 30 : 10) ? '#c0392b' : '#fa8c16' }"
      >
        <div style="font-size: 13px; font-weight: 600; color: var(--color-text);">{{ item.locationName }}</div>
        <div style="display: flex; align-items: center; gap: 8px; font-size: 12px; color: #666; margin-top: 4px;">
          <span class="editorial-tag" style="font-size: 10px;">{{ item.dataType }}</span>
          <span style="font-weight: 600; color: var(--color-alert);">{{ item.value }}{{ item.dataType === 'SST' ? '°C' : ' mg/m³' }}</span>
          <span style="color: var(--color-text-muted);">阈值 {{ item.threshold }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Replace style block**

```css
<style scoped>
.alert-item {
  padding: 10px 12px;
  margin-bottom: 8px;
  border-left: 3px solid;
}
</style>
```

- [ ] **Step 3: Remove unused CircleCheck import**

Remove `import { CircleCheck } from '@element-plus/icons-vue'`.

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/dashboard/AlertPanel.vue
git commit -m "feat: restyle AlertPanel without el-card"
```

---

### Task 11: Restyle LatestDataTable — Native Table

**Files:**
- Modify: `ocean-web/src/views/dashboard/LatestDataTable.vue`

- [ ] **Step 1: Replace template with native table**

```vue
<template>
  <div>
    <p class="editorial-section-label">数据附录</p>
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

- [ ] **Step 2: Replace style block**

```css
<style scoped>
/* uses editorial-table from editorial.css */
</style>
```

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/views/dashboard/LatestDataTable.vue
git commit -m "feat: replace el-table with native editorial table in LatestDataTable"
```

---

### Task 12: Restyle DashboardView — Section Layout

**Files:**
- Modify: `ocean-web/src/views/dashboard/DashboardView.vue`

- [ ] **Step 1: Replace template with editorial section layout**

```vue
<template>
  <div>
    <h1 class="editorial-page-title">系统仪表盘</h1>
    <p class="editorial-page-subtitle">System Dashboard · {{ todayStr }}</p>

    <StatCards
      :modelCount="data.modelCount"
      :runningModelCount="data.runningModelCount"
      :todayRecordCount="data.todayRecordCount"
      :alertCount="data.alertCount"
    />

    <div style="display: flex; gap: 40px;">
      <div style="flex: 1;">
        <TrendCard
          title="海表温度 SST"
          dataType="SST"
          :series="sstTrend"
          :loading="loading.trendSst"
        />
      </div>
      <div style="flex: 1;">
        <TrendCard
          title="叶绿素浓度 CHL"
          dataType="CHL"
          :series="chlTrend"
          :loading="loading.trendChl"
        />
      </div>
    </div>

    <div class="editorial-section">
      <p class="editorial-section-label">Interactive</p>
      <h3 class="editorial-section-heading">预报栅格地图</h3>
      <div style="display: flex; gap: 40px;">
        <div style="flex: 2;">
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
        </div>
        <div style="flex: 1;">
          <AlertPanel :alerts="alerts" :loading="loading.alerts" />
        </div>
      </div>
    </div>

    <div style="display: flex; gap: 40px;">
      <div style="flex: 1;">
        <LatestDataTable
          title="最新海表温度 (SST)"
          dataType="SST"
          :data="data.latestSstData"
          :loading="loading.dashboard"
        />
      </div>
      <div style="flex: 1;">
        <LatestDataTable
          title="最新叶绿素浓度 (CHL)"
          dataType="CHL"
          :data="data.latestChlData"
          :loading="loading.dashboard"
        />
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Add todayStr computed in script**

```js
const todayStr = computed(() => {
  const d = new Date()
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
  return `${months[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`
})
```

Add `computed` to the `vue` import.

- [ ] **Step 3: Remove old style block**

Replace with empty or minimal styles.

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/dashboard/DashboardView.vue
git commit -m "feat: restyle DashboardView with editorial section layout"
```

---

### Task 13: Restyle UserView — Native Table + Editorial Form

**Files:**
- Modify: `ocean-web/src/views/user/UserView.vue`

- [ ] **Step 1: Replace template**

```vue
<template>
  <div>
    <h1 class="editorial-page-title">用户管理</h1>
    <p class="editorial-page-subtitle">User Management · 共 {{ total }} 条记录</p>

    <!-- Filter bar -->
    <div class="editorial-filter-bar">
      <input v-model="query.username" class="editorial-search" placeholder="用户名" style="width: 160px;" />
      <select v-model="query.role" class="editorial-select" style="width: 140px;">
        <option value="">全部角色</option>
        <option value="ADMIN">管理员</option>
        <option value="USER">普通用户</option>
      </select>
      <select v-model="query.status" class="editorial-select" style="width: 120px;">
        <option :value="null">全部状态</option>
        <option :value="1">启用</option>
        <option :value="0">禁用</option>
      </select>
      <button class="editorial-btn-outline" @click="handleSearch">查询</button>
      <button class="editorial-btn-outline" @click="handleReset">重置</button>
      <span style="flex: 1;"></span>
      <button class="editorial-btn-outline" @click="handleAdd">+ 新增用户</button>
    </div>

    <!-- Table -->
    <table class="editorial-table" v-loading="loading">
      <thead>
        <tr>
          <td>用户名</td>
          <td>真实姓名</td>
          <td>角色</td>
          <td>状态</td>
          <td>创建时间</td>
          <td style="text-align: right;">操作</td>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in tableData" :key="row.id">
          <td>{{ row.username }}</td>
          <td>{{ row.realName }}</td>
          <td><span class="editorial-tag">{{ row.role === 'ADMIN' ? 'ADMIN' : 'USER' }}</span></td>
          <td>{{ row.status === 1 ? '启用' : '禁用' }}</td>
          <td class="text-muted">{{ row.createTime }}</td>
          <td style="text-align: right;">
            <a class="editorial-link" @click="handleEdit(row)">编辑</a>
            <a class="editorial-link editorial-link--muted" style="margin-left: 12px;" @click="handleDelete(row)">删除</a>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Pagination -->
    <div class="editorial-pagination">
      <span>共 {{ total }} 条</span>
      <select v-model="query.pageSize" class="editorial-select" style="width: 80px;" @change="loadData">
        <option :value="10">10</option>
        <option :value="20">20</option>
        <option :value="50">50</option>
      </select>
      <a class="editorial-link" @click="prevPage">&larr;</a>
      <span class="editorial-pagination__page editorial-pagination__page--active">{{ query.pageNum }}</span>
      <a class="editorial-link" @click="nextPage">&rarr;</a>
    </div>

    <!-- Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="480px"
      :close-on-click-modal="false"
    >
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">用户名</label>
        <input v-model="form.username" class="editorial-input" placeholder="请输入用户名" />
      </div>
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">密码</label>
        <input
          v-model="form.password"
          class="editorial-input"
          type="password"
          :placeholder="isEdit ? '留空则不修改密码' : '请输入密码'"
        />
      </div>
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">真实姓名</label>
        <input v-model="form.realName" class="editorial-input" placeholder="请输入真实姓名" />
      </div>
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">角色</label>
        <select v-model="form.role" class="editorial-select">
          <option value="ADMIN">管理员</option>
          <option value="USER">普通用户</option>
        </select>
      </div>
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">状态</label>
        <select v-model="form.status" class="editorial-select">
          <option :value="1">启用</option>
          <option :value="0">禁用</option>
        </select>
      </div>
      <template #footer>
        <button class="editorial-btn-outline" @click="dialogVisible = false">取消</button>
        <button class="editorial-btn" style="padding: 8px 24px; margin-left: 12px;" :disabled="submitLoading" @click="handleSubmit">
          {{ submitLoading ? '...' : '确定' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>
```

- [ ] **Step 2: Add pagination methods to script**

```js
function prevPage() {
  if (query.pageNum > 1) {
    query.pageNum--
    loadData()
  }
}
function nextPage() {
  query.pageNum++
  loadData()
}
```

Remove `formRef` and `rules` references. Replace `handleSubmit`:

```js
async function handleSubmit() {
  if (!form.username || !form.realName) {
    ElMessage.warning('请填写用户名和真实姓名')
    return
  }
  submitLoading.value = true
  try {
    const data = { ...form }
    if (isEdit.value) {
      if (!data.password) delete data.password
      await updateUser(editId.value, data)
      ElMessage.success('用户更新成功')
    } else {
      if (!data.password) {
        ElMessage.warning('请输入密码')
        submitLoading.value = false
        return
      }
      await addUser(data)
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}
```

- [ ] **Step 3: Remove old style block, keep minimal**

```css
<style scoped>
/* uses editorial classes from editorial.css */
</style>
```

Remove unused imports (`el-form`, `el-form-item`, etc.) — the script imports are fine as-is since Element Plus components are globally registered.

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/user/UserView.vue
git commit -m "feat: restyle UserView with native editorial table and form"
```

---

### Task 14: Restyle ModelView — Native Table + Editorial Form

**Files:**
- Modify: `ocean-web/src/views/model/ModelView.vue`

- [ ] **Step 1: Replace template with editorial native table**

```vue
<template>
  <div>
    <h1 class="editorial-page-title">预报模型管理</h1>
    <p class="editorial-page-subtitle">Model Management · 共 {{ total }} 条记录</p>

    <div class="editorial-filter-bar">
      <select v-model="query.modelType" class="editorial-select" style="width: 160px;">
        <option value="">全部类型</option>
        <option value="SST">海表温度 (SST)</option>
        <option value="CHL">叶绿素浓度 (CHL)</option>
      </select>
      <input v-model="query.keyword" class="editorial-search" placeholder="模型名称" style="width: 180px;" />
      <button class="editorial-btn-outline" @click="handleSearch">查询</button>
      <button class="editorial-btn-outline" @click="handleReset">重置</button>
      <span style="flex: 1;"></span>
      <button class="editorial-btn-outline" @click="handleAdd">+ 新增模型</button>
    </div>

    <table class="editorial-table" v-loading="loading">
      <thead>
        <tr>
          <td>模型名称</td>
          <td>类型</td>
          <td>运行周期</td>
          <td>状态</td>
          <td>最近运行</td>
          <td style="text-align: right;">操作</td>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in tableData" :key="row.id">
          <td>{{ row.modelName }}</td>
          <td><span class="editorial-tag">{{ row.modelType === 'SST' ? 'SST' : 'CHL' }}</span></td>
          <td>{{ row.cronExpression }}</td>
          <td>{{ statusMap[row.status] }}</td>
          <td class="text-muted">{{ row.lastRunTime || '-' }}</td>
          <td style="text-align: right;">
            <a v-if="row.status !== 'RUNNING'" class="editorial-link" @click="handleToggle(row, 'RUNNING')">启动</a>
            <a v-else class="editorial-link" @click="handleToggle(row, 'STOPPED')">停止</a>
            <a class="editorial-link" style="margin-left: 12px;" @click="handleEdit(row)">编辑</a>
            <a class="editorial-link editorial-link--muted" style="margin-left: 12px;" @click="handleDelete(row)">删除</a>
          </td>
        </tr>
      </tbody>
    </table>

    <div class="editorial-pagination">
      <span>共 {{ total }} 条</span>
      <select v-model="query.pageSize" class="editorial-select" style="width: 80px;" @change="loadData">
        <option :value="10">10</option>
        <option :value="20">20</option>
        <option :value="50">50</option>
      </select>
      <a class="editorial-link" @click="prevPage">&larr;</a>
      <span class="editorial-pagination__page editorial-pagination__page--active">{{ query.pageNum }}</span>
      <a class="editorial-link" @click="nextPage">&rarr;</a>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :close-on-click-modal="false">
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">模型名称</label>
        <input v-model="form.modelName" class="editorial-input" placeholder="请输入模型名称" />
      </div>
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">模型类型</label>
        <select v-model="form.modelType" class="editorial-select">
          <option value="SST">海表温度 (SST)</option>
          <option value="CHL">叶绿素浓度 (CHL)</option>
        </select>
      </div>
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">运行周期 (Cron)</label>
        <input v-model="form.cronExpression" class="editorial-input" placeholder="如: 0 0 6 * * ?" />
      </div>
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">参数配置 (JSON)</label>
        <textarea v-model="form.paramsConfig" class="editorial-input" rows="3" placeholder='{"algorithm":"ROMS"}' style="resize: vertical;"></textarea>
      </div>
      <div style="margin-bottom: 18px;">
        <label class="editorial-form-label">模型描述</label>
        <input v-model="form.description" class="editorial-input" placeholder="请输入模型描述" />
      </div>
      <template #footer>
        <button class="editorial-btn-outline" @click="dialogVisible = false">取消</button>
        <button class="editorial-btn" style="padding: 8px 24px; margin-left: 12px;" :disabled="submitLoading" @click="handleSubmit">确定</button>
      </template>
    </el-dialog>
  </div>
</template>
```

- [ ] **Step 2: Add pagination methods and simplify handleSubmit**

```js
function prevPage() {
  if (query.pageNum > 1) { query.pageNum--; loadData() }
}
function nextPage() {
  query.pageNum++; loadData()
}
```

Replace `handleSubmit`:

```js
async function handleSubmit() {
  if (!form.modelName) { ElMessage.warning('请输入模型名称'); return }
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateModel(editId.value, { ...form })
      ElMessage.success('模型更新成功')
    } else {
      await addModel({ ...form })
      ElMessage.success('模型创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally { submitLoading.value = false }
}
```

Remove `formRef` and `rules` from script (keep the reactive form as-is, just remove formRef/validation rules references).

- [ ] **Step 3: Replace style block**

```css
<style scoped>
/* uses editorial classes from editorial.css */
</style>
```

- [ ] **Step 4: Commit**

---

### Task 15: Restyle OceanDataView — Remove Card Wrappers

**Files:**
- Modify: `ocean-web/src/views/ocean/OceanDataView.vue`

- [ ] **Step 1: Replace the entire template**

```vue
<template>
  <div>
    <h1 class="editorial-page-title">海洋观测数据</h1>
    <p class="editorial-page-subtitle">Ocean Observation Data</p>

    <!-- Chl time series chart -->
    <div class="editorial-section">
      <p class="editorial-section-label">Feature · 时间序列</p>
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
        <h3 class="editorial-section-heading" style="margin: 0;">叶绿素浓度时间序列</h3>
        <div style="display: flex; align-items: center; gap: 8px;">
          <el-select
            v-model="chlLocations"
            placeholder="筛选观测点"
            multiple collapse-tags collapse-tags-tooltip filterable
            size="small" style="width: 280px"
            @change="renderChlTimeSeries"
          >
            <el-option v-for="loc in locationOptions" :key="loc.key" :label="loc.label" :value="loc.key" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange" range-separator="至"
            start-placeholder="开始" end-placeholder="结束"
            value-format="YYYY-MM-DD" size="small" style="width: 260px"
            @change="onDateRangeChange"
          />
          <el-button size="small" text @click="openFullscreen">
            <el-icon><FullScreen /></el-icon>
          </el-button>
        </div>
      </div>
      <div v-loading="chartLoading" class="chart-container" ref="timeSeriesChartRef">
        <div v-if="chartEmpty" style="display: flex; align-items: center; justify-content: center; height: 100%; color: var(--color-text-muted); font-size: 13px;">暂无符合条件的观测数据</div>
      </div>
    </div>

    <!-- Fullscreen modal unchanged -->
    <el-dialog v-model="fullscreenVisible" title="叶绿素浓度时间序列" fullscreen :close-on-click-modal="false" @opened="onFullscreenOpened" @close="onFullscreenClosed">
      <div ref="fullscreenChartRef" style="height: calc(100vh - 100px);"></div>
    </el-dialog>

    <!-- Data table -->
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
      <h3 class="editorial-section-heading" style="margin: 0;">观测数据记录</h3>
      <button class="editorial-btn-outline" @click="loadTableData">刷新</button>
    </div>
    <table class="editorial-table" v-loading="tableLoading">
      <thead>
        <tr>
          <td>日期</td><td>纬度</td><td>经度</td><td>深度(m)</td><td>叶绿素</td>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, idx) in tableData" :key="idx">
          <td>{{ row.time }}</td>
          <td>{{ row.lat }}</td>
          <td>{{ row.lon }}</td>
          <td>{{ row.depth }}</td>
          <td>{{ row.chl }}</td>
        </tr>
      </tbody>
    </table>
    <div class="editorial-pagination">
      <span>共 {{ tableTotal }} 条</span>
      <select v-model="tableQuery.pageSize" class="editorial-select" style="width: 80px;" @change="loadTableData">
        <option :value="10">10</option>
        <option :value="20">20</option>
        <option :value="50">50</option>
      </select>
      <a class="editorial-link" @click="tableQuery.pageNum--; loadTableData()">&larr;</a>
      <span class="editorial-pagination__page editorial-pagination__page--active">{{ tableQuery.pageNum }}</span>
      <a class="editorial-link" @click="tableQuery.pageNum++; loadTableData()">&rarr;</a>
    </div>
  </div>
</template>
```

Note: `el-select`, `el-date-picker`, `el-dialog`, `el-button` from Element Plus are kept for the complex chart filter controls and fullscreen dialog since native alternatives would require significant rework.

- [ ] **Step 2: Replace style block**

```css
<style scoped>
.chart-container {
  width: 100%;
  height: 400px;
}
</style>
```

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/views/ocean/OceanDataView.vue
git commit -m "feat: restyle OceanDataView with editorial sections"
```

---

### Task 16: Restyle Forecast Pages — Remove Card Wrappers

**Files:**
- Modify: `ocean-web/src/views/forecast/SstMapView.vue`
- Modify: `ocean-web/src/views/forecast/ChxMapView.vue`
- Modify: `ocean-web/src/views/forecast/HistoryView.vue`

- [ ] **Step 1: SstMapView — remove all three el-card wrappers**

Replace template:

```vue
<template>
  <div>
    <h1 class="editorial-page-title">海表温度预测</h1>
    <p class="editorial-page-subtitle">Sea Surface Temperature Forecast</p>

    <!-- Filter bar -->
    <div class="editorial-section" style="padding-bottom: 20px; margin-bottom: 20px;">
      <div class="editorial-filter-bar">
        <span class="editorial-form-label" style="margin: 0 8px 0 0;">数据筛选</span>
        <el-date-picker v-model="filterDate" type="date" placeholder="选择预报日期" value-format="YYYY-MM-DD" style="width: 180px" />
        <el-select v-model="seaArea" placeholder="海域筛选" style="width: 160px" @change="onSeaAreaChange">
          <el-option v-for="area in seaAreas" :key="area.name" :label="area.name" :value="area" />
        </el-select>
        <button class="editorial-btn-outline" @click="handleSearch">查询</button>
        <button class="editorial-btn-outline" @click="handleReset">重置</button>
        <span style="font-size: 12px; color: var(--color-text-muted); margin-left: auto;">也可在地图上拖拽框选海域</span>
      </div>
    </div>

    <!-- Map -->
    <div class="editorial-section">
      <p class="editorial-section-label">Interactive</p>
      <h3 class="editorial-section-heading">预报栅格地图</h3>
      <OceanMap
        :grid-data="gridData"
        :color-ranges="SST_MAP_COLORS"
        :legend-labels="legendLabels"
        legend-title="温度 (°C)"
        :loading="mapLoading"
        @cell-click="onMapCellClick"
        @bbox-change="onBboxChange"
      />
    </div>

    <!-- Trend -->
    <div class="editorial-section" style="border-bottom: none;">
      <p class="editorial-section-label">Feature · 趋势分析</p>
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;">
        <h3 class="editorial-section-heading" style="margin: 0;">温度变化趋势</h3>
        <span v-if="selectedPoint" style="font-size: 13px; color: var(--color-text-muted);">
          当前选中: ({{ selectedPoint.lon.toFixed(2) }}, {{ selectedPoint.lat.toFixed(2) }})
        </span>
      </div>
      <TrendChart
        :series-data="trendSeries"
        :x-axis-data="trendDates"
        y-axis-name="温度 (°C)"
        y-axis-unit="°C"
        :loading="trendLoading"
        :colors="SST_COLORS"
      />
    </div>
  </div>
</template>
```

Replace style block:

```css
<style scoped>
/* uses editorial classes from editorial.css */
</style>
```

- [ ] **Step 2: ChxMapView — same pattern**

Replace `el-card` wrappers with same editorial section structure as SstMapView. Keep the `el-radio-group`, `el-date-picker`, `el-input-number`, `el-select` Element Plus components since they have complex behavior (date pickers, number inputs, radio button groups). Only the container wrappers change.

```vue
<template>
  <div>
    <h1 class="editorial-page-title">叶绿素预测</h1>
    <p class="editorial-page-subtitle">Chlorophyll Concentration Forecast</p>

    <div class="editorial-section" style="padding-bottom: 20px; margin-bottom: 20px;">
      <div class="editorial-filter-bar">
        <span class="editorial-form-label" style="margin: 0 8px 0 0;">数据筛选</span>
        <el-radio-group v-model="chlMode" @change="onModeChange" size="small">
          <el-radio-button value="concentration">浓度值</el-radio-button>
          <el-radio-button value="probability">超阈值概率</el-radio-button>
        </el-radio-group>
        <el-date-picker v-if="chlMode === 'concentration'" v-model="filterDate" type="date" placeholder="选择预报日期" value-format="YYYY-MM-DD" style="width: 180px" />
        <template v-if="chlMode === 'probability'">
          <el-input-number v-model="probDays" :min="1" :max="90" style="width: 140px" />
          <span style="color: var(--color-text-secondary); font-size: 13px;">天</span>
          <el-input-number v-model="threshold" :min="0.1" :step="0.5" :precision="1" style="width: 140px" />
          <span style="color: var(--color-text-secondary); font-size: 13px;">阈值 mg/m³</span>
        </template>
        <el-select v-model="seaArea" placeholder="海域筛选" style="width: 160px" @change="onSeaAreaChange">
          <el-option v-for="area in seaAreas" :key="area.name" :label="area.name" :value="area" />
        </el-select>
        <button class="editorial-btn-outline" @click="handleSearch">查询</button>
        <button class="editorial-btn-outline" @click="handleReset">重置</button>
      </div>
    </div>

    <div class="editorial-section">
      <p class="editorial-section-label">Interactive</p>
      <h3 class="editorial-section-heading">预报栅格地图</h3>
      <OceanMap ... />
    </div>

    <div class="editorial-section" style="border-bottom: none;">
      <p class="editorial-section-label">Feature · 趋势分析</p>
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;">
        <h3 class="editorial-section-heading" style="margin: 0;">{{ chlMode === 'concentration' ? '叶绿素浓度变化趋势' : '趋势' }}</h3>
        <span v-if="selectedPoint" style="font-size: 13px; color: var(--color-text-muted);">选中: ({{ selectedPoint.lon.toFixed(2) }}, {{ selectedPoint.lat.toFixed(2) }})</span>
      </div>
      <TrendChart ... />
    </div>
  </div>
</template>
```

Replace style block with empty scoped style (all classes from editorial.css).

- [ ] **Step 3: HistoryView — remove el-card wrapper, convert to native table**

```vue
<template>
  <div>
    <h1 class="editorial-page-title">历史预报记录</h1>
    <p class="editorial-page-subtitle">Forecast History · 共 {{ tableTotal }} 条记录</p>

    <div class="editorial-filter-bar">
      <select v-model="tableQuery.dataType" class="editorial-select" style="width: 150px;">
        <option value="">全部类型</option>
        <option value="SST">海表温度</option>
        <option value="CHL">叶绿素浓度</option>
      </select>
      <el-date-picker
        v-model="dateRange"
        type="daterange" range-separator="至"
        start-placeholder="开始日期" end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 280px"
      />
      <button class="editorial-btn-outline" @click="handleSearch">查询</button>
      <button class="editorial-btn-outline" @click="handleReset">重置</button>
    </div>

    <table class="editorial-table" v-loading="tableLoading">
      <thead>
        <tr>
          <td>模型名称</td><td>数据类型</td><td>预报日期</td><td>数值</td><td>单位</td><td>经度</td><td>纬度</td><td>创建时间</td>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in tableData" :key="row.id">
          <td>{{ row.modelName }}</td>
          <td><span class="editorial-tag">{{ row.dataType }}</span></td>
          <td>{{ row.forecastDate }}</td>
          <td>{{ row.value }}</td>
          <td>{{ row.unit }}</td>
          <td>{{ row.longitude }}</td>
          <td>{{ row.latitude }}</td>
          <td class="text-muted">{{ row.createTime }}</td>
        </tr>
      </tbody>
    </table>

    <div class="editorial-pagination">
      <span>共 {{ tableTotal }} 条</span>
      <select v-model="tableQuery.pageSize" class="editorial-select" style="width: 80px;" @change="loadTableData">
        <option :value="10">10</option>
        <option :value="20">20</option>
        <option :value="50">50</option>
      </select>
      <a class="editorial-link" @click="tableQuery.pageNum--; loadTableData()">&larr;</a>
      <span class="editorial-pagination__page editorial-pagination__page--active">{{ tableQuery.pageNum }}</span>
      <a class="editorial-link" @click="tableQuery.pageNum++; loadTableData()">&rarr;</a>
    </div>
  </div>
</template>
```

Replace style block with empty.

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/forecast/SstMapView.vue ocean-web/src/views/forecast/ChxMapView.vue ocean-web/src/views/forecast/HistoryView.vue
git commit -m "feat: remove el-card wrappers from forecast pages"
```

---

### Task 17: Final Verification

- [ ] **Step 1: Build the frontend**

Run: `cd ocean-web && npm run build`
Expected: Build succeeds with no errors.

- [ ] **Step 2: Dev server smoke test**

Run: `cd ocean-web && npm run dev`
Expected: Dev server starts, navigate to login page, verify editorial styling renders.

- [ ] **Step 3: Check all pages render without console errors**

Navigate through: login → dashboard → user management → model management → ocean data → SST map → CHL map → history.

- [ ] **Step 4: Commit any remaining cleanup**

```bash
git add -A
git commit -m "chore: final editorial redesign cleanup"
```
