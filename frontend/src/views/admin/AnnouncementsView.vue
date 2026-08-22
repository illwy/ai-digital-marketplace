<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { motion } from 'motion-v'
import { ElMessage } from 'element-plus'
import { fetchAdminAnnouncements, saveAnnouncement } from '../../api/admin'
import { readApiError } from '../../api/http'
import FoilBadge from '../../components/shop/FoilBadge.vue'
import { enablementLabel } from '../../utils/labels'
import { formatDateTime } from '../../utils/time'
import type { AnnouncementView } from '../../types/api'

const items = ref<AnnouncementView[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const errorMessage = ref('')
const form = reactive({ id: 0, title: '', body: '', status: 'ENABLED' })

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchAdminAnnouncements()
    items.value = data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
  } finally {
    loading.value = false
  }
}

function resetForm(): void {
  form.id = 0
  form.title = ''
  form.body = ''
  form.status = 'ENABLED'
}

function openCreate(): void {
  resetForm()
  dialogVisible.value = true
}

function openEdit(item: AnnouncementView): void {
  form.id = item.id
  form.title = item.title
  form.body = item.body
  form.status = item.status
  dialogVisible.value = true
}

async function submit(): Promise<void> {
  if (!form.title.trim() || !form.body.trim()) {
    ElMessage.warning('请填写标题和正文')
    return
  }
  saving.value = true
  try {
    await saveAnnouncement(
      { title: form.title.trim(), body: form.body, status: form.status },
      form.id || undefined,
    )
    ElMessage.success(form.id ? '已更新公告' : '已发布公告')
    dialogVisible.value = false
    resetForm()
    await load()
  } catch (error) {
    ElMessage.error(readApiError(error).message)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <motion.div
    class="page"
    :initial="{ opacity: 0, y: 24 }"
    :animate="{ opacity: 1, y: 0 }"
    :transition="{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }"
  >
    <header class="page-head">
      <div>
        <h2 class="page-title">公告</h2>
        <p class="page-desc">前台公告栏内容，启用后立即展示给买家。</p>
      </div>
      <el-button type="primary" @click="openCreate">新建公告</el-button>
    </header>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
      class="page-alert"
    />

    <div class="table-card glass-panel">
      <el-table v-loading="loading" :data="items">
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <FoilBadge :label="enablementLabel(row.status)" :tone="row.status === 'ENABLED' ? 'ok' : 'mute'" />
          </template>
        </el-table-column>
        <el-table-column label="发布时间" min-width="160">
          <template #default="{ row }">
            <span class="font-mono">{{ formatDateTime(row.publishedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">
            <span class="font-mono">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无公告" />
        </template>
      </el-table>
    </div>
  </motion.div>

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '新建公告'" width="640px" destroy-on-close>
    <el-form label-width="80px">
      <el-form-item label="标题">
        <el-input v-model="form.title" maxlength="128" />
      </el-form-item>
      <el-form-item label="正文">
        <el-input v-model="form.body" type="textarea" :rows="8" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status" style="width: 160px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 24px;
  letter-spacing: 0.02em;
}

.page-desc {
  margin: 4px 0 0;
  color: var(--mute);
  font-size: 13.5px;
}

.page-alert {
  border-radius: 12px;
}

.table-card {
  overflow: hidden;
}

.table-card :deep(.el-table::before) {
  display: none;
}
</style>
