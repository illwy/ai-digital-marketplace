<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { motion } from 'motion-v'
import { createOrder, fetchProduct } from '../../api/shop'
import { readApiError } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import { deliveryTypeLabel } from '../../utils/labels'
import { formatFen } from '../../utils/money'
import { renderMarkdown } from '../../utils/markdown'
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
const renderedDescription = computed(() => renderMarkdown(product.value?.description))

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
    <motion.div
      class="detail-cover"
      :initial="{ opacity: 0, x: -24 }"
      :animate="{ opacity: 1, x: 0 }"
      :transition="{ duration: 0.55, ease: [0.22, 1, 0.36, 1] }"
    >
      <img
        v-if="product.coverUrl && !coverFailed"
        class="detail-cover-img"
        :src="product.coverUrl"
        alt=""
        @error="coverFailed = true"
      />
      <div v-else class="detail-cover-fallback">
        <span class="font-display">AI</span>
      </div>
      <div class="detail-cover-glow" aria-hidden="true"></div>
    </motion.div>

    <motion.div
      class="detail-buy glass-panel"
      :initial="{ opacity: 0, y: 24 }"
      :animate="{ opacity: 1, y: 0 }"
      :transition="{ duration: 0.55, delay: 0.08, ease: [0.22, 1, 0.36, 1] }"
    >
      <FoilBadge :label="deliveryTypeLabel(product.deliveryType)" />
      <h2 class="detail-title">{{ product.name }}</h2>
      <p class="detail-price font-mono">{{ formatFen(product.priceFen) }}</p>

        <p v-if="!soldOut" class="detail-meta">
          <span class="detail-stock-dot"></span>{{ product.deliveryType === 'NODE_SUBSCRIPTION' ? '在线开通 · 支付后自动交付' : `现货 ${product.availableCount} 件 · 一单一件` }}
      </p>
      <p v-else class="detail-sold-out">售罄，请等待补货</p>

      <p class="detail-desc md" v-html="renderedDescription"></p>

      <div class="detail-notice">
        <p class="detail-notice-title">购买须知</p>
        <p class="detail-notice-body">{{ product.deliveryType === 'NODE_SUBSCRIPTION' ? '付款成功后由后台自动开通 2S-UI 节点，完成后在「节点」页面复制订阅地址。' : '付款成功后自动出卡。卡密当场显示，也可在「卡密」里再复制。发出后请自行保管。' }}</p>
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
    </motion.div>
  </article>
  <el-skeleton v-else-if="loading" :rows="6" animated />
  <el-empty v-else :description="errorMessage || '商品不存在'" />
</template>

<style scoped>
.detail {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 30px;
  align-items: stretch;
}

.detail-cover {
  position: relative;
  min-height: 320px;
  overflow: hidden;
  border-radius: 22px;
  border: 1px solid var(--line);
  background: #0b0e1a;
}

.detail-cover-img,
.detail-cover-fallback {
  display: block;
  width: 100%;
  height: 100%;
  min-height: inherit;
  object-fit: cover;
}

.detail-cover-fallback {
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 28% 22%, rgba(139, 92, 246, 0.5), transparent 62%),
    radial-gradient(circle at 74% 78%, rgba(34, 211, 238, 0.38), transparent 58%),
    var(--bg-raised);
}

.detail-cover-fallback span {
  font-size: 64px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.18);
}

.detail-cover-glow {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(to top, rgba(5, 6, 13, 0.65), transparent 45%);
}

/* ---------- 购买面板 ---------- */
.detail-buy {
  display: flex;
  flex-direction: column;
  padding: 28px 30px 30px;
  align-self: start;
}

.detail-title {
  margin: 14px 0 10px;
  font-size: clamp(25px, 2.8vw, 33px);
  font-weight: 800;
  line-height: 1.3;
  text-wrap: balance;
}

.detail-price {
  margin: 0 0 12px;
  font-size: 34px;
  font-weight: 600;
  color: var(--cyan-soft);
  text-shadow: var(--glow-cyan);
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 6px 0;
  color: var(--ink-soft);
  font-size: 13.5px;
}

.detail-stock-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--green);
  box-shadow: 0 0 12px rgba(52, 211, 153, 0.8);
}

.detail-sold-out {
  margin: 6px 0;
  color: var(--red);
  font-size: 13.5px;
}

.detail-desc {
  margin: 16px 0;
  color: var(--ink-soft);
  white-space: pre-wrap;
  line-height: 1.75;
  font-size: 14.5px;
}

.detail-error {
  color: var(--red);
  font-size: 13.5px;
}

.detail-notice {
  margin: auto 0 20px;
  padding: 14px 16px;
  border-radius: 12px;
  background: rgba(139, 92, 246, 0.07);
  border: 1px dashed rgba(139, 92, 246, 0.4);
}

.detail-notice-title {
  margin: 0 0 6px;
  color: var(--violet-soft);
  font-weight: 600;
  font-size: 13.5px;
}

.detail-notice-body {
  margin: 0;
  color: var(--mute);
  line-height: 1.65;
  font-size: 13px;
}

.detail-cta {
  width: 100%;
  height: 46px;
  font-size: 15px;
}

@media (max-width: 800px) {
  .detail {
    grid-template-columns: 1fr;
  }

  .detail-cover {
    min-height: 200px;
  }
}
</style>
