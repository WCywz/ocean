<template>
  <router-view />
</template>

<script setup>
</script>

<style>
@import './styles/editorial.css';

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
