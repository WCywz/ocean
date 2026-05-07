# Landing Page Hero Animation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the static HomeView with an inversa.com-style scroll-driven landing page featuring a GSAP-animated hero with mask reveal, parallax, desaturation filter, and grid overlay, ending with login form.

**Architecture:** Single long-scrolling page (HomeView.vue) composed of 4 child components: LandingHeader (fixed nav), LandingHero (GSAP scroll animation core), LandingLogin (dark-themed login form), LandingFooter. Lenis provides smooth scrolling. GSAP ScrollTrigger drives 5 animation phases mapped to scroll progress. Landing page uses its own dark CSS variables scoped to a `.landing` wrapper class, isolated from the editorial light theme used by the rest of the app.

**Tech Stack:** Vue 3 (Composition API), GSAP (ScrollTrigger), Lenis, Vue Router

---

### Task 1: Install dependencies

**Files:**
- Modify: `ocean-web/package.json`

- [ ] **Step 1: Install gsap and lenis**

```bash
cd ocean-web && pnpm add gsap lenis
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/package.json ocean-web/pnpm-lock.yaml
git commit -m "chore: add gsap and lenis dependencies"
```

---

### Task 2: Add JetBrains Mono font and landing page CSS variables

**Files:**
- Create: `ocean-web/src/styles/landing.css`

- [ ] **Step 1: Create landing.css with font-face and dark theme variables**

Write `ocean-web/src/styles/landing.css`:

```css
/* ===== Landing Page (inversa.com-style) Design Tokens ===== */

@font-face {
  font-display: swap;
  font-family: 'JetBrains Mono';
  font-style: normal;
  font-weight: 700;
  src: url('https://cdn.jsdelivr.net/npm/@fontsource/jetbrains-mono@5.0.20/files/jetbrains-mono-latin-700-normal.woff2') format('woff2');
}
@font-face {
  font-display: swap;
  font-family: 'JetBrains Mono';
  font-style: normal;
  font-weight: 400;
  src: url('https://cdn.jsdelivr.net/npm/@fontsource/jetbrains-mono@5.0.20/files/jetbrains-mono-latin-400-normal.woff2') format('woff2');
}
@font-face {
  font-display: swap;
  font-family: 'JetBrains Mono';
  font-style: normal;
  font-weight: 300;
  src: url('https://cdn.jsdelivr.net/npm/@fontsource/jetbrains-mono@5.0.20/files/jetbrains-mono-latin-300-normal.woff2') format('woff2');
}

.landing {
  --color-black: #13140e;
  --color-black-2: #181813;
  --color-creme: #f4f3e8;
  --color-grey: #595a51;
  --color-grey-2: #404040;
  --color-yellow: #ebfc72;
  --color-red: #f1664d;
  --color-green: #00d399;
  --color-purple: #c084fc;
  --ease-in-out-quart: cubic-bezier(.77, 0, .175, 1);
  --ease-out-cubic: cubic-bezier(.215, .61, .355, 1);

  position: fixed;
  inset: 0;
  overflow: hidden;
  background: var(--color-black);
  color: var(--color-creme);
  font-family: 'JetBrains Mono', monospace;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.landing *,
.landing *::before,
.landing *::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

.landing a {
  color: inherit;
  text-decoration: none;
}

.landing button {
  background: none;
  border: 0;
  color: inherit;
  font: inherit;
  cursor: pointer;
  padding: 0;
}

.landing strong {
  font-weight: 400;
}

.landing img,
.landing svg {
  display: block;
  user-select: none;
}

.landing svg {
  overflow: visible;
}

/* Heading style — serif fallback for large display text */
.landing .hero-heading {
  font-family: Georgia, 'Times New Roman', serif;
  font-weight: 400;
  letter-spacing: -0.03em;
  line-height: 0.9;
  text-wrap: balance;
}

@media (max-width: 600px) {
  .landing .hero-heading {
    font-size: 4.6rem;
    letter-spacing: -0.0217391304em;
    line-height: 1;
    margin-bottom: 1.2rem;
  }
}

@media (min-width: 601px) {
  .landing .hero-heading {
    font-size: 8rem;
    margin-bottom: 1.6rem;
  }
}

/* Blinking dot for section titles */
.landing .section-title {
  display: flex;
  align-items: center;
  font-weight: 300;
  text-transform: uppercase;
  margin-bottom: 2rem;
}

@media (max-width: 600px) {
  .landing .section-title {
    font-size: 1.6rem;
    line-height: 1.25;
  }
}

@media (min-width: 601px) {
  .landing .section-title {
    font-size: 2rem;
    line-height: 1.25;
  }
}

.landing .section-title.is-blinker::before {
  content: '';
  animation: landing-flash 1s step-start 0s infinite;
  background: currentColor;
  display: block;
  height: 0.4rem;
  margin-right: 1.2rem;
  width: 0.4rem;
}

@keyframes landing-flash {
  0% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}

/* Body text */
.landing .body-text {
  font-family: Georgia, 'Times New Roman', serif;
  font-weight: 400;
}

@media (max-width: 600px) {
  .landing .body-text {
    font-size: 1.6rem;
    line-height: 1.62;
  }
}

@media (min-width: 601px) {
  .landing .body-text {
    font-size: 2rem;
    line-height: 1.25;
  }
}

/* Yellow CTA button with angled clip-path */
.landing .cta-button {
  display: inline-flex;
  align-items: center;
  background: var(--color-yellow);
  border-radius: 0.4rem;
  clip-path: polygon(0 0, 100% 0, 100% calc(100% - 1.5rem), calc(100% - 1.5rem) 100%, 0 100%, 0 0);
  color: var(--color-black);
  font-family: 'JetBrains Mono', monospace;
  font-size: 1.4rem;
  font-weight: 300;
  height: 4.5rem;
  line-height: 1.28;
  padding: 0 2rem;
  text-transform: uppercase;
  transition: clip-path 0.25s ease-out;
  pointer-events: all;
}

.landing .cta-button:hover {
  clip-path: polygon(1.5rem 0, 100% 0, 100% 100%, 100% 100%, 0 100%, 0 1.5rem);
}

/* Underline link with hover expand */
.landing .text-link {
  color: var(--color-yellow);
  position: relative;
}

.landing .text-link::after {
  background: currentColor;
  content: '';
  height: 1px;
  position: absolute;
  inset: auto auto 0 0;
  transform: scaleX(0);
  transform-origin: right center;
  transition: transform 0.5s cubic-bezier(1, 0, 0, 1);
  width: 100%;
}

.landing .text-link:hover::after {
  transform: scaleX(1);
  transform-origin: left center;
}
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/styles/landing.css
git commit -m "feat: add landing page dark theme and JetBrains Mono font"
```

