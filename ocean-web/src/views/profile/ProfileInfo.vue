<template>
  <div class="profile-page">
    <h1 class="editorial-page-title">个人信息</h1>
    <p class="editorial-page-subtitle">管理您的账户基本信息</p>

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
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../store/user'
import { getProfile, updateProfile, uploadAvatar } from '../../api/profile'

const userStore = useUserStore()

const fileInput = ref(null)
const saving = ref(false)
const form = ref({ username: '', realName: '', phone: '' })

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
  } catch (e) { console.error('获取用户信息失败', e); ElMessage.error('获取用户信息失败，请刷新页面重试') }
})

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) { ElMessage.error('文件大小不能超过5MB'); return }
  try {
    const res = await uploadAvatar(file)
    userStore.setAvatar(res.data.avatarUrl)
    ElMessage.success('头像已更新')
  } catch (e) { console.error('头像上传失败', e); ElMessage.error('头像上传失败，请重试') }
}

async function handleSave() {
  if (form.value.phone && !/^1[3-9]\d{9}$/.test(form.value.phone)) {
    ElMessage.error('手机号格式不正确')
    return
  }
  saving.value = true
  try {
    await updateProfile(form.value)
    userStore.setUserInfo({ ...userStore.userInfo, username: form.value.username, realName: form.value.realName, phone: form.value.phone })
    ElMessage.success('个人信息已更新')
  } catch (e) { console.error('更新个人信息失败', e); ElMessage.error('保存失败，请重试') } finally { saving.value = false }
}
</script>

<style scoped>
.profile-page { max-width: 600px; }

.profile-avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
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

.profile-form__item { margin-bottom: 20px; }
.profile-form .editorial-btn { width: 100%; margin-top: 8px; }
</style>
