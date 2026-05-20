<template>
  <section class="landing-login" id="login-section" ref="loginRef">
    <div class="landing-login__form">
      <div class="landing-login__header">
        <h2 class="landing-login__title">登录</h2>
        <p class="landing-login__subtitle">海洋环境预报系统</p>
      </div>

      <div class="landing-login__fields">
        <input
          v-model="form.username"
          class="landing-login__input"
          placeholder="用户名"
          @keyup.enter="handleLogin"
        />
        <input
          v-model="form.password"
          class="landing-login__input"
          type="password"
          placeholder="密码"
          @keyup.enter="handleLogin"
        />
        <button
          class="cta-button landing-login__submit"
          :disabled="loading"
          @click="handleLogin"
        >
          {{ loading ? '...' : '登 录' }}
        </button>
      </div>

      <div class="landing-login__footer">
        <span class="landing-login__hint">还没有账号？</span>
        <a class="text-link" @click="$router.push('/register')">注册</a>
      </div>

    </div>
  </section>
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

const loginRef = ref(null)

const form = reactive({
  username: '',
  password: ''
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

defineExpose({ loginRef })
</script>

<style scoped>
.landing-login {
  height: 100svh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-black);
}

.landing-login__form {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2.8rem;
}

.landing-login__header {
  text-align: center;
}

.landing-login__title {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 2.4rem;
  font-weight: 400;
  color: var(--color-creme);
  margin: 0 0 0.4rem 0;
}

.landing-login__subtitle {
  font-size: 1.1rem;
  color: rgba(244, 243, 232, 0.5);
  margin: 0;
}

.landing-login__fields {
  display: flex;
  flex-direction: column;
  gap: 1.6rem;
  width: 28rem;
}

.landing-login__input {
  background: transparent;
  border: none;
  border-bottom: 1px solid var(--color-grey-2);
  padding: 1rem 0;
  color: var(--color-creme);
  font-family: 'JetBrains Mono', monospace;
  font-size: 1.4rem;
  outline: none;
  transition: border-color 0.2s;
}

.landing-login__input::placeholder {
  color: var(--color-grey);
}

.landing-login__input:focus {
  border-bottom-color: var(--color-yellow);
}

.landing-login__submit {
  width: 100%;
  justify-content: center;
  margin-top: 0.8rem;
}

.landing-login__submit:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.landing-login__footer {
  font-size: 1.2rem;
}

.landing-login__hint {
  color: rgba(244, 243, 232, 0.5);
  margin-right: 0.4rem;
}

.landing-login__creds {
  font-size: 1.1rem;
  color: rgba(244, 243, 232, 0.3);
  text-align: center;
}
</style>
