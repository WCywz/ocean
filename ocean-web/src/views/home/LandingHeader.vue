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
