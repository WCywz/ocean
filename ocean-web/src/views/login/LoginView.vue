<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <el-icon :size="48" color="#409EFF"><Ship /></el-icon>
        <h2>海洋环境预报系统</h2>
        <p>Ocean Environment Forecast System</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-tip">
        <span>管理员: admin / admin123</span>
        <span>普通用户: user / user123</span>
      </div>

      <div class="login-footer-link">
        <span>还没有账号？</span>
        <el-link type="primary" @click="$router.push('/register')">去注册</el-link>
      </div>

      <div class="back-home">
        <el-link type="info" @click="$router.push('/')">
          <el-icon><ArrowLeft /></el-icon> 返回首页
        </el-link>
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
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

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
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0c1d3e 0%, #1a3a5c 50%, #0d4f7c 100%);
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(ellipse at center, transparent 0%, rgba(0,0,0,0.3) 100%);
  animation: rotate 30s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.login-card {
  width: 420px;
  padding: 40px;
  background: rgba(255,255,255,0.95);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  position: relative;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  margin: 12px 0 6px;
  color: #1a3a5c;
  font-size: 22px;
}

.login-header p {
  color: #8899aa;
  font-size: 13px;
}

.login-tip {
  display: flex;
  justify-content: space-between;
  color: #999;
  font-size: 12px;
  margin-top: 10px;
}

.login-footer-link {
  display: flex;
  justify-content: center;
  gap: 6px;
  color: #999;
  font-size: 14px;
  margin-top: 12px;
}

.back-home {
  text-align: center;
  margin-top: 12px;
}
</style>