---

### Task 3: Create LandingHeader.vue

**Files:**
- Create: `ocean-web/src/views/home/LandingHeader.vue`

- [ ] **Step 1: Create the header component**

Write `ocean-web/src/views/home/LandingHeader.vue`:

```vue
<template>
  <header class="landing-header">
    <a class="landing-header__logo" @click="$emit('scrollToTop')">海洋环境预报系统</a>
    <div class="landing-header__links">
      <button class="landing-header__link" @click="$router.push('/register')">注册</button>
    </div>
  </header>
</template>

<script setup>
defineEmits(['scrollToTop'])
</script>

<style scoped>
.landing-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: fixed;
  inset: 0 0 auto;
  z-index: 50;
  pointer-events: none;
  padding: 1.6rem;
  font-family: Georgia, 'Times New Roman', serif;
}

.landing-header__logo {
  pointer-events: auto;
  color: var(--color-creme);
  font-size: 1.6rem;
  cursor: pointer;
  transition: color 0.25s linear;
}

.landing-header__links {
  display: flex;
  gap: 2rem;
}

.landing-header__link {
  pointer-events: auto;
  color: var(--color-yellow);
  font-size: 1.4rem;
  font-weight: 300;
  text-transform: uppercase;
  text-decoration: underline;
  text-underline-offset: 4px;
  transition: color 0.25s linear;
  cursor: pointer;
}

@media (max-width: 600px) {
  .landing-header {
    padding: 1.2rem;
  }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/home/LandingHeader.vue
git commit -m "feat: add LandingHeader component"
```

---

### Task 4: Create LandingHero shell (HTML structure + static layers)

**Files:**
- Create: `ocean-web/src/views/home/LandingHero.vue`

- [ ] **Step 1: Create the hero component with all layer DOM**

Write `ocean-web/src/views/home/LandingHero.vue`:

