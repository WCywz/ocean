# 模型板块前端改造 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构模型板块前端：引入侧边栏导航，拆分为 5 个子页面，新建告警管理页，占位调度总览和模型对比。

**Architecture:** 新增 `ModelLayout.vue` 作为模型板块的布局壳（侧边栏 + `<router-view>`），模型路由改为嵌套结构。`ModelView.vue` 拆分为 `ModelList.vue`（列表）和 `ModelDetail.vue`（详情+版本）。后端仅新增告警分页接口。

**Tech Stack:** Vue 3 + Vue Router + Element Plus + Axios，Java 21 + Spring Boot + MyBatis-Plus

---

### Task 1: 后端 — 新增告警分页查询接口

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/service/AlertEventService.java`
- Modify: `ocean-server/src/main/java/com/ocean/service/impl/AlertEventServiceImpl.java`
- Modify: `ocean-server/src/main/java/com/ocean/controller/AlertEventController.java`

- [ ] **Step 1: 在 Service 接口中添加分页方法**

在 `AlertEventService.java` 的 `markAllAsRead()` 之后添加：

```java
import com.baomidou.mybatisplus.core.metadata.IPage;

IPage<AlertEventVO> getAlertPage(Integer pageNum, Integer pageSize,
                                  LocalDateTime startTime, LocalDateTime endTime,
                                  Long modelId, String alertType, Integer isRead);
```

- [ ] **Step 2: 在 ServiceImpl 中实现分页逻辑**

在 `AlertEventServiceImpl.java` 的类体中添加：

```java
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDateTime;

@Override
public IPage<AlertEventVO> getAlertPage(Integer pageNum, Integer pageSize,
                                         LocalDateTime startTime, LocalDateTime endTime,
                                         Long modelId, String alertType, Integer isRead) {
    LambdaQueryWrapper<AlertEvent> wrapper = new LambdaQueryWrapper<>();
    if (startTime != null) {
        wrapper.ge(AlertEvent::getCreateTime, startTime);
    }
    if (endTime != null) {
        wrapper.le(AlertEvent::getCreateTime, endTime);
    }
    if (modelId != null) {
        wrapper.eq(AlertEvent::getModelId, modelId);
    }
    if (alertType != null && !alertType.isEmpty()) {
        wrapper.eq(AlertEvent::getAlertType, alertType);
    }
    if (isRead != null) {
        wrapper.eq(AlertEvent::getIsRead, isRead);
    }
    wrapper.orderByDesc(AlertEvent::getCreateTime);

    Page<AlertEvent> page = new Page<>(pageNum, pageSize);
    Page<AlertEvent> result = alertEventMapper.selectPage(page, wrapper);
    return result.convert(this::toVO);
}
```

- [ ] **Step 3: 在 Controller 中添加分页查询端点**

在 `AlertEventController.java` 的 `markAllAsRead()` 方法之后添加：

```java
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@GetMapping("/page")
public Result<IPage<AlertEventVO>> getAlertPage(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
        @RequestParam(required = false) Long modelId,
        @RequestParam(required = false) String alertType,
        @RequestParam(required = false) Integer isRead) {
    return Result.success(alertEventService.getAlertPage(pageNum, pageSize,
            startTime, endTime, modelId, alertType, isRead));
}
```

- [ ] **Step 4: 编译验证**

```bash
cd ocean-server && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/service/AlertEventService.java \
        ocean-server/src/main/java/com/ocean/service/impl/AlertEventServiceImpl.java \
        ocean-server/src/main/java/com/ocean/controller/AlertEventController.java
git commit -m "feat: 告警分页查询接口 GET /api/alert/page"
```

---

### Task 2: 前端 — 新建 ModelLayout（侧边栏布局壳）

**Files:**
- Create: `ocean-web/src/layout/ModelLayout.vue`

- [ ] **Step 1: 创建 ModelLayout.vue**

```vue
<template>
  <div class="model-shell" :style="{ background: 'var(--color-bg)', minHeight: 'calc(100vh - 48px)' }">
    <aside class="model-sidebar">
      <div class="model-sidebar__section">模型管理</div>
      <router-link
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        class="model-sidebar__item"
        :class="{ 'model-sidebar__item--active': isActive(item.match) }"
      >{{ item.label }}</router-link>

      <div class="model-sidebar__divider"></div>
      <div class="model-sidebar__section">快捷操作</div>
      <router-link to="/app/model" class="model-sidebar__action">+ 新建模型</router-link>
    </aside>

    <main class="model-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const menuItems = [
  { label: '模型管理', path: '/app/model', match: '/app/model' },
  { label: '调度总览', path: '/app/model/schedule', match: '/app/model/schedule' },
  { label: '运行监控', path: '/app/model/monitor', match: '/app/model/monitor' },
  { label: '告警管理', path: '/app/model/alerts', match: '/app/model/alerts' },
  { label: '模型对比', path: '/app/model/compare', match: '/app/model/compare' },
]

