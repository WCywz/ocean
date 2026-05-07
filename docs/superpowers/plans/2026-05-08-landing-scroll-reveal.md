# Landing Page Scroll Reveal Animation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace static LandingHero.vue with inversa.com-style scroll-driven hero animation (block-grid mask erosion, reverse parallax, grid/dots/progress bar, Lenis smooth scroll).

**Architecture:** Single Vue SFC rewrite (LandingHero.vue) with 7 layered elements driven by a GSAP ScrollTrigger timeline. Lenis provides smooth inertial scrolling from HomeView.vue. SVG `<mask>` with ~50 black `<rect>` blocks at edges creates the block-grid erosion; GSAP animates the mask container's `scale` to move blocks inward on scroll.

**Tech Stack:** Vue 3, GSAP ScrollTrigger, Lenis, SVG mask

---

## File Structure

| File | Action | Purpose |
|------|--------|---------|
| `ocean-web/src/views/home/HomeView.vue` | Modify | Add Lenis smooth scroll instance, RAF loop, ScrollTrigger sync |
| `ocean-web/src/views/home/LandingHero.vue` | Rewrite | Full 7-layer scroll-driven hero component |

Single-file approach: all hero layers (mask, grid, dots, slides, progress) live in `LandingHero.vue` as inline template elements. No new component files — keeps the landing page surface area small.

---

### Task 1: Add Lenis to HomeView.vue

**Files:**
- Modify: `ocean-web/src/views/home/HomeView.vue`

- [ ] **Step 1: Import Lenis and register GSAP ScrollTrigger plugin**

Replace the `<script setup>` block in `HomeView.vue`:

```vue
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import Lenis from 'lenis'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import LandingHeader from './LandingHeader.vue'
import LandingHero from './LandingHero.vue'
import LandingLogin from './LandingLogin.vue'
import LandingFooter from './LandingFooter.vue'

gsap.registerPlugin(ScrollTrigger)

const heroRef = ref(null)
const loginRef = ref(null)
let lenis = null

function scrollToLogin() {
  if (loginRef.value) {
    const el = loginRef.value.loginRef || loginRef.value.$el
    lenis?.scrollTo(el, { offset: 0, duration: 1.5 })
  }
}

function scrollToTop() {
  lenis?.scrollTo(0, { duration: 1.5 })
}

onMounted(() => {
  lenis = new Lenis({
    duration: 1.2,
    easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
    smoothWheel: true,
  })

  function raf(time) {
    lenis?.raf(time)
    requestAnimationFrame(raf)
  }
  requestAnimationFrame(raf)

  // Sync GSAP ScrollTrigger with Lenis
  ScrollTrigger.scrollerProxy(window, {
    scrollTop(value) {
      if (arguments.length) {
        lenis?.scrollTo(value, { immediate: true })
      }
      return lenis?.scroll ?? 0
    },
    getBoundingClientRect() {
      return { top: 0, left: 0, width: window.innerWidth, height: window.innerHeight }
    },
  })

  // Refresh ScrollTrigger on Lenis scroll
  lenis.on('scroll', ScrollTrigger.update)
})

onUnmounted(() => {
  lenis?.destroy()
  ScrollTrigger.getAll().forEach((st) => st.kill())
})
</script>
```

- [ ] **Step 2: Remove old scroll-behavior CSS**

Remove this line from the unscoped `<style>` block:

```css
/* REMOVE this line: */
scroll-behavior: smooth;
```

The updated unscoped `<style>` becomes:

```css
<style>
html {
  overflow-y: scroll;
}
body {
  min-height: 100%;
}
</style>
```

The scoped `<style>` stays unchanged.

- [ ] **Step 3: Verify Lenis is working**

