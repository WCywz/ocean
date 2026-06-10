<template>
  <div class="profile-page">
    <h1 class="editorial-page-title">系统设置</h1>
    <p class="editorial-page-subtitle">账户与系统信息</p>

    <div class="editorial-section">
      <h2 class="editorial-section-heading">联系信息</h2>
      <p class="editorial-section__text">
        如有问题或建议，可发送邮件至
        <a href="mailto:admin@ocean-forecast.cn" class="editorial-link">admin@ocean-forecast.cn</a>
      </p>
    </div>

    <div class="editorial-section">
      <h2 class="editorial-section-heading">账户注销</h2>
      <div class="warning-card">
        <p class="warning-card__text">注销后账户将被永久删除，所有数据不可恢复。</p>
        <button class="editorial-btn-outline" :disabled="deleting" @click="handleDeleteAccount">
          {{ deleting ? '注销中...' : '注销账户' }}
        </button>
      </div>
    </div>

    <div class="editorial-section">
      <h2 class="editorial-section-heading">系统版本</h2>
      <p class="editorial-section__text">当前版本：v{{ __APP_VERSION__ }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import { deleteAccount } from '../../api/profile'

const router = useRouter()
const userStore = useUserStore()
const deleting = ref(false)

async function handleDeleteAccount() {
  try {
    await ElMessageBox.confirm(
      '注销后账户将被永久删除，所有数据不可恢复。确定继续？',
      '确认注销',
      { type: 'warning', confirmButtonText: '确定注销', cancelButtonText: '取消' }
    )
    deleting.value = true
    await deleteAccount()
    userStore.logout()
    ElMessage.success('账户已注销')
    router.push('/')
  } catch (e) {
    if (e !== 'cancel') { console.error('注销失败', e); ElMessage.error('注销失败，请重试') }
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
.profile-page { max-width: 600px; }

.editorial-section__text {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-top: 0;
  line-height: 1.7;
}

.warning-card {
  margin-top: 15px;
  padding: 16px;
  background: #fef5f5;
  border-left: 3px solid var(--color-alert);
  border-radius: 0 4px 4px 0;
}
.warning-card__text {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 0 0 12px 0;
  line-height: 1.7;
}

[data-theme="dark"] .warning-card {
  background: #1a1114;
}
</style>
