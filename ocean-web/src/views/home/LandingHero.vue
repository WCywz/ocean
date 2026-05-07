<template>
  <section class="landing-hero" ref="heroRef">
    <div class="landing-hero__wrapper" ref="wrapperRef">
      <!-- Masked scene: contains bg images, grid, filter, hotspots -->
      <div class="landing-hero__scene" ref="sceneRef">
        <div class="landing-hero__bg">
          <img
            ref="bgImgARef"
            class="landing-hero__bg-img landing-hero__bg-img--a"
            src="https://images.unsplash.com/photo-1582967788606-a17106d5b57?w=1920&q=80"
            alt="Ocean surface"
          />
          <img
            ref="bgImgBRef"
            class="landing-hero__bg-img landing-hero__bg-img--b"
            src="https://images.unsplash.com/photo-1457365050282-c53d772ef8b2?w=1920&q=80"
            alt="Ocean restoration"
          />
        </div>

        <div class="landing-hero__grid" ref="gridRef">
          <svg viewBox="0 0 1080 748" fill="none" xmlns="http://www.w3.org/2000/svg" class="landing-hero__grid-svg">
            <path fill-rule="evenodd" clip-rule="evenodd" d="M582 83H830V0H914V83H997V249H1080V416H997V498H1080V582H997V665H831V748H747V665H665V748H498V665H250V748H166V665H0V498H83V333H0V166H83V83H332V0H582V83ZM167 747H249V665H167V747ZM499 747H581V665H499V747ZM582 747H664V665H582V747ZM748 747H830V665H748V747ZM1 664H83V582H1V664ZM84 664H166V582H84V664ZM167 664H249V582H167V664ZM250 664H332V582H250V664ZM333 664H415V582H333V664ZM416 664H498V582H416V664ZM499 664H581V582H499V664ZM582 664H664V582H582V664ZM665 664H747V582H665V664ZM748 664H830V582H748V664ZM831 664H913V582H831V664ZM914 664H996V582H914V664ZM1 581H83V499H1V581ZM84 581H166V499H84V581ZM167 581H249V499H167V581ZM250 581H332V499H250V581ZM333 581H415V499H333V581ZM416 581H498V499H416V581ZM499 581H581V499H499V581ZM582 581H664V499H582V581ZM665 581H747V499H665V581ZM748 581H830V499H748V581ZM831 581H913V499H831V581ZM914 581H996V499H914V581ZM997 581H1079V499H997V581ZM84 498H166V416H84V498ZM167 498H249V416H167V498ZM250 498H332V416H250V498ZM333 498H415V416H333V498ZM416 498H498V416H416V498ZM499 498H581V416H499V498ZM582 498H664V416H582V498ZM665 498H747V416H665V498ZM748 498H830V416H748V498ZM831 498H913V416H831V498ZM914 498H996V416H914V498ZM84 415H166V333H84V415ZM167 415H249V333H167V415ZM250 415H332V333H250V415ZM333 415H415V333H333V415ZM416 415H498V333H416V415ZM499 415H581V333H499V415ZM582 415H664V333H582V415ZM665 415H747V333H665V415ZM748 415H830V333H748V415ZM831 415H913V333H831V415ZM914 415H996V333H914V415ZM997 415H1079V333H997V415ZM1 332H83V250H1V332ZM84 332H166V250H84V332ZM167 332H249V250H167V332ZM250 332H332V250H250V332ZM333 332H415V250H333V332ZM416 332H498V250H416V332ZM499 332H581V250H499V332ZM582 332H664V250H582V332ZM665 332H747V250H665V332ZM748 332H830V250H748V332ZM831 332H913V250H831V332ZM914 332H996V250H914V332ZM997 332H1079V250H997V332ZM1 249H83V167H1V249ZM84 249H166V167H84V249ZM167 249H249V167H167V249ZM250 249H332V167H250V249ZM333 249H415V167H333V249ZM416 249H498V167H416V249ZM499 249H581V167H499V249ZM582 249H664V167H582V249ZM665 249H747V167H665V249ZM748 249H830V167H748V249ZM831 249H913V167H831V249ZM914 249H996V167H914V249ZM84 166H166V84H84V166ZM167 166H249V84H167V166ZM250 166H332V84H250V166ZM333 166H415V84H333V166ZM416 166H498V84H416V166ZM499 166H581V84H499V166ZM582 166H664V84H582V166ZM665 166H747V84H665V166ZM748 166H830V84H748V166ZM831 166H913V84H831V166ZM914 166H996V84H914V166ZM333 83H415V1H333V83ZM416 83H498V1H416V83ZM499 83H581V1H499V83ZM831 83H913V1H831V83Z" fill="color-mix(in srgb, currentColor 30%, transparent)"/>
          </svg>
        </div>

        <div class="landing-hero__filter" ref="filterRef"></div>

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

      <!-- Text slides (above mask, not clipped) -->
      <div class="landing-hero__slides">
        <div class="landing-hero__slide" ref="slide1Ref">
          <h2 class="hero-heading">海洋预报<br/>守护蔚蓝</h2>
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
const sceneRef = ref(null)
const bgImgARef = ref(null)
const bgImgBRef = ref(null)
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
  // Lock initial states before GSAP touches them
  gsap.set(slide1Ref.value, { opacity: 0 })
  gsap.set(slide2Ref.value, { opacity: 0 })
  gsap.set(slide3Ref.value, { opacity: 0 })
  gsap.set(slide4Ref.value, { opacity: 0 })
  gsap.set(gridRef.value, { opacity: 0 })
  gsap.set(filterRef.value, { opacity: 0 })
  gsap.set(hotspotsSvgRef.value, { opacity: 0 })
  gsap.set(sceneRef.value, { scale: 1 })
  gsap.set(bgImgARef.value, { yPercent: 0 })
  gsap.set(bgImgBRef.value, { yPercent: 0 })
  gsap.set(wrapperRef.value, { opacity: 1 })

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

  // Phase 1 (0%–25%): Background parallax reverse + slide 1
  tl.to(bgImgARef.value, { yPercent: -10, ease: 'none' }, 0)
    .to(bgImgBRef.value, { yPercent: 5, ease: 'none' }, 0)
    .to(slide1Ref.value, { opacity: 1, ease: 'none' }, 0)
    .to(slide1Ref.value, { opacity: 0, ease: 'none' }, 0.22)

  // Phase 2 (15%–35%): Mask scale + slide 2
  tl.to(sceneRef.value, { scale: 0.92, ease: 'none' }, 0.15)
    .to(slide2Ref.value, { opacity: 1, ease: 'none' }, 0.18)
    .to(slide2Ref.value, { opacity: 0, ease: 'none' }, 0.35)

  // Phase 3 (30%–55%): Desaturation + grid appear + slide 3
  tl.to(filterRef.value, { opacity: 1, ease: 'none' }, 0.3)
    .to(gridRef.value, { opacity: 1, ease: 'none' }, 0.32)
    .to(hotspotsSvgRef.value, { opacity: 1, ease: 'none' }, 0.32)
    .to(slide3Ref.value, { opacity: 1, ease: 'none' }, 0.35)
    .to(slide3Ref.value, { opacity: 0, ease: 'none' }, 0.5)

  // Phase 4 (48%–70%): Release — mask back, desaturation fades, grid fades + slide 4
  tl.to(sceneRef.value, { scale: 1, ease: 'none' }, 0.48)
    .to(filterRef.value, { opacity: 0, ease: 'none' }, 0.5)
    .to(gridRef.value, { opacity: 0, ease: 'none' }, 0.52)
    .to(hotspotsSvgRef.value, { opacity: 0, ease: 'none' }, 0.52)
    .to(slide4Ref.value, { opacity: 1, ease: 'none' }, 0.55)
    .to(slide4Ref.value, { opacity: 0, ease: 'none' }, 0.72)

  // Phase 5 (70%–100%): Hero fade out
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
  overflow: hidden;
  background-color: var(--color-black);
}