Run: `cd ocean-web && npm run dev`
Expected: Open the landing page, scroll — should feel smooth with inertia. No console errors about Lenis or ScrollTrigger.

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/home/HomeView.vue
git commit -m "feat: add Lenis smooth scroll and GSAP ScrollTrigger sync to HomeView"
```

---

### Task 2: Rewrite LandingHero.vue — template structure and SVG mask

**Files:**
- Rewrite: `ocean-web/src/views/home/LandingHero.vue`

- [ ] **Step 1: Build the full template**

Replace the entire `LandingHero.vue` file. Start with the template and script skeleton (styles in later tasks):

```vue
<template>
  <section class="landing-hero" ref="heroRef">
    <!-- Layer 0: Background image (reverse parallax) -->
    <div class="hero-bg" ref="bgRef">
      <img
        class="hero-bg__image"
        src="/index.jpg"
        alt="Ocean surface"
      />
    </div>

    <!-- Layer 1: SVG mask overlay (block-grid erosion) -->
    <div class="hero-mask" ref="maskRef">
      <svg
        class="hero-mask__svg"
        viewBox="0 0 1000 1000"
        preserveAspectRatio="none"
      >
        <defs>
          <mask id="hero-block-mask">
            <!-- White = show overlay (hide image). Start with nothing visible. -->
            <rect width="1000" height="1000" fill="black"/>
            <!-- Blocks added dynamically in script -->
            <g ref="maskBlocksRef">
              <rect
                v-for="(block, i) in maskBlocks"
                :key="i"
                :x="block.x"
                :y="block.y"
                :width="block.w"
                :height="block.h"
                fill="white"
              />
            </g>
          </mask>
        </defs>
        <rect
          width="1000"
          height="1000"
          fill="#13140e"
          mask="url(#hero-block-mask)"
        />
      </svg>
    </div>

    <!-- Layer 2: Grid lines -->
    <div class="hero-grid" ref="gridRef">
      <svg
        class="hero-grid__svg"
        viewBox="0 0 1000 1000"
        preserveAspectRatio="none"
      >
        <line v-for="(line, i) in gridLines" :key="'h'+i"
          :x1="line.x1" :y1="line.y1" :x2="line.x2" :y2="line.y2"
          stroke="#404040" stroke-width="0.5" vector-effect="non-scaling-stroke"
        />
      </svg>
    </div>

    <!-- Layer 3: Indicator dots -->
    <div class="hero-dots" ref="dotsRef">
      <svg
        class="hero-dots__svg"
        viewBox="0 0 1000 1000"
        preserveAspectRatio="none"
      >
        <circle v-for="(dot, i) in indicatorDots" :key="i"
          :cx="dot.cx" :cy="dot.cy" :r="dot.r"
          :fill="dot.color" opacity="0.5"
        />
      </svg>
    </div>

    <!-- Layer 4: Text slides -->
    <div class="hero-slides" ref="slidesRef">
      <div class="hero-slide hero-slide--1" ref="slide1Ref">
        <h2 class="hero-heading">海洋预报<br/>守护蔚蓝</h2>
        <a class="cta-button" @click="$emit('scrollToLogin')">探索系统</a>
      </div>
      <div class="hero-slide hero-slide--2" ref="slide2Ref">
        <h3 class="section-title is-blinker">海洋环境监测</h3>
        <p class="body-text">实时追踪海表温度与叶绿素浓度变化，为海洋科学研究与环境保护提供精准预报数据。</p>
      </div>
      <div class="hero-slide hero-slide--3" ref="slide3Ref">
        <h3 class="section-title is-blinker">数据驱动决策</h3>
        <p class="body-text">集成多种海洋预报模型，支持参数配置与状态监控，让数据成为科学决策的基础。</p>
      </div>
      <div class="hero-slide hero-slide--4" ref="slide4Ref">
        <h3 class="section-title is-blinker">生态修复</h3>
        <p class="body-text">通过精准的海洋环境预报，助力生态保护与可持续发展，为海洋未来贡献力量。</p>
      </div>
    </div>

    <!-- Layer 10: Progress bar -->
    <div class="hero-progress" ref="progressRef">
      <div class="hero-progress__track">
        <div class="hero-progress__fill" ref="progressFillRef"/>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { gsap } from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

defineEmits(['scrollToLogin'])

const heroRef = ref(null)
const bgRef = ref(null)
const maskRef = ref(null)
const gridRef = ref(null)
const dotsRef = ref(null)
const slidesRef = ref(null)
const slide1Ref = ref(null)
const slide2Ref = ref(null)
const slide3Ref = ref(null)
const slide4Ref = ref(null)
const progressRef = ref(null)
const progressFillRef = ref(null)

// --- Mask block data ---
// Each block is a rect at an edge. Positioned so at scale ~1.08 they're
// outside the viewport; at scale 0.92 they erode inward ~4% per side.
const maskBlocks = ref([])
const gridLines = ref([])
const indicatorDots = ref([])

