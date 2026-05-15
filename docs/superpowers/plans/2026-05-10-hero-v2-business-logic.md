# Hero V2 Business Logic Integration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate existing LandingHeader, LandingLogin, and LandingFooter components into HeroV2View.vue, update text content to ocean-specific copy, and wire scroll-to-login/scroll-to-top interactions.

**Architecture:** Single-file change to HeroV2View.vue. No new files created. Existing LandingHeader/LandingLogin/LandingFooter components imported directly — they already work with the global CSS variables and utility classes from `landing.css` (imported in `App.vue`). Header sits outside the Lenis wrapper (fixed position). Login and Footer sit inside the Lenis wrapper after the outro section.

**Tech Stack:** Vue 3.4, GSAP 3.15, Lenis 1.3

---

### Task 1: Integrate business components and update content

**Files:**
- Modify: `ocean-web/src/views/home/HeroV2View.vue` (template + script + style)

- [ ] **Step 1: Update template — add LandingHeader outside wrapper, add Login + Footer inside wrapper after outro, add CTA button, update text**

Replace the entire template block with:

```vue
<template>
  <LandingHeader @scroll-to-top="scrollToTop" />

  <div ref="lenisWrapperRef" class="hero-v2-root landing">
    <div ref="containerRef">
      <section ref="heroSectionRef" class="hero-v2">
        <div class="hero-v2-img" ref="heroImgRef">
          <img ref="heroImgElementRef" src="/hero-v2/hero-v2-img.jpg" alt="" />
        </div>

        <div class="hero-v2-mask" ref="heroMaskRef"></div>

        <div class="hero-v2-grid-overlay" ref="heroGridOverlayRef">
          <img src="/hero-v2/hero-v2-grid-overlay.svg" alt="" />
        </div>

        <div class="marker marker-1" ref="marker1Ref">
          <span class="marker-icon"></span>
          <p class="marker-label">Anchor Field</p>
        </div>

        <div class="marker marker-2" ref="marker2Ref">
          <span class="marker-icon"></span>
          <p class="marker-label">Drift Field</p>
        </div>

        <div class="hero-v2-content" ref="heroContentRef">
          <div class="hero-v2-content-block">
            <div class="hero-v2-content-copy">
              <h1>海洋预报<br/>Ocean Forecasting System</h1>
              <button class="hero-v2-cta" @click="scrollToLogin">探索系统</button>
            </div>
          </div>
          <div class="hero-v2-content-block">
            <div class="hero-v2-content-copy">
              <h2>海洋环境监测</h2>
              <p>
                实时追踪海表温度与叶绿素浓度变化，为海洋科学研究与环境保护提供精准预报数据。
              </p>
              <p>
                Real-time tracking of sea surface temperature and chlorophyll concentration changes provides precise forecast data for marine scientific research and environmental protection.
              </p>
            </div>
          </div>
          <div class="hero-v2-content-block">
            <div class="hero-v2-content-copy">
              <h2>数据驱动决策</h2>
              <p>
                集成多种海洋预报模型，支持参数配置与状态监控，让数据成为科学决策的基础。
              </p>
              <p>
                Integrating multiple marine forecasting models, supporting parameter configuration and status monitoring, to make data the foundation of scientific decision-making.
              </p>
            </div>
          </div>
          <div class="hero-v2-content-block">
            <div class="hero-v2-content-copy">
              <h2>生态修复</h2>
              <p>
                通过精准的海洋环境预报，助力生态保护与可持续发展，为海洋未来贡献力量。
              </p>
              <p>
                Through accurate marine environmental forecasting, we contribute to ecological conservation and sustainable development, making a difference for the future of the oceans.
              </p>
            </div>
          </div>
        </div>

        <div class="hero-v2-scroll-progress-bar" ref="progressBarRef"></div>
      </section>

      <section class="hero-v2-outro">
        <p>The system has reached its final spatial state.</p>
      </section>

      <LandingLogin ref="landingLoginRef" />
      <LandingFooter />
    </div>
  </div>
</template>
```

Key changes from current template:
- `<LandingHeader>` added outside `lenisWrapperRef` (fixed position element, should not be inside Lenis)
- `landing` class added to `lenisWrapperRef` so global `.landing .cta-button` / `.landing .text-link` styles from `landing.css` apply to LandingLogin
- First hero content block: h1 text changed to "海洋预报 / Ocean Forecasting System", CTA button added
- Blocks 2-4: text changed from generic English to ocean-specific bilingual content
- `<LandingLogin>` and `<LandingFooter>` added after outro section, inside Lenis wrapper

