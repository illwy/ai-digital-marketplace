<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createOrder, fetchProduct } from '../../api/shop'
import { readApiError } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import { deliveryTypeLabel } from '../../utils/labels'
import { formatFen } from '../../utils/money'
import type { ProductView } from '../../types/api'
import FoilBadge from '../../components/shop/FoilBadge.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const product = ref<ProductView | null>(null)
const errorMessage = ref('')
const loading = ref(false)
const buying = ref(false)
const coverFailed = ref(false)

const productId = computed(() => Number(route.params.id))
const soldOut = computed(() => (product.value?.availableCount ?? 0) < 1)

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  coverFailed.value = false
  try {
    const { data } = await fetchProduct(productId.value)
    product.value = data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
    product.value = null
  } finally {
    loading.value = false
  }
}

async function buy(): Promise<void> {
  if (!auth.isAuthenticated) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  buying.value = true
  errorMessage.value = ''
  try {
    const { data } = await createOrder(productId.value)
    await router.push(`/orders/${data.data.id}`)
  } catch (error) {
    const body = readApiError(error)
    if (body.code === 'ORDER_PENDING') {
      const details = body.details as { orderId?: number } | undefined
      if (details?.orderId) {
        await router.push(`/orders/${details.orderId}`)
        return
      }
    }
    errorMessage.value = body.message
  } finally {
    buying.value = false
  }
}

onMounted(load)
watch(productId, load)
</script>

<template>
  <article v-if="product" class="detail">
    <div class="detail-cover">
      <img
        v-if="product.coverUrl && !coverFailed"
        class="detail-cover-img"
        :src="product.coverUrl"
        alt=""
        @error="coverFailed = true"
      />
      <div v-else class="detail-cover-fallback" />
    </div>
    <div class="detail-buy">
      <span class="detail-notch detail-notch-top" aria-hidden="true" />
      <span class="detail-notch detail-notch-bottom" aria-hidden="true" />
      <FoilBadge :label="deliveryTypeLabel(product.deliveryType)" />
      <h2 class="detail-title">{{ product.name }}</h2>
      <p class="detail-price">{{ formatFen(product.priceFen) }}</p>
      <p class="detail-meta">一单一件</p>
      <p v-if="!soldOut" class="detail-meta">库存 {{ product.availableCount }} 张</p>
      <p v-else class="detail-sold-out">售罄，请等待补货</p>
      <p class="detail-desc">{{ product.description || '暂无描述' }}</p>
      <div class="detail-notice">
        <p class="detail-notice-title">购买须知</p>
        <p class="detail-notice-body">付款成功后自动出卡。卡密当场显示，也可在「卡密」里再复制。发出后请自行保管。</p>
      </div>
      <p v-if="errorMessage" class="detail-error">{{ errorMessage }}</p>
      <el-button
        class="detail-cta"
        type="primary"
        size="large"
        :loading="buying"
        :disabled="soldOut"
        @click="buy"
      >
        {{ soldOut ? '暂无库存' : '立即购买' }}
      </el-button>
    </div>
  </article>
  <el-skeleton v-else-if="loading" :rows="6" animated />
  <el-empty v-else :description="errorMessage || '商品不存在'" />
</template>

<style scoped>
.detail {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 28px;
  align-items: start;
}

.detail-cover {
  min-height: 280px;
  overflow: hidden;
  background: #2a211c;
}

.detail-buy {
  position: relative;
  padding: 22px 24px 24px;
  background: var(--ticket);
  color: var(--ticket-ink);
}

.detail-notch {
  position: absolute;
  left: -9px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--paper);
}

.detail-notch-top {
  top: 22%;
}

.detail-notch-bottom {
  bottom: 22%;
}

.detail-cover-img,
.detail-cover-fallback {
  display: block;
  width: 100%;
  height: 320px;
  object-fit: cover;
}

.detail-cover-fallback {
  background: linear-gradient(145deg, #3a2a22, #c45c28);
}

.detail-title {
  margin: 10px 0 12px;
  font-family: "Noto Serif SC", serif;
  font-size: clamp(26px, 3vw, 34px);
  line-height: 1.25;
}

.detail-price {
  margin: 0 0 12px;
  color: #9a3b16;
  font-size: 32px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.detail-meta {
  margin: 6px 0;
  color: var(--ticket-mute);
}

.detail-sold-out {
  margin: 6px 0;
  color: #e48a78;
}

.detail-desc {
  margin: 14px 0;
  color: var(--ticket-mute);
  white-space: pre-wrap;
  line-height: 1.7;
}

.detail-error {
  color: #e48a78;
}

.detail-notice {
  margin: 16px 0 20px;
  padding: 14px;
  background: color-mix(in srgb, #1a1512 6%, var(--ticket));
  border: 1.5px dashed #cbb8a6;
}

.detail-cta {
  width: 100%;
}

.detail-notice-title {
  margin: 0 0 6px;
  font-weight: 600;
}

.detail-notice-body {
  margin: 0;
  color: var(--ticket-mute);
  line-height: 1.6;
}

@media (max-width: 800px) {
  .detail {
    grid-template-columns: 1fr;
  }

  .detail-cover-img,
  .detail-cover-fallback {
    height: 200px;
  }
}
</style>
