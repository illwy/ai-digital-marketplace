<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchAfterSales } from '../../api/shop'
import { readApiError } from '../../api/http'
import type { AfterSaleView } from '../../types/api'

const items = ref<AfterSaleView[]>([])
const errorMessage = ref('')

onMounted(async () => {
  try {
    const { data } = await fetchAfterSales(1)
    items.value = data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
})
</script>

<template>
  <el-card>
    <h2 class="page-title">我的售后</h2>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <el-table :data="items">
      <el-table-column prop="id" label="单号" />
      <el-table-column prop="orderId" label="订单" />
      <el-table-column prop="reason" label="原因" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="adminReply" label="回复" />
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