function isActive(match) {
  if (match === '/app/model') {
    // 精确匹配 /app/model 或 /app/model/:id（数字）
    return route.path === '/app/model' || /^\/app\/model\/\d+$/.test(route.path)
  }
  return route.path.startsWith(match)
}
</script>

<style scoped>
.model-shell {
  display: flex;
}

.model-sidebar {
  width: 200px;
  flex-shrink: 0;
  background: var(--color-surface);
  border-right: 1px solid var(--color-divider-strong);
  padding-top: 16px;
  min-height: calc(100vh - 48px);
}

.model-sidebar__section {
  padding: 8px 16px 4px;
  font-size: 10px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.model-sidebar__item {
  display: block;
  padding: 8px 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
  text-decoration: none;
  border-left: 3px solid transparent;
  transition: color 0.15s, background 0.15s;
}

.model-sidebar__item:hover {
  color: var(--color-text);
  background: var(--color-bg);
}

.model-sidebar__item--active {
  color: var(--color-text);
  font-weight: 600;
  border-left-color: var(--color-text);
  background: var(--color-bg);
}

.model-sidebar__divider {
  margin: 16px 16px;
  border-top: 1px solid var(--color-divider);
}

.model-sidebar__action {
  display: block;
  padding: 6px 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  text-decoration: none;
}

.model-sidebar__action:hover {
  color: var(--color-text);
}

.model-content {
  flex: 1;
  min-width: 0;
  padding: 24px 28px;
  background: var(--color-bg);
}
</style>
```

- [ ] **Step 2: Verify file is well-formed**

```bash
# Manual check: the file should compile without errors, no missing imports
```

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/layout/ModelLayout.vue
git commit -m "feat: 新建 ModelLayout 侧边栏布局壳"
```

---

### Task 3: 前端 — 重写模型列表页 ModelList.vue

**Files:**
- Create: `ocean-web/src/views/model/ModelList.vue`
- Keep (no change): `ocean-web/src/views/model/ModelDialog.vue`

**说明：** 从 `ModelView.vue` 提取模型列表部分，去掉版本展开/折叠、RunningOverview、状态栏，简化为干净的模型卡片列表。

- [ ] **Step 1: 创建 ModelList.vue**

```vue
<template>
  <div>
    <h1 class="editorial-page-title">模型管理</h1>
    <p class="editorial-page-subtitle">Model Management · 共 {{ total }} 个模型</p>

    <div class="editorial-filter-bar">
      <select v-model="query.modelType" class="editorial-select" style="width: 160px;">
        <option value="">全部类型</option>
        <option value="SST">海表温度 (SST)</option>
        <option value="CHL">叶绿素浓度 (CHL)</option>
        <option value="SALINITY">盐度 (SALINITY)</option>
      </select>
      <input v-model="query.keyword" class="editorial-search" placeholder="模型名称" style="width: 180px;" @keyup.enter="handleSearch" />
      <button class="editorial-btn-outline" @click="handleSearch">查询</button>
      <button class="editorial-btn-outline" @click="handleReset">重置</button>
      <span style="flex: 1;"></span>
      <button class="editorial-btn-outline" @click="handleAddModel">+ 新增模型</button>
    </div>

    <div v-if="tableData.length === 0 && !loading" style="text-align: center; padding: 60px 0; color: var(--color-text-muted); font-size: 14px;">
      暂无模型数据
    </div>

    <div v-else v-loading="loading" style="min-height: 200px;">
      <div v-for="row in tableData" :key="row.id" class="model-card" @click="goDetail(row)" :style="{ borderLeftColor: row._runningCount > 0 ? '#22c55e' : '#ccc' }">
        <div class="model-card__header">
          <div class="model-card__info">
            <span class="model-card__name">{{ row.modelName }}</span>
            <span class="model-card__meta">
              类型：{{ row.modelType }} &ensp;|&ensp; 版本：{{ row.versionCount ?? 0 }} 个<span v-if="row._runningCount > 0">（{{ row._runningCount }} 个运行中）</span>
            </span>
          </div>
          <span style="display: flex; gap: 12px;" @click.stop>
            <a class="editorial-link" @click="goDetail(row)">管理版本</a>
            <a class="editorial-link" @click="handleEditModel(row)">编辑</a>
            <a class="editorial-link editorial-link--muted" @click="handleDeleteModel(row)">删除</a>
          </span>
        </div>
        <div class="model-card__desc" v-if="row.description">{{ row.description }}</div>
        <div class="model-card__time">创建时间：{{ formatDate(row.createTime) }}</div>
      </div>
    </div>

    <div class="editorial-pagination" v-if="total > 0">
      <span>共 {{ total }} 条</span>
      <select v-model="query.pageSize" class="editorial-select" style="width: 80px;" @change="loadModels">
        <option :value="10">10</option>
        <option :value="20">20</option>
        <option :value="50">50</option>
      </select>
      <a class="editorial-link" @click="prevPage">&larr;</a>
      <span class="editorial-pagination__page editorial-pagination__page--active">{{ query.pageNum }}</span>
      <a class="editorial-link" @click="nextPage">&rarr;</a>
    </div>

    <ModelDialog v-model="modelDialogVisible" :model="editingModel" @submit="handleModelSubmit" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getModelPage, addModel, updateModel, deleteModel, getRunningVersions } from '../../api/model'
import { ElMessage, ElMessageBox } from 'element-plus'
import ModelDialog from './ModelDialog.vue'

const router = useRouter()

const query = reactive({ pageNum: 1, pageSize: 10, modelType: '', keyword: '' })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const runningVersions = ref([])

const modelDialogVisible = ref(false)
const editingModel = ref(null)

onMounted(() => { loadModels(); loadRunning() })

async function loadModels() {
  loading.value = true
  try {
    const res = await getModelPage({ ...query })
    const records = res.data.records || []
    records.forEach(r => { r._runningCount = runningVersions.value.filter(v => v.modelId === r.id).length })
    tableData.value = records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function loadRunning() {
  try {
    const res = await getRunningVersions()
    runningVersions.value = res.data || []
    for (const r of tableData.value) {
      r._runningCount = runningVersions.value.filter(v => v.modelId === r.id).length
    }
  } catch { runningVersions.value = [] }
}

function handleSearch() { query.pageNum = 1; loadModels() }
function handleReset() { query.modelType = ''; query.keyword = ''; query.pageNum = 1; loadModels() }
function prevPage() { if (query.pageNum > 1) { query.pageNum--; loadModels() } }
function nextPage() { query.pageNum++; loadModels() }

function goDetail(row) { router.push(`/app/model/${row.id}`) }

function handleAddModel() { editingModel.value = null; modelDialogVisible.value = true }

function handleEditModel(row) { editingModel.value = { ...row }; modelDialogVisible.value = true }

async function handleModelSubmit(data) {
  try {
    if (data.id) { await updateModel(data.id, data); ElMessage.success('模型更新成功') }
    else { await addModel(data); ElMessage.success('模型创建成功') }
    modelDialogVisible.value = false
    loadModels()
  } catch { /* interceptor handles */ }
}

async function handleDeleteModel(row) {
  try {
    await ElMessageBox.confirm(`确定要删除模型 "${row.modelName}" 及其所有版本吗？`, '删除确认', { type: 'warning' })
    await deleteModel(row.id)
    ElMessage.success('模型已删除')
    loadModels()
    loadRunning()
  } catch { /* cancelled */ }
}

function formatDate(d) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 10)
}
</script>

<style scoped>
.model-card {
  border: 1px solid var(--color-divider-strong);
  background: var(--color-bg);
  overflow: hidden;
  margin-bottom: 6px;
  cursor: pointer;
  border-left: 3px solid var(--color-border);
}

.model-card:hover { background: var(--color-surface); }

.model-card__header {
  display: flex;
  align-items: center;
  padding: 10px 14px;
}

.model-card__info { flex: 1; min-width: 0; }

.model-card__name {
  font-weight: 600;
  color: var(--color-text);
  font-size: 14px;
  display: block;
}

.model-card__meta {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 2px;
  display: block;
}

.model-card__desc {
  padding: 0 14px 6px 14px;
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.model-card__time {
  padding: 0 14px 10px 14px;
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
```

- [ ] **Step 2: 验证文件**

```bash
# Check: no imports reference ModelView-specific components
# Check: goDetail navigates to /app/model/:id
```

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/views/model/ModelList.vue
git commit -m "feat: 新建模型列表页 ModelList，去除版本折叠逻辑"
```

---

### Task 4: 前端 — 新建模型详情页 ModelDetail.vue

**Files:**
- Create: `ocean-web/src/views/model/ModelDetail.vue`
- Keep (no change): `ocean-web/src/views/model/VersionDialog.vue`

**说明：** 展示单个模型的详情，包含模型信息编辑 + 该模型下的版本列表 CRUD。版本启停、RunningOverview 移入本页。

- [ ] **Step 1: 创建 ModelDetail.vue**

```vue
<template>
  <div>
    <div style="margin-bottom: 24px;">
      <router-link to="/app/model" class="editorial-link">&larr; 返回模型列表</router-link>
    </div>

    <h1 class="editorial-page-title">{{ model.modelName || '加载中...' }}</h1>
    <p class="editorial-page-subtitle">
      类型：{{ model.modelType }} &ensp;|&ensp; 版本：{{ versions.length }} 个
      <span v-if="runningCount > 0">（{{ runningCount }} 个运行中）</span>
    </p>

    <div v-if="versions.length > 0" style="margin-bottom: 24px;">
      <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 10px;">
        <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #22c55e;"></span>
        <span style="font-size: 10px; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.1em;">运行概览</span>
        <span style="font-size: 12px; color: var(--color-text-muted);">{{ runningCount }} 个版本运行中</span>
      </div>
      <div style="display: flex; flex-wrap: wrap; gap: 10px;">
        <div v-for="v in runningVersions" :key="v.id" style="display: flex; align-items: center; gap: 10px; border: 1px solid var(--color-border-light); padding: 8px 14px; font-size: 13px;">
          <span style="font-weight: 600; color: var(--color-text);">{{ model.modelName }}</span>
          <span style="background: var(--color-border-light); color: var(--color-text-secondary); padding: 1px 7px; font-size: 11px; font-weight: 500;">{{ v.versionLabel }}</span>
          <span style="color: #22c55e; font-size: 12px;">运行中</span>
          <a class="editorial-link" style="color: #ef4444;" @click="handleStopVersion(v)">停止</a>
        </div>
      </div>
    </div>

    <div style="margin-bottom: 24px;">
      <button class="editorial-btn-outline" @click="showEditModel = !showEditModel">
        {{ showEditModel ? '收起编辑' : '编辑模型信息' }}
      </button>
    </div>

    <div v-if="showEditModel" style="margin-bottom: 28px; padding: 20px; border: 1px solid var(--color-divider-strong); background: var(--color-surface);">
      <div style="margin-bottom: 14px;">
        <label class="editorial-form-label">模型名称</label>
        <input v-model="editForm.modelName" class="editorial-input" />
      </div>
      <div style="margin-bottom: 14px;">
        <label class="editorial-form-label">模型类型</label>
        <select v-model="editForm.modelType" class="editorial-select">
          <option value="SST">海表温度 (SST)</option>
          <option value="CHL">叶绿素浓度 (CHL)</option>
          <option value="SALINITY">盐度 (SALINITY)</option>
        </select>
      </div>
      <div style="margin-bottom: 14px;">
        <label class="editorial-form-label">模型介绍</label>
        <textarea v-model="editForm.description" class="editorial-input" rows="3" style="resize: vertical;"></textarea>
      </div>
      <button class="editorial-btn-outline" @click="handleUpdateModel">保存</button>
    </div>

    <div class="editorial-filter-bar">
      <span style="font-size: 12px; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.08em; font-weight: 600;">版本列表</span>
      <span style="flex: 1;"></span>
      <button class="editorial-btn-outline" @click="handleAddVersion">+ 新建版本</button>
    </div>

    <div v-if="versions.length === 0 && !versionLoading" style="text-align: center; padding: 40px 0; color: var(--color-text-muted); font-size: 13px;">暂无版本</div>

    <div v-else v-loading="versionLoading" style="min-height: 120px;">
      <div v-for="v in versions" :key="v.id" class="version-row">
        <span class="version-label">{{ v.versionLabel }}</span>
        <span style="color: var(--color-text-secondary); font-size: 12px; min-width: 80px;">{{ v.cronExpression }}</span>
        <span style="font-size: 12px; min-width: 44px;" :style="{ color: v.status === 'RUNNING' ? '#22c55e' : '#ef4444' }">{{ statusMap[v.status] }}</span>
        <span class="version-ds" :title="v.dataSource">{{ v.dataSource || '-' }}</span>
        <span style="display: flex; gap: 10px;">
          <a v-if="v.status !== 'RUNNING'" class="editorial-link" style="color: #22c55e;" @click="handleToggleVersion(v, 'RUNNING')">启动</a>
          <a v-else class="editorial-link" style="color: #ef4444;" @click="handleToggleVersion(v, 'STOPPED')">停止</a>
          <a class="editorial-link" @click="handleEditVersion(v)">编辑</a>
          <a class="editorial-link editorial-link--muted" @click="handleDeleteVersion(v)">删除</a>
        </span>
      </div>
    </div>

    <VersionDialog v-model="versionDialogVisible" :version="editingVersion" :model-name="model.modelName || ''" :next-version-label="nextVersionLabel" @submit="handleVersionSubmit" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getModelById, updateModel, getModelVersions, addVersion, updateVersion, deleteVersion, toggleVersionStatus } from '../../api/model'
