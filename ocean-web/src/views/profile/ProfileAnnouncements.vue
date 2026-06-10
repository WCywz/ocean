<template>
  <div class="profile-page">
    <h1 class="editorial-page-title">系统公告</h1>
    <p class="editorial-page-subtitle">查看系统更新与维护通知</p>

    <div v-if="isAdmin" style="margin-bottom:20px">
      <button class="editorial-btn" style="padding-left:24px;padding-right:24px" @click="handleAdd">发布公告</button>
    </div>

    <div v-if="announcements.length === 0 && !loading" style="padding:60px 0;text-align:center;color:var(--color-text-muted);font-size:13px;">
      暂无公告
    </div>

    <div v-for="item in announcements" :key="item.id" class="announcement-item">
      <div class="announcement-item__header">
        <h3 class="announcement-item__title">{{ item.title }}</h3>
        <span class="announcement-item__time">{{ formatTime(item.createTime) }}</span>
      </div>
      <p class="announcement-item__content">{{ item.content }}</p>
      <div v-if="isAdmin" class="announcement-item__actions">
        <span class="editorial-link" @click="handleEdit(item)">编辑</span>
        <span class="editorial-link" style="color:var(--color-alert);margin-left:12px" @click="handleDelete(item)">删除</span>
      </div>
    </div>

    <div style="margin-top:24px;text-align:center">
      <button v-if="hasMore" class="editorial-btn-outline" :disabled="loading" @click="loadMore">
        {{ loading ? '加载中...' : '加载更多' }}
      </button>
    </div>

    <el-dialog
      v-model="showDialog"
      :title="editingId ? '编辑公告' : '发布公告'"
      width="780px"
      :close-on-click-modal="false"
    >
      <div class="profile-form__item">
        <label class="editorial-form-label">标题</label>
        <input v-model="form.title" class="editorial-input" placeholder="公告标题" />
      </div>
      <div class="profile-form__item">
        <label class="editorial-form-label">内容</label>
        <textarea v-model="form.content" class="editorial-input" rows="10" placeholder="公告内容" style="resize:vertical"></textarea>
      </div>
      <template #footer>
        <button class="editorial-btn-outline" @click="showDialog = false">取消</button>
        <button class="editorial-btn" style="margin-left:8px;padding-left:16px;padding-right:16px" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../store/user'
import { getAnnouncements, addAnnouncement, updateAnnouncement, deleteAnnouncement } from '../../api/profile'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin())

const announcements = ref([])
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const editingId = ref(null)
const pageNum = ref(1)
const total = ref(0)
const hasMore = computed(() => announcements.value.length < total.value)

const form = ref({ title: '', content: '' })

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await getAnnouncements({ pageNum: 1, pageSize: 10 })
    announcements.value = res.data.records || []
    total.value = res.data.total || 0
    pageNum.value = 1
  } catch (e) { console.error('获取公告失败', e) } finally { loading.value = false }
}

async function loadMore() {
  loading.value = true
  try {
    const res = await getAnnouncements({ pageNum: pageNum.value + 1, pageSize: 10 })
    announcements.value = [...announcements.value, ...(res.data.records || [])]
    total.value = res.data.total || 0
    pageNum.value++
  } catch (e) { console.error('加载更多失败', e) } finally { loading.value = false }
}

function handleAdd() {
  editingId.value = null
  form.value = { title: '', content: '' }
  showDialog.value = true
}

function handleEdit(item) {
  editingId.value = item.id
  form.value = { title: item.title, content: item.content }
  showDialog.value = true
}

async function handleDelete(item) {
  try {
    await ElMessageBox.confirm(`确定删除「${item.title}」？`, '删除确认', { type: 'warning' })
    await deleteAnnouncement(item.id)
    ElMessage.success('公告已删除')
    loadData()
  } catch (e) { if (e !== 'cancel') { console.error('删除失败', e); ElMessage.error('删除失败') } }
}

async function handleSave() {
  if (!form.value.title.trim()) { ElMessage.error('请输入标题'); return }
  if (!form.value.content.trim()) { ElMessage.error('请输入内容'); return }
  saving.value = true
  try {
    if (editingId.value) {
      await updateAnnouncement(editingId.value, form.value)
      ElMessage.success('公告已更新')
    } else {
      await addAnnouncement(form.value)
      ElMessage.success('公告已发布')
    }
    showDialog.value = false
    form.value = { title: '', content: '' }
    editingId.value = null
    loadData()
  } catch (e) { console.error('保存失败', e); ElMessage.error('保存失败') } finally { saving.value = false }
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<style scoped>
.profile-page { max-width: 600px; }

.announcement-item {
  padding: 32px 0;
  border-bottom: 1px solid var(--color-divider-strong);
}
.announcement-item:last-child { border-bottom: none; }

.announcement-item__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.announcement-item__title {
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
  line-height: 1.4;
}
.announcement-item__time {
  font-size: 13px;
  color: var(--color-text-muted);
  white-space: nowrap;
  flex-shrink: 0;
}
.announcement-item__content {
  font-size: 15px;
  color: var(--color-text-secondary);
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
}
.announcement-item__actions {
  margin-top: 4px;
}

.profile-form__item { margin-bottom: 20px; }
.profile-form__item .editorial-input { width: 100%; box-sizing: border-box; }
</style>
