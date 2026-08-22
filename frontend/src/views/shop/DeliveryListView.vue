<script setup lang="ts">
import { onMounted, ref } from 'vue'
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
  <section>
    <h2 class="page-title">我的卡密</h2>
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
        <h3 class="secret-name">{{ row.productName || '已购卡密' }}</h3>
        <p class="secret-meta">{{ row.orderNo || `订单 #${row.orderId}` }} · {{ formatDateTime(row.deliveredAt) }}</p>
        <CardSecretPanel :content="row.content" />
        <RouterLink class="secret-detail" :to="`/orders/${row.orderId}`">查看订单</RouterLink>
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
  </section>
</template>

<style scoped>
.page-title {
  margin: 0 0 8px;
  font-family: "Noto Serif SC", serif;
  font-size: 28px;
}

.page-lead {
  margin: 0 0 16px;
  color: var(--mute);
}

.page-error {
  color: var(--el-color-danger);
}

.secret-list {
  display: grid;
  gap: 12px;
}

.secret-card {
  padding: 18px 18px 16px;
  background: var(--ticket);
  color: var(--ticket-ink);
}

.secret-name {
  margin: 0 0 4px;
  font-family: "Noto Serif SC", serif;
  font-size: 18px;
}

.secret-meta {
  margin: 0;
  color: var(--ticket-mute);
  font-size: 13px;
}

.secret-detail {
  display: inline-block;
  margin-top: 8px;
}

.page-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