// Generate mask blocks, grid lines, and dots in onMounted
onMounted(() => {
  generateMaskBlocks()
  generateGridLines()
  generateDots()
  initScrollAnimation()
})

onUnmounted(() => {
  ScrollTrigger.getAll().forEach((st) => st.kill())
})

function generateMaskBlocks() {
  const blocks = []
  const S = 1000 // viewBox size

  // Helper: add a block
  function add(x, y, w, h) {
    blocks.push({ x, y, w, h })
  }

  // Top edge blocks (y near 0, varying heights reach downward)
  const topHeights = [30, 60, 25, 80, 40, 55, 20, 70, 35, 50, 45, 65, 28, 75, 38]
  let tx = 0
  for (let i = 0; i < topHeights.length; i++) {
    const w = 30 + Math.random() * 100
    add(tx, 0, Math.min(w, S - tx), topHeights[i])
    tx += w
    if (tx >= S) break
  }

  // Bottom edge blocks (y near S, varying heights reach upward)
  const bottomHeights = [40, 55, 25, 70, 35, 60, 30, 80, 45, 50, 28, 65, 38, 72, 32]
  let bx = 0
  for (let i = 0; i < bottomHeights.length; i++) {
    const w = 30 + Math.random() * 100
    const h = bottomHeights[i]
    add(bx, S - h, Math.min(w, S - bx), h)
    bx += w
    if (bx >= S) break
  }

  // Left edge blocks (x near 0, varying widths reach rightward)
  const leftWidths = [25, 55, 35, 65, 20, 50, 40, 70, 30, 45, 28, 60, 38, 52, 32]
  let ly = 80
  for (let i = 0; i < leftWidths.length; i++) {
    const h = 30 + Math.random() * 60
    add(0, ly, leftWidths[i], Math.min(h, S - ly))
    ly += h
    if (ly >= S - 80) break
  }

  // Right edge blocks (x near S, varying widths reach leftward)
  const rightWidths = [35, 50, 22, 60, 40, 55, 28, 68, 32, 48, 25, 58, 38, 52, 30]
  let ry = 80
  for (let i = 0; i < rightWidths.length; i++) {
    const h = 30 + Math.random() * 60
    const w = rightWidths[i]
    add(S - w, ry, w, Math.min(h, S - ry))
    ry += h
    if (ry >= S - 80) break
  }

  maskBlocks.value = blocks
}

function generateGridLines() {
  const lines = []
  // Horizontal lines
  for (let y = 0; y <= 1000; y += 80) {
    lines.push({ x1: 0, y1: y, x2: 1000, y2: y })
  }
  // Vertical lines
  for (let x = 0; x <= 1000; x += 80) {
    lines.push({ x1: x, y1: 0, x2: x, y2: 1000 })
  }
  gridLines.value = lines
}

function generateDots() {
  const colors = ['#ebfc72', '#f1664d', '#00d399']
  const dots = []
  for (let i = 0; i < 7; i++) {
    dots.push({
      cx: 50 + Math.random() * 900,
      cy: 50 + Math.random() * 900,
      r: 2 + Math.random() * 4,
      color: colors[i % colors.length],
    })
  }
  indicatorDots.value = dots
}

let scrollTl = null

function initScrollAnimation() {
  // Implemented in Task 3, tuned in Task 4
}
</script>

<style scoped>
/* Minimal structural styles — full styling in Task 6 */
.landing-hero {
  position: relative;
  height: 350svh;
  overflow: hidden;
  background: var(--color-black);
}

.hero-bg {
  position: absolute;
  inset: 0;
  will-change: transform;
}

.hero-bg__image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-mask {
  position: absolute;
  inset: 0;
  z-index: 1;
  transform-origin: center center;
  will-change: transform;
}

.hero-mask__svg {
  display: block;
  width: 100%;
  height: 100%;
}

.hero-grid {
  position: absolute;
  inset: 0;
  z-index: 2;
  opacity: 0;
  pointer-events: none;
}

.hero-grid__svg {
  display: block;
  width: 100%;
  height: 100%;
}

.hero-dots {
  position: absolute;
  inset: 0;
  z-index: 3;
  opacity: 0;
  pointer-events: none;
}

.hero-dots__svg {
  display: block;
  width: 100%;
  height: 100%;
}

.hero-slides {
  position: absolute;
  inset: 0;
  z-index: 4;
  pointer-events: none;
}

