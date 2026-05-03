<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <el-icon :size="40" color="#409EFF"><Ship /></el-icon>
        <h2>创建账号</h2>
        <p>加入海洋环境预报系统</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleRegister">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>

        <el-form-item prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" prefix-icon="Avatar" />
        </el-form-item>

        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" prefix-icon="Lock" show-password />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleRegister">
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        <span>已有账号？</span>
        <el-link type="primary" @click="$router.push('/login')">去登录</el-link>
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
import { register } from '../../api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  realName: '',
  password: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

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
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0c1d3e 0%, #1a3a5c 50%, #0d4f7c 100%);
  position: relative;
  overflow: hidden;
}

.register-container::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(ellipse at center, transparent 0%, rgba(0,0,0,0.2) 100%);
}

.register-card {
  width: 420px;
  padding: 36px 40px;
  background: rgba(255,255,255,0.95);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  position: relative;
  z-index: 1;
}

.register-header {
  text-align: center;
  margin-bottom: 28px;
}

.register-header h2 {
  margin: 10px 0 4px;
  color: #1a3a5c;
  font-size: 20px;
}

.register-header p {
  color: #8899aa;
  font-size: 13px;
}

.register-footer {
  display: flex;
  justify-content: center;
  gap: 6px;
  color: #999;
  font-size: 14px;
  margin-top: 6px;
}

.back-home {
  text-align: center;
  margin-top: 16px;
}
</style>
