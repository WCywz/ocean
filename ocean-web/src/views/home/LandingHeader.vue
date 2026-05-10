<template>
  <header class="landing-header">
    <a class="landing-header__logo" @click="$emit('scrollToTop')">海洋环境预报系统</a>
    <div class="landing-header__links">
      <button class="landing-header__cta" @click="$router.push('/register')">注册</button>
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

.landing-header__cta {
  pointer-events: auto;
  display: inline-flex;
  align-items: center;
  background: #ebfc72;
  border: 0;
  border-radius: 0.4rem;
  clip-path: polygon(0 0, 100% 0, 100% calc(100% - 0.6rem), calc(100% - 0.6rem) 100%, 0 100%, 0 0);
  color: #13140e;
  font-size: 0.8rem;
  font-weight: 300;
  height: 2.2rem;
  line-height: 1.28;
  padding: 0 1.2rem;
  text-transform: uppercase;
  cursor: pointer;
  transition: clip-path 0.25s ease-out;
}

.landing-header__cta:hover {
  clip-path: polygon(0.6rem 0, 100% 0, 100% 100%, 100% 100%, 0 100%, 0 0.6rem);
}

@media (max-width: 600px) {
  .landing-header {
    padding: 1.2rem;
  }
}
</style>