import { ElMessage, ElMessageBox } from 'element-plus'
import VersionDialog from './VersionDialog.vue'

const route = useRoute()
const router = useRouter()
const modelId = computed(() => Number(route.params.id))

const statusMap = { RUNNING: '运行中', STOPPED: '已停止', ERROR: '异常' }

const model = ref({})
const versions = ref([])
const versionLoading = ref(false)
const showEditModel = ref(false)

const editForm = reactive({ modelName: '', modelType: 'SST', description: '' })

const versionDialogVisible = ref(false)
const editingVersion = ref(null)

const runningVersions = computed(() => versions.value.filter(v => v.status === 'RUNNING'))
const runningCount = computed(() => runningVersions.value.length)

const nextVersionLabel = computed(() => {
  if (!versions.value.length) return 'v1'
  const nums = versions.value.map(v => parseInt(v.versionLabel.replace('v', ''))).filter(n => !isNaN(n))
  return nums.length ? `v${Math.max(...nums) + 1}` : 'v1'
})

onMounted(() => loadAll())
watch(modelId, () => loadAll())

async function loadAll() {
  try {
    const res = await getModelById(modelId.value)
    model.value = res.data
    editForm.modelName = res.data.modelName
    editForm.modelType = res.data.modelType
    editForm.description = res.data.description || ''
  } catch { router.push('/app/model') }
  await loadVersions()
}

