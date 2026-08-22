<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { motion } from 'motion-v'
import FoilBadge from '../../components/shop/FoilBadge.vue'
import { fetchAfterSales } from '../../api/shop'
import { readApiError } from '../../api/http'
import { ticketStatusLabel } from '../../utils/labels'
import type { AfterSaleView } from '../../types/api'

const items = ref<AfterSaleView[]>([])
const errorMessage = ref('')
const loading = ref(false)
const page = ref(1)
const total = ref(0)

function statusTone(status: string): 'warn' | 'cyan' | 'ok' {
  if (status === 'CLOSED') {
    return 'ok'
  }
  if (status === 'PROCESSING') {
    return 'cyan'
  }
  return 'warn'
}

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
  <motion.div
    :initial="{ opacity: 0, y: 20 }"
    :animate="{ opacity: 1, y: 0 }"
    :transition="{ duration: 0.45, ease: [0.22, 1, 0.36, 1] }"
  >
    <el-card class="panel">
      <h2 class="page-title font-display">我的<span class="grad-text">售后</span></h2>
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
            <RouterLink class="order-link font-mono" :to="`/orders/${row.orderId}`">
              {{ row.orderNo || `#${row.orderId}` }}
            </RouterLink>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <FoilBadge :label="ticketStatusLabel(row.status)" :tone="statusTone(row.status)" />
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
  </motion.div>
</template>

<style scoped>
.panel {
  padding: 6px;
}

.page-title {
  margin: 0 0 18px;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.page-error {
  color: var(--red);
}

.order-link {
  font-size: 12.5px;
}

.page-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
