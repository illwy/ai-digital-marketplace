<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { motion } from 'motion-v'
import CardSecretPanel from '../../components/shop/CardSecretPanel.vue'
import { fetchDeliveries } from '../../api/shop'
import { readApiError } from '../../api/http'
import { formatDateTime } from '../../utils/time'
import type { DeliveryView } from '../../types/api'

const items = ref<DeliveryView[]>([])
const errorMessage = ref('')
const loading = ref(false)
const page = ref(1)
const total = ref(0)

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchDeliveries({ page: page.value })
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
    <h2 class="page-title font-display">我的<span class="grad-text">卡密</span></h2>
    <p class="page-lead">已发卡的账号、卡密、额度都在这里，可随时复制。</p>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <el-skeleton v-else-if="loading" :rows="6" animated />
    <el-empty v-else-if="!items.length" description="还没有卡密">
      <RouterLink to="/">
        <el-button type="primary">去选一张卡</el-button>
      </RouterLink>
    </el-empty>
    <div v-else class="secret-list">
      <article v-for="row in items" :key="row.id" class="secret-card">
        <div class="secret-head-row">
          <h3 class="secret-name">{{ row.productName || '已购卡密' }}</h3>
          <RouterLink class="secret-detail" :to="`/orders/${row.orderId}`">查看订单 →</RouterLink>
        </div>
        <p class="secret-meta">
          <span class="font-mono">{{ row.orderNo || `订单 #${row.orderId}` }}</span>
          <span class="secret-meta-dot">·</span>
          {{ formatDateTime(row.deliveredAt) }}
        </p>
        <CardSecretPanel :content="row.content" />
      </article>
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
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.page-lead {
  margin: 0 0 16px;
  color: var(--mute);
}

.page-error {
  color: var(--red);
}

.secret-list {
  display: grid;
  gap: 16px;
}

.secret-card {
  padding: 18px 18px 6px;
  border-radius: 18px;
  background:
    linear-gradient(160deg, rgba(47, 143, 219, 0.05), rgba(18, 179, 154, 0.06)),
    var(--surface);
  border: 1px solid var(--line);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
  transition: transform 0.25s var(--ease-out), border-color 0.25s var(--ease-out),
    box-shadow 0.25s var(--ease-out), background-color 0.25s ease;
}

.secret-card:hover {
  transform: translateY(-2px);
  border-color: rgba(47, 143, 219, 0.5);
  box-shadow: var(--glow-azure), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  background-color: var(--surface-strong);
}

.secret-head-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.secret-name {
  margin: 0 0 4px;
  font-size: 17px;
  font-weight: 700;
  color: var(--ink);
}

.secret-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: 0;
  color: var(--mute);
  font-size: 13px;
}

.secret-meta-dot {
  color: var(--line-strong);
}

.secret-detail {
  color: var(--azure-soft);
  text-decoration: none;
  font-size: 13.5px;
  white-space: nowrap;
}

.secret-detail:hover {
  text-decoration: underline;
}

.page-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
