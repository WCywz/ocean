<template>
  <div class="profile-layout">
    <aside class="profile-sidebar">
      <div class="profile-sidebar__label">设置</div>
      <nav class="profile-sidebar__nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="profile-sidebar__item"
          :class="{ 'profile-sidebar__item--active': isActive(item.path) }"
        >{{ item.label }}</router-link>
      </nav>
    </aside>
    <section class="profile-content">
      <router-view />
    </section>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  { path: '/app/profile/info', label: '个人信息' },
  { path: '/app/profile/security', label: '账户安全' },
  { path: '/app/profile/notifications', label: '通知设置' },
  { path: '/app/profile/announcements', label: '系统公告' },
  { path: '/app/profile/settings', label: '系统设置' }
]

function isActive(path) {
  return route.path === path
}
</script>

<style scoped>
.profile-layout {
  display: flex;
  gap: 0;
  min-height: calc(100vh - 64px - 49px);
}

.profile-sidebar {
  width: 160px;
  flex-shrink: 0;
  padding: 32px 24px;
  border-right: 1px solid var(--color-divider-strong);
}

.profile-sidebar__label {
  font-size: 10px;
  color: var(--color-text-muted);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  margin-bottom: 20px;
}

.profile-sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.profile-sidebar__item {
  font-size: 13px;
  color: var(--color-text-muted);
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 8px 0;
  text-decoration: none;
  transition: color 0.15s;
}

.profile-sidebar__item:hover {
  color: var(--color-text);
}

.profile-sidebar__item--active {
  color: var(--color-text);
  font-weight: 600;
}

.profile-content {
  flex: 1;
  padding: 36px 40px;
  min-width: 0;
}
</style>
