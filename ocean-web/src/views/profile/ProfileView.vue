<template>
  <div class="profile-page">
    <h1 class="editorial-page-title">个人中心</h1>
    <p class="editorial-page-subtitle">管理您的账户信息和偏好设置</p>

    <!-- 个人信息 -->
    <div class="editorial-section">
      <h2 class="editorial-section-heading">个人信息</h2>

      <div class="profile-avatar-section">
        <div class="profile-avatar" @click="triggerUpload">
          <img v-if="avatarUrl" :src="avatarUrl" alt="头像" />
          <span v-else class="profile-avatar__placeholder">{{ avatarLetter }}</span>
        </div>
        <input
          ref="fileInput"
          type="file"
          accept="image/jpeg,image/png,image/gif"
          style="display:none"
          @change="handleFileChange"
        />
        <span class="editorial-link" @click="triggerUpload">更换头像</span>
      </div>

      <div class="profile-form">
        <div class="profile-form__item">
          <label class="editorial-form-label">用户名</label>
          <input v-model="form.username" class="editorial-input" />
        </div>
        <div class="profile-form__item">
          <label class="editorial-form-label">真实姓名</label>
          <input v-model="form.realName" class="editorial-input" />
        </div>
        <div class="profile-form__item">
          <label class="editorial-form-label">手机号</label>
          <input v-model="form.phone" class="editorial-input" />
        </div>
        <button class="editorial-btn" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存修改' }}
        </button>
      </div>
    </div>

    <!-- 修改密码 -->
    <div class="editorial-section">
      <h2 class="editorial-section-heading">修改密码</h2>
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

    <!-- 通知设置 -->
    <div class="editorial-section">
      <h2 class="editorial-section-heading">通知设置</h2>
      <div class="profile-settings">
        <div class="profile-settings__item">
          <span class="profile-settings__label">短信通知</span>
          <el-switch v-model="settingsForm.sms_enabled" />
        </div>
        <div class="profile-settings__item">
          <span class="profile-settings__label">ServerChan 推送</span>
          <el-switch v-model="settingsForm.push_enabled" />
          <button class="editorial-btn-outline" @click="showCredentialDialog = true">
            配置Key
          </button>
        </div>
      </div>
    </div>

    <!-- Key配置弹窗 -->
    <el-dialog
      v-model="showCredentialDialog"
      title="ServerChan Key 配置"
      width="420px"
      :close-on-click-modal="false"
    >
      <div v-if="credential">
        <label class="editorial-form-label">当前Key</label>
        <p style="font-family:var(--font-mono);font-size:13px;color:var(--color-text-muted)">
          {{ credential.credentialValue }}
        </p>
      </div>
      <div style="margin-top:16px">
        <label class="editorial-form-label">新Key</label>
        <input v-model="credentialForm.credentialValue" class="editorial-input" placeholder="输入 ServerChan SendKey" />
      </div>
      <template #footer>
        <button class="editorial-btn-outline" @click="showCredentialDialog = false">取消</button>
        <button class="editorial-btn" style="margin-left:8px;padding-left:16px;padding-right:16px" @click="handleSaveCredential">保存</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../store/user'
import {
  getProfile, updateProfile, changePassword, uploadAvatar,
  getSettings, updateSettings, getCredentials, saveCredential
} from '../../api/profile'

const userStore = useUserStore()

const fileInput = ref(null)
const saving = ref(false)
const changingPwd = ref(false)
const showCredentialDialog = ref(false)

const form = ref({ username: '', realName: '', phone: '' })
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const settingsForm = ref({ sms_enabled: true, push_enabled: true })
const credential = ref(null)
const credentialForm = ref({ credentialKey: 'serverchan_key', credentialValue: '' })

const avatarUrl = computed(() => userStore.userInfo?.avatarUrl || '')
const avatarLetter = computed(() => {
  const name = userStore.userInfo?.realName || userStore.userInfo?.username || '?'
  return name.charAt(0).toUpperCase()
})

onMounted(async () => {
  try {
    const res = await getProfile()
    const u = res.data
    form.value = { username: u.username, realName: u.realName || '', phone: u.phone || '' }
    userStore.setUserInfo({ ...userStore.userInfo, ...u })
  } catch {}

  try {
    const res = await getSettings()
    settingsForm.value = { sms_enabled: res.data.sms_enabled === 'true', push_enabled: res.data.push_enabled === 'true' }
  } catch {}

  try {
    const res = await getCredentials()
    const list = res.data || []
    credential.value = list.find(c => c.credentialKey === 'serverchan_key') || null
  } catch {}
})

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) { ElMessage.error('文件大小不能超过2MB'); return }
  try {
    const res = await uploadAvatar(file)
    userStore.setAvatar(res.data.avatarUrl)
    ElMessage.success('头像已更新')
  } catch {}
}

async function handleSave() {
  saving.value = true
  try {
    await updateProfile(form.value)
    userStore.setUserInfo({ ...userStore.userInfo, username: form.value.username, realName: form.value.realName, phone: form.value.phone })
    ElMessage.success('个人信息已更新')
  } catch {} finally { saving.value = false }
}

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
  } catch {} finally { changingPwd.value = false }
}

watch([() => settingsForm.value.sms_enabled, () => settingsForm.value.push_enabled], () => {
  updateSettings({
    settings: {
      sms_enabled: String(settingsForm.value.sms_enabled),
      push_enabled: String(settingsForm.value.push_enabled)
    }
  })
})

async function handleSaveCredential() {
  if (!credentialForm.value.credentialValue) { ElMessage.error('请输入Key'); return }
  try {
    await saveCredential(credentialForm.value)
    ElMessage.success('Key已保存')
    showCredentialDialog.value = false
    const res = await getCredentials()
    const list = res.data || []
    credential.value = list.find(c => c.credentialKey === 'serverchan_key') || null
  } catch {}
}
</script>

<style scoped>
.profile-page { max-width: 600px; }

.profile-avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.profile-avatar {
  width: 64px; height: 64px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--color-divider-strong);
}
.profile-avatar img {
  width: 100%; height: 100%;
  object-fit: cover;
}
.profile-avatar__placeholder {
  display: flex; align-items: center; justify-content: center;
  width: 100%; height: 100%;
  font-family: var(--font-serif); font-size: 24px;
  color: var(--color-text-muted); background: var(--color-surface);
}

.profile-form {}
.profile-form__item { margin-bottom: 16px; }
.profile-form .editorial-btn { width: 100%; margin-top: 8px; }

.profile-settings { display: flex; flex-direction: column; gap: 16px; }
.profile-settings__item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.profile-settings__label {
  font-size: 13px;
  color: var(--color-text);
}
</style>