- [ ] **Step 2: Update script — import components, add scroll methods, add refs**

Replace the `<script setup>` block with:

```vue
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import Lenis from 'lenis'
import LandingHeader from './LandingHeader.vue'
import LandingLogin from './LandingLogin.vue'
import LandingFooter from './LandingFooter.vue'

gsap.registerPlugin(ScrollTrigger)

const lenisWrapperRef = ref(null)
const containerRef = ref(null)
const heroSectionRef = ref(null)
const heroImgRef = ref(null)
const heroImgElementRef = ref(null)
const heroMaskRef = ref(null)
const heroGridOverlayRef = ref(null)
const marker1Ref = ref(null)
const marker2Ref = ref(null)
const heroContentRef = ref(null)
const progressBarRef = ref(null)
const landingLoginRef = ref(null)

let lenis = null
let rafId = null

function scrollToLogin() {
  if (landingLoginRef.value) {
    const el = landingLoginRef.value.loginRef || landingLoginRef.value.$el
    lenis?.scrollTo(el, { offset: 0, duration: 1.5 })
  }
}

function scrollToTop() {
  lenis?.scrollTo(0, { duration: 1.5 })
}

onMounted(() => {
  lenis = new Lenis({
    wrapper: lenisWrapperRef.value,
    content: lenisWrapperRef.value,
    duration: 1.2,
    easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
    smoothWheel: true,
  })

  function raf(time) {
    lenis?.raf(time)
    rafId = requestAnimationFrame(raf)
  }
  rafId = requestAnimationFrame(raf)

  ScrollTrigger.scrollerProxy(lenisWrapperRef.value, {
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

  lenis.on('scroll', ScrollTrigger.update)

  const heroContent = heroContentRef.value
  const heroImg = heroImgRef.value
  const heroImgElement = heroImgElementRef.value
  const heroMask = heroMaskRef.value
  const heroGridOverlay = heroGridOverlayRef.value
  const marker1 = marker1Ref.value
  const marker2 = marker2Ref.value
  const progressBar = progressBarRef.value

  const ease = (x) => x * x * (3 - 2 * x)

  ScrollTrigger.create({
    trigger: heroSectionRef.value,
    scroller: lenisWrapperRef.value,
    start: 'top top',
    end: `+=${window.innerHeight * 4}px`,
    pin: true,
    pinSpacing: true,
    scrub: 1,
    onUpdate: (self) => {
      const heroContentHeight = heroContent.offsetHeight
      const viewportHeight = window.innerHeight
      const heroContentMovedDistance = heroContentHeight - viewportHeight

      const heroImgHeight = heroImg.offsetHeight
      const heroImgMovedDistance = heroImgHeight - viewportHeight

      gsap.set(progressBar, {
        scaleX: self.progress,
        '--hv2-progress': self.progress,
      })

      gsap.set(heroContent, {
        y: -self.progress * heroContentMovedDistance,
      })

      let heroImgProgress
      if (self.progress < 0.45) {
        heroImgProgress = ease(self.progress / 0.45) * 0.65
      } else if (self.progress < 0.75) {
        heroImgProgress = 0.65
      } else {
        heroImgProgress = 0.65 + ease((self.progress - 0.75) / 0.25) * 0.35
      }

      gsap.set(heroImg, {
        y: heroImgProgress * heroImgMovedDistance,
      })

      let heroMaskScale
      let heroImgSaturation
      let heroImgOverlayOpacity

      if (self.progress <= 0.4) {
        heroMaskScale = 2.5
        heroImgSaturation = 1
        heroImgOverlayOpacity = 0.35
      } else if (self.progress <= 0.5) {
        const phaseProgress = ease((self.progress - 0.4) / 0.1)
        heroMaskScale = 2.5 - phaseProgress * 1.5
        heroImgSaturation = 1 - phaseProgress
        heroImgOverlayOpacity = 0.35 + phaseProgress * 0.35
      } else if (self.progress <= 0.75) {
        heroMaskScale = 1
        heroImgSaturation = 0
        heroImgOverlayOpacity = 0.7
      } else if (self.progress <= 0.85) {
        const phaseProgress = ease((self.progress - 0.75) / 0.1)
        heroMaskScale = 1 + phaseProgress * 1.5
        heroImgSaturation = phaseProgress
        heroImgOverlayOpacity = 0.7 - phaseProgress * 0.35
      } else {
        heroMaskScale = 2.5
        heroImgSaturation = 1
        heroImgOverlayOpacity = 0.35
      }

      gsap.set(heroMask, {
        scale: heroMaskScale,
      })

      gsap.set(heroImgElement, {
        filter: `saturate(${heroImgSaturation})`,
      })

      gsap.set(heroImg, {
        '--hv2-overlay-opacity': heroImgOverlayOpacity,
      })

      let heroGridOpacity
      if (self.progress <= 0.475) {
        heroGridOpacity = 0
      } else if (self.progress <= 0.5) {
        heroGridOpacity = ease((self.progress - 0.475) / 0.025)
      } else if (self.progress <= 0.75) {
        heroGridOpacity = 1
      } else if (self.progress <= 0.775) {
        heroGridOpacity = 1 - ease((self.progress - 0.75) / 0.025)
      } else {
        heroGridOpacity = 0
      }

      gsap.set(heroGridOverlay, {
        opacity: heroGridOpacity,
      })

      let marker1Opacity
      if (self.progress <= 0.5) {
        marker1Opacity = 0
      } else if (self.progress <= 0.525) {
        marker1Opacity = ease((self.progress - 0.5) / 0.025)
      } else if (self.progress <= 0.7) {
        marker1Opacity = 1
      } else if (self.progress <= 0.75) {
        marker1Opacity = 1 - ease((self.progress - 0.7) / 0.05)
      } else {
        marker1Opacity = 0
      }

      gsap.set(marker1, {
        opacity: marker1Opacity,
      })

      let marker2Opacity
      if (self.progress <= 0.55) {
        marker2Opacity = 0
      } else if (self.progress <= 0.575) {
        marker2Opacity = ease((self.progress - 0.55) / 0.025)
      } else if (self.progress <= 0.7) {
        marker2Opacity = 1
      } else if (self.progress <= 0.75) {
        marker2Opacity = 1 - ease((self.progress - 0.7) / 0.05)
      } else {
        marker2Opacity = 0
      }

      gsap.set(marker2, {
        opacity: marker2Opacity,
      })
    },
  })
})

onUnmounted(() => {
  if (rafId) cancelAnimationFrame(rafId)
  rafId = null
  ScrollTrigger.getAll().forEach(t => t.kill())
  lenis?.destroy()
})
</script>
```

