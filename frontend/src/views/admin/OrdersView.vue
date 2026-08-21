<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { cancelAdminOrder, fetchAdminOrders } from '../../api/admin'
import { readApiError } from '../../api/http'
import { formatFen } from '../../utils/money'
import type { OrderView } from '../../types/api'

const items = ref<OrderView[]>([])
const errorMessage = ref('')

async function load(): Promise<void> {
  const { data } = await fetchAdminOrders({ page: 1 })
  items.value = data.data
}

async function cancel(id: number): Promise<void> {
  try {
    await cancelAdminOrder(id)
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
    <h2 class="page-title">订单</h2>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <el-table :data="items">
      <el-table-column prop="orderNo" label="订单号" />
      <el-table-column prop="productName" label="商品" />
      <el-table-column label="金额">
        <template #default="{ row }">{{ formatFen(row.amountFen) }}</template>
      </el-table-column>
      <el-table-column prop="payStatus" label="支付" />
      <el-table-column prop="deliveryStatus" label="交付" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button v-if="row.payStatus === 'PENDING'" text @click="cancel(row.id)">关闭</el-button>
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
