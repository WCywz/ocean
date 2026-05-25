# 首页着陆页（Hero V2）

## 背景

将基于 React 的滚动驱动英雄页动画迁移到 Vue 3 项目，并集成业务功能（登录、导航）。

## 设计决策

### 动画迁移策略
- **逐字迁移** GSAP ScrollTrigger 动画逻辑——GSAP API 是纯 JS，不依赖框架
- 仅替换框架胶水代码：React 的 `useGSAP` → Vue 的 `onMounted/onUnmounted`，`<ReactLenis>` → 原生 Lenis 初始化
- 样式全部限定作用域，类名加 `--hv2-` 前缀，防止与全局 Editorial 样式冲突

### 动画结构
- 4 个视口高度的滚动序列：
  - 遮罩合成（mask compositing）
  - 图片去饱和（image desaturation）
  - 网格叠加层（grid overlay）
  - 标记淡出（marker fade）
  - 内容视差（content parallax）

### 业务集成
- 集成 `LandingHeader`、`LandingLogin`、`LandingFooter` 组件
- 添加中英双语海洋科普文案（面向国内和国际研究者）
- "探索系统" CTA 按钮，平滑滚动至登录区域

### 技术栈
- GSAP（ScrollTrigger）+ Lenis 平滑滚动
- Vue 3 组合式 API，单文件组件
- 静态资源放在 `public/hero-v2/` 目录下

## 路由

- `/` — 首页（HeroV2View）
- `/hero-v2` — 备用路由

## 关键文件

- `ocean-web/src/views/home/HeroV2View.vue` — 主组件
- `ocean-web/src/views/home/LandingHeader.vue` — 顶部导航
- `ocean-web/src/views/home/LandingLogin.vue` — 登录表单
- `ocean-web/src/views/home/LandingFooter.vue` — 页脚
- `ocean-web/src/styles/landing.css` — 着陆页专属样式
