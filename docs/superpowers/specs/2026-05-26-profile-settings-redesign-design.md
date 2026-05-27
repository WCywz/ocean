# 个人中心与设置中心重构 — 设计文档

## 概述

将现有的单一页面个人中心重构为侧边栏导航 + 嵌套路由的模块化结构，迁移主题切换入口至设置页，并预留 4 个未来模块的占位。

## 导航结构

6 项侧边栏导航，每项对应一个子路由：

| # | 导航项 | 子内容 | 状态 |
|---|--------|--------|------|
| 1 | 个人信息 | 基本资料（头像、用户名、姓名、手机号） | 已有，从 ProfileView 迁移 |
| 2 | 账户安全 | 修改密码 | 已有，从 ProfileView 迁移 |
| 3 | 通知设置 | 通知渠道（短信/Push 开关）+ 推送配置（ServerChan Key） | 已有，从 ProfileView 迁移 |
| 4 | 显示偏好 | 外观主题（浅色/深色/跟随系统）+ 数据展示 | 主题已有（从导航栏迁入），数据展示占位 |
| 5 | 系统公告 | 公告列表 | 占位 |
| 6 | 系统设置 | 隐私设置、账户注销、关于信息、系统版本 | 占位 |

## 前端变更

### 文件变更

```
新增:
  ocean-web/src/views/profile/ProfileLayout.vue       # 侧边栏布局壳
  ocean-web/src/views/profile/ProfileInfo.vue          # 个人信息
  ocean-web/src/views/profile/ProfileSecurity.vue      # 账户安全
  ocean-web/src/views/profile/ProfileNotifications.vue # 通知设置
  ocean-web/src/views/profile/ProfilePreferences.vue   # 显示偏好
  ocean-web/src/views/profile/ProfileAnnouncements.vue # 系统公告（占位）
  ocean-web/src/views/profile/ProfileSettings.vue      # 系统设置（占位）

删除:
  ocean-web/src/views/profile/ProfileView.vue          # 拆分为以上文件

修改:
  ocean-web/src/router/index.js                        # 嵌套路由
  ocean-web/src/layout/MainLayout.vue                  # 移除主题切换按钮
```

### 路由

```js
{
  path: '/app/profile',
  component: ProfileLayout,
  redirect: '/app/profile/info',
  children: [
    { path: 'info', component: ProfileInfo },
    { path: 'security', component: ProfileSecurity },
    { path: 'notifications', component: ProfileNotifications },
    { path: 'preferences', component: ProfilePreferences },
    { path: 'announcements', component: ProfileAnnouncements },
    { path: 'settings', component: ProfileSettings },
  ]
}
```

### ProfileLayout.vue

- 左侧 160px 侧边栏，使用 `editorial-nav__item` 同款样式（13px, uppercase, letter-spacing 0.06em）
- 当前激活项通过 `route.path` 匹配，加 `font-weight: 600` 和 `color: --color-text`
- 右侧 `<router-view>` 渲染子路由
- 顶部标签 `个人中心`（10px uppercase muted）

### 子页面组件

每个子页面复用现有 editorial 样式类：
- `editorial-page-title` / `editorial-page-subtitle` — 页面标题区
- `editorial-form-label` / `editorial-input` — 表单
- `editorial-btn` / `editorial-btn-outline` — 按钮
- `editorial-section` — 内容分区

组件从 ProfileView.vue 中提取已有逻辑（script setup），无需重写业务代码。

占位组件显示 `editorial-page-title` + `editorial-page-subtitle` + 居中「即将上线」文案。

### 主题切换迁移

- 从 `MainLayout.vue` 中移除 `.nav-theme-toggle` 及其相关代码（模板 + script + 样式）
- 在 `ProfilePreferences.vue` 中调用 `useTheme().setMode(mode)` 实现主题切换
- `useTheme.js` 和 `editorial.css` 不变

## 后端变更

无。所有 API（`/api/profile`, `/api/profile/settings`, `/api/profile/credentials` 等）已存在且功能完整。

## 深色模式

所有新组件仅使用 CSS 变量（`--color-*`），不写死颜色值。现有 `[data-theme="dark"]` 规则自动生效，无需额外处理。

## 占位模块定义

以下模块仅显示占位页面，不实现功能：

1. **显示偏好 > 数据展示** — 显示「即将上线」
2. **系统公告** — 显示「即将上线」
3. **系统设置 > 隐私设置** — 显示「即将上线」
4. **系统设置 > 账户注销** — 显示「即将上线」
5. **系统设置 > 关于信息** — 显示「即将上线」
6. **系统设置 > 系统版本** — 显示「即将上线」

所有占位沿用 editorial 风格，不引入额外设计元素。