async function loadVersions() {
  versionLoading.value = true
  try {
    const res = await getModelVersions(modelId.value)
    versions.value = res.data || []
  } catch { versions.value = [] }
  finally { versionLoading.value = false }
}

async function handleUpdateModel() {
  try {
    await updateModel(modelId.value, {
      modelName: editForm.modelName.trim(),
      modelType: editForm.modelType,
      description: editForm.description.trim()
    })
    model.value.modelName = editForm.modelName.trim()
    model.value.modelType = editForm.modelType
    model.value.description = editForm.description.trim()
    ElMessage.success('模型信息已更新')
  } catch { /* interceptor handles */ }
}

function handleAddVersion() { editingVersion.value = null; versionDialogVisible.value = true }

function handleEditVersion(v) { editingVersion.value = { ...v }; versionDialogVisible.value = true }

async function handleVersionSubmit(data) {
  try {
    if (data.id) { await updateVersion(modelId.value, data.id, data); ElMessage.success('版本更新成功') }
    else { await addVersion(modelId.value, data); ElMessage.success('版本创建成功') }
    versionDialogVisible.value = false
    loadVersions()
  } catch { /* interceptor handles */ }
}

async function handleDeleteVersion(v) {
  try {
    await ElMessageBox.confirm(`确定要删除版本 "${v.versionLabel}" 吗？`, '删除确认', { type: 'warning' })
    await deleteVersion(modelId.value, v.id)
    ElMessage.success('版本已删除')
    loadVersions()
  } catch { /* cancelled */ }
}

