<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { fetchAdminAnnouncements, saveAnnouncement } from '../../api/admin'
import { readApiError } from '../../api/http'
import type { AnnouncementView } from '../../types/api'

const items = ref<AnnouncementView[]>([])
const errorMessage = ref('')
const form = reactive({ id: 0, title: '', body: '', status: 'ENABLED' })

async function load(): Promise<void> {
  const { data } = await fetchAdminAnnouncements()
  items.value = data.data
}

async function submit(): Promise<void> {
  try {
    await saveAnnouncement({ title: form.title, body: form.body, status: form.status }, form.id || undefined)
    form.id = 0
    form.title = ''
    form.body = ''
    await load()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
}

function edit(item: AnnouncementView): void {
  form.id = item.id
  form.title = item.title
  form.body = item.body
  form.status = item.status
}

onMounted(async () => {
  try {
    await load()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
})
</script>

<template>
  <el-card>
    <h2 class="page-title">公告</h2>
    <el-form label-width="80px">
      <el-form-item label="标题">
        <el-input v-model="form.title" />
      </el-form-item>
      <el-form-item label="正文">
        <el-input v-model="form.body" type="textarea" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="submit">保存</el-button>
    </el-form>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <el-table :data="items">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button text @click="edit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>
.page-title {
  margin-top: 0;
}

.page-error {
  color: var(--el-color-danger);
}
</style>
