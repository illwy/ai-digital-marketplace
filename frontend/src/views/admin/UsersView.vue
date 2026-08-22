<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { creditUserWallet, fetchAdminUsers, updateUserStatus } from '../../api/admin'
import { readApiError } from '../../api/http'
import { enablementLabel, roleLabel } from '../../utils/labels'
import { formatDateTime } from '../../utils/time'
import type { AdminUserView } from '../../types/api'

const users = ref<AdminUserView[]>([])
const username = ref('')
const status = ref('')
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchAdminUsers({
      username: username.value.trim() || undefined,
      status: status.value || undefined,
      page: page.value,
    })
    users.value = data.data
    total.value = data.pagination.totalItems
  } catch (error) {
    errorMessage.value = readApiError(error).message
  } finally {
    loading.value = false
  }
}

function search(): void {
  page.value = 1
  void load()
}

async function onPageChange(next: number): Promise<void> {
  page.value = next
  await load()
}

function isDismissed(error: unknown): boolean {
  return error === 'cancel' || error === 'close'
}

async function toggle(user: AdminUserView): Promise<void> {
  const next = user.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const action = next === 'DISABLED' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}用户 ${user.username}？`, '确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await updateUserStatus(user.id, next)
    ElMessage.success(`已${action}`)
    await load()
  } catch (error) {
    if (isDismissed(error)) {
      return
    }
    errorMessage.value = readApiError(error).message
  }
}

async function credit(user: AdminUserView): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认给用户 ${user.username} 入账 10 元？`, '确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await creditUserWallet(user.id, 1000)
    ElMessage.success('已入账 10 元')
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
  <el-card>
    <div class="page-header">
      <h2 class="page-title">用户</h2>
    </div>
    <el-form inline class="page-filters" @submit.prevent="search">
      <el-form-item label="用户名">
        <el-input v-model="username" clearable placeholder="搜索用户名" @keyup.enter="search" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="status" clearable placeholder="全部" style="width: 140px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
      </el-form-item>
    </el-form>
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
      class="page-alert"
    />
    <el-table v-loading="loading" :data="users">
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">{{ enablementLabel(row.status) }}</template>
      </el-table-column>
      <el-table-column label="角色">
        <template #default="{ row }">{{ row.roles.map(roleLabel).join('、') }}</template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="160">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="toggle(row)">
            {{ row.status === 'ENABLED' ? '停用' : '启用' }}
          </el-button>
          <el-button text type="primary" @click="credit(row)">入账 10 元</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无用户" />
      </template>
    </el-table>
    <div v-if="total > 0" class="page-pagination">
      <el-pagination
        :current-page="page"
        :page-size="20"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="onPageChange"
      />
    </div>
  </el-card>
</template>

<style scoped>
.page-header {
  margin-bottom: 12px;
}

.page-title {
  margin: 0;
  font-size: 18px;
}

.page-filters {
  margin-bottom: 4px;
}

.page-alert {
  margin-bottom: 12px;
}

.page-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