async function handleToggleVersion(v, status) {
  try {
    await toggleVersionStatus(modelId.value, v.id, status)
    ElMessage.success(status === 'RUNNING' ? '版本已启动' : '版本已停止')
    loadVersions()
  } catch { /* interceptor handles */ }
}

async function handleStopVersion(v) {
  try {
    await toggleVersionStatus(modelId.value, v.id, 'STOPPED')
    ElMessage.success('版本已停止')
    loadVersions()
  } catch { /* interceptor handles */ }
}
</script>

<style scoped>
.version-row {
  display: flex;
  align-items: center;
  padding: 8px 0;
  font-size: 13px;
  border-bottom: 1px solid var(--color-divider);
}

.version-label {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
  padding: 1px 7px;
  font-size: 11px;
  font-weight: 500;
  margin-right: 12px;
  min-width: 28px;
  text-align: center;
}

.version-ds {
  color: var(--color-text-muted);
  font-size: 12px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0 16px;
}
</style>
```

- [ ] **Step 2: 验证路由参数获取**

```bash
# Check: modelId computed from route.params.id
# Check: watch modelId to reload when navigating between models
```

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/views/model/ModelDetail.vue
git commit -m "feat: 新建模型详情页 ModelDetail，含版本 CRUD"
```

---

### Task 5: 前端 — 新建告警管理页 AlertManagement.vue

