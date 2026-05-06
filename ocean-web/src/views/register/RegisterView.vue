<template>
  <div class="register-page">
    <div class="register-form">
      <div style="text-align: center; margin-bottom: 36px;">
        <h1 class="editorial-page-title" style="font-size: 24px; text-align: center;">创建账号</h1>
        <p style="font-family: var(--font-serif); font-size: 13px; color: var(--color-text-muted); font-style: italic; margin: 4px 0 0 0;">
          加入海洋环境预报系统
        </p>
      </div>

      <div style="width: 320px;">
        <div style="margin-bottom: 20px;">
          <input v-model="form.username" class="editorial-input" placeholder="用户名" />
        </div>
        <div style="margin-bottom: 20px;">
          <input v-model="form.realName" class="editorial-input" placeholder="真实姓名" />
        </div>
        <div style="margin-bottom: 20px;">
          <input v-model="form.password" class="editorial-input" type="password" placeholder="密码" />
        </div>
        <div style="margin-bottom: 28px;">
          <input v-model="form.confirmPassword" class="editorial-input" type="password" placeholder="再次输入密码" />
        </div>
        <button
          class="editorial-btn"
          style="width: 100%; padding: 12px 0;"
          :disabled="loading"
          @click="handleRegister"
        >
          {{ loading ? '...' : '注  册' }}
        </button>
        <div style="text-align: center; margin-top: 20px;">
          <span style="color: var(--color-text-muted); font-size: 12px;">已有账号？</span>
          <a class="editorial-link" style="color: var(--color-text);" @click="$router.push('/login')">去登录</a>
        </div>
        <div style="text-align: center; margin-top: 12px;">
          <a class="editorial-link" @click="$router.push('/')">返回首页</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../../api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: '',
  realName: '',
  password: '',
  confirmPassword: ''
})

async function handleRegister() {
  if (!form.username || !form.realName || !form.password || !form.confirmPassword) {
    ElMessage.warning('请填写所有字段')
    return
  }
  if (form.username.length < 3 || form.username.length > 20) {
    ElMessage.warning('用户名长度在 3 到 20 个字符')
    return
  }
  if (form.password.length < 6 || form.password.length > 20) {
    ElMessage.warning('密码长度在 6 到 20 个字符')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await register({
      username: form.username,
      realName: form.realName,
      password: form.password,
      role: 'USER',
      status: 1
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    // error handled in interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
}
.register-form {
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>
