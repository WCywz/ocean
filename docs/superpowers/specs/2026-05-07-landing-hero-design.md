# Landing Page Hero Animation — Design Document

**Date:** 2026-05-07
**Status:** Approved
**Scope:** Pre-login landing page with inversa.com-style scroll-driven hero animation, ending with login form

## Goal

Replace the current static `HomeView.vue` with a single long-scrolling landing page that replicates inversa.com's signature hero animation sequence, then transitions into the existing login form at the bottom.

## Design Reference

inversa.com — dark theme, GSAP scroll-driven hero with mask reveal, parallax background, desaturation filter, grid overlay, and Lenis smooth scrolling.

## Color Scheme

Full inversa.com palette:

| Token | Value | Usage |
|-------|-------|-------|
| `--color-black` | `#13140e` | Page background |
| `--color-black-2` | `#181813` | Section bg variant |
| `--color-creme` | `#f4f3e8` | Body text |
| `--color-grey` | `#595a51` | Borders, muted elements |
| `--color-grey-2` | `#404040` | Divider lines |
| `--color-yellow` | `#ebfc72` | Accent, CTAs, highlights |
| `--color-red` | `#f1664d` | Hotspots, alerts |
| `--color-green` | `#00d399` | Success indicators |

## Fonts

- **Primary**: JetBrains Mono (headings, labels, buttons)
- **Secondary**: NB International Pro alternative → system serif stack for large headings

Add `@font-face` for JetBrains Mono (woff2, 300/400/700 weights) like inversa.com.

## Page Structure

```
┌──────────────────────────────────────────┐
│ Fixed Header: Logo │ 登录 · 注册          │
├──────────────────────────────────────────┤
│                                          │
│  HERO (300-400svh, scroll-driven)        │
│  ┌────────────────────────────────────┐  │
│  │ Layer 0: 2 background images       │  │
│  │   - img A (退化) / img B (恢复)    │  │
│  │ Layer 1: SVG mask container        │  │
│  │ Layer 2: Grid coordinate overlay   │  │
│  │ Layer 3: Desaturation filter       │  │
│  │ Layer 4: Text slides (3-4)         │  │
│  │ Right: Progress indicator          │  │
│  └────────────────────────────────────┘  │
│                                          │
├──────────────────────────────────────────┤
│  LOGIN SECTION (100svh)                   │
│  ┌────────────────────────────────────┐  │
│  │ Centered login form                │  │
│  └────────────────────────────────────┘  │
│                                          │
├──────────────────────────────────────────┤
│  Footer                                  │
└──────────────────────────────────────────┘
```

## Hero Animation Sequence

5 phases driven by GSAP ScrollTrigger, each mapped to scroll progress (0%–100%):

### Phase 1: 反向漂移 (0%–25%)
- Background images move opposite to scroll direction via `yPercent` negative
- Right-side progress indicator fills from top (`scaleY 0→1`)
- Custom cursor visible with scroll progress ring

### Phase 2: 遮罩缩放 (20%–40%)
- SVG mask-image scales from 1 → 0.92
- Visual window narrows
- Text slide 1 fades out, text slide 2 fades in

### Phase 3: 去饱和 + 网格 (35%–60%)
- Desaturation filter layer opacity 0→1 (CSS `filter: grayscale` or `mix-blend-mode: color` overlay)
- Grid coordinate overlay opacity 0→1
- Background image appears to "snap" to a mechanical geographic coordinate map

### Phase 4: 释放还原 (55%–80%)
- Mask scale returns to 1
- Desaturation filter fades out
- Grid overlay fades out
- Full color restored
- Text slide 3 fades in

### Phase 5: 过渡登录 (75%–100%)
- Hero content fades out
- Login section scrolls into view
- Lenis provides smooth inertial transition

## Technical Architecture

### Dependencies
```
gsap        — ScrollTrigger + core animation engine
lenis       — Smooth inertial scrolling
```

### Component Tree
```
HomeView.vue (full-page scroll container)
├── LandingHeader.vue        — Fixed top nav
├── LandingHero.vue          — Hero animation (core)
│   ├── img.bg-img-a         — Background image layer
│   ├── img.bg-img-b         — Background image layer
│   ├── .mask                — SVG mask-image container
│   ├── .grid                — Grid coordinate overlay
│   ├── .filter              — Desaturation overlay
│   ├── .slides              — Text content slides
│   ├── .indicator           — Right progress bar
│   └── .cursor              — Custom cursor (desktop only)
├── LandingLogin.vue         — Login form section
│   (reuses existing login form logic)
└── LandingFooter.vue        — Minimal footer
```

### Layer Stacking (z-index)

| z-index | Layer | Description |
|---------|-------|-------------|
| -1 | Background images | Ocean/satellite imagery, parallax |
| 0 | Mask container | SVG mask-image, defines visual window |
| 1 | Grid overlay | Coordinate grid lines |
| 2 | Desaturation filter | `mix-blend-mode: color` overlay |
| 3 | Hotspot indicators | Red/yellow dots on map |
| 4 | Text slides | Scroll-driven content |
| 10 | Progress indicator | Right-side vertical bar |
| 50 | Fixed header | Top navigation |
| 99 | Custom cursor | Circle cursor with ring |

### SVG Mask Design

A custom irregular polygon path (similar to inversa.com's `M347 0v21h60V0...` path) that creates the hollow window effect. Designed in Figma and exported as an inline SVG path or `mask-image` URL.

### Router Changes

- `/` — `HomeView.vue` (new landing page with hero + login)
- Remove separate `/login` route
- Login form scroll position identified by `#login` anchor or computed scroll position
- `/register` remains as separate route

## Files Changed

| Action | File |
|--------|------|
| Rewrite | `ocean-web/src/views/home/HomeView.vue` |
| Add | `ocean-web/src/views/home/LandingHero.vue` |
| Add | `ocean-web/src/views/home/LandingLogin.vue` |
| Add | `ocean-web/src/views/home/LandingHeader.vue` |
| Add | `ocean-web/src/views/home/LandingFooter.vue` |
| Modify | `ocean-web/src/router/index.js` |
| Add | `ocean-web/src/assets/fonts/` (JetBrains Mono woff2) |
| Install | `gsap`, `lenis` in `ocean-web/package.json` |

## Edge Cases

- **Mobile**: Disable custom cursor. Adjust mask size via media queries. Reduce animation intensity.
- **No JS / SSR fallback**: Static hero with full-color image and visible login form — all animations are progressive enhancement.
- **Performance**: Use `will-change: transform` on animated layers. Preload background images.
- **Login redirect**: If user has token, skip landing and redirect to `/app/dashboard` (existing router guard handles this).