**Files:**
- Create: `ocean-web/src/views/model/AlertManagement.vue`
- Modify: `ocean-web/src/api/model.js` — 添加 `getAlertPage` API 调用

- [ ] **Step 1: 添加告警分页 API**

在 `ocean-web/src/api/model.js` 的告警部分（`markAllAlertsRead` 之后）添加：

```js
/** 分页查询告警 */
export function getAlertPage(params) {
  return request({ url: '/alert/page', method: 'get', params })
}
```

- [ ] **Step 2: 创建 AlertManagement.vue**

```vue
<template>
  <div>
    <h1 class="editorial-page-title">告警管理</h1>
    <p class="editorial-page-subtitle">Alert Management · 共 {{ total }} 条告警</p>

    <div class="editorial-filter-bar">
      <select v-model="query.alertType" class="editorial-select" style="width: 140px;">
        <option value="">全部类型</option>
        <option value="EXECUTION_FAILED">执行失败</option>
        <option value="CONSECUTIVE_FAILURES">连续失败</option>
        <option value="EXECUTION_TIMEOUT">执行超时</option>
      </select>
      <select v-model="query.isRead" class="editorial-select" style="width: 100px;">
        <option :value="null">全部状态</option>
        <option :value="0">未读</option>
        <option :value="1">已读</option>
      </select>
      <button class="editorial-btn-outline" @click="handleSearch">查询</button>
      <button class="editorial-btn-outline" @click="handleReset">重置</button>
      <span style="flex: 1;"></span>
      <button class="editorial-btn-outline" @click="handleMarkAllRead">全部标记已读</button>
    </div>

    <div v-if="tableData.length === 0 && !loading" style="text-align: center; padding: 60px 0; color: var(--color-text-muted); font-size: 14px;">
      暂无告警记录
    </div>

    <div v-else v-loading="loading" style="min-height: 200px;">
      <div class="alert-table">
        <div class="alert-table__header">
          <span style="flex: 2;">模型版本</span>
          <span style="flex: 1;">类型</span>
          <span style="flex: 2.5;">消息</span>
          <span style="flex: 1.5;">时间</span>
          <span style="flex: 0.5;">状态</span>
          <span style="flex: 1;">操作</span>
        </div>
        <div v-for="row in tableData" :key="row.id" class="alert-table__row" :class="{ 'alert-table__row--unread': row.isRead === 0 }">
          <span style="flex: 2;">
            <span style="font-weight: 600; color: var(--color-text);">{{ row.modelName }}</span>
            <span class="ver-badge">{{ row.versionLabel }}</span>
          </span>
          <span style="flex: 1; font-size: 12px; color: var(--color-text-secondary);">{{ row.typeLabel }}</span>
          <span style="flex: 2.5; font-size: 12px; color: var(--color-text-secondary);" :title="row.message">{{ row.message }}</span>
          <span style="flex: 1.5; font-size: 12px; color: var(--color-text-muted);">{{ formatTime(row.createTime) }}</span>
          <span style="flex: 0.5;">
            <span v-if="row.isRead === 0" style="color: #ef4444; font-size: 11px;">未读</span>
            <span v-else style="color: var(--color-text-muted); font-size: 11px;">已读</span>
          </span>
          <span style="flex: 1; display: flex; gap: 8px;">
            <a v-if="row.isRead === 0" class="editorial-link" @click="handleMarkRead(row)">标为已读</a>
            <a v-if="row.runLogId" class="editorial-link" @click="goRunLog(row)">查看日志</a>
          </span>
        </div>
      </div>
    </div>

    <div class="editorial-pagination" v-if="total > 0">
      <span>共 {{ total }} 条</span>
      <select v-model="query.pageSize" class="editorial-select" style="width: 80px;" @change="loadData">
        <option :value="10">10</option>
        <option :value="20">20</option>
        <option :value="50">50</option>
      </select>
      <a class="editorial-link" @click="prevPage">&larr;</a>
      <span class="editorial-pagination__page editorial-pagination__page--active">{{ query.pageNum }}</span>
      <a class="editorial-link" @click="nextPage">&rarr;</a>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAlertPage, markAlertRead, markAllAlertsRead } from '../../api/model'
import { ElMessage } from 'element-plus'

const router = useRouter()

const query = reactive({ pageNum: 1, pageSize: 10, alertType: '', isRead: null })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.alertType) params.alertType = query.alertType
    if (query.isRead !== null && query.isRead !== '') params.isRead = query.isRead
    const res = await getAlertPage(params)
    tableData.value = res.data.records || []
    total.value = res.data.total
  } finally { loading.value = false }
}

function handleSearch() { query.pageNum = 1; loadData() }
function handleReset() { query.alertType = ''; query.isRead = null; query.pageNum = 1; loadData() }
function prevPage() { if (query.pageNum > 1) { query.pageNum--; loadData() } }
function nextPage() { query.pageNum++; loadData() }

async function handleMarkRead(row) {
  try {
    await markAlertRead(row.id)
    row.isRead = 1
    ElMessage.success('已标记为已读')
  } catch { /* interceptor handles */ }
}

async function handleMarkAllRead() {
  try {
    await markAllAlertsRead()
    loadData()
    ElMessage.success('全部已标记为已读')
  } catch { /* interceptor handles */ }
}

function goRunLog(row) {
  router.push('/app/model/monitor')
}

function formatTime(d) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
.alert-table__header,
.alert-table__row {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  gap: 12px;
}

.alert-table__header {
  font-size: 11px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  border-bottom: 1px solid var(--color-divider-strong);
}

.alert-table__row {
  border-bottom: 1px solid var(--color-divider);
  font-size: 13px;
}

.alert-table__row:hover { background: var(--color-surface); }

.alert-table__row--unread {
  border-left: 2px solid #ef4444;
}

.ver-badge {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
  padding: 1px 7px;
  font-size: 11px;
  font-weight: 500;
  margin-left: 8px;
}
</style>
```

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/views/model/AlertManagement.vue ocean-web/src/api/model.js
git commit -m "feat: 新建告警管理页 AlertManagement，含分页筛选"
```

---

### Task 6: 前端 — 新建占位页面

**Files:**
- Create: `ocean-web/src/views/model/ScheduleOverview.vue`
- Create: `ocean-web/src/views/model/ModelCompare.vue`

- [ ] **Step 1: 创建 ScheduleOverview.vue（占位）**

```vue
<template>
  <div>
    <h1 class="editorial-page-title">调度总览</h1>
    <p class="editorial-page-subtitle">Schedule Overview · 模型调度状态与管理</p>
    <div style="text-align: center; padding: 80px 0; color: var(--color-text-muted); font-size: 14px; line-height: 2;">
      调度总览功能开发中<br />
      <span style="font-size: 12px;">后续将支持拖拽排程，敬请期待</span>
    </div>
  </div>
