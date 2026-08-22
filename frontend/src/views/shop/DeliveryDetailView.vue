<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import CardSecretPanel from '../../components/shop/CardSecretPanel.vue'
import { fetchDelivery } from '../../api/shop'
import { readApiError } from '../../api/http'
import { deliveryStatusLabel } from '../../utils/labels'
import { formatDateTime } from '../../utils/time'
import type { DeliveryView } from '../../types/api'

const route = useRoute()
const item = ref<DeliveryView | null>(null)
const errorMessage = ref('')
const id = computed(() => Number(route.params.id))

async function load(): Promise<void> {
  try {
    const { data } = await fetchDelivery(id.value)
    item.value = data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
}

onMounted(load)
watch(id, load)
</script>

<template>
  <article v-if="item" class="voucher">
    <h2 class="page-title">{{ item.productName || '卡密' }}</h2>
    <p class="meta">
      {{ item.orderNo || `订单 #${item.orderId}` }} · {{ deliveryStatusLabel(item.status) }} ·
      {{ formatDateTime(item.deliveredAt) }}
    </p>
    <CardSecretPanel :content="item.content" />
    <RouterLink class="order-link" :to="`/orders/${item.orderId}`">查看订单</RouterLink>
  </article>
  <el-empty v-else :description="errorMessage || '加载中'" />
</template>

<style scoped>
.voucher {
  position: relative;
  padding: 22px 24px;
  background: var(--ticket);
  color: var(--ticket-ink);
}

.voucher::before,
.voucher::after {
  content: "";
  position: absolute;
  left: -9px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--paper);
}

.voucher::before {
  top: 28px;
}

.voucher::after {
  bottom: 28px;
}

.page-title {
  margin: 0 0 8px;
  font-family: "Noto Serif SC", serif;
}

.meta {
  margin: 0;
  color: var(--ticket-mute);
}

.order-link {
  display: inline-block;
  margin-top: 12px;
}
</style>
