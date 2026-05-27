# OceanMap 暗色模式适配

**Status:** Approved  
**Date:** 2026-05-26  
**Scope:** `OceanMap.vue` only（HealthAlertSection.vue 的地图后续单独处理）

## 背景

暗色模式已通过 CSS 变量 + `useTheme.js` 实现，覆盖 Element Plus、ECharts、全局样式。原始暗色模式 spec（`2026-05-25-dark-mode-design.md`）明确将 Leaflet 地图排除在外。本次补齐 OceanMap.vue 的暗色适配。

OceanMap 不使用栅格瓦片，地图完全由本地 GeoJSON（陆地、国界）+ Canvas 热力图叠加渲染。所有样式均为硬编码浅色主题颜色。

## 配色方案

### 地图图层

| 元素 | 浅色 | 暗色 |
|------|------|------|
| 海洋背景 | `#a8d8ea` | `#111820` |
| 陆地填充 | `#f5f0e8` | `#1c2128` |
| 陆地边框 | `#c8c0b0` | `#2a3040` |
| 国界线 | `#b8a88a` | `#3a4050` |
| 网格线 | `rgba(120,160,180,0.25)` | `rgba(140,170,200,0.12)` |
| 国家标签 | `#5d5348` | `#8b949e` |
| 城市标记 fill | `#e87d5c` | `#f0883e` |
| 城市标记 stroke | `#fff` | `#1c2128` |
| 城市标签 | `#555` | `#c0c8d0` |
| 海域标签 | `#4a8faa` | `#58a6ff` |
| 选中点 fill | `#1a3a5c` | `#58a6ff` |
| 选中点 stroke | `#fff` | `#e6edf3` |

### 热力图色标

在 `chart-config.js` 中新增三组暗色色标常量，与浅色色标一一对应：

**SST_MAP_COLORS_DARK**
```
<10:  #1a2d4a   13: #2a5090   16: #4d8cc0   19: #7ab8d8   22: #d4a84b
25:   #d4853a   28: #c9553a   31: #b8302a   34: #8a1a22   >34: #551018
```

**CHL_CONC_COLORS_DARK**
```
<0.5: #0d4a3a   1.5: #1a7d60   3.0: #26a885   5.0: #3dc06a   >5.0: #5ee080
```

**CHL_PROB_COLORS_DARK**
```
<20:  #1a7e4a   40: #c9a018   60: #d48018   80: #c46520   >80: #c0392b
```

### Leaflet 控件

缩放控件和绘制工具栏保持默认样式，不做暗色适配。

## 实现方案

**方案 A：OceanMap 内部感知主题（选中）**

### 文件改动清单

| 文件 | 改动 |
|------|------|
| `utils/chart-config.js` | 新增 3 个暗色色标常量 |
| `components/OceanMap.vue` | 引入 `useTheme()`，图层样式函数化，watch 切换，标签 CSS 变量化 |
| `views/forecast/SstMapView.vue` | 根据 `isDark` 选择色标 |
| `views/forecast/ChxMapView.vue` | 根据 `isDark` 选择色标（浓度+概率两套） |
| `views/observation/ObsSstView.vue` | 根据 `isDark` 选择色标 |
| `views/observation/ObsChlView.vue` | 根据 `isDark` 选择色标 |
| `styles/editorial.css` | 新增地图相关 CSS 变量 |

### OceanMap.vue 改动细节

1. **主题感知** — `import { useTheme }`，`const isDark = computed(() => theme.resolved.value === 'dark')`
2. **图层样式函数化** — `getLandStyle(isDark)`、`getCountryStyle(isDark)`、`getGridStyle(isDark)`、`getCityStyle(isDark)` 返回对应的样式对象
3. **图层引用存储** — `initBaseLayers()` 中将 GeoJSON 图层和 CircleMarker 存为组件变量
4. **watch isDark** — 调用各图层的 `.setStyle()` 更新样式，无需销毁重建
5. **容器背景** — scoped CSS 使用新 CSS 变量 `--map-ocean-bg`
6. **选中点标记** — 在 watch 中同步更新 `setStyle()`

### 标签颜色：CSS 变量自动切换

Label 的 DivIcon 使用 CSS class（`.country-label`、`.city-label`、`.sea-label`），在 `editorial.css` 中定义 CSS 变量，`:root` 和 `[data-theme="dark"]` 分别设置值。主题切换时标签颜色自动跟随，无需 JS 干预。

新增 CSS 变量：
```css
:root {
  --map-ocean-bg: #a8d8ea;
  --map-label-country: #5d5348;
  --map-label-country-shadow: rgba(245,240,232,0.8);
  --map-label-city: #555;
  --map-label-city-shadow: rgba(255,255,255,0.9);
  --map-label-sea: #4a8faa;
  --map-label-sea-shadow: rgba(168,216,234,0.7);
}

[data-theme="dark"] {
  --map-ocean-bg: #111820;
  --map-label-country: #8b949e;
  --map-label-country-shadow: rgba(28,33,40,0.8);
  --map-label-city: #c0c8d0;
  --map-label-city-shadow: rgba(17,24,32,0.9);
  --map-label-sea: #58a6ff;
  --map-label-sea-shadow: rgba(17,24,32,0.7);
}
```

### 父组件改动模式（4 个 View 相同）

```js
import { useTheme } from '@/composables/useTheme'
import { SST_MAP_COLORS, SST_MAP_COLORS_DARK } from '@/utils/chart-config'

const { resolved } = useTheme()
const isDark = computed(() => resolved.value === 'dark')
const mapColors = computed(() => isDark.value ? SST_MAP_COLORS_DARK : SST_MAP_COLORS)
```

模板中 `:color-ranges="mapColors"`。

## 不改动

- Leaflet 缩放/绘制控件保持默认样式
- 热力图 Canvas 渲染逻辑不变（仅消费不同色标）
- `HealthAlertSection.vue` 的地图（后续单独处理）
- `legendLabels`、`legendTitle` 等 prop（已使用 CSS 变量，自动适配）
