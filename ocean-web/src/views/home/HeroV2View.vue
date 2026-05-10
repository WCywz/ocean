<template>
  <div class="hero-v2-page">
    <LandingHeader @scroll-to-top="scrollToTop" />

    <div ref="lenisWrapperRef" class="hero-v2-root">
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

      <div class="hero-v2-login-area">
        <LandingLogin ref="landingLoginRef" />
        <LandingFooter />
      </div>
    </div>
  </div>
  </div>
</template>

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

<style scoped>
.hero-v2-page {
  --color-creme: #f4f3e8;
  --color-yellow: #ebfc72;
  --color-grey-2: #404040;
}

.hero-v2-root {
  --hv2-light: #fff;
  --hv2-dark: #141414;
  --hv2-accent-1: #dc5935;
  --hv2-accent-2: #d3ef76;

  font-family: 'DM Sans', sans-serif;
  background-color: var(--hv2-dark);
  color: var(--hv2-light);
  position: fixed;
  inset: 0;
  overflow: hidden;
}

.hero-v2-root::-webkit-scrollbar {
  display: none;
}

.hero-v2-root img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-v2-root h1,
.hero-v2-root h2 {
  font-weight: 400;
  line-height: 1.1;
}

.hero-v2-root h1 {
  font-size: clamp(3rem, 4vw, 5rem);
}

.hero-v2-root h2 {
  font-size: clamp(1.5rem, 2.25vw, 3rem);
}

.hero-v2-root p {
  font-size: 1.125rem;
  font-weight: 400;
  line-height: 1.4;
}

.hero-v2-root section {
  position: relative;
  width: 100%;
  height: 100svh;
  background-color: var(--hv2-dark);
  overflow: hidden;
}

.hero-v2-outro {
  display: flex;
  justify-content: center;
  align-items: center;
}

.hero-v2-img {
  position: absolute;
  bottom: 0;
  width: 100%;
  height: 200svh;
  --hv2-overlay-opacity: 0.35;
  will-change: transform;
}

.hero-v2-img::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: var(--hv2-dark);
  opacity: var(--hv2-overlay-opacity);
  will-change: opacity;
}

.hero-v2-img img {
  will-change: filter;
}

.hero-v2-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100svh;
  background-color: var(--hv2-dark);
  mask: linear-gradient(var(--hv2-light), var(--hv2-light)),
    url('/hero-v2/hero-v2-mask.svg') center/50% no-repeat;
  -webkit-mask: linear-gradient(var(--hv2-light), var(--hv2-light)),
    url('/hero-v2/hero-v2-mask.svg') center/50% no-repeat;
  mask-composite: subtract;
  -webkit-mask-composite: xor;
  will-change: transform;
  pointer-events: none;
}

.hero-v2-grid-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 55%;
  will-change: opacity;
}

.hero-v2-grid-overlay img {
  opacity: 0.25;
}

.marker {
  position: absolute;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  gap: 1rem;
  will-change: opacity;
}

.marker-1 {
  top: 50svh;
  left: 50vw;
}

.marker-2 {
  top: 35svh;
  left: 60vw;
}

.marker .marker-label {
  text-transform: uppercase;
  font-family: 'DM Mono';
  font-size: 0.7rem;
  font-weight: 500;
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
}

.marker .marker-icon {
  position: relative;
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 2rem;
}

.marker .marker-icon::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 10rem;
  height: 10rem;
  border-radius: 100%;
  animation: hero-v2-pulse 1.5s cubic-bezier(0.2, 0.6, 0.35, 1) infinite;
}

.marker.marker-1 .marker-icon,
.marker.marker-1 .marker-icon::before,
.marker.marker-1 .marker-label {
  background-color: var(--hv2-accent-1);
  color: var(--hv2-light);
}

.marker.marker-2 .marker-icon,
.marker.marker-2 .marker-icon::before,
.marker.marker-2 .marker-label {
  background-color: var(--hv2-accent-2);
  color: var(--hv2-dark);
}

@keyframes hero-v2-pulse {
  0% {
    transform: translate(-50%, -50%) scale(0.25);
  }
  80%,
  100% {
    opacity: 0;
  }
}

.hero-v2-content {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 400svh;
  display: flex;
  flex-direction: column;
  will-change: transform;
}

.hero-v2-content .hero-v2-content-block {
  width: 100%;
  height: 100svh;
  padding: 4rem;
  display: flex;
}

