<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchAdminNodeSubscriptions } from '../../api/admin'
import { readApiError } from '../../api/http'
import { formatDateTime } from '../../utils/time'
import type { AdminNodeSubscriptionView } from '../../types/api'

const items = ref<AdminNodeSubscriptionView[]>([])
const username = ref('')
const status = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchAdminNodeSubscriptions({ username: username.value.trim() || undefined, status: status.value || undefined })
    items.value = data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
  } finally {
    loading.value = false
  }
}

async function copy(value: string): Promise<void> {
  if (!value) return
  await navigator.clipboard.writeText(value)
  ElMessage.success('HTTP 订阅地址已复制')
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header class="page-head"><div><h2 class="page-title">节点订阅</h2><p class="page-desc">管理用户的 HTTP 订阅地址和开通状态。</p></div></header>
    <div class="filter-bar glass-panel">
      <el-form inline @submit.prevent="load">
        <el-form-item label="用户 ID"><el-input v-model="username" placeholder="例如 10001" clearable /></el-form-item>
        <el-form-item label="状态"><el-select v-model="status" placeholder="全部" clearable><el-option label="正常" value="ACTIVE" /><el-option label="过期" value="EXPIRED" /><el-option label="失败" value="FAILED" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" native-type="submit" :loading="loading">查询</el-button></el-form-item>
      </el-form>
    </div>
    <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" />
    <div class="table-card glass-panel">
      <el-table v-loading="loading" :data="items">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="用户" width="120"><template #default="{ row }">{{ row.username }}</template></el-table-column>
        <el-table-column prop="clientName" label="客户端" min-width="180" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column label="到期时间" width="180"><template #default="{ row }">{{ formatDateTime(row.expiresAt) }}</template></el-table-column>
        <el-table-column label="HTTP 订阅地址" min-width="300"><template #default="{ row }"><span class="url">{{ row.subscriptionUrl || '未生成' }}</span></template></el-table-column>
        <el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button text type="primary" :disabled="!row.subscriptionUrl" @click="copy(row.subscriptionUrl)">复制</el-button></template></el-table-column>
        <template #empty><el-empty description="暂无节点订阅" /></template>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.page { display:flex; flex-direction:column; gap:16px; }
.url { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-family:var(--font-mono); color:var(--azure-soft); }
</style>
