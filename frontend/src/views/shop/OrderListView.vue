<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { motion } from 'motion-v'
import FoilBadge from '../../components/shop/FoilBadge.vue'
import { fetchOrders } from '../../api/shop'
import { readApiError } from '../../api/http'
import { deliveryStatusLabel, payStatusLabel } from '../../utils/labels'
import { formatFen } from '../../utils/money'
import type { OrderView } from '../../types/api'

const orders = ref<OrderView[]>([])
const errorMessage = ref('')
const loading = ref(false)
const page = ref(1)
const total = ref(0)

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchOrders(page.value)
    orders.value = data.data
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

function payTone(status: string): 'warn' | 'ok' | 'mute' | 'danger' {
  if (status === 'PAID') {
    return 'ok'
  }
  if (status === 'PENDING') {
    return 'warn'
  }
  if (status === 'EXPIRED') {
    return 'danger'
  }
  return 'mute'
}

function deliveryTone(status: string): 'warn' | 'ok' | 'danger' {
  if (status === 'DELIVERED') {
    return 'ok'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  return 'warn'
}

onMounted(load)
</script>

<template>
  <motion.div
    :initial="{ opacity: 0, y: 20 }"
    :animate="{ opacity: 1, y: 0 }"
    :transition="{ duration: 0.45, ease: [0.22, 1, 0.36, 1] }"
  >
    <h2 class="page-title font-display">我的<span class="grad-text">订单</span></h2>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <el-skeleton v-else-if="loading" :rows="6" animated />
    <el-empty v-else-if="!orders.length" description="还没有订单">
      <RouterLink to="/">
        <el-button type="primary">去选一张卡</el-button>
      </RouterLink>
    </el-empty>
    <div v-else class="order-list">
      <RouterLink v-for="row in orders" :key="row.id" class="order-row" :to="`/orders/${row.id}`">
        <article class="order-card">
          <p class="order-no font-mono">{{ row.orderNo }}</p>
          <div class="order-main">
            <h3 class="order-name">{{ row.productName }}</h3>
            <p class="order-price font-mono">{{ formatFen(row.amountFen) }}</p>
          </div>
          <div class="order-flags">
            <FoilBadge :label="payStatusLabel(row.payStatus)" :tone="payTone(row.payStatus)" />
            <FoilBadge
              :label="deliveryStatusLabel(row.deliveryStatus)"
              :tone="deliveryTone(row.deliveryStatus)"
            />
          </div>
          <span class="order-cta">{{ row.deliveryStatus === 'DELIVERED' ? '查看卡密' : '详情' }} →</span>
        </article>
      </RouterLink>
    </div>
    <div v-if="total > 0" class="page-pagination">
      <el-pagination
        :current-page="page"
        :page-size="20"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="onPageChange"
      />
    </div>
  </motion.div>
</template>

<style scoped>
.page-title {
  margin: 0 0 18px;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.page-error {
  color: var(--red);
}

.order-list {
  display: grid;
  gap: 14px;
}

.order-row {
  display: block;
  color: inherit;
  text-decoration: none;
}

.order-row:focus-visible {
  outline: 2px solid var(--violet);
  outline-offset: 4px;
  border-radius: 18px;
}

.order-card {
  display: grid;
  grid-template-columns: minmax(150px, 0.7fr) minmax(0, 1.4fr) auto auto;
  align-items: center;
  gap: 12px 18px;
  padding: 18px 20px;
  border-radius: 18px;
  background:
    linear-gradient(160deg, rgba(139, 92, 246, 0.06), rgba(34, 211, 238, 0.04)),
    var(--surface);
  border: 1px solid var(--line);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
  transition: transform 0.25s var(--ease-out), border-color 0.25s var(--ease-out),
    box-shadow 0.25s var(--ease-out), background-color 0.25s ease;
}

.order-row:hover .order-card,
.order-row:focus-visible .order-card {
  transform: translateY(-2px);
  border-color: rgba(139, 92, 246, 0.55);
  box-shadow: var(--glow-violet), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  background-color: var(--surface-strong);
}

.order-no {
  margin: 0;
  color: var(--mute);
  font-size: 12px;
  letter-spacing: 0.03em;
}

.order-name {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
}

.order-price {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--cyan-soft);
  font-variant-numeric: tabular-nums;
}

.order-flags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.order-cta {
  color: var(--ink-soft);
  font-size: 13px;
  transition: color 0.25s var(--ease-out);
}

.order-row:hover .order-cta {
  color: var(--violet-soft);
}

.page-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 800px) {
  .order-card {
    grid-template-columns: 1fr auto;
  }

  .order-no,
  .order-flags {
    grid-column: 1 / -1;
  }
}
</style>