```vue
<template>
  <section class="landing-hero" ref="heroRef">
    <!-- Scroll container (pin target) -->
    <div class="landing-hero__wrapper" ref="wrapperRef">
      <!-- Layer 0: Background images -->
      <div class="landing-hero__bg">
        <img
          ref="bgImgARef"
          class="landing-hero__bg-img landing-hero__bg-img--a"
          src="https://images.unsplash.com/photo-1582967788606-a171d06d5b57?w=1920&q=80"
          alt="Ocean surface"
        />
        <img
          ref="bgImgBRef"
          class="landing-hero__bg-img landing-hero__bg-img--b"
          src="https://images.unsplash.com/photo-1457365050282-c53d772ef8b2?w=1920&q=80"
          alt="Ocean restoration"
        />
      </div>

      <!-- Layer 1: SVG Mask container -->
      <div class="landing-hero__mask" ref="maskRef">
        <!-- Layer 2: Grid overlay -->
        <div class="landing-hero__grid" ref="gridRef">
          <svg viewBox="0 0 1080 748" fill="none" xmlns="http://www.w3.org/2000/svg" class="landing-hero__grid-svg">
            <path fill-rule="evenodd" clip-rule="evenodd" d="M582 83H830V0H914V83H997V249H1080V416H997V498H1080V582H997V665H831V748H747V665H665V748H498V665H250V748H166V665H0V498H83V333H0V166H83V83H332V0H582V83ZM167 747H249V665H167V747ZM499 747H581V665H499V747ZM582 747H664V665H582V747ZM748 747H830V665H748V747ZM1 664H83V582H1V664ZM84 664H166V582H84V664ZM167 664H249V582H167V664ZM250 664H332V582H250V664ZM333 664H415V582H333V664ZM416 664H498V582H416V664ZM499 664H581V582H499V664ZM582 664H664V582H582V664ZM665 664H747V582H665V664ZM748 664H830V582H748V664ZM831 664H913V582H831V664ZM914 664H996V582H914V664ZM1 581H83V499H1V581ZM84 581H166V499H84V581ZM167 581H249V499H167V581ZM250 581H332V499H250V581ZM333 581H415V499H333V581ZM416 581H498V499H416V581ZM499 581H581V499H499V581ZM582 581H664V499H582V581ZM665 581H747V499H665V581ZM748 581H830V499H748V581ZM831 581H913V499H831V581ZM914 581H996V499H914V581ZM997 581H1079V499H997V581ZM84 498H166V416H84V498ZM167 498H249V416H167V498ZM250 498H332V416H250V498ZM333 498H415V416H333V498ZM416 498H498V416H416V498ZM499 498H581V416H499V498ZM582 498H664V416H582V498ZM665 498H747V416H665V498ZM748 498H830V416H748V498ZM831 498H913V416H831V498ZM914 498H996V416H914V498ZM84 415H166V333H84V415ZM167 415H249V333H167V415ZM250 415H332V333H250V415ZM333 415H415V333H333V415ZM416 415H498V333H416V415ZM499 415H581V333H499V415ZM582 415H664V333H582V415ZM665 415H747V333H665V415ZM748 415H830V333H748V415ZM831 415H913V333H831V415ZM914 415H996V333H914V415ZM997 415H1079V333H997V415ZM1 332H83V250H1V332ZM84 332H166V250H84V332ZM167 332H249V250H167V332ZM250 332H332V250H250V332ZM333 332H415V250H333V332ZM416 332H498V250H416V332ZM499 332H581V250H499V332ZM582 332H664V250H582V332ZM665 332H747V250H665V332ZM748 332H830V250H748V332ZM831 332H913V250H831V332ZM914 332H996V250H914V332ZM997 332H1079V250H997V332ZM1 249H83V167H1V249ZM84 249H166V167H84V249ZM167 249H249V167H167V249ZM250 249H332V167H250V249ZM333 249H415V167H333V249ZM416 249H498V167H416V249ZM499 249H581V167H499V249ZM582 249H664V167H582V249ZM665 249H747V167H665V249ZM748 249H830V167H748V249ZM831 249H913V167H831V249ZM914 249H996V167H914V249ZM84 166H166V84H84V166ZM167 166H249V84H167V166ZM250 166H332V84H250V166ZM333 166H415V84H333V166ZM416 166H498V84H416V166ZM499 166H581V84H499V166ZM582 166H664V84H582V166ZM665 166H747V84H665V166ZM748 166H830V84H748V166ZM831 166H913V84H831V166ZM914 166H996V84H914V166ZM333 83H415V1H333V83ZM416 83H498V1H416V83ZM499 83H581V1H499V83ZM831 83H913V1H831V83Z" fill="color-mix(in srgb, currentColor 30%, transparent)"/>
          </svg>
        </div>

        <!-- Layer 3: Desaturation filter -->
        <div class="landing-hero__filter" ref="filterRef"></div>

        <!-- Layer 4: Hotspot indicators (decorative) -->
        <div class="landing-hero__hotspots">
          <svg viewBox="0 0 764 542" fill="none" xmlns="http://www.w3.org/2000/svg" class="landing-hero__hotspots-svg" ref="hotspotsSvgRef">
            <g class="hotspot-group">
              <circle cx="291" cy="326" r="3" fill="#F1664D"/>
              <circle cx="580" cy="180" r="3" fill="#F1664D"/>
              <circle cx="128" cy="410" r="3" fill="#F1664D"/>
              <circle cx="650" cy="350" r="3" fill="#F1664D"/>
              <circle cx="450" cy="450" r="3" fill="#F1664D"/>
              <circle cx="200" cy="200" r="3" fill="#F1664D"/>
            </g>
            <g class="specialist-group">
              <circle cx="350" cy="280" r="3" fill="#EBFC72"/>
              <circle cx="500" cy="150" r="3" fill="#EBFC72"/>
              <circle cx="300" cy="400" r="3" fill="#EBFC72"/>
              <circle cx="550" cy="300" r="3" fill="#EBFC72"/>
            </g>
          </svg>
        </div>
      </div>

      <!-- Layer 5: Text slides -->
      <div class="landing-hero__slides">
        <div class="landing-hero__slide" ref="slide1Ref">
          <h2 class="hero-heading">Invasions move fast.<br/>Be faster.</h2>
          <a class="cta-button" @click="$emit('scrollToLogin')">探索系统</a>
        </div>
        <div class="landing-hero__slide" ref="slide2Ref">
          <h3 class="section-title is-blinker">海洋环境监测</h3>
          <p class="body-text">实时追踪海表温度与叶绿素浓度变化，为海洋科学研究与环境保护提供精准预报数据。</p>
        </div>
        <div class="landing-hero__slide" ref="slide3Ref">
          <h3 class="section-title is-blinker">数据驱动决策</h3>
          <p class="body-text">集成多种海洋预报模型，支持参数配置与状态监控，让数据成为科学决策的基础。</p>
        </div>
        <div class="landing-hero__slide" ref="slide4Ref">
          <h3 class="section-title is-blinker">生态修复</h3>
          <p class="body-text">通过精准的海洋环境预报，助力生态保护与可持续发展，为海洋未来贡献力量。</p>
        </div>
      </div>

      <!-- Right progress indicator -->
      <div class="landing-hero__indicator" ref="indicatorRef">
        <div class="landing-hero__indicator-progress" ref="indicatorProgressRef"></div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import gsap from 'gsap'
import ScrollTrigger from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

const emit = defineEmits(['scrollToLogin'])

const heroRef = ref(null)
const wrapperRef = ref(null)
const bgImgARef = ref(null)
const bgImgBRef = ref(null)
const maskRef = ref(null)
const gridRef = ref(null)
const filterRef = ref(null)
const hotspotsSvgRef = ref(null)
const slide1Ref = ref(null)
const slide2Ref = ref(null)
const slide3Ref = ref(null)
const slide4Ref = ref(null)
const indicatorRef = ref(null)
const indicatorProgressRef = ref(null)

let scrollTrigger = null

onMounted(() => {
  const tl = gsap.timeline({
    scrollTrigger: {
      trigger: heroRef.value,
      start: 'top top',
      end: 'bottom bottom',
      scrub: 1,
      pin: wrapperRef.value,
      anticipatePin: 1,
      onUpdate: (self) => {
        if (indicatorProgressRef.value) {
          gsap.set(indicatorProgressRef.value, { scaleY: self.progress })
        }
      }
    }
  })

  // Phase 1 (0%–25%): Background parallax reverse + progress bar
  tl.to(bgImgARef.value, { yPercent: -10, ease: 'none' }, 0)
    .to(bgImgBRef.value, { yPercent: 5, ease: 'none' }, 0)
    .to(slide1Ref.value, { opacity: 1, ease: 'none' }, 0)
    .to(slide1Ref.value, { opacity: 0, ease: 'none' }, 0.2)

  // Phase 2 (15%–35%): Mask scale
  tl.to(maskRef.value, { scale: 0.92, ease: 'none' }, 0.15)
    .to(slide2Ref.value, { opacity: 1, ease: 'none' }, 0.18)
    .to(slide2Ref.value, { opacity: 0, ease: 'none' }, 0.35)

  // Phase 3 (30%–55%): Desaturation + grid appear
  tl.to(filterRef.value, { opacity: 1, ease: 'none' }, 0.3)
    .to(gridRef.value, { opacity: 1, ease: 'none' }, 0.32)
    .to(hotspotsSvgRef.value, { opacity: 1, ease: 'none' }, 0.32)
    .to(slide3Ref.value, { opacity: 1, ease: 'none' }, 0.35)
    .to(slide3Ref.value, { opacity: 0, ease: 'none' }, 0.5)

  // Phase 4 (48%–70%): Release — mask back, desaturation fades, grid fades
  tl.to(maskRef.value, { scale: 1, ease: 'none' }, 0.48)
    .to(filterRef.value, { opacity: 0, ease: 'none' }, 0.5)
    .to(gridRef.value, { opacity: 0, ease: 'none' }, 0.52)
    .to(hotspotsSvgRef.value, { opacity: 0, ease: 'none' }, 0.52)
    .to(slide4Ref.value, { opacity: 1, ease: 'none' }, 0.55)
    .to(slide4Ref.value, { opacity: 0, ease: 'none' }, 0.72)

  // Phase 5 (70%–100%): Hero fade out, transition to login
  tl.to(wrapperRef.value, { opacity: 0, ease: 'none' }, 0.75)

  scrollTrigger = tl.scrollTrigger
})

onUnmounted(() => {
  if (scrollTrigger) scrollTrigger.kill()
})
</script>

<style scoped>
.landing-hero {
  height: 400svh;
  position: relative;
  contain: paint;
}

.landing-hero__wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100svh;
  position: sticky;
  top: 0;
  background-image: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 140 140'%3E%3Cpath fill='%23f4f3e8' d='M0 0h3.5v3.5H0z' opacity='.2'/%3E%3C/svg%3E");
  background-position: 50%;
  background-repeat: repeat;
  background-size: 14rem;
  pointer-events: none;
}

.landing-hero__bg {
  position: absolute;
  inset: 0;
  z-index: -1;
}

.landing-hero__bg-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.landing-hero__bg-img--a {
  z-index: 1;
}

.landing-hero__bg-img--b {
  transform: scale(-1, 1);
}

/* SVG Mask container */
.landing-hero__mask {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100svh;
  inset: 0;
  mask-image: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 1030 701'%3E%3Cpath fill='currentColor' d='M347 0v21h60V0h43v21h44v21h121V21h31V0h117v21h59V0h107v21h59v62h21v42h21v36h-21v38h-21v96h21v14h21v46h-21v21h21v38h-21v50h21v79h-21v74h21v24h-21v20H886v-20h-41v20h-40v-20h-21v20h-42v20h-41v20H559v-20h-23v-20h-50v-20h-41v20H330v-20h-21v20h-52v20h-24v20h-47v-20h-21v20h-21v-20h-21v-20H40v-41H20v-62h20v-46H20v-58h20v-20H20v-20H0V240h20v-53H0v-62h20V63h20V42h41v21h83V42h21V21h85V0z'/%3E%3C/svg%3E");
  -webkit-mask-image: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 1030 701'%3E%3Cpath fill='currentColor' d='M347 0v21h60V0h43v21h44v21h121V21h31V0h117v21h59V0h107v21h59v62h21v42h21v36h-21v38h-21v96h21v14h21v46h-21v21h21v38h-21v50h21v79h-21v74h21v24h-21v20H886v-20h-41v20h-40v-20h-21v20h-42v20h-41v20H559v-20h-23v-20h-50v-20h-41v20H330v-20h-21v20h-52v20h-24v20h-47v-20h-21v20h-21v-20h-21v-20H40v-41H20v-62h20v-46H20v-58h20v-20H20v-20H0V240h20v-53H0v-62h20V63h20V42h41v21h83V42h21V21h85V0z'/%3E%3C/svg%3E");
  mask-position: center;
  -webkit-mask-position: center;
  mask-repeat: no-repeat;
  -webkit-mask-repeat: no-repeat;
  will-change: transform;
}

@media (max-width: 600px) {
  .landing-hero__mask {
    mask-size: 57.6rem 39.2rem;
    -webkit-mask-size: 57.6rem 39.2rem;
  }
}

@media (min-width: 601px) {
  .landing-hero__mask {
    mask-size: 103rem 70.1rem;
    -webkit-mask-size: 103rem 70.1rem;
  }
}

/* Grid overlay */
.landing-hero__grid {
  position: absolute;
  opacity: 0;
  will-change: opacity;
}

.landing-hero__grid-svg {
  color: rgba(244, 243, 232, 0.3);
  transform: translate(1px, 1px);
}

@media (max-width: 600px) {
  .landing-hero__grid-svg {
    height: 41.8rem;
    width: 60.4rem;
  }
}

@media (min-width: 601px) {
  .landing-hero__grid-svg {
    height: 74.5rem;
    width: 107.6rem;
  }
}

/* Desaturation filter */
.landing-hero__filter {
  position: absolute;
  inset: 0;
  background: #b0b3b4;
  mix-blend-mode: color;
  opacity: 0;
  will-change: opacity;
}

/* Hotspots */
.landing-hero__hotspots {
  position: absolute;
  inset: 0;
}

.landing-hero__hotspots-svg {
  position: absolute;
  opacity: 0;
}

@media (max-width: 600px) {
  .landing-hero__hotspots-svg {
    height: 30.3rem;
    width: 42.8rem;
  }
}

@media (min-width: 601px) {
  .landing-hero__hotspots-svg {
    height: 54.2rem;
    width: 76.4rem;
  }
}

/* Text slides */
.landing-hero__slides {
  position: absolute;
  inset: 0;
}

.landing-hero__slide {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  height: 100svh;
  width: 100%;
  opacity: 0;
  will-change: opacity;
  text-align: center;
}

.landing-hero__slide:first-child .hero-heading,
.landing-hero__slide:first-child .cta-button {
  position: absolute;
}

@media (max-width: 600px) {
  .landing-hero__slide:first-child .hero-heading {
    bottom: 1.2rem;
  }
}

@media (min-width: 601px) {
  .landing-hero__slide:first-child .hero-heading {
    bottom: 1.6rem;
    left: 1.6rem;
  }
}

@media (min-width: 601px) {
  .landing-hero__slide:nth-child(2),
  .landing-hero__slide:nth-child(4) {
    align-items: flex-end;
    padding-right: 8.2rem;
    width: 39rem;
    right: 0;
  }
  .landing-hero__slide:nth-child(3) {
    align-items: flex-start;
    padding-left: 8.2rem;
    width: 38rem;
    left: 0;
  }
}

@media (max-width: 600px) {
  .landing-hero__slide {
    padding: 0 1.2rem;
    bottom: 5rem;
    justify-content: flex-end;
  }
}

/* Right progress indicator */
.landing-hero__indicator {
  position: absolute;
  background: rgba(244, 243, 232, 0.5);
  width: 1px;
  z-index: 3;
}

@media (max-width: 600px) {
  .landing-hero__indicator {
    height: 9rem;
    right: 1.2rem;
  }
}

@media (min-width: 601px) {
  .landing-hero__indicator {
    height: 18.2rem;
    right: 6rem;
  }
}

.landing-hero__indicator-progress {
  position: absolute;
  inset: 0;
  background: var(--color-yellow);
  transform: scaleY(0);
  transform-origin: top;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/home/LandingHero.vue
git commit -m "feat: add LandingHero with GSAP scroll animation and mask layers"
```

