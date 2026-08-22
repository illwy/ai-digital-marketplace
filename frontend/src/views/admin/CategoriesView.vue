<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { motion } from 'motion-v'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteCategory, fetchAdminCategories, saveCategory } from '../../api/admin'
import { readApiError } from '../../api/http'
import FoilBadge from '../../components/shop/FoilBadge.vue'
import { enablementLabel } from '../../utils/labels'
import type { CategoryView } from '../../types/api'

const items = ref<CategoryView[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const errorMessage = ref('')
const form = reactive({ id: 0, name: '', sortOrder: 0, status: 'ENABLED' })

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchAdminCategories()
    items.value = data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
  } finally {
    loading.value = false
  }
}

function resetForm(): void {
  form.id = 0
  form.name = ''
  form.sortOrder = 0
  form.status = 'ENABLED'
}

function openCreate(): void {
  resetForm()
  dialogVisible.value = true
}

function openEdit(item: CategoryView): void {
  form.id = item.id
  form.name = item.name
  form.sortOrder = item.sortOrder
  form.status = item.status
  dialogVisible.value = true
}

async function submit(): Promise<void> {
  if (!form.name.trim()) {
    ElMessage.warning('请填写分类名称')
    return
  }
  saving.value = true
  try {
    await saveCategory(
      { name: form.name.trim(), sortOrder: form.sortOrder, status: form.status },
      form.id || undefined,
    )
    ElMessage.success(form.id ? '已更新分类' : '已创建分类')
    dialogVisible.value = false
    resetForm()
    await load()
  } catch (error) {
    ElMessage.error(readApiError(error).message)
  } finally {
    saving.value = false
  }
}

function isDismissed(error: unknown): boolean {
  return error === 'cancel' || error === 'close'
}

async function remove(item: CategoryView): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除分类 ${item.name}？`, '确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteCategory(item.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    if (isDismissed(error)) {
      return
    }
    errorMessage.value = readApiError(error).message
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
        <h2 class="page-title">分类</h2>
        <p class="page-desc">商品分类与排序，前台货架按此分组展示。</p>
      </div>
      <el-button type="primary" @click="openCreate">新建分类</el-button>
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
        <el-table-column prop="name" label="名称" />
        <el-table-column label="排序" width="100">
          <template #default="{ row }">
            <span class="font-mono">{{ row.sortOrder }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <FoilBadge :label="enablementLabel(row.status)" :tone="row.status === 'ENABLED' ? 'ok' : 'mute'" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无分类" />
        </template>
      </el-table>
    </div>
  </motion.div>

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新建分类'" width="480px" destroy-on-close>
    <el-form label-width="80px">
      <el-form-item label="名称">
        <el-input v-model="form.name" maxlength="64" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sortOrder" />
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