.hero-slide {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  text-align: center;
}

.hero-slide--1 { top: 5%; }
.hero-slide--2 { top: 30%; }
.hero-slide--3 { top: 55%; }
.hero-slide--4 { top: 80%; }

.hero-progress {
  position: absolute;
  right: 8px;
  top: 0;
  bottom: 0;
  width: 2px;
  z-index: 10;
  pointer-events: none;
}

.hero-progress__track {
  width: 100%;
  height: 100%;
  background: #404040;
  border-radius: 1px;
}

.hero-progress__fill {
  width: 100%;
  background: #ebfc72;
  border-radius: 1px;
  transform-origin: top center;
  transform: scaleY(0);
}
</style>
```

- [ ] **Step 2: Verify the template renders statically**

Run: `cd ocean-web && npm run dev`
Expected: Page loads with hero section. Background image visible. Mask blocks should NOT be visible at scale 1.0 (they're at the edges). Grid and dots invisible (opacity 0). Text slides visible.

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/views/home/LandingHero.vue
git commit -m "feat: scaffold LandingHero with 7-layer template and SVG block mask"
```

---

### Task 3: Add GSAP ScrollTrigger animation timeline

**Files:**
- Modify: `ocean-web/src/views/home/LandingHero.vue` (replace `initScrollAnimation` function)

- [ ] **Step 1: Implement the animation timeline**

Replace the empty `initScrollAnimation()` function in `LandingHero.vue`:

```js
function initScrollAnimation() {
  if (!heroRef.value) return

  scrollTl = gsap.timeline({
    scrollTrigger: {
      trigger: heroRef.value,
      start: 'top top',
      end: 'bottom bottom',
      scrub: true,
      invalidateOnRefresh: true,
    },
  })

  // Phase 1-4: Reverse parallax on background image (full scroll range)
  scrollTl.to(bgRef.value, {
    yPercent: -15,
    ease: 'none',
  }, 0)

  // Phase 2 (25%-40%): Mask scales 1.0 → 0.92 — blocks erode inward
  scrollTl.to(maskRef.value, {
    scale: 0.92,
    ease: 'none',
  }, '25%')
  // Phase 3 (40%-60%): Mask scales 0.92 → 1.0 — blocks retreat
  scrollTl.to(maskRef.value, {
    scale: 1.0,
    ease: 'none',
  }, '40%')

  // Phase 2 (25%-40%): Grid + dots fade in
  scrollTl.fromTo(gridRef.value,
    { opacity: 0 },
    { opacity: 1, ease: 'none' },
    '25%'
  )
  scrollTl.fromTo(dotsRef.value,
    { opacity: 0 },
    { opacity: 1, ease: 'none' },
    '25%'
  )

  // Phase 3 (40%-60%): Grid + dots fade out
  scrollTl.to(gridRef.value, {
    opacity: 0,
    ease: 'none',
  }, '40%')
  scrollTl.to(dotsRef.value, {
    opacity: 0,
    ease: 'none',
  }, '40%')

  // Phase 1→2: Text slide 1 fades out, slide 2 fades in
  scrollTl.to(slide1Ref.value, {
    opacity: 0,
    ease: 'none',
  }, '15%')
  scrollTl.fromTo(slide2Ref.value,
    { opacity: 0 },
    { opacity: 1, ease: 'none' },
    '20%'
  )

  // Phase 2→3: Slide 2→3 crossfade
  scrollTl.to(slide2Ref.value, {
    opacity: 0,
    ease: 'none',
  }, '35%')
  scrollTl.fromTo(slide3Ref.value,
    { opacity: 0 },
    { opacity: 1, ease: 'none' },
    '38%'
  )

  // Phase 3→4: Slide 3→4 crossfade
  scrollTl.to(slide3Ref.value, {
    opacity: 0,
    ease: 'none',
  }, '52%')
  scrollTl.fromTo(slide4Ref.value,
    { opacity: 0 },
    { opacity: 1, ease: 'none' },
    '55%'
  )

  // Phase 4 (60%-100%): Hero fades out
  scrollTl.to(heroRef.value, {
    opacity: 0,
    ease: 'none',
  }, '60%')

  // Progress bar: scaleY 0→1 over full scroll
  scrollTl.fromTo(progressFillRef.value,
    { scaleY: 0 },
    { scaleY: 1, ease: 'none' },
    0
  )
}
```

