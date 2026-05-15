# Editorial Style UI Redesign — Design Spec

**Date:** 2026-05-06
**Status:** Approved
**Scope:** Full admin interface redesign (sidebar, topbar, cards, tables, forms, login page)

## Summary

Replace the current dark-sidebar Element Plus style with an editorial/magazine-inspired design: pure white background, serif headings, minimal navigation, and zero "SaaS template" tropes (no colored icon blocks, no equal-column card grids, no box shadows).

## Design Direction

**Editorial** — like a high-end magazine interior page. Typography is the primary visual element. Content is organized by section labels and thin dividing lines instead of cards. Glassmorphism is absent; depth comes from typographic hierarchy and whitespace, not frosted blur effects.

Reference sites: Dribbble, Ramotion (clean minimalism, not glassmorphism)

---

## Design Tokens

### Typography

| Role | Font | Size | Weight | Color |
|---|---|---|---|---|
| Page title | Georgia, serif | 28-32px | 400 | `#2c3e50` |
| Section heading | Georgia, serif | 18-20px | 400 | `#2c3e50` |
| Body text | PingFang SC / Microsoft YaHei | 12-14px | 400 | `#2c3e50` |
| Secondary text | PingFang SC / Microsoft YaHei | 12-13px | 400 | `#aaa` / `#bbb` |
| Stat numbers | Georgia, serif | 42px | 400 | `#2c3e50` |
| Section labels | PingFang SC | 10px | 400 | `#bbb`, uppercase, `letter-spacing: 0.1em` |
| Monospace accents | monospace | 10-11px | 400 | `#bbb` |

### Colors

| Token | Value | Usage |
|---|---|---|
| Page background | `#ffffff` | All pages |
| Secondary surface | `#fafafa` | Chart backgrounds, table header hover |
| Primary text | `#2c3e50` | Headings, body, active nav |
| Secondary text | `#aaa` / `#bbb` | Labels, descriptions, inactive nav |
| Divider | `#f0f0f0` / `#eee` | Section separators, table borders |
| Border (subtle) | `#e8e8e8` | Input borders, button borders |
| Alert red | `#c0392b` | Alert count only — no other color accents |

**No theme color, no blue, no purple.** The design is intentionally monochromatic with red reserved for alerts.

### Spacing

| Token | Value |
|---|---|
| Content horizontal padding | 40-48px |
| Content vertical padding | 36-40px |
| Section gap | 32-40px |
| Element gap | 12-16px |
| Section divider margin | 36px + `border-bottom: 1px solid #f0f0f0` |

### Shapes

- No border-radius on cards (there are no cards)
- No box-shadow
- No colored icon backgrounds
- Inputs: underline-style (`border-bottom` only)
- Buttons: no border-radius, monospace font
- Section dividers: `1px solid #f0f0f0`

---

## Layout: Navigation Bar

Top navigation bar replaces the left sidebar entirely.

```
海洋预报系统    仪表盘    预报    观测    模型    用户          OCEAN DEV
———————————————————————————————————————————————————————————————————————
```

- Brand name: Georgia serif, 16px, `#2c3e50`
- Nav items: 11px, uppercase, `letter-spacing: 0.06em`
  - Active: `color: #2c3e50`, `font-weight: 600`
  - Inactive: `color: #bbb`
- Username: monospace, 11px, `#bbb`, right-aligned
- Bottom border: `1px solid #eee`
- Height: 52px
- No background, no shadow
- No icons anywhere in the nav

**Forecast sub-navigation:** Replace the current `el-sub-menu` expandable item with a top-level "预报" nav item that reveals a dropdown on hover/click containing SST/CHL/History links. Alternatively, render a secondary nav row below the main nav bar when on any forecast page, showing the three sub-pages as inline links.

---

## Page: Login / Register

```
                   海洋环境预报系统
                   Ocean Forecast System

              ┌─────────────────────────┐
              │  用户名                  │
              │  ─────────────────────── │
              │  密码                    │
              │  ─────────────────────── │
              │                          │
              │  [    登  录    ]        │
              │                          │
              │  还没有账号？注册        │
              └─────────────────────────┘
```

- Form centered on page, 320px wide
- Title: Georgia serif, 28px
- Subtitle: Georgia serif italic, 13px, `#bbb`
- Inputs: no background, `border-bottom: 1px solid #e0e0e0`, padding 10px, focus bottom border darkens
- Submit button: `background: #2c3e50`, white text, monospace, `letter-spacing: 0.2em`, full width
- Register link: `#bbb`, hover → `#2c3e50`

---

## Page: Dashboard

```
系统仪表盘
System Dashboard · May 6, 2026

12    3    156    2
模型总数  运行中  今日预报  告警

─────────────────────────────────────

FEATURE · 趋势分析

海表温度 SST              叶绿素浓度 CHL
过去7天东海海域海表温      近海叶绿素浓度维持正常
度呈上升趋势...           水平，无异常藻华预警...
[ 趋势图 ]                [ 趋势图 ]

─────────────────────────────────────

INTERACTIVE
预报栅格地图
[ Leaflet 地图 ]

─────────────────────────────────────

数据附录
最新 SST                    最新 CHL
观测点   温度   日期          观测点   浓度   日期
Point A  24.5  05-06         Point A  3.2   05-06
```

