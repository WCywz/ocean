# Hero V2 Migration: React → Vue

**Date**: 2026-05-10
**Status**: Approved

## Summary

Migrate the scroll-driven hero animation from `Hero/` (React) into `ocean-web/` (Vue 3) as a standalone component `HeroV2View.vue`. No existing files in either project are modified.

## Context

- Team unifying on Vue tech stack
- `Hero/` is a standalone React + Vite project implementing a specific hero page effect
- `ocean-web/` is the active Vue 3 project (Vue 3.4 + Vite 5.2 + Pinia + Vue Router + Element Plus)
- `gsap` and `lenis` are already dependencies in ocean-web — no new packages needed

## Files to add

### 1. Static assets → `public/hero-v2/`
- `hero-v2-img.jpg` (copied from `Hero/public/hero-img.jpg`)
- `hero-v2-mask.svg` (copied from `Hero/public/mask.svg`)
- `hero-v2-grid-overlay.svg` (copied from `Hero/public/grid-overlay.svg`)

### 2. Component → `src/views/home/HeroV2View.vue`
Single-file Vue component with `<template>`, `<script setup>`, `<style scoped>` + one unscoped `<style>` block for font import.

## Component design

### Template
JSX → Vue template syntax:
- `className` → `class`
- `ref={x}` → `ref="x"`
- Conditional attributes/logic stay in JS (the template is mostly static DOM with refs for GSAP)

DOM structure preserved exactly:
- `<div class="hero-v2">` — root section, ScrollTrigger trigger
  - `div.hero-v2-img` — background image layer (200svh height, reverse parallax via GSAP `y`)
  - `div.hero-v2-mask` — circular SVG mask overlay (fixed position, scale animation)
  - `div.hero-v2-grid-overlay` — grid overlay SVG (opacity animation)
  - `div.marker.marker-1` / `div.marker.marker-2` — Anchor Field / Drift Field markers
  - `div.hero-v2-content` — 4 text blocks with headline/content
  - `div.hero-v2-progress-bar` — vertical scroll progress indicator
- `section.outro` — ending section

Template refs use Vue's `ref(null)` with matching `ref` attribute names.

### Script
Composition API (`<script setup>`):
- `ref(null)` for each DOM element (equivalent to React `useRef`)
- `onMounted` — Lenis init + ScrollTrigger.scrollerProxy + GSAP animation setup (equivalent to `useGSAP`)
- `onUnmounted` — Lenis destroy + ScrollTrigger kill
- GSAP animation logic from `App.jsx:43-164` copied verbatim — all `gsap.set()` and `ScrollTrigger.create()` calls are pure GSAP API, framework-agnostic
- Lenis pattern matches existing `HomeView.vue:41-76` in ocean-web

Key adaptations:
- `containerRef.current` → `containerRef.value`
- `heroImgRef.current` → `heroImgRef.value`
- Remove `"use client"` directive
- Remove `import { ReactLenis } from "lenis/react"` and `<ReactLenis root />` — use Lenis directly
- Remove `import { useGSAP } from "@gsap/react"` — use `onMounted` instead

### Style
All CSS from `Hero/src/App.css` and `Hero/src/index.css` merged into `<style scoped>`. Adaptations:
- All class names prefixed/moved under `.hero-v2` root to avoid conflicts with ocean global styles
- CSS variables renamed with `--hv2-` prefix (e.g., `--light` → `--hv2-light`)
- Font import (`@import url(...)`) for DM Sans + DM Mono in an unscoped `<style>` block

## What does NOT change
- GSAP `ScrollTrigger.create({...})` animation logic
- DOM hierarchy and class structure (beyond prefixing)
- Visual output — the rendered hero looks identical to the React version
- No modifications to `router/index.js`, `App.vue`, `HomeView.vue`, or any other existing file

## Route integration (manual, by user)
Add a route entry in `router/index.js`:
```js
{
  path: '/hero-v2',
  name: 'HeroV2',
  component: () => import('../views/home/HeroV2View.vue'),
  meta: { title: 'Hero V2', noAuth: true }
}
```

## Testing
- Manual: visit `/hero-v2` after route is added, scroll through the page
- Verify: smooth scrolling, mask scale animation, grid overlay fade, marker fade, progress bar, text block parallax
- No automated tests included (visual/animation behavior)
