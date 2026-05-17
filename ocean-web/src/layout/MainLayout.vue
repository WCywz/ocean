<template>
  <div class="app-shell">
    <!-- Top navigation bar -->
    <nav class="editorial-nav">
      <router-link to="/app/dashboard" class="editorial-nav__brand">
        海洋预报系统
      </router-link>

      <div class="editorial-nav__items">
        <router-link
          to="/app/dashboard"
          class="editorial-nav__item"
          :class="{ 'editorial-nav__item--active': isActive('/app/dashboard') }"
        >仪表盘</router-link>

        <!-- Forecast dropdown -->
        <div
          class="editorial-nav__item"
          :class="{ 'editorial-nav__item--active': isActive('/app/forecast') }"
          @mouseenter="showForecastMenu = true"
          @mouseleave="showForecastMenu = false"
          style="position: relative;"
        >
          预报
          <div
            v-show="showForecastMenu"
            class="forecast-dropdown"
            @mouseenter="showForecastMenu = true"
            @mouseleave="showForecastMenu = false"
          >
            <router-link to="/app/forecast/sst" class="forecast-dropdown__item" :class="{ 'forecast-dropdown__item--active': $route.path === '/app/forecast/sst' }" active-class="" exact-active-class="">海表温度预测</router-link>
            <router-link to="/app/forecast/chl" class="forecast-dropdown__item" :class="{ 'forecast-dropdown__item--active': $route.path === '/app/forecast/chl' }" active-class="" exact-active-class="">叶绿素预测</router-link>
            <router-link to="/app/forecast/history" class="forecast-dropdown__item" :class="{ 'forecast-dropdown__item--active': $route.path === '/app/forecast/history' }" active-class="" exact-active-class="">历史预报记录</router-link>
          </div>
        </div>

        <!-- Observation dropdown -->
        <div
          class="editorial-nav__item"
          :class="{ 'editorial-nav__item--active': isActive('/app/observation') }"
          @mouseenter="showObsMenu = true"
          @mouseleave="showObsMenu = false"
          style="position: relative;"
        >
          观测
          <div
            v-show="showObsMenu"
            class="forecast-dropdown"
            @mouseenter="showObsMenu = true"
            @mouseleave="showObsMenu = false"
          >
            <router-link to="/app/observation/sst" class="forecast-dropdown__item" :class="{ 'forecast-dropdown__item--active': $route.path === '/app/observation/sst' }" active-class="" exact-active-class="">海表温度观测</router-link>
            <router-link to="/app/observation/chl" class="forecast-dropdown__item" :class="{ 'forecast-dropdown__item--active': $route.path === '/app/observation/chl' }" active-class="" exact-active-class="">叶绿素观测</router-link>
            <router-link to="/app/observation/history" class="forecast-dropdown__item" :class="{ 'forecast-dropdown__item--active': $route.path === '/app/observation/history' }" active-class="" exact-active-class="">历史观测记录</router-link>
          </div>
        </div>

        <router-link
          to="/app/ocean-health"
          class="editorial-nav__item"
          :class="{ 'editorial-nav__item--active': isActive('/app/ocean-health') }"
        >健康</router-link>

        <template v-if="isAdmin">
          <router-link
            to="/app/model"
            class="editorial-nav__item"
            :class="{ 'editorial-nav__item--active': isActive('/app/model') }"
          >模型</router-link>
          <router-link
            to="/app/user"
            class="editorial-nav__item"
            :class="{ 'editorial-nav__item--active': isActive('/app/user') }"
          >用户</router-link>
        </template>
      </div>

      <span class="editorial-nav__spacer"></span>

      <span v-if="isAdmin" class="editorial-tag" style="margin-right: 12px;">ADMIN</span>
      <span class="editorial-nav__user">{{ userInfo?.realName || userInfo?.username }}</span>
      <a class="editorial-link" style="margin-left: 16px;" @click="handleLogout">退出</a>
    </nav>

    <!-- Content area -->
    <main class="editorial-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const showForecastMenu = ref(false)
const showObsMenu = ref(false)
const userInfo = computed(() => userStore.userInfo)
const isAdmin = computed(() => userStore.isAdmin())

function isActive(basePath) {
  return route.path.startsWith(basePath)
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    .then(() => {
      userStore.logout()
      router.push('/')
    })
    .catch(() => {})
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.forecast-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  background: var(--color-bg);
  border: 1px solid var(--color-divider-strong);
  min-width: 140px;
  z-index: 200;
  padding: 8px 0;
}
.forecast-dropdown__item {
  display: block;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 400;
  color: var(--color-text-muted);
  text-decoration: none;
  text-transform: none;
  letter-spacing: 0;
  transition: background 0.15s, color 0.15s;
}
.forecast-dropdown__item:hover {
  background: var(--color-surface);
  color: var(--color-text);
}
.forecast-dropdown__item--active {
  color: var(--color-text);
  font-weight: 600;
}
</style>
