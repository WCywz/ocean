<template>
  <el-container class="main-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo" @click="$router.push('/dashboard')">
        <el-icon :size="28" color="#409EFF"><Ship /></el-icon>
        <span v-show="!isCollapse" class="logo-text">海洋预报系统</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="#001529"
        text-color="#ffffffb3"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/app/dashboard">
          <el-icon><DataBoard /></el-icon>
          <template #title>首页仪表盘</template>
        </el-menu-item>

        <el-menu-item index="/app/forecast">
          <el-icon><TrendCharts /></el-icon>
          <template #title>预报数据可视化</template>
        </el-menu-item>

        <el-menu-item index="/app/ocean-data">
          <el-icon><Watermelon /></el-icon>
          <template #title>海洋观测数据</template>
        </el-menu-item>

        <template v-if="isAdmin">
          <el-menu-item index="/app/model">
            <el-icon><Setting /></el-icon>
            <template #title>预报模型管理</template>
          </el-menu-item>

          <el-menu-item index="/app/user">
            <el-icon><UserFilled /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container>
      <!-- 顶栏 -->
      <el-header class="topbar">
        <div class="topbar-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse" :size="20">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
        </div>
        <div class="topbar-right">
          <el-tag :type="isAdmin ? 'danger' : 'success'" size="small">
            {{ isAdmin ? '管理员' : '普通用户' }}
          </el-tag>
          <span class="username">{{ userInfo?.realName || userInfo?.username }}</span>
          <el-button type="danger" text @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const userInfo = computed(() => userStore.userInfo)
const isAdmin = computed(() => userStore.isAdmin())
const activeMenu = computed(() => route.path)

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
.main-container {
  height: 100vh;
}

.sidebar {
  background-color: #001529;
  overflow: hidden;
  transition: width 0.3s;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  cursor: pointer;
  border-bottom: 1px solid #ffffff1a;
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.topbar {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.collapse-btn {
  cursor: pointer;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  color: #333;
  font-weight: 500;
}

.el-main {
  background: #f0f2f5;
}
</style>