- [ ] **Step 2: Set initial states**

Add these GSAP `set` calls before the timeline to lock initial states (prevents flash of wrong state):

Add right after `scrollTl = gsap.timeline(...)`:

```js
  // Lock initial states
  gsap.set(maskRef.value, { scale: 1.0 })
  gsap.set(gridRef.value, { opacity: 0 })
  gsap.set(dotsRef.value, { opacity: 0 })
  gsap.set(slide2Ref.value, { opacity: 0 })
  gsap.set(slide3Ref.value, { opacity: 0 })
  gsap.set(slide4Ref.value, { opacity: 0 })
  gsap.set(progressFillRef.value, { scaleY: 0 })
  gsap.set(heroRef.value, { opacity: 1 })
```

- [ ] **Step 3: Verify the animation in browser**

Run: `cd ocean-web && npm run dev`
Expected behavior:
- Scroll down: background image shifts up slightly
- At ~25% scroll: mask blocks start becoming visible from edges, grid + dots fade in
- At ~40%: mask blocks start retreating, grid + dots fade out
- At ~60%: hero section starts fading out
- Progress bar fills top-to-bottom
- Text slides crossfade at the right times

- [ ] **Step 4: Commit**

```bash
git add ocean-web/src/views/home/LandingHero.vue
git commit -m "feat: add GSAP ScrollTrigger animation timeline for hero layers"
```

---

### Task 4: Tune mask block visibility and scale range

**Files:**
- Modify: `ocean-web/src/views/home/LandingHero.vue`

- [ ] **Step 1: Adjust initial scale so mask starts outside viewport**

The user wants "initially no mask visible." Update the initial scale set and the scale keyframes so blocks start outside the viewport:

Update the timeline scale keyframes (both the initial set and the tweens) from 1.0 to 1.08:

In `gsap.set`:
```js
  gsap.set(maskRef.value, { scale: 1.08 })  // was 1.0
```

In the timeline tweens:
```js
  // Phase 2 (25%-40%): Mask 1.08 → 0.92 — blocks erode inward from outside viewport
  scrollTl.to(maskRef.value, {
    scale: 0.92,
    ease: 'none',
  }, '25%')
  // Phase 3 (40%-60%): Mask 0.92 → 1.08 — blocks retreat back outside viewport
  scrollTl.to(maskRef.value, {
    scale: 1.08,
    ease: 'none',
  }, '40%')
```

- [ ] **Step 2: Verify mask visibility in browser**

Run: `cd ocean-web && npm run dev`
Expected: At scroll position 0%, no mask blocks visible (image fully visible). Scrolling down to 25%-40%: blocks erode inward from 4 edges with irregular pattern. Scrolling past 40%: blocks retreat.

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/views/home/LandingHero.vue
git commit -m "fix: tune mask scale range so blocks start outside viewport"
```

---

### Task 5: Add responsive styles and mobile adjustments

**Files:**
- Modify: `ocean-web/src/views/home/LandingHero.vue`

- [ ] **Step 1: Add responsive CSS**

Replace the scoped `<style>` block in `LandingHero.vue` with the full version including mobile adjustments:

```css
<style scoped>
.landing-hero {
  position: relative;
  height: 350svh;
  overflow: hidden;
  background: var(--color-black);
}

.hero-bg {
  position: absolute;
  inset: 0;
  will-change: transform;
}

.hero-bg__image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ===== Mask ===== */
.hero-mask {
  position: absolute;
  inset: 0;
  z-index: 1;
  transform-origin: center center;
  will-change: transform;
}

.hero-mask__svg {
  display: block;
  width: 100%;
  height: 100%;
}

/* ===== Grid ===== */
.hero-grid {
  position: absolute;
  inset: 0;
  z-index: 2;
  opacity: 0;
  pointer-events: none;
  will-change: opacity;
}

.hero-grid__svg {
  display: block;
  width: 100%;
  height: 100%;
}

/* ===== Dots ===== */
.hero-dots {
  position: absolute;
  inset: 0;
  z-index: 3;
  opacity: 0;
  pointer-events: none;
  will-change: opacity;
}

.hero-dots__svg {
  display: block;
  width: 100%;
  height: 100%;
}

/* ===== Text slides ===== */
.hero-slides {
  position: absolute;
  inset: 0;
  z-index: 4;
  pointer-events: none;
}

