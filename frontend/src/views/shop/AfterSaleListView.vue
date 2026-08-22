<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchAfterSales } from '../../api/shop'
import { readApiError } from '../../api/http'
import { ticketStatusLabel, ticketStatusTagType } from '../../utils/labels'
import type { AfterSaleView } from '../../types/api'

const items = ref<AfterSaleView[]>([])
const errorMessage = ref('')
const loading = ref(false)
const page = ref(1)
const total = ref(0)

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchAfterSales(page.value)
    items.value = data.data
    total.value = data.pagination.totalItems
  } catch (error) {
    errorMessage.value = readApiError(error).message
  } finally {
    loading.value = false
  }
}

async function onPageChange(next: number): Promise<void> {
  page.value = next
  await load()
}

onMounted(load)
</script>

<template>
  <el-card>
    <h2 class="page-title">我的售后</h2>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <el-skeleton v-else-if="loading" :rows="6" animated />
    <el-empty v-else-if="!items.length" description="暂无售后">
      <RouterLink to="/orders">
        <el-button type="primary">从已出卡订单发起</el-button>
      </RouterLink>
    </el-empty>
    <el-table v-else-if="items.length" :data="items">
      <el-table-column label="商品" min-width="140">
        <template #default="{ row }">{{ row.productName || '-' }}</template>
      </el-table-column>
      <el-table-column label="订单" min-width="180">
        <template #default="{ row }">
          <RouterLink :to="`/orders/${row.orderId}`">{{ row.orderNo || `#${row.orderId}` }}</RouterLink>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" />
      <el-table-column label="状态">
        <template #default="{ row }">
          <el-tag :type="ticketStatusTagType(row.status)" size="small">
            {{ ticketStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="回复">
        <template #default="{ row }">{{ row.adminReply || '暂无' }}</template>
      </el-table-column>
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
.page-title {
  margin-top: 0;
  font-family: "Noto Serif SC", serif;
}

.page-error {
  color: var(--el-color-danger);
}

.page-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
