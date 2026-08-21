<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchOrders } from '../../api/shop'
import { readApiError } from '../../api/http'
import { formatFen } from '../../utils/money'
import type { OrderView } from '../../types/api'

const orders = ref<OrderView[]>([])
const errorMessage = ref('')

onMounted(async () => {
  try {
    const { data } = await fetchOrders(1)
    orders.value = data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
})
</script>

<template>
  <el-card>
    <h2 class="page-title">我的订单</h2>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <el-table :data="orders">
      <el-table-column prop="orderNo" label="订单号" />
      <el-table-column prop="productName" label="商品" />
      <el-table-column label="金额">
        <template #default="{ row }">{{ formatFen(row.amountFen) }}</template>
      </el-table-column>
      <el-table-column prop="payStatus" label="支付" />
      <el-table-column prop="deliveryStatus" label="交付" />
      <el-table-column label="">
        <template #default="{ row }">
          <RouterLink :to="`/orders/${row.id}`">详情</RouterLink>
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
