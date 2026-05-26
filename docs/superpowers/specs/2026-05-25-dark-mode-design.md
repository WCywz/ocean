# 暗色模式 设计方案

> **Target:** 主应用 (`/app/*`) 暗色模式支持，GitHub 风格配色（`#0d1117` 背景）

**Status:** approved | **Tech:** Vue 3, CSS Variables, Element Plus, ECharts

---

## 需求摘要

- 主应用支持亮/暗色切换，默认跟随系统 `prefers-color-scheme`
- 用户可手动选择 跟随系统 / 浅色 / 深色（类似 GitHub）
- 登录用户偏好存后端 `user_setting` 表，未登录/游客用 `localStorage`
- GitHub 风格暗色（`#0d1117` 背景、`#e6edf3` 正文、`#8b949e` 弱化文字）
- Element Plus 组件适配暗色（官方暗色变量 + 少量 editorial 风格覆盖）
- ECharts 图表适配暗色（注册暗色主题配置）
- Leaflet 地图、Landing 页保持不变

---

## 文件变更

```
新建:
  ocean-web/src/composables/useTheme.js         — 主题状态管理 composable

修改:
  ocean-web/src/styles/editorial.css            — 新增 [data-theme="dark"] 块 + 硬编码色改为变量
  ocean-web/src/App.vue                         — onMounted 调用 useTheme().init()
  ocean-web/src/main.js                         — 引入 Element Plus 暗色 CSS
  ocean-web/src/layout/MainLayout.vue           — 导航栏加主题切换按钮
  ocean-web/src/components/TrendChart.vue       — ECharts 初始化时传入暗色主题
  ocean-web/src/views/profile/ProfileView.vue   — 暗色下 Element Plus 开关/弹窗适配（如有需要）
```

---

## 设计细节

### 1. CSS 变量暗色映射

```css
[data-theme="dark"] {
  --color-bg: #0d1117;
  --color-surface: #161b22;
  --color-text: #e6edf3;
  --color-text-secondary: #6e7681;
  --color-text-muted: #8b949e;
  --color-divider: #21262d;
  --color-divider-strong: #30363d;
  --color-border: #30363d;
  --color-border-light: #21262d;
  --color-alert: #f85149;
}
```

### 2. editorial.css 硬编码色修复

现有 5 处硬编码色改为引用 CSS 变量：
- `.editorial-table tbody td` `#f5f5f5` → `var(--color-divider)`
- `.editorial-input` `#e0e0e0` → `var(--color-border)`
- `.editorial-btn` `color: #fff` → `color: var(--color-bg)` — 亮色下背景深文字白，暗色下反转（背景`#e6edf3`文字`#0d1117`），对比度正确
- `.editorial-link--muted` `#ddd` → `var(--color-divider-strong)`
- `.editorial-input:focus` `var(--color-text)` 正确（自动跟随暗色变量）

### 3. useTheme composable

```js
// composables/useTheme.js
// 状态: themeMode ('system' | 'light' | 'dark') + resolvedTheme ('light' | 'dark')
// init(): 读取偏好 → 解析实际主题 → 设置 data-theme → 注册 ECharts 主题
// setMode(mode): 切换模式 → 写 localStorage → 调后端 API → 更新 DOM
// 导出单例（模块级 reactive state）
```

优先级：后端 `user_setting.theme` > `localStorage` > `prefers-color-scheme`

后端存储：`user_setting` 表中 `setting_key = 'theme'`，值为 `'light'` / `'dark'` / `'system'`，复用现有 `/api/profile/settings` API。

### 4. Element Plus 暗色

- `main.js` 中 `import 'element-plus/theme-chalk/dark/css-vars.css'`
- 在 `editorial.css` 的 `[data-theme="dark"]` 块中对 Element Plus 变量做 editorial 风格覆盖：
  - `--el-bg-color` → `var(--color-bg)`
  - `--el-border-color` → `var(--color-divider-strong)`
  - `--el-dialog-bg-color` → `var(--color-surface)`
  - 等

### 5. ECharts 图表

- 在 `useTheme.js` 中 `echarts.registerTheme('ocean-dark', {...})`，覆盖坐标轴、分割线、文字、tooltip 颜色
- `TrendChart.vue` 初始化时：`echarts.init(dom, resolvedTheme === 'dark' ? 'ocean-dark' : undefined)`
- 主题切换时监听 `resolvedTheme` 变化，调用 `chart.dispose()` + 重新 `init`

### 6. 切换按钮

MainLayout 导航栏右侧、头像左侧，按钮显示当前主题图标，点击展开下拉菜单：
- 跟随系统（显示 ✓ 如果当前选中）
- 浅色
- 深色

---

## 执行顺序

```
Task 1: CSS 变量 + 硬编码色修复 (editorial.css)
Task 2: useTheme composable (composables/useTheme.js)
Task 3: Element Plus 暗色引入 + 覆盖 (main.js + editorial.css)
Task 4: App.vue 初始化主题 (App.vue)
Task 5: ECharts 暗色主题注册 + TrendChart 适配
Task 6: MainLayout 切换按钮
Task 7: 端到端验证
```
