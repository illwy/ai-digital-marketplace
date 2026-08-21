<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { creditUserWallet, fetchAdminUsers, updateUserStatus } from '../../api/admin'
import { readApiError } from '../../api/http'
import type { AdminUserView } from '../../types/api'

const users = ref<AdminUserView[]>([])
const errorMessage = ref('')

async function load(): Promise<void> {
  try {
    const { data } = await fetchAdminUsers({ page: 1 })
    users.value = data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
}

async function toggle(user: AdminUserView): Promise<void> {
  const next = user.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await updateUserStatus(user.id, next)
    await load()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
}

async function credit(user: AdminUserView): Promise<void> {
  try {
    await creditUserWallet(user.id, 1000)
    await load()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
}

onMounted(load)
</script>

<template>
  <el-card>
    <h2 class="page-title">用户</h2>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <el-table :data="users">
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="角色">
        <template #default="{ row }">{{ row.roles.join(', ') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button text @click="toggle(row)">启停</el-button>
          <el-button text @click="credit(row)">入账 10 元</el-button>
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
