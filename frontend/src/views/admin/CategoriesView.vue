<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { deleteCategory, fetchAdminCategories, saveCategory } from '../../api/admin'
import { readApiError } from '../../api/http'
import type { CategoryView } from '../../types/api'

const items = ref<CategoryView[]>([])
const errorMessage = ref('')
const form = reactive({ id: 0, name: '', sortOrder: 0, status: 'ENABLED' })

async function load(): Promise<void> {
  const { data } = await fetchAdminCategories()
  items.value = data.data
}

async function submit(): Promise<void> {
  try {
    await saveCategory(
      { name: form.name, sortOrder: form.sortOrder, status: form.status },
      form.id || undefined,
    )
    form.id = 0
    form.name = ''
    await load()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
}

function edit(item: CategoryView): void {
  form.id = item.id
  form.name = item.name
  form.sortOrder = item.sortOrder
  form.status = item.status
}

async function remove(id: number): Promise<void> {
  try {
    await deleteCategory(id)
    await load()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
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
    <h2 class="page-title">分类</h2>
    <el-form inline>
      <el-form-item label="名称">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sortOrder" />
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
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="sortOrder" label="排序" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button text @click="edit(row)">编辑</el-button>
          <el-button text @click="remove(row.id)">删除</el-button>
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
