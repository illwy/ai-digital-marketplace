<script setup lang="ts">
import { onMounted, ref } from 'vue'
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
  <section>
    <h2 class="page-title">我的订单</h2>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <el-skeleton v-else-if="loading" :rows="6" animated />
    <el-empty v-else-if="!orders.length" description="还没有订单">
      <RouterLink to="/">
        <el-button type="primary">去选一张卡</el-button>
      </RouterLink>
    </el-empty>
    <div v-else class="order-list">
      <RouterLink v-for="row in orders" :key="row.id" class="order-ticket" :to="`/orders/${row.id}`">
        <span class="order-dash" aria-hidden="true" />
        <article class="order-sheet">
          <span class="order-notch order-notch-left" aria-hidden="true" />
          <span class="order-notch order-notch-right" aria-hidden="true" />
          <p class="order-no">{{ row.orderNo }}</p>
          <div class="order-main">
            <h3 class="order-name">{{ row.productName }}</h3>
            <p class="order-price">{{ formatFen(row.amountFen) }}</p>
          </div>
          <div class="order-flags">
            <FoilBadge :label="payStatusLabel(row.payStatus)" :tone="payTone(row.payStatus)" />
            <FoilBadge
              :label="deliveryStatusLabel(row.deliveryStatus)"
              :tone="deliveryTone(row.deliveryStatus)"
            />
          </div>
          <span class="order-cta">{{ row.deliveryStatus === 'DELIVERED' ? '查看卡密' : '详情' }}</span>
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
  </section>
</template>

<style scoped>
.page-title {
  margin: 0 0 18px;
  font-family: "Noto Serif SC", serif;
}

.page-error {
  color: var(--el-color-danger);
}

.order-list {
  display: grid;
  gap: 14px;
}

.order-ticket {
  position: relative;
  display: block;
  color: inherit;
  text-decoration: none;
}

.order-ticket:focus-visible {
  outline: 2px solid var(--copper);
  outline-offset: 4px;
}

.order-dash {
  position: absolute;
  inset: 0;
  border: 1.5px dashed var(--copper);
  pointer-events: none;
}

.order-sheet {
  position: relative;
  display: grid;
  grid-template-columns: minmax(140px, 0.7fr) minmax(0, 1.4fr) auto auto;
  align-items: center;
  gap: 12px 18px;
  padding: 16px 18px;
  background: var(--ticket);
  color: var(--ticket-ink);
  transition: transform 180ms cubic-bezier(0.16, 1, 0.3, 1);
}

.order-ticket:hover .order-sheet,
.order-ticket:focus-visible .order-sheet {
  transform: translate(-5px, -5px);
}

.order-notch {
  position: absolute;
  top: 50%;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--paper);
  transform: translateY(-50%);
}

.order-notch-left {
  left: -8px;
}

.order-notch-right {
  right: -8px;
}

.order-no {
  margin: 0;
  color: var(--ticket-mute);
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 12px;
}

.order-name {
  margin: 0 0 4px;
  font-family: "Noto Serif SC", serif;
  font-size: 16px;
}

.order-price {
  margin: 0;
  color: #9a3b16;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.order-flags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.order-cta {
  padding: 5px 10px;
  background: var(--copper);
  color: #1a120c;
  font-size: 12px;
  font-weight: 700;
}

.page-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 800px) {
  .order-sheet {
    grid-template-columns: 1fr auto;
  }

  .order-no,
  .order-flags {
    grid-column: 1 / -1;
  }
}
</style>
