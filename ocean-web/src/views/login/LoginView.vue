<template>
  <div class="login-page">
    <div class="login-form">
      <div style="text-align: center; margin-bottom: 40px;">
        <h1 class="editorial-page-title" style="font-size: 28px; text-align: center;">海洋环境预报系统</h1>
        <p style="font-family: var(--font-serif); font-size: 13px; color: var(--color-text-muted); font-style: italic; margin: 6px 0 0 0;">
          Ocean Forecast System
        </p>
      </div>

      <div style="width: 320px;">
        <div style="margin-bottom: 24px;">
          <input
            v-model="form.username"
            class="editorial-input"
            placeholder="用户名"
            @keyup.enter="handleLogin"
          />
        </div>
        <div style="margin-bottom: 28px;">
          <input
            v-model="form.password"
            class="editorial-input"
            type="password"
            placeholder="密码"
            @keyup.enter="handleLogin"
          />
        </div>
        <button
          class="editorial-btn"
          style="width: 100%; padding: 12px 0;"
          :disabled="loading"
          @click="handleLogin"
        >
          {{ loading ? '...' : '登  录' }}
        </button>
        <div style="text-align: center; margin-top: 20px;">
          <span style="color: var(--color-text-muted); font-size: 12px;">还没有账号？</span>
          <a class="editorial-link" style="color: var(--color-text);" @click="$router.push('/register')">注册</a>
        </div>
        <div style="text-align: center; margin-top: 12px;">
          <a class="editorial-link" @click="$router.push('/')">返回首页</a>
        </div>
      </div>

      <div style="margin-top: 28px; font-size: 11px; color: var(--color-text-muted); text-align: center;">
        管理员: admin / admin123 &nbsp;·&nbsp; 用户: user / user123
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import { login } from '../../api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123'
})

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await login(form)
    userStore.setToken(res.data.token)
    userStore.setUserInfo({
      userId: res.data.userId,
      username: res.data.username,
      realName: res.data.realName,
      role: res.data.role
    })
    ElMessage.success('登录成功')
    router.push('/app/dashboard')
  } catch (e) {
    // error handled in interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
}
.login-form {
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>