---

### Task 5: Create LandingLogin.vue

**Files:**
- Create: `ocean-web/src/views/home/LandingLogin.vue`

- [ ] **Step 1: Create the login section — restyled with dark theme, reuses login logic**

Write `ocean-web/src/views/home/LandingLogin.vue`:

```vue
<template>
  <section class="landing-login" id="login-section" ref="loginRef">
    <div class="landing-login__form">
      <div class="landing-login__header">
        <h2 class="landing-login__title">登录</h2>
        <p class="landing-login__subtitle">海洋环境预报系统</p>
      </div>

      <div class="landing-login__fields">
        <input
          v-model="form.username"
          class="landing-login__input"
          placeholder="用户名"
          @keyup.enter="handleLogin"
        />
        <input
          v-model="form.password"
          class="landing-login__input"
          type="password"
          placeholder="密码"
          @keyup.enter="handleLogin"
        />
        <button
          class="cta-button landing-login__submit"
          :disabled="loading"
          @click="handleLogin"
        >
          {{ loading ? '...' : '登 录' }}
        </button>
      </div>

      <div class="landing-login__footer">
        <span class="landing-login__hint">还没有账号？</span>
        <a class="text-link" @click="$router.push('/register')">注册</a>
      </div>

      <div class="landing-login__creds">
        管理员: admin / admin123 &nbsp;·&nbsp; 用户: user / user123
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import { login } from '../../api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const loginRef = ref(null)

const form = reactive({
  username: 'admin',
  password: 'admin123'
})

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

defineExpose({ loginRef })
</script>

<style scoped>
.landing-login {
  height: 100svh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-black);
}

.landing-login__form {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2.8rem;
}

.landing-login__header {
  text-align: center;
}

.landing-login__title {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 2.4rem;
  font-weight: 400;
  color: var(--color-creme);
  margin: 0 0 0.4rem 0;
}

.landing-login__subtitle {
  font-size: 1.1rem;
  color: rgba(244, 243, 232, 0.5);
  margin: 0;
}

.landing-login__fields {
  display: flex;
  flex-direction: column;
  gap: 1.6rem;
  width: 28rem;
}

.landing-login__input {
  background: transparent;
  border: none;
  border-bottom: 1px solid var(--color-grey-2);
  padding: 1rem 0;
  color: var(--color-creme);
  font-family: 'JetBrains Mono', monospace;
  font-size: 1.4rem;
  outline: none;
  transition: border-color 0.2s;
}

.landing-login__input::placeholder {
  color: var(--color-grey);
}

.landing-login__input:focus {
  border-bottom-color: var(--color-yellow);
}

.landing-login__submit {
  width: 100%;
  justify-content: center;
  margin-top: 0.8rem;
}

.landing-login__submit:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.landing-login__footer {
  font-size: 1.2rem;
}

.landing-login__hint {
  color: rgba(244, 243, 232, 0.5);
  margin-right: 0.4rem;
}

.landing-login__creds {
  font-size: 1.1rem;
  color: rgba(244, 243, 232, 0.3);
  text-align: center;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/home/LandingLogin.vue
git commit -m "feat: add LandingLogin with dark-themed login form"
```