</template>
```

- [ ] **Step 2: 创建 ModelCompare.vue（占位）**

```vue
<template>
  <div>
    <h1 class="editorial-page-title">模型对比</h1>
    <p class="editorial-page-subtitle">Model Comparison · 版本效果指标对比</p>
    <div style="text-align: center; padding: 80px 0; color: var(--color-text-muted); font-size: 14px; line-height: 2;">
      模型对比功能开发中<br />
      <span style="font-size: 12px;">后续将支持版本间运行指标对比</span>
    </div>
  </div>
</template>
```

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/views/model/ScheduleOverview.vue ocean-web/src/views/model/ModelCompare.vue
git commit -m "feat: 调度总览、模型对比占位页面"
```

---

### Task 7: 前端 — 更新路由配置

**Files:**
- Modify: `ocean-web/src/router/index.js`

- [ ] **Step 1: 重构模型路由为嵌套结构**

在 `ocean-web/src/router/index.js` 中，将以下路由项（第 39-50 行）：

```js
{
  path: 'model',
  name: 'Model',
  component: () => import('../views/model/ModelView.vue'),
  meta: { title: '预报模型管理', role: 'ADMIN' }
},
{
  path: 'model/monitor',
  name: 'ModelMonitor',
  component: () => import('../views/model/RunMonitor.vue'),
  meta: { title: '运行状态监控', role: 'ADMIN' }
},
```

替换为：

