<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchAdminDeliveries } from '../../api/admin'
import { readApiError } from '../../api/http'
import type { DeliveryView } from '../../types/api'

const items = ref<DeliveryView[]>([])
const errorMessage = ref('')

onMounted(async () => {
  try {
    const { data } = await fetchAdminDeliveries(1)
    items.value = data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
})
</script>

<template>
  <el-card>
    <h2 class="page-title">交付记录</h2>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <el-table :data="items">
      <el-table-column prop="orderId" label="订单" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="content" label="掩码内容" />
      <el-table-column prop="remark" label="备注" />
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
