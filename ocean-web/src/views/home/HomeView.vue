<template>
  <div class="landing">
    <LandingHeader @scroll-to-top="scrollToTop" />
    <main class="landing__main">
      <LandingHero ref="heroRef" @scroll-to-login="scrollToLogin" />
      <LandingLogin ref="loginRef" />
      <LandingFooter />
    </main>
  </div>
</template>

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

<style>
html {
  overflow-y: scroll;
}
body {
  min-height: 100%;
}
</style>