```js
{
  path: 'model',
  component: () => import('../layout/ModelLayout.vue'),
  redirect: '/app/model',
  meta: { role: 'ADMIN' },
  children: [
    {
      path: '',
      name: 'Model',
      component: () => import('../views/model/ModelList.vue'),
      meta: { title: '模型管理', role: 'ADMIN' }
    },
    {
      path: ':id',
      name: 'ModelDetail',
      component: () => import('../views/model/ModelDetail.vue'),
      meta: { title: '模型详情', role: 'ADMIN' }
    },
    {
      path: 'schedule',
      name: 'ModelSchedule',
      component: () => import('../views/model/ScheduleOverview.vue'),
      meta: { title: '调度总览', role: 'ADMIN' }
    },
    {
      path: 'monitor',
      name: 'ModelMonitor',
      component: () => import('../views/model/RunMonitor.vue'),
      meta: { title: '运行监控', role: 'ADMIN' }
    },
    {
      path: 'alerts',
      name: 'ModelAlerts',
      component: () => import('../views/model/AlertManagement.vue'),
      meta: { title: '告警管理', role: 'ADMIN' }
    },
    {
      path: 'compare',
      name: 'ModelCompare',
      component: () => import('../views/model/ModelCompare.vue'),
      meta: { title: '模型对比', role: 'ADMIN' }
    }
  ]
},
```

> **注意：** `/app/model/:id` 必须在 `schedule/monitor/alerts/compare` 之后注册，或者 `:id` 使用正则限定数字。推荐在 `router/index.js` 顶部添加数字限定：
>
> ```js
> path: ':id(\\d+)',
> ```
>
> 这样 `/app/model/123` 匹配 ModelDetail，`/app/model/schedule` 继续匹配 ScheduleOverview。

- [ ] **Step 2: 验证路由不会冲突**

```bash
# 确认: /app/model → ModelList
# 确认: /app/model/1 → ModelDetail
# 确认: /app/model/schedule → ScheduleOverview (不会被 :id 截获)
```

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/router/index.js
git commit -m "feat: 重构模型路由为嵌套结构，侧边栏布局"
```

---

### Task 8: 前端 — 更新 MainLayout 告警铃铛

**Files:**
- Modify: `ocean-web/src/layout/MainLayout.vue`

**说明：** `ModelLayout` 接管了模型板块，`MainLayout` 的 `isActive('/app/model')` 仍然生效（`startsWith` 匹配所有子路由）。唯一需要确认的是铃铛点击跳转路径不变。

- [ ] **Step 1: 确认 goToMonitor 路径**

`MainLayout.vue` 第 179 行 `router.push('/app/model/monitor')` 路径不变，路由仍然有效。

- [ ] **Step 2: 运行 dev server 验证**

```bash
cd ocean-web && npm run dev
```

手动验证：
1. 导航栏「模型」→ 进入模型列表页，左侧出现侧边栏
2. 侧边栏各菜单项可正常切换
3. 点击模型卡片 → 进入模型详情页
4. 离开模型板块（点击仪表盘）→ 侧边栏消失

```bash
# 验证完成后停止 dev server (Ctrl+C)
```

- [ ] **Step 3: Commit（如有需要调整的代码）**

```bash
git add ocean-web/src/layout/MainLayout.vue
git commit -m "fix: 模型板块路由结构调整后导航高亮适配"
```

---

### Task 9: 清理 — 删除旧 ModelView.vue

**Files:**
- Delete: `ocean-web/src/views/model/ModelView.vue`

**说明：** `ModelView.vue` 已被 `ModelList.vue` + `ModelDetail.vue` 完全替代。

- [ ] **Step 1: 确认无其他文件引用 ModelView.vue**

```bash
cd ocean-web && grep -r "ModelView" src/ --include="*.vue" --include="*.js"
```

Expected: No matches (路由已改为 ModelList)。

- [ ] **Step 2: 删除旧文件**

```bash
git rm ocean-web/src/views/model/ModelView.vue
```

- [ ] **Step 3: 最终验证**

```bash
cd ocean-web && npm run build
```

Expected: Build succeeds without errors.

- [ ] **Step 4: Commit**

```bash
git commit -m "chore: 删除旧 ModelView.vue，已被 ModelList + ModelDetail 替代"
```

---

## 验证清单

全部任务完成后，手动验证以下场景：

1. `/app/model` — 模型列表正常展示，筛选/分页/新增/编辑/删除正常
2. `/app/model/:id` — 模型详情正常，版本 CRUD/启停正常
3. `/app/model/schedule` — 占位页正常显示
4. `/app/model/monitor` — 运行监控正常，模型名颜色已修正
5. `/app/model/alerts` — 告警列表正常，筛选/分页/标记已读正常
6. `/app/model/compare` — 占位页正常显示
7. 侧边栏选中态在各子页面间正确切换
8. 离开模型板块侧边栏消失，返回模型板块侧边栏出现
9. 深色模式兼容
10. `npm run build` 无错误
