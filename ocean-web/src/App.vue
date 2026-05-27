<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useTheme } from './composables/useTheme'

const { init } = useTheme()
onMounted(() => init())
</script>

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