**Key principles:**

1. **Stats are not cards.** Big serif numbers + gray labels, laid out horizontally, separated by a thin divider below.
2. **Sections separated by `border-bottom: 1px solid #f0f0f0`**, not by card wrappers.
3. **Each section has a label:** `FEATURE · 趋势分析`, `INTERACTIVE`, `数据附录` — 10px uppercase, `#bbb`.
4. **Trend charts** include a brief narrative description (12px, `#aaa`) before the chart.
5. **Map** sits alone in its own section, full width.
6. **Data tables** are minimal — no vertical borders, no stripe, no hover highlight. Only `border-top: 1px solid #f5f5f5` on rows.

---

## Page: Data Tables (User Management, Model Management, Ocean Data)

```
用户管理
User Management · 共 4 条记录

[+ 新增用户]                            [搜索框]

用户名      真实姓名    角色        创建时间        操作
zhangsan    张三       ADMIN      2026-05-03   编辑  删除
lisi        李四       USER       2026-05-01   编辑  删除
─────────────────────────────────────────────────────
                                    ←  1  2  →
```

**Table rules:**
- Header: 10px uppercase, `#bbb`, `letter-spacing: 0.08em`
- Rows: `border-bottom: 1px solid #f5f5f5`, no vertical borders, no zebra striping
- No hover background color on rows
- "Add" button: transparent background, `border: 1px solid #d0d0d0`, 12px text
- Action links: `#bbb` (edit), `#ddd` (delete), pure text, no buttons
- Pagination: minimal, current page underlined with `border-bottom: 2px solid #2c3e50`

---

## Page: Forms (Model Save/Edit, User Save/Edit)

- Form layout: stacked labels, not inline
- Labels: 11px uppercase, `#bbb`, above each input
- Inputs: underline-style, 14px, full width of form container (~480px)
- Selects: same underline-style where possible
- Submit button: same as login — `#2c3e50` solid, monospace, `letter-spacing: 0.2em`
- Cancel: plain text link, `#bbb`

---

## Component Mapping

| Current Element Plus Component | Replacement |
|---|---|
| `el-menu` / `el-sub-menu` | Plain div nav bar with CSS |
| `el-card` with `shadow="hover"` | No wrapper — content directly on page with dividers |
| `el-tag` | Plain text with monospace font (`#bbb` for tags) |
| `el-table` with `stripe` | Native `<table>` with minimal border styling |
| `el-button` (default/primary) | Custom button: transparent border or `#2c3e50` solid |
| `el-input` | Native `<input>` with underline style |
| `el-pagination` | Minimal custom pagination (plain text links) |

## Files to Modify

1. `ocean-web/src/layout/MainLayout.vue` — replace sidebar with top nav
2. `ocean-web/src/App.vue` — global typography reset
3. `ocean-web/src/views/login/LoginView.vue` — editorial restyle
4. `ocean-web/src/views/register/RegisterView.vue` — editorial restyle
5. `ocean-web/src/views/dashboard/DashboardView.vue` — remove card wrappers, editorial layout
6. `ocean-web/src/views/dashboard/StatCards.vue` — big serif numbers, no card backgrounds
7. `ocean-web/src/views/dashboard/TrendCard.vue` — add narrative text, remove el-card
8. `ocean-web/src/views/dashboard/DashboardMap.vue` — remove el-card wrapper
9. `ocean-web/src/views/dashboard/AlertPanel.vue` — editorial alert list
10. `ocean-web/src/views/dashboard/LatestDataTable.vue` — minimal native table
11. `ocean-web/src/views/user/UserView.vue` — minimal table, custom pagination
12. `ocean-web/src/views/model/ModelView.vue` — minimal table
13. `ocean-web/src/views/ocean/OceanDataView.vue` — minimal table
14. `ocean-web/src/views/forecast/*.vue` — remove card wrappers, follow editorial style

## Pages Needing Nav Restructure

With the sidebar removed:
- The forecast sub-menu (`el-sub-menu`) becomes a top-level "预报" nav item
- SST/CHL/History pages get inline sub-navigation within the page content
- Admin-only nav items (模型, 用户) shown/hidden based on role, same as before

---

## Implementation Approach

1. **Backend: no changes.** This is purely a frontend visual redesign.
2. Element Plus remains as a dependency — but usage is reduced: layout components and icons remain, card/table/tag wrappers are replaced with native HTML + CSS.
3. ECharts and Leaflet integrations are unaffected — only their container styling changes.
4. Apply global CSS variables in `App.vue` or a new `styles/editorial.css` for the design tokens, so all components share the same typography and spacing values.