---

### Task 6: Create LandingFooter.vue

**Files:**
- Create: `ocean-web/src/views/home/LandingFooter.vue`

- [ ] **Step 1: Create minimal footer**

Write `ocean-web/src/views/home/LandingFooter.vue`:

```vue
<template>
  <footer class="landing-footer">
    <div class="landing-footer__content">
      <p class="landing-footer__text">Ocean Environment Forecast System &copy; 2026</p>
    </div>
  </footer>
</template>

<style scoped>
.landing-footer {
  border-top: 1px solid var(--color-grey-2);
  padding: 2rem 1.6rem;
}

.landing-footer__content {
  text-align: center;
}

.landing-footer__text {
  font-size: 1rem;
  color: rgba(244, 243, 232, 0.3);
  margin: 0;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/home/LandingFooter.vue
git commit -m "feat: add LandingFooter component"
```

---

### Task 7: Rewrite HomeView.vue as container with Lenis scroll

**Files:**
- Modify: `ocean-web/src/views/home/HomeView.vue`

- [ ] **Step 1: Rewrite HomeView — Lenis scroll container assembling all landing components**

Write `ocean-web/src/views/home/HomeView.vue`:

```vue
<template>
  <div class="landing">
    <LandingHeader @scroll-to-top="scrollToTop" />
    <main class="landing__main" ref="mainRef">
      <LandingHero ref="heroRef" @scroll-to-login="scrollToLogin" />
      <LandingLogin ref="loginRef" />
      <LandingFooter />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import Lenis from 'lenis'
import gsap from 'gsap'
import ScrollTrigger from 'gsap/ScrollTrigger'
import LandingHeader from './LandingHeader.vue'
import LandingHero from './LandingHero.vue'
import LandingLogin from './LandingLogin.vue'
import LandingFooter from './LandingFooter.vue'

gsap.registerPlugin(ScrollTrigger)

const mainRef = ref(null)
const heroRef = ref(null)
const loginRef = ref(null)

let lenis = null

function scrollToLogin() {
  if (lenis && loginRef.value) {
    const loginEl = loginRef.value.loginRef || loginRef.value.$el
    lenis.scrollTo(loginEl)
  }
}

function scrollToTop() {
  if (lenis) lenis.scrollTo(0)
}

onMounted(() => {
  lenis = new Lenis({
    wrapper: mainRef.value,
    content: mainRef.value,
    lerp: 0.1,
    smoothWheel: true
  })

  function raf(time) {
    lenis.raf(time)
    requestAnimationFrame(raf)
  }
  requestAnimationFrame(raf)

  // Sync Lenis with GSAP ScrollTrigger
  lenis.on('scroll', ScrollTrigger.update)
  gsap.ticker.add((time) => lenis.raf(time * 1000))
  gsap.ticker.lagSmoothing(0)
})

onUnmounted(() => {
  if (lenis) lenis.destroy()
})
</script>

<style scoped>
.landing__main {
  position: absolute;
  inset: 0;
  overflow: hidden;
  overflow-y: auto;
  z-index: 2;
}

@media (min-width: 601px) {
  .landing__main {
    border-right: 1px solid var(--color-grey-2);
  }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/home/HomeView.vue
git commit -m "feat: rewrite HomeView as Lenis scroll container with landing components"
```

