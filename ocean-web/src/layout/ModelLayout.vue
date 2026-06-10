<template>
  <div class="model-shell">
    <aside class="model-sidebar">
      <div class="model-sidebar__section">模型管理</div>
      <router-link
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        class="model-sidebar__item"
        :class="{ 'model-sidebar__item--active': isActive(item.match) }"
      >{{ item.label }}</router-link>

      <div class="model-sidebar__divider"></div>
      <div class="model-sidebar__section">快捷操作</div>
      <router-link to="/app/model" class="model-sidebar__action">+ 新建模型</router-link>
    </aside>

    <main class="model-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const menuItems = [
  { label: '模型管理', path: '/app/model', match: '/app/model' },
  { label: '调度总览', path: '/app/model/schedule', match: '/app/model/schedule' },
  { label: '运行监控', path: '/app/model/monitor', match: '/app/model/monitor' },
  { label: '告警管理', path: '/app/model/alerts', match: '/app/model/alerts' },
  { label: '模型对比', path: '/app/model/compare', match: '/app/model/compare' },
]

function isActive(match) {
  if (match === '/app/model') {
    return route.path === '/app/model' || /^\/app\/model\/\d+$/.test(route.path)
  }
  return route.path.startsWith(match)
}
</script>

<style scoped>
.model-shell {
  display: flex;
}

.model-sidebar {
  width: 200px;
  flex-shrink: 0;
  background: var(--color-surface);
  border-right: 1px solid var(--color-divider-strong);
  padding-top: 16px;
  min-height: calc(100vh - 48px);
}

.model-sidebar__section {
  padding: 8px 16px 4px;
  font-size: 10px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.model-sidebar__item {
  display: block;
  padding: 8px 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
  text-decoration: none;
  border-left: 3px solid transparent;
  transition: color 0.15s, background 0.15s;
}

.model-sidebar__item:hover {
  color: var(--color-text);
  background: var(--color-bg);
}

.model-sidebar__item--active {
  color: var(--color-text);
  font-weight: 600;
  border-left-color: var(--color-text);
  background: var(--color-bg);
}

.model-sidebar__divider {
  margin: 16px 16px;
  border-top: 1px solid var(--color-divider);
}

.model-sidebar__action {
  display: block;
  padding: 6px 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  text-decoration: none;
}

.model-sidebar__action:hover {
  color: var(--color-text);
}

.model-content {
  flex: 1;
  min-width: 0;
  padding: 24px 28px;
  background: var(--color-bg);
}
</style>
