<template>
  <section class="landing-hero" ref="heroRef">
    <!-- Layer 0: Background image (reverse parallax) -->
    <div class="hero-bg" ref="bgRef">
      <img
        class="hero-bg__image"
        src="https://images.unsplash.com/photo-1582967788606-a17106d5b57?w=1920&q=80"
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
            <!-- Black = show image (overlay transparent). White blocks = hide image. -->
            <rect width="1000" height="1000" fill="black"/>
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

const props = defineProps({
  scroller: { type: HTMLElement, default: null }
})

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
const maskBlocks = ref([])
const gridLines = ref([])
const indicatorDots = ref([])

onMounted(() => {
  generateMaskBlocks()
  generateGridLines()
  generateDots()
  initScrollAnimation()
})

onUnmounted(() => {
  scrollTl?.scrollTrigger?.kill()
  scrollTl?.kill()
  scrollTl = null
})

function generateMaskBlocks() {
  const blocks = []
  const S = 1000

  function add(x, y, w, h) {
    blocks.push({ x, y, w, h })
  }

  // Top edge blocks
  const topHeights = [30, 60, 25, 80, 40, 55, 20, 70, 35, 50, 45, 65, 28, 75, 38]
  let tx = 0
  for (let i = 0; i < topHeights.length; i++) {
    const w = 30 + Math.random() * 100
    add(tx, 0, Math.min(w, S - tx), topHeights[i])
    tx += w
    if (tx >= S) break
  }

  // Bottom edge blocks
  const bottomHeights = [40, 55, 25, 70, 35, 60, 30, 80, 45, 50, 28, 65, 38, 72, 32]
  let bx = 0
  for (let i = 0; i < bottomHeights.length; i++) {
    const w = 30 + Math.random() * 100
    const h = bottomHeights[i]
    add(bx, S - h, Math.min(w, S - bx), h)
    bx += w
    if (bx >= S) break
  }

  // Left edge blocks
  const leftWidths = [25, 55, 35, 65, 20, 50, 40, 70, 30, 45, 28, 60, 38, 52, 32]
  let ly = 80
  for (let i = 0; i < leftWidths.length; i++) {
    const h = 30 + Math.random() * 60
    add(0, ly, leftWidths[i], Math.min(h, S - ly))
    ly += h
    if (ly >= S - 80) break
  }

  // Right edge blocks
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
  for (let y = 0; y <= 1000; y += 80) {
    lines.push({ x1: 0, y1: y, x2: 1000, y2: y })
  }
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
  if (!heroRef.value) return

  scrollTl = gsap.timeline({
    scrollTrigger: {
      trigger: heroRef.value,
      scroller: props.scroller,
      start: 'top top',
      end: 'bottom bottom',
      scrub: true,
      invalidateOnRefresh: true,
    },
  })

  // Lock initial states
  gsap.set(maskRef.value, { scale: 1.0 })
  gsap.set(gridRef.value, { opacity: 0 })
  gsap.set(dotsRef.value, { opacity: 0 })
  gsap.set(slide1Ref.value, { opacity: 1 })
  gsap.set(slide2Ref.value, { opacity: 0 })
  gsap.set(slide3Ref.value, { opacity: 0 })
  gsap.set(slide4Ref.value, { opacity: 0 })
  gsap.set(progressFillRef.value, { scaleY: 0 })

  // Phase 1-4: Reverse parallax (full scroll range, duration 1.0 spans entire timeline)
  scrollTl.to(bgRef.value, { yPercent: -15, ease: 'none', duration: 1.0 }, 0)

  // Phase 2 (25%-40%): Mask scale 1.0 -> 0.92 (duration 0.15 = 15% of scroll)
  scrollTl.to(maskRef.value, { scale: 0.92, ease: 'none', duration: 0.15 }, 0.25)
  // Phase 3 (40%-60%): Mask scale 0.92 -> 1.0 (duration 0.20 = 20% of scroll)
  scrollTl.to(maskRef.value, { scale: 1.0, ease: 'none', duration: 0.20 }, 0.40)

  // Phase 2 (25%): Grid + dots appear (instant, duration 0)
  scrollTl.fromTo(gridRef.value, { opacity: 0 }, { opacity: 1, ease: 'none', duration: 0 }, 0.25)
  scrollTl.fromTo(dotsRef.value, { opacity: 0 }, { opacity: 1, ease: 'none', duration: 0 }, 0.25)

  // Phase 3 (40%): Grid + dots disappear (instant)
  scrollTl.to(gridRef.value, { opacity: 0, ease: 'none', duration: 0 }, 0.40)
  scrollTl.to(dotsRef.value, { opacity: 0, ease: 'none', duration: 0 }, 0.40)

  // Text slide crossfades (instant)
  scrollTl.to(slide1Ref.value, { opacity: 0, ease: 'none', duration: 0 }, 0.15)
  scrollTl.fromTo(slide2Ref.value, { opacity: 0 }, { opacity: 1, ease: 'none', duration: 0 }, 0.20)
  scrollTl.to(slide2Ref.value, { opacity: 0, ease: 'none', duration: 0 }, 0.35)
  scrollTl.fromTo(slide3Ref.value, { opacity: 0 }, { opacity: 1, ease: 'none', duration: 0 }, 0.38)
  scrollTl.to(slide3Ref.value, { opacity: 0, ease: 'none', duration: 0 }, 0.52)
  scrollTl.fromTo(slide4Ref.value, { opacity: 0 }, { opacity: 1, ease: 'none', duration: 0 }, 0.55)

  // Phase 4 (60%): Hero fades out (instant)
  scrollTl.to(heroRef.value, { opacity: 0, ease: 'none', duration: 0 }, 0.60)

  // Progress bar: full scroll range
  scrollTl.fromTo(progressFillRef.value, { scaleY: 0 }, { scaleY: 1, ease: 'none', duration: 1.0 }, 0)
}
</script>

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
  opacity: 0;
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
