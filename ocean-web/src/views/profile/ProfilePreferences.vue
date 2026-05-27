<template>
  <div class="profile-page">
    <h1 class="editorial-page-title">显示偏好</h1>
    <p class="editorial-page-subtitle">自定义界面外观与数据展示</p>

    <div class="editorial-section">
      <h2 class="editorial-section-heading">外观主题</h2>
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

    <div class="editorial-section">
      <h2 class="editorial-section-heading">数据展示</h2>
      <p style="font-size:13px;color:var(--color-text-muted);padding:40px 0;text-align:center">即将上线</p>
    </div>
  </div>
</template>

<script setup>
import { useTheme } from '../../composables/useTheme'

const { mode: themeMode, setMode } = useTheme()

const themeOptions = [
  { value: 'system', label: '跟随系统' },
  { value: 'light', label: '浅色' },
  { value: 'dark', label: '深色' }
]
</script>

<style scoped>
.profile-page { max-width: 600px; }

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
