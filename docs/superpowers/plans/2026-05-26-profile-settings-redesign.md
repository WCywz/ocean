# 个人中心与设置中心重构 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将单文件 ProfileView.vue 重构为侧边栏导航 + 6 个子路由模块，主题切换从导航栏迁入设置页，新增模块占位

**Architecture:** ProfileLayout.vue 作为布局壳（侧边栏 + `<router-view>`），6 个子组件各自独立，嵌套路由在 `/app/profile/*` 下。现有业务逻辑从 ProfileView.vue 直接提取，无需重写

**Tech Stack:** Vue 3 (Composition API), Vue Router (nested routes), Element Plus (el-switch, el-dialog), Pinia (userStore), existing editorial CSS system

---

### Task 1: 创建 ProfileLayout.vue

**Files:**
- Create: `ocean-web/src/views/profile/ProfileLayout.vue`

- [ ] **Step 1: 编写布局壳组件**

```vue
<template>
  <div class="profile-layout">
    <aside class="profile-sidebar">
      <div class="profile-sidebar__label">个人中心</div>
      <nav class="profile-sidebar__nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="profile-sidebar__item"
          :class="{ 'profile-sidebar__item--active': isActive(item.path) }"
        >{{ item.label }}</router-link>
      </nav>
    </aside>
    <section class="profile-content">
      <router-view />
    </section>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const navItems = [
  { path: '/app/profile/info', label: '个人信息' },
  { path: '/app/profile/security', label: '账户安全' },
  { path: '/app/profile/notifications', label: '通知设置' },
  { path: '/app/profile/preferences', label: '显示偏好' },
  { path: '/app/profile/announcements', label: '系统公告' },
  { path: '/app/profile/settings', label: '系统设置' }
]

function isActive(path) {
  return route.path === path
}
</script>

<style scoped>
.profile-layout {
  display: flex;
  gap: 0;
  min-height: calc(100vh - 64px - 49px); /* nav + footer */
}

.profile-sidebar {
  width: 160px;
  flex-shrink: 0;
  padding: 32px 24px;
  border-right: 1px solid var(--color-divider-strong);
}

.profile-sidebar__label {
  font-size: 10px;
  color: var(--color-text-muted);
  letter-spacing: 0.1em;
  text-transform: uppercase;
  margin-bottom: 20px;
}

.profile-sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.profile-sidebar__item {
  font-size: 13px;
  color: var(--color-text-muted);
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 8px 0;
  text-decoration: none;
  transition: color 0.15s;
}

.profile-sidebar__item:hover {
  color: var(--color-text);
}

.profile-sidebar__item--active {
  color: var(--color-text);
  font-weight: 600;
}

.profile-content {
  flex: 1;
  padding: 36px 40px;
  min-width: 0;
}
</style>
```

- [ ] **Step 2: 验证文件无语法错误**

Run: `npx eslint oceon-web/src/views/profile/ProfileLayout.vue --fix 2>&1 || true`
Expected: No critical errors

- [ ] **Step 3: 提交**

```bash
git add ocean-web/src/views/profile/ProfileLayout.vue
git commit -m "feat: add ProfileLayout with sidebar navigation"
```

---

### Task 2: 创建 ProfileInfo.vue

**Files:**
- Create: `ocean-web/src/views/profile/ProfileInfo.vue`

- [ ] **Step 1: 从 ProfileView.vue 提取个人信息部分**

```vue
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
  } catch (e) { console.error('获取用户信息失败', e) }
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
  } catch (e) { console.error('头像上传失败', e) }
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
  } catch (e) { console.error('更新个人信息失败', e) } finally { saving.value = false }
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
```

- [ ] **Step 2: 提交**

```bash
git add ocean-web/src/views/profile/ProfileInfo.vue
git commit -m "feat: add ProfileInfo component extracted from ProfileView"
```

---

### Task 3: 创建 ProfileSecurity.vue

**Files:**
- Create: `ocean-web/src/views/profile/ProfileSecurity.vue`

- [ ] **Step 1: 从 ProfileView.vue 提取密码修改部分**

```vue
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
  } catch (e) { console.error('密码修改失败', e) } finally { changingPwd.value = false }
}
</script>

<style scoped>
.profile-page { max-width: 600px; }
.profile-form__item { margin-bottom: 20px; }
.profile-form .editorial-btn { width: 100%; margin-top: 8px; }
</style>
```

- [ ] **Step 2: 提交**