Key changes from current script:
- Added imports: `LandingHeader`, `LandingLogin`, `LandingFooter`
- Added `landingLoginRef` ref
- Added `scrollToLogin()` — uses `lenis.scrollTo()` to reach the login section, same pattern as old `HomeView.vue:30-35`
- Added `scrollToTop()` — uses `lenis.scrollTo(0)`, same pattern as old `HomeView.vue:37-39`
- GSAP animation logic in `onMounted` remains completely unchanged

- [ ] **Step 3: Add CTA button style to scoped CSS**

Append to the existing `<style scoped>` block (before the closing `</style>` tag):

```css
.hero-v2-cta {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.6rem;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: var(--hv2-light);
  font-family: 'DM Mono', monospace;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: background 0.25s, border-color 0.25s;
  margin-top: 1.2rem;
}

.hero-v2-cta:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.6);
}
```

- [ ] **Step 4: Build verify**

```bash
cd c:/Users/chutaorui/Desktop/ocean/ocean-web && npx vite build --emptyOutDir false 2>&1 | tail -5
```

Expected: Build completes without errors.

- [ ] **Step 5: Commit**

```bash
cd c:/Users/chutaorui/Desktop/ocean && git add ocean-web/src/views/home/HeroV2View.vue && git commit -m "feat: integrate header, login, footer into HeroV2View with ocean content"
```

---

### Task 2: Route switch (manual, by user decision)

When ready to make HeroV2 the default landing page, change the `/` route component:

In `ocean-web/src/router/index.js` line 7 — this is already done (current state points `/` to HeroV2View).

If not yet switched, ensure:
```js
{
  path: '/',
  name: 'Home',
  component: () => import('../views/home/HeroV2View.vue'),
  meta: { title: '首页', noAuth: true }
},
```

---

### Verification checklist

- [x] LandingHeader visible with logo text "海洋环境预报系统" and register button
- [x] Clicking header logo smooth-scrolls to top
- [x] Hero scroll animation works (mask, grid, markers, content parallax, progress bar)
- [x] "探索系统" CTA button visible in first hero content block
- [x] Clicking CTA smooth-scrolls to login section
- [x] Login form functional (username/password fields, submit button)
- [x] Login API call works and redirects to `/app/dashboard` on success
- [x] Login form shows demo credentials hint
- [x] Footer visible with copyright text
- [x] Already-logged-in users get redirected from `/` to `/app/dashboard`
- [x] Mobile responsive (hero + login + footer all adapt)
- [x] No console errors