.hero-slide {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  text-align: center;
  will-change: opacity;
}

.hero-slide--1 { top: 5%; }
.hero-slide--2 { top: 30%; }
.hero-slide--3 { top: 55%; }
.hero-slide--4 { top: 80%; }

.hero-slide:first-child .hero-heading,
.hero-slide:first-child .cta-button {
  position: absolute;
}

/* ===== Progress bar ===== */
.hero-progress {
  position: absolute;
  right: 8px;
  top: 0;
  bottom: 0;
  width: 2px;
  z-index: 10;
  pointer-events: none;
}

.hero-progress__track {
  width: 100%;
  height: 100%;
  background: #404040;
  border-radius: 1px;
}

.hero-progress__fill {
  width: 100%;
  background: #ebfc72;
  border-radius: 1px;
  transform-origin: top center;
  transform: scaleY(0);
  will-change: transform;
}

/* ===== Mobile (max-width: 600px) ===== */
@media (max-width: 600px) {
  .landing-hero {
    height: 300svh;
  }

  .hero-slide--1 {
    top: 5%;
  }

  .hero-slide--2,
  .hero-slide--3,
  .hero-slide--4 {
    top: auto;
    bottom: 25%;
    padding: 0 1.2rem;
  }

  .hero-slide--3 {
    bottom: 15%;
  }

  .hero-slide--4 {
    bottom: 5%;
  }

  .hero-slide:first-child .hero-heading {
    bottom: 1.2rem;
  }

  .hero-progress {
    right: 4px;
    width: 1.5px;
  }
}

/* ===== Desktop (min-width: 601px) ===== */
@media (min-width: 601px) {
  .hero-slide:first-child .hero-heading {
    bottom: 1.6rem;
    left: 1.6rem;
  }

  .hero-slide:nth-child(2),
  .hero-slide:nth-child(4) {
    align-items: flex-end;
    padding-right: 8.2rem;
    width: 39rem;
    right: 0;
  }

  .hero-slide:nth-child(3) {
    align-items: flex-start;
    padding-left: 8.2rem;
    width: 38rem;
    left: 0;
  }
}
</style>
```

- [ ] **Step 2: Verify responsive layout**

Run: `cd ocean-web && npm run dev`
Expected: Desktop: text slides positioned with offset (slide 2/4 right-aligned, slide 3 left-aligned). Mobile (< 600px): slides stacked at bottom, hero shorter (300svh). Check Chrome DevTools responsive mode.

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/views/home/LandingHero.vue
git commit -m "style: add responsive CSS and mobile adjustments for hero layers"
```

---

### Task 6: Final verification and cleanup

**Files:**
- Verify: `ocean-web/src/views/home/LandingHero.vue`
- Verify: `ocean-web/src/views/home/HomeView.vue`

- [ ] **Step 1: Check for unused code and warnings**

Run: `cd ocean-web && npm run build`
Expected: No build errors, no TypeScript/Vue warnings related to the hero components.

- [ ] **Step 2: Manual visual verification checklist**

Run dev server: `cd ocean-web && npm run dev`

Check each item:
- [ ] Scroll down from top: smooth Lenis inertia, no jank
- [ ] 0%-25%: Full ocean image visible, no mask blocks at edges
- [ ] 25%-40%: Blocks erode inward from 4 edges (irregular pattern), grid + dots appear
- [ ] 40%-60%: Blocks retreat, grid + dots fade out
- [ ] 60%-100%: Hero fades out, login form visible below
- [ ] Progress bar: fills top-to-bottom through entire hero scroll
- [ ] Text slides: crossfade at correct scroll positions
- [ ] Background image: slight upward shift on scroll (reverse parallax)
- [ ] Mobile (< 600px): layout adapts, no horizontal overflow
- [ ] Reload page at various scroll positions: no flash of wrong state
- [ ] Resize window during scroll: animations recalculate correctly

- [ ] **Step 3: Fix any issues found**

Address any visual or behavioral issues discovered in Step 2. Each fix should be a small, targeted edit followed by a verify-again step.

- [ ] **Step 4: Final commit**

```bash
git add ocean-web/src/views/home/LandingHero.vue ocean-web/src/views/home/HomeView.vue
git commit -m "chore: final cleanup and verification of landing scroll reveal"
```
