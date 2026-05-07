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