.hero-v2-content .hero-v2-content-copy {
  width: 35%;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.hero-v2-content .hero-v2-content-block:nth-child(1) {
  align-items: flex-end;
}

.hero-v2-content .hero-v2-content-block:nth-child(2),
.hero-v2-content .hero-v2-content-block:nth-child(4) {
  justify-content: flex-end;
  align-items: center;
}

.hero-v2-content .hero-v2-content-block:nth-child(3) {
  align-items: center;
}

.hero-v2-scroll-progress-bar {
  position: absolute;
  top: 50%;
  right: 2rem;
  transform: translateY(-50%);
  width: 0.1rem;
  height: 10rem;
  background-color: rgba(255, 255, 255, 0.2);
  --hv2-progress: 0;
}

.hero-v2-scroll-progress-bar::after {
  content: '';
  position: absolute;
  width: 100%;
  height: 100%;
  background-color: var(--hv2-light);
  transform-origin: top;
  transform: scaleY(var(--hv2-progress));
  will-change: transform;
}

.hero-v2-login-area {
  --color-black: #13140e;
  --color-black-2: #181813;
  --color-creme: #f4f3e8;
  --color-grey: #595a51;
  --color-grey-2: #404040;
  --color-yellow: #ebfc72;
  --color-red: #f1664d;
  --color-green: #00d399;
  --color-purple: #c084fc;
  background: var(--color-black);
  color: var(--color-creme);
  font-family: 'JetBrains Mono', monospace;
}

.hero-v2-login-area *,
.hero-v2-login-area *::before,
.hero-v2-login-area *::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

.hero-v2-login-area a {
  color: inherit;
  text-decoration: none;
}

.hero-v2-login-area button {
  background: none;
  border: 0;
  color: inherit;
  font: inherit;
  cursor: pointer;
  padding: 0;
}

.hero-v2-cta {
  display: inline-flex;
  align-items: center;
  background: #ebfc72;
  border: 0;
  border-radius: 0.4rem;
  clip-path: polygon(0 0, 100% 0, 100% calc(100% - 0.5rem), calc(100% - 0.5rem) 100%, 0 100%, 0 0);
  color: #13140e;
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.7rem;
  font-weight: 400;
  height: 2.8rem;
  line-height: 1.28;
  padding: 0 0.35rem;
  text-transform: uppercase;
  cursor: pointer;
  transition: clip-path 0.25s ease-out;
  margin-top: 1.2rem;
}

.hero-v2-cta:hover {
  clip-path: polygon(0.5rem 0, 100% 0, 100% 100%, 100% 100%, 0 100%, 0 0.5rem);
}

@media (max-width: 800px) {
  .hero-v2-mask {
    mask: linear-gradient(var(--hv2-light), var(--hv2-light)),
      url('/hero-v2/hero-v2-mask.svg') center/75% no-repeat;
    -webkit-mask: linear-gradient(var(--hv2-light), var(--hv2-light)),
      url('/hero-v2/hero-v2-mask.svg') center/75% no-repeat;
    mask-composite: subtract;
    -webkit-mask-composite: xor;
  }

  .hero-v2-grid-overlay {
    width: 100%;
  }

  .marker-1 {
    top: 52.5svh;
    left: 50vw;
  }

  .marker-2 {
    top: 45svh;
    left: 70vw;
  }

  .hero-v2-content .hero-v2-content-block {
    padding: 1.5rem;
  }

  .hero-v2-content .hero-v2-content-copy {
    width: 75%;
  }

  .hero-v2-scroll-progress-bar {
    right: 1rem;
  }
}
</style>

<style>
@import url('https://fonts.googleapis.com/css2?family=DM+Mono:ital,wght@0,300;0,400;0,500;1,300;1,400;1,500&family=DM+Sans:ital,opsz,wght@0,9..40,100..1000;1,9..40,100..1000&display=swap');

.hero-v2-login-area .cta-button {
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
}

.hero-v2-login-area .cta-button:hover {
  clip-path: polygon(1.5rem 0, 100% 0, 100% 100%, 100% 100%, 0 100%, 0 1.5rem);
}

.hero-v2-login-area .text-link {
  color: var(--color-yellow);
  position: relative;
}

.hero-v2-login-area .text-link::after {
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

.hero-v2-login-area .text-link:hover::after {
  transform: scaleX(1);
  transform-origin: left center;
}

@keyframes hero-v2-flash {
  0% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}
</style>
