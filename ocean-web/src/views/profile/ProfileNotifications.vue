<template>
  <div class="profile-page">
    <h1 class="editorial-page-title">通知设置</h1>
    <p class="editorial-page-subtitle">管理通知渠道与推送配置</p>

    <div class="editorial-section">
      <h2 class="editorial-section-heading">通知渠道</h2>
      <div class="profile-settings">
        <div class="profile-settings__item">
          <span class="profile-settings__label">短信通知</span>
          <el-switch v-model="settingsForm.sms_enabled" />
        </div>
        <div class="profile-settings__item">
          <span class="profile-settings__label">ServerChan 推送</span>
          <el-switch v-model="settingsForm.push_enabled" />
        </div>
      </div>
    </div>

    <div class="editorial-section">
      <h2 class="editorial-section-heading">推送配置</h2>
      <div v-if="credential" class="profile-credential">
        <span class="profile-settings__label">ServerChan Key</span>
        <span class="profile-credential__value">{{ credential.credentialValue }}</span>
      </div>
      <button class="editorial-btn-outline" @click="showCredentialDialog = true">
        {{ credential ? '更换 Key' : '配置 Key' }}
      </button>
    </div>

    <el-dialog
      v-model="showCredentialDialog"
      title="ServerChan Key 配置"
      width="420px"
      :close-on-click-modal="false"
    >
      <div v-if="credential">
        <label class="editorial-form-label">当前 Key</label>
        <p style="font-family:var(--font-mono);font-size:13px;color:var(--color-text-muted)">
          {{ credential.credentialValue }}
        </p>
      </div>
      <div style="margin-top:16px">
        <label class="editorial-form-label">新 Key</label>
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
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getSettings, updateSettings, getCredentials, saveCredential } from '../../api/profile'

const showCredentialDialog = ref(false)
const loaded = ref(false)
const settingsForm = ref({ sms_enabled: true, push_enabled: true })
const credential = ref(null)
const credentialForm = ref({ credentialKey: 'serverchan_key', credentialValue: '' })

onMounted(async () => {
  try {
    const res = await getSettings()
    settingsForm.value = { sms_enabled: res.data.sms_enabled === 'true', push_enabled: res.data.push_enabled === 'true' }
  } catch (e) { console.error('获取设置失败', e); ElMessage.error('获取设置失败，请刷新页面重试') }

  try {
    const res = await getCredentials()
    const list = res.data || []
    credential.value = list.find(c => c.credentialKey === 'serverchan_key') || null
  } catch (e) { console.error('获取密钥失败', e); ElMessage.error('获取密钥配置失败，请刷新页面重试') }

  loaded.value = true
})

watch([() => settingsForm.value.sms_enabled, () => settingsForm.value.push_enabled], async () => {
  if (!loaded.value) return
  try {
    await updateSettings({
      settings: {
        sms_enabled: String(settingsForm.value.sms_enabled),
        push_enabled: String(settingsForm.value.push_enabled)
      }
    })
  } catch (e) {
    console.error('保存设置失败', e)
    ElMessage.error('设置保存失败，请重试')
  }
})

async function handleSaveCredential() {
  if (!credentialForm.value.credentialValue) { ElMessage.error('请输入 Key'); return }
  try {
    await saveCredential(credentialForm.value)
    ElMessage.success('Key 已保存')
    showCredentialDialog.value = false
    const res = await getCredentials()
    const list = res.data || []
    credential.value = list.find(c => c.credentialKey === 'serverchan_key') || null
  } catch (e) { console.error('保存密钥失败', e); ElMessage.error('保存 Key 失败，请重试') }
}
</script>

<style scoped>
.profile-page { max-width: 600px; }

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

.profile-credential {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.profile-credential__value {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--color-text-muted);
}
</style>
