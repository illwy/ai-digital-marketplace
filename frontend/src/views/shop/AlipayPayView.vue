<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { motion } from 'motion-v'
import PayAlipayDesk from '../../components/shop/PayAlipayDesk.vue'
import PayOrderBill from '../../components/shop/PayOrderBill.vue'
import {
  fetchOrder,
  fetchPayChannels,
  fetchProduct,
  payOrder,
  submitAlipayForm,
  syncAlipayPayment,
} from '../../api/shop'
import { formatApiError, readApiError } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import { expireCountdown } from '../../utils/time'
import type { OrderView, ProductView } from '../../types/api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const order = ref<OrderView | null>(null)
const product = ref<ProductView | null>(null)
const errorMessage = shallowRef('')
const loading = shallowRef(false)
const paying = shallowRef(false)
const confirming = shallowRef(false)
const alipayEnabled = shallowRef(false)
const cashierOpen = shallowRef(false)
const remain = shallowRef('')
const orderId = computed(() => Number(route.params.id))
const waitingResult = computed(() => route.query.from === 'alipay')
let timer = 0
let pollTimer = 0

function tick(): void {
  remain.value = order.value?.payStatus === 'PENDING' ? expireCountdown(order.value.expireAt) : ''
}

async function loadOrder(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchOrder(orderId.value)
    order.value = data.data
    if (order.value.payStatus === 'PAID') {
      await router.replace(`/orders/${orderId.value}`)
      return
    }
    try {
      const productRes = await fetchProduct(order.value.productId)
      product.value = productRes.data.data
    } catch {
      product.value = null
    }
  } catch (error) {
    errorMessage.value = formatApiError(error)
    order.value = null
    product.value = null
  } finally {
    loading.value = false
  }
}

async function confirmFromAlipay(): Promise<void> {
  confirming.value = true
  errorMessage.value = ''
  try {
    await syncAlipayPayment(orderId.value)
  } catch (error) {
    const code = readApiError(error).code
    if (code !== 'PAYMENT_NOT_FOUND') {
      errorMessage.value = formatApiError(error)
    }
  }
  try {
    const { data } = await fetchOrder(orderId.value)
    order.value = data.data
    if (order.value.payStatus === 'PAID') {
      await router.replace(`/orders/${orderId.value}`)
    }
  } catch (error) {
    errorMessage.value = formatApiError(error)
  } finally {
    confirming.value = false
  }
}

async function goAlipay(): Promise<void> {
  paying.value = true
  errorMessage.value = ''
  try {
    const { data } = await payOrder(orderId.value, 'ALIPAY')
    if (!data.data.paymentHtml) {
      errorMessage.value = '支付宝未返回支付表单'
      return
    }
    cashierOpen.value = true
    await nextTick()
    submitAlipayForm(data.data.paymentHtml, 'alipayCashier')
  } catch (error) {
    errorMessage.value = formatApiError(error)
  } finally {
    paying.value = false
  }
}

onMounted(async () => {
  try {
    const { data } = await fetchPayChannels()
    alipayEnabled.value = data.data.alipayEnabled
  } catch {
    alipayEnabled.value = false
  }
  await loadOrder()
  timer = window.setInterval(tick, 1000)
  if (waitingResult.value) {
    await confirmFromAlipay()
    pollTimer = window.setInterval(() => {
      void confirmFromAlipay()
    }, 4000)
  }
})

onUnmounted(() => {
  window.clearInterval(timer)
  window.clearInterval(pollTimer)
})
watch(orderId, loadOrder)
watch(order, tick, { immediate: true })
</script>

<template>
  <motion.section
    class="pay-page"
    :initial="{ opacity: 0, y: 16 }"
    :animate="{ opacity: 1, y: 0 }"
    :transition="{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }"
  >
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-empty v-else-if="!order" :description="errorMessage || '订单不存在'" />
    <div v-else class="pay-grid">
      <PayOrderBill
        :order="order"
        :product="product"
        :remain="remain"
        :buyer-name="auth.displayName"
      />
      <PayAlipayDesk
        :amount-fen="order.amountFen"
        :alipay-enabled="alipayEnabled"
        :paying="paying"
        :confirming="confirming"
        :cashier-open="cashierOpen"
        :waiting-result="waitingResult"
        :error-message="errorMessage"
        :order-id="order.id"
        @pay="goAlipay"
        @confirm="confirmFromAlipay"
      />
    </div>
  </motion.section>
</template>

<style scoped>
.pay-page {
  max-width: 1080px;
  margin: 0 auto;
}

.pay-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 18px;
  align-items: start;
}

@media (max-width: 900px) {
  .pay-grid {
    grid-template-columns: 1fr;
  }
}
</style>