---

### Task 8: Update router — remove /login route, redirect to /

**Files:**
- Modify: `ocean-web/src/router/index.js`

- [ ] **Step 1: Remove /login route, add redirect, update register's login link target**

Read the current file, then replace the routes array with:

Write `ocean-web/src/router/index.js`:

```js
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/home/HomeView.vue'),
    meta: { title: '首页', noAuth: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/register/RegisterView.vue'),
    meta: { title: '注册', noAuth: true }
  },
  {
    path: '/app',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/app/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/DashboardView.vue'),
        meta: { title: '首页仪表盘' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('../views/user/UserView.vue'),
        meta: { title: '用户管理', role: 'ADMIN' }
      },
      {
        path: 'model',
        name: 'Model',
        component: () => import('../views/model/ModelView.vue'),
        meta: { title: '预报模型管理', role: 'ADMIN' }
      },
      {
        path: 'forecast/sst',
        name: 'ForecastSst',
        component: () => import('../views/forecast/SstMapView.vue'),
        meta: { title: '海表温度预测' }
      },
      {
        path: 'forecast/chl',
        name: 'ForecastCHL',
        component: () => import('../views/forecast/ChxMapView.vue'),
        meta: { title: '叶绿素预测' }
      },
      {
        path: 'forecast/history',
        name: 'ForecastHistory',
        component: () => import('../views/forecast/HistoryView.vue'),
        meta: { title: '历史预报记录' }
      },
      {
        path: 'ocean-data',
        name: 'OceanData',
        component: () => import('../views/ocean/OceanDataView.vue'),
        meta: { title: '海洋观测数据' }
      }
    ]
  },
  {
    path: '/login',
    redirect: '/'
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `海洋环境预报系统 - ${to.meta.title}` : '海洋环境预报系统'

  const token = localStorage.getItem('token')

  if (to.meta.noAuth) {
    if (token && (to.path === '/login' || to.path === '/register' || to.path === '/')) {
      next('/app/dashboard')
      return
    }
    next()
    return
  }

  if (!token) {
    next('/')
    return
  }

  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
  if (to.meta.role === 'ADMIN' && userInfo?.role !== 'ADMIN') {
    next('/app/dashboard')
    return
  }

  next()
})

export default router
```