```bash
git add ocean-web/src/views/profile/ProfileSecurity.vue
git commit -m "feat: add ProfileSecurity component extracted from ProfileView"
```

---

### Task 4: 创建 ProfileNotifications.vue

**Files:**
- Create: `ocean-web/src/views/profile/ProfileNotifications.vue`

- [ ] **Step 1: 从 ProfileView.vue 提取通知设置 + 密钥配置部分**

```vue
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
  } catch (e) { console.error('获取设置失败', e) }

  try {
    const res = await getCredentials()
    const list = res.data || []
    credential.value = list.find(c => c.credentialKey === 'serverchan_key') || null
  } catch (e) { console.error('获取密钥失败', e) }

  loaded.value = true
})

watch([() => settingsForm.value.sms_enabled, () => settingsForm.value.push_enabled], () => {
  if (!loaded.value) return
  updateSettings({
    settings: {
      sms_enabled: String(settingsForm.value.sms_enabled),
      push_enabled: String(settingsForm.value.push_enabled)
    }
  })
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
  } catch (e) { console.error('保存密钥失败', e) }
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
```

- [ ] **Step 2: 提交**

```bash
git add ocean-web/src/views/profile/ProfileNotifications.vue
git commit -m "feat: add ProfileNotifications component extracted from ProfileView"
```

---

### Task 5: 创建 ProfilePreferences.vue

**Files:**
- Create: `ocean-web/src/views/profile/ProfilePreferences.vue`

- [ ] **Step 1: 编写显示偏好组件**

```vue
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
```

- [ ] **Step 2: 提交**

```bash
git add ocean-web/src/views/profile/ProfilePreferences.vue
git commit -m "feat: add ProfilePreferences with theme toggle and data display placeholder"
```

---

### Task 6: 创建占位组件

**Files:**
- Create: `ocean-web/src/views/profile/ProfileAnnouncements.vue`
- Create: `ocean-web/src/views/profile/ProfileSettings.vue`

- [ ] **Step 1: 编写 ProfileAnnouncements.vue**

```vue
<template>
  <div class="profile-page">
    <h1 class="editorial-page-title">系统公告</h1>
    <p class="editorial-page-subtitle">查看系统更新与维护通知</p>
    <p style="font-size:13px;color:var(--color-text-muted);padding:60px 0;text-align:center">即将上线</p>
  </div>
</template>

<style scoped>
.profile-page { max-width: 600px; }
</style>
```

- [ ] **Step 2: 编写 ProfileSettings.vue**

```vue
<template>
  <div class="profile-page">
    <h1 class="editorial-page-title">系统设置</h1>
    <p class="editorial-page-subtitle">隐私、账户与系统信息</p>

    <div class="editorial-section">
      <h2 class="editorial-section-heading">隐私设置</h2>
      <p style="font-size:13px;color:var(--color-text-muted);padding:40px 0;text-align:center">即将上线</p>
    </div>

    <div class="editorial-section">
      <h2 class="editorial-section-heading">账户注销</h2>
      <p style="font-size:13px;color:var(--color-text-muted);padding:40px 0;text-align:center">即将上线</p>
    </div>

    <div class="editorial-section">
      <h2 class="editorial-section-heading">关于信息</h2>
      <p style="font-size:13px;color:var(--color-text-muted);padding:40px 0;text-align:center">即将上线</p>
    </div>

    <div class="editorial-section">
      <h2 class="editorial-section-heading">系统版本</h2>
      <p class="editorial-narrative">当前版本：v{{ __APP_VERSION__ }}</p>
    </div>
  </div>
</template>

<style scoped>
.profile-page { max-width: 600px; }
</style>
```

- [ ] **Step 3: 提交**

```bash
git add ocean-web/src/views/profile/ProfileAnnouncements.vue ocean-web/src/views/profile/ProfileSettings.vue
git commit -m "feat: add placeholder components for announcements and settings"
```

---

### Task 7: 更新路由

**Files:**
- Modify: `ocean-web/src/router/index.js:87-92`

- [ ] **Step 1: 将单一路由替换为嵌套路由**

Replace:
```js
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/profile/ProfileView.vue'),
        meta: { title: '个人中心' }
      }
```

