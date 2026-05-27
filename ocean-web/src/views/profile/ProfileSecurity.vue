<template>
  <div class="profile-page">
    <h1 class="editorial-page-title">账户安全</h1>
    <p class="editorial-page-subtitle">修改您的登录密码</p>

    <div class="profile-form">
      <div class="profile-form__item">
        <label class="editorial-form-label">旧密码</label>
        <input v-model="pwdForm.oldPassword" type="password" class="editorial-input" />
      </div>
      <div class="profile-form__item">
        <label class="editorial-form-label">新密码</label>
        <input v-model="pwdForm.newPassword" type="password" class="editorial-input" />
      </div>
      <div class="profile-form__item">
        <label class="editorial-form-label">确认密码</label>
        <input v-model="pwdForm.confirmPassword" type="password" class="editorial-input" />
      </div>
      <button class="editorial-btn" :disabled="changingPwd" @click="handleChangePassword">
        {{ changingPwd ? '修改中...' : '修改密码' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changePassword } from '../../api/profile'

const changingPwd = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

async function handleChangePassword() {
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    ElMessage.error('两次密码输入不一致')
    return
  }
  if (pwdForm.value.newPassword.length < 6) {
    ElMessage.error('新密码至少6位')
    return
  }
  changingPwd.value = true
  try {
    await changePassword(pwdForm.value)
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    ElMessage.success('密码已修改')
  } catch (e) { console.error('密码修改失败', e); ElMessage.error('密码修改失败，请重试') } finally { changingPwd.value = false }
}
</script>

<style scoped>
.profile-page { max-width: 600px; }
.profile-form__item { margin-bottom: 20px; }
.profile-form .editorial-btn { width: 100%; margin-top: 8px; }
</style>
