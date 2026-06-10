<template>
  <div class="profile-page">
    <h1 class="editorial-page-title">个人信息</h1>
    <p class="editorial-page-subtitle">管理您的账户基本信息</p>

    <div class="profile-body">
      <div class="profile-avatar-col">
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

        <div class="profile-theme-section">
          <h3 class="profile-theme-section__heading">外观主题</h3>
          <div class="profile-theme-options">
            <label
              v-for="opt in themeOptions"
              :key="opt.value"
              class="profile-theme-option"
              :class="{ 'profile-theme-option--active': themeMode === opt.value }"
            >
              <input
                type="radio"
                :value="opt.value"
                :checked="themeMode === opt.value"
                @change="setMode(opt.value)"
                class="profile-theme-option__radio"
              />
              <span class="profile-theme-option__label">{{ opt.label }}</span>
            </label>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../store/user'
import { useTheme } from '../../composables/useTheme'
import { getProfile, updateProfile, uploadAvatar } from '../../api/profile'

const userStore = useUserStore()
const { mode: themeMode, setMode } = useTheme()
const themeOptions = [
  { value: 'system', label: '跟随系统' },
  { value: 'light', label: '浅色' },
  { value: 'dark', label: '深色' }
]

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
.profile-page { max-width: 720px; }

.profile-body {
  display: flex;
  gap: 40px;
  align-items: flex-start;
}

.profile-avatar-col {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.profile-avatar {
  width: 120px; height: 120px;
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
  font-family: var(--font-serif); font-size: 48px;
  color: var(--color-text-muted); background: var(--color-surface);
}

.profile-form {
  flex: 1;
  min-width: 0;
}
.profile-form__item { margin-bottom: 20px; }
.profile-form .editorial-btn { width: 100%; margin-top: 8px; }

.profile-theme-section {
  margin-top: 36px;
  padding-top: 24px;
  border-top: 1px solid var(--color-divider-strong);
}
.profile-theme-section__heading {
  font-size: 11px;
  color: var(--color-text-muted);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  margin: 0 0 12px 0;
}
.profile-theme-options {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.profile-theme-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.15s;
}
.profile-theme-option:hover {
  background: var(--color-surface);
}
.profile-theme-option__radio {
  accent-color: var(--color-text);
  width: 14px;
  height: 14px;
  margin: 0;
}
.profile-theme-option__label {
  font-size: 14px;
  color: var(--color-text);
}
.profile-theme-option--active .profile-theme-option__label {
  font-weight: 600;
}
</style>