With:
```js
      {
        path: 'profile',
        component: () => import('../views/profile/ProfileLayout.vue'),
        redirect: '/app/profile/info',
        children: [
          {
            path: 'info',
            name: 'ProfileInfo',
            component: () => import('../views/profile/ProfileInfo.vue'),
            meta: { title: '个人信息' }
          },
          {
            path: 'security',
            name: 'ProfileSecurity',
            component: () => import('../views/profile/ProfileSecurity.vue'),
            meta: { title: '账户安全' }
          },
          {
            path: 'notifications',
            name: 'ProfileNotifications',
            component: () => import('../views/profile/ProfileNotifications.vue'),
            meta: { title: '通知设置' }
          },
          {
            path: 'preferences',
            name: 'ProfilePreferences',
            component: () => import('../views/profile/ProfilePreferences.vue'),
            meta: { title: '显示偏好' }
          },
          {
            path: 'announcements',
            name: 'ProfileAnnouncements',
            component: () => import('../views/profile/ProfileAnnouncements.vue'),
            meta: { title: '系统公告' }
          },
          {
            path: 'settings',
            name: 'ProfileSettings',
            component: () => import('../views/profile/ProfileSettings.vue'),
            meta: { title: '系统设置' }
          }
        ]
      }
```

- [ ] **Step 2: 提交**

```bash
git add ocean-web/src/router/index.js
git commit -m "feat: replace profile route with nested routes for 6 sub-pages"
```

---

### Task 8: 更新 MainLayout.vue — 移除主题切换按钮

**Files:**
- Modify: `ocean-web/src/layout/MainLayout.vue`

- [ ] **Step 1: 从模板中移除主题切换按钮**

Remove lines 76-91:
```html
      <!-- Theme toggle -->
      <div class="nav-theme-toggle" @click.stop="toggleDropdown('theme')">
        <div class="nav-theme-toggle__btn" :title="themeLabel">
          <svg v-if="themeResolved === 'dark'" ...>...</svg>
          <svg v-else ...>...</svg>
        </div>
        <div v-show="activeDropdown === 'theme'" class="nav-user-dropdown" style="min-width: 140px;">
          ...
        </div>
      </div>
```

- [ ] **Step 2: 从 script 中移除主题相关代码**

Remove lines 126, 129-139:
```js
const { mode: themeMode, resolved: themeResolved, setMode } = useTheme()

const themeOptions = [
  { value: 'system', label: '跟随系统' },
  { value: 'light', label: '浅色' },
  { value: 'dark', label: '深色' }
]

const themeLabel = computed(() => {
  const m = themeMode.value
  const opt = themeOptions.find(o => o.value === m)
  return opt ? opt.label : '跟随系统'
})
```

Also remove the `useTheme` import on line 120:
```js
import { useTheme } from '../composables/useTheme'
```

- [ ] **Step 3: 从 style 中移除主题切换样式**

Remove lines 210-225:
```css
.nav-theme-toggle { ... }
.nav-theme-toggle__btn { ... }
.nav-theme-toggle__btn:hover { ... }
```

- [ ] **Step 4: 提交**

```bash
git add ocean-web/src/layout/MainLayout.vue
git commit -m "feat: remove theme toggle from navigation bar, moved to ProfilePreferences"
```

---

### Task 9: 删除 ProfileView.vue

**Files:**
- Delete: `ocean-web/src/views/profile/ProfileView.vue`

- [ ] **Step 1: 删除旧文件并提交**

```bash
git rm ocean-web/src/views/profile/ProfileView.vue
git commit -m "refactor: remove ProfileView.vue, replaced by 7 modular components"
```

---

### Task 10: 验证

**Files:**
- 无新文件

- [ ] **Step 1: 启动开发服务器**

Run: `cd ocean-web && npm run dev`
Expected: 开发服务器启动成功，无编译错误

- [ ] **Step 2: 测试导航**

Manually verify in browser:
1. 访问 `http://localhost:3000/app/profile` → 自动跳转到 `/app/profile/info`
2. 点击侧边栏各导航项 → 正确切换子页面
3. 个人信息页：表单可编辑、头像可上传、保存生效
4. 账户安全页：密码修改正常
5. 通知设置页：开关切换自动保存、Key 配置弹窗正常
6. 显示偏好页：主题切换生效（浅色/深色/跟随系统）
7. 系统公告页：显示「即将上线」
8. 系统设置页：各节显示「即将上线」+ 版本号正确
9. 导航栏确认主题切换按钮已移除
10. 顶部导航栏「个人中心」菜单项点击后正确进入 ProfileLayout
11. 深色模式下各页面颜色正常

- [ ] **Step 3: 全部通过后提交**

```bash
git add -A
git commit -m "chore: final verification of profile/settings redesign"
```
