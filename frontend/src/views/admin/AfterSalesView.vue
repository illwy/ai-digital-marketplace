<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchAdminAfterSales, handleAfterSale } from '../../api/admin'
import { readApiError } from '../../api/http'
import type { AfterSaleView } from '../../types/api'

const items = ref<AfterSaleView[]>([])
const errorMessage = ref('')
const reply = ref('')

async function load(): Promise<void> {
  const { data } = await fetchAdminAfterSales({ page: 1 })
  items.value = data.data
}

async function close(id: number): Promise<void> {
  try {
    await handleAfterSale(id, 'CLOSED', reply.value || '已处理')
    reply.value = ''
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
    <h2 class="page-title">售后</h2>
    <el-input v-model="reply" placeholder="处理说明" />
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <el-table :data="items">
      <el-table-column prop="id" label="单号" />
      <el-table-column prop="orderId" label="订单" />
      <el-table-column prop="reason" label="原因" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'CLOSED'" text @click="close(row.id)">关闭</el-button>
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
