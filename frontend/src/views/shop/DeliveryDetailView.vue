<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { motion } from 'motion-v'
import CardSecretPanel from '../../components/shop/CardSecretPanel.vue'
import FoilBadge from '../../components/shop/FoilBadge.vue'
import { fetchDelivery } from '../../api/shop'
import { readApiError } from '../../api/http'
import { deliveryStatusLabel } from '../../utils/labels'
import { formatDateTime } from '../../utils/time'
import type { DeliveryView } from '../../types/api'

const route = useRoute()
const item = ref<DeliveryView | null>(null)
const errorMessage = ref('')
const id = computed(() => Number(route.params.id))

function deliveryTone(status: string): 'warn' | 'ok' | 'danger' {
  if (status === 'DELIVERED') {
    return 'ok'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  return 'warn'
}

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
  <motion.div
    :initial="{ opacity: 0, y: 20 }"
    :animate="{ opacity: 1, y: 0 }"
    :transition="{ duration: 0.45, ease: [0.22, 1, 0.36, 1] }"
  >
    <article v-if="item" class="voucher">
      <header class="voucher-head">
        <h2 class="page-title font-display">{{ item.productName || '卡密' }}</h2>
        <FoilBadge :label="deliveryStatusLabel(item.status)" :tone="deliveryTone(item.status)" />
      </header>
      <p class="meta">
        <span class="font-mono">{{ item.orderNo || `订单 #${item.orderId}` }}</span>
        <span class="meta-dot">·</span>
        {{ formatDateTime(item.deliveredAt) }}
      </p>
      <CardSecretPanel :content="item.content" />
      <RouterLink class="order-link" :to="`/orders/${item.orderId}`">查看订单 →</RouterLink>
    </article>
    <el-empty v-else :description="errorMessage || '加载中'" />
  </motion.div>
</template>

<style scoped>
.voucher {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px;
  border-radius: 22px;
  background:
    linear-gradient(160deg, rgba(34, 211, 238, 0.08), rgba(139, 92, 246, 0.1) 55%, rgba(244, 114, 182, 0.05)),
    var(--bg-raised);
  border: 1px solid rgba(34, 211, 238, 0.3);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.voucher-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 800;
}

.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: 0;
  color: var(--mute);
  font-size: 13px;
}

.meta-dot {
  color: var(--line-strong);
}

.order-link {
  display: inline-block;
  margin-top: 14px;
  color: var(--cyan-soft);
  text-decoration: none;
  font-size: 14px;
}

.order-link:hover {
  text-decoration: underline;
}
</style>