- [ ] **Step 2: Update RegisterView.vue — change "去登录" link target from '/login' to '/'**

Read `ocean-web/src/views/register/RegisterView.vue`, then replace the "去登录" link:

Change line 34 from `@click="$router.push('/login')"` to `@click="$router.push('/')"`.

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/router/index.js ocean-web/src/views/register/RegisterView.vue
git commit -m "feat: update router — remove /login, redirect to landing page"
```

---

### Task 9: Import landing.css in App.vue

**Files:**
- Modify: `ocean-web/src/App.vue`

- [ ] **Step 1: Add landing.css import**

Read `ocean-web/src/App.vue`, then add the landing CSS import alongside the existing editorial.css import.

Replace the `<style>` block with:

```css
<style>
@import './styles/editorial.css';
@import './styles/landing.css';

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
git commit -m "feat: import landing page CSS in App.vue"
```

---

### Task 10: Test the dev server and verify animations

- [ ] **Step 1: Start dev server**

```bash
cd ocean-web && pnpm dev
```

- [ ] **Step 2: Verify the page loads without errors**

Open the browser at the dev server URL (usually http://localhost:5173). Confirm:
- Page loads with dark background
- Header shows "海洋环境预报系统" logo and "注册" link
- Background images are visible through the SVG mask window
- Scroll down — the hero animation plays: background parallax → mask scale → desaturation + grid → release
- Progress bar on the right tracks scroll
- Login form appears at the bottom
- Login works and redirects to /app/dashboard
- If already logged in (token exists), redirects to /app/dashboard automatically
- Mobile viewport: mask size adjusts, no custom cursor needed

- [ ] **Step 3: Fix any issues found, then commit**

```bash
git add ocean-web/src/
git commit -m "fix: landing page dev server verification fixes"
```