/* Scene: masked container clips bg + overlays */
.landing-hero__scene {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100svh;
  inset: 0;
  mask-image: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 1030 701'%3E%3Cpath fill='white' d='M347 0v21h60V0h43v21h44v21h121V21h31V0h117v21h59V0h107v21h59v62h21v42h21v36h-21v38h-21v96h21v14h21v46h-21v21h21v38h-21v50h21v79h-21v74h21v24h-21v20H886v-20h-41v20h-40v-20h-21v20h-42v20h-41v20H559v-20h-23v-20h-50v-20h-41v20H330v-20h-21v20h-52v20h-24v20h-47v-20h-21v20h-21v-20h-21v-20H40v-41H20v-62h20v-46H20v-58h20v-20H20v-20H0V240h20v-53H0v-62h20V63h20V42h41v21h83V42h21V21h85V0z'/%3E%3C/svg%3E");
  -webkit-mask-image: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 1030 701'%3E%3Cpath fill='white' d='M347 0v21h60V0h43v21h44v21h121V21h31V0h117v21h59V0h107v21h59v62h21v42h21v36h-21v38h-21v96h21v14h21v46h-21v21h21v38h-21v50h21v79h-21v74h21v24h-21v20H886v-20h-41v20h-40v-20h-21v20h-42v20h-41v20H559v-20h-23v-20h-50v-20h-41v20H330v-20h-21v20h-52v20h-24v20h-47v-20h-21v20h-21v-20h-21v-20H40v-41H20v-62h20v-46H20v-58h20v-20H20v-20H0V240h20v-53H0v-62h20V63h20V42h41v21h83V42h21V21h85V0z'/%3E%3C/svg%3E");
  mask-position: center;
  -webkit-mask-position: center;
  mask-repeat: no-repeat;
  -webkit-mask-repeat: no-repeat;
  will-change: transform;
}

@media (max-width: 600px) {
  .landing-hero__scene {
    mask-size: 57.6rem 39.2rem;
    -webkit-mask-size: 57.6rem 39.2rem;
  }
}

@media (min-width: 601px) {
  .landing-hero__scene {
    mask-size: 103rem 70.1rem;
    -webkit-mask-size: 103rem 70.1rem;
  }
}

.landing-hero__bg {
  position: absolute;
  inset: 0;
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

/* Grid overlay */
.landing-hero__grid {
  position: absolute;
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
  will-change: opacity;
}

/* Hotspots */
.landing-hero__hotspots {
  position: absolute;
  inset: 0;
}

.landing-hero__hotspots-svg {
  position: absolute;
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

/* Text slides — above mask, not clipped */
.landing-hero__slides {
  position: absolute;
  inset: 0;
  z-index: 5;
}

.landing-hero__slide {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  height: 100svh;
  width: 100%;
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
