<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { motion } from 'motion-v'
import CardSecretPanel from '../../components/shop/CardSecretPanel.vue'
import FoilBadge from '../../components/shop/FoilBadge.vue'
import { cancelOrder, createAfterSale, fetchDeliveries, fetchOrder, payOrder, sandboxTopup } from '../../api/shop'
import { readApiError } from '../../api/http'
import { useWalletStore } from '../../stores/wallet'
import { deliveryStatusLabel, orderAftersaleLabel, payStatusLabel } from '../../utils/labels'
import { formatFen } from '../../utils/money'
import { expireCountdown } from '../../utils/time'
import type { DeliveryView, OrderView } from '../../types/api'

const route = useRoute()
const router = useRouter()
const walletStore = useWalletStore()
const { wallet } = storeToRefs(walletStore)
const order = ref<OrderView | null>(null)
const delivery = ref<DeliveryView | null>(null)
const errorMessage = ref('')
const paying = ref(false)
const loading = ref(false)
const reason = ref('')

const orderId = computed(() => Number(route.params.id))
const canPay = computed(() => order.value?.payStatus === 'PENDING')
const issued = computed(
  () => order.value?.payStatus === 'PAID' && order.value.deliveryStatus === 'DELIVERED',
)
const remain = ref('')
let timer = 0

function tick(): void {
  remain.value = order.value?.payStatus === 'PENDING' ? expireCountdown(order.value.expireAt) : ''
}

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const orderRes = await fetchOrder(orderId.value)
    order.value = orderRes.data.data
    delivery.value = null
    if (order.value.deliveryStatus === 'DELIVERED') {
      const { data } = await fetchDeliveries({ orderId: orderId.value, page: 1 })
      delivery.value = data.data[0] ?? null
    }
    await walletStore.refresh()
  } catch (error) {
    errorMessage.value = readApiError(error).message
    order.value = null
    delivery.value = null
  } finally {
    loading.value = false
  }
}

async function pay(channel: 'WALLET' | 'SANDBOX'): Promise<void> {
  paying.value = true
  errorMessage.value = ''
  try {
    await payOrder(orderId.value, channel)
    await load()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  } finally {
    paying.value = false
  }
}

async function topup(): Promise<void> {
  try {
    const { data } = await sandboxTopup(1000)
    walletStore.setBalance(data.data.balanceFen)
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
}

async function onCancel(): Promise<void> {
  try {
    const { data } = await cancelOrder(orderId.value)
    order.value = data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
}

async function submitAfterSale(): Promise<void> {
  if (!reason.value.trim()) {
    errorMessage.value = '请填写售后原因'
    return
  }
  try {
    await createAfterSale(orderId.value, reason.value.trim())
    reason.value = ''
    await load()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
}

onMounted(() => {
  void load()
  timer = window.setInterval(tick, 1000)
})
onUnmounted(() => {
  window.clearInterval(timer)
})
watch(orderId, load)
watch(order, tick, { immediate: true })
</script>

<template>
  <!-- 已出卡：交付凭证 -->
  <motion.div
    v-if="order && issued"
    :initial="{ opacity: 0, scale: 0.97 }"
    :animate="{ opacity: 1, scale: 1 }"
    :transition="{ duration: 0.45, ease: [0.22, 1, 0.36, 1] }"
  >
    <el-card class="voucher">
    <div class="voucher-head">
      <span class="voucher-badge font-display">DELIVERED</span>
      <FoilBadge :label="orderAftersaleLabel(order.aftersaleStatus)" :tone="order.aftersaleStatus === 'NONE' ? 'mute' : 'warn'" />
    </div>
    <h2 class="page-title">{{ order.productName }}</h2>
    <p class="issued-copy font-mono">{{ order.orderNo }} · {{ formatFen(order.amountFen) }}</p>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <CardSecretPanel v-if="delivery?.content" :content="delivery.content" hint="请立即复制并自行保存。" />
    <p v-else class="page-error">节点订阅或卡密正在准备，请打开对应的交付页面。</p>
    <RouterLink v-if="delivery?.content" class="issued-link" to="/deliveries">查看我的卡密 →</RouterLink>
    <RouterLink v-else class="issued-link" to="/node-subscriptions">查看我的节点订阅 →</RouterLink>

    <el-collapse class="aftersale-fold">
      <el-collapse-item title="卡密有问题？提交售后" name="aftersale">
        <div v-if="order.aftersaleStatus === 'NONE'" class="aftersale">
          <el-input v-model="reason" type="textarea" placeholder="售后原因" />
          <el-button class="aftersale-btn" @click="submitAfterSale">提交售后</el-button>
        </div>
        <p v-else class="aftersale-hint">
          售后状态：{{ orderAftersaleLabel(order.aftersaleStatus) }}，可在
          <RouterLink to="/after-sales">我的售后</RouterLink>
          查看回复。
        </p>
      </el-collapse-item>
    </el-collapse>
    </el-card>
  </motion.div>

  <!-- 待支付 -->
  <motion.div
    v-if="order && !issued"
    :initial="{ opacity: 0, y: 20 }"
    :animate="{ opacity: 1, y: 0 }"
    :transition="{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }"
  >
    <el-card class="pending-slip glass-panel">
    <div class="pending-head">
      <h2 class="page-title">订单支付</h2>
      <FoilBadge :label="payStatusLabel(order.payStatus)" :tone="canPay ? 'warn' : 'mute'" />
    </div>

    <div class="pending-product">
      <p class="pending-name">{{ order.productName }}</p>
      <p class="pending-amount font-mono">{{ formatFen(order.amountFen) }}</p>
    </div>

    <div v-if="canPay && remain" class="countdown">
      <span class="countdown-label">剩余支付时间</span>
      <span class="countdown-value font-mono">{{ remain }}</span>
    </div>

    <dl class="pending-meta">
      <div><dt>订单号</dt><dd class="font-mono">{{ order.orderNo }}</dd></div>
      <div><dt>支付状态</dt><dd>{{ payStatusLabel(order.payStatus) }}</dd></div>
      <div><dt>交付状态</dt><dd>{{ deliveryStatusLabel(order.deliveryStatus) }}</dd></div>
      <div><dt>售后状态</dt><dd>{{ orderAftersaleLabel(order.aftersaleStatus) }}</dd></div>
      <div v-if="wallet"><dt>钱包余额</dt><dd class="font-mono">{{ formatFen(wallet.balanceFen) }}</dd></div>
    </dl>

    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>

    <div v-if="canPay" class="actions">
      <el-button type="primary" size="large" class="pay-main" @click="router.push(`/orders/${order.id}/pay`)">
        支付宝付款
      </el-button>
      <el-button size="large" :loading="paying" @click="pay('SANDBOX')">演示出卡</el-button>
      <el-button size="large" :loading="paying" @click="pay('WALLET')">钱包支付</el-button>
      <el-button size="large" @click="topup">钱包充值 ¥10</el-button>
      <el-button size="large" text @click="onCancel">取消订单</el-button>
    </div>
    </el-card>
  </motion.div>

  <el-skeleton v-if="loading" :rows="6" animated />
  <el-empty v-else-if="!order" :description="errorMessage || '订单不存在'" />
</template>

<style scoped>
.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
}

.page-error {
  color: var(--red);
  font-size: 13.5px;
}

.actions,
.aftersale {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}

.aftersale-btn {
  align-self: flex-start;
}

.aftersale-hint {
  margin-top: 16px;
  color: var(--mute);
}

/* ---------- 交付凭证 ---------- */
.voucher {
  position: relative;
  overflow: hidden;
  border-radius: 22px !important;
  background:
    linear-gradient(160deg, rgba(34, 211, 238, 0.09), rgba(139, 92, 246, 0.12) 55%, rgba(244, 114, 182, 0.07)),
    var(--bg-raised) !important;
  border: 1px solid rgba(34, 211, 238, 0.3) !important;
  box-shadow: 0 26px 70px rgba(0, 0, 0, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  padding: 6px;
}

.voucher::before {
  content: "";
  position: absolute;
  top: -50%;
  left: -20%;
  width: 60%;
  height: 200%;
  pointer-events: none;
  background: radial-gradient(ellipse, rgba(139, 92, 246, 0.14), transparent 65%);
}

.voucher-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.voucher-badge {
  padding: 5px 13px;
  border-radius: 999px;
  border: 1px solid rgba(52, 211, 153, 0.55);
  color: var(--green);
  font-size: 11px;
  letter-spacing: 0.2em;
  animation: pulse-glow 2.6s ease infinite;
}

.issued-copy {
  margin: 6px 0 0;
  color: var(--mute);
  font-size: 13px;
}

.issued-link {
  display: inline-block;
  margin-top: 14px;
  color: var(--cyan-soft);
  text-decoration: none;
  font-size: 14px;
}

.issued-link:hover {
  text-decoration: underline;
}

.aftersale-fold {
  margin-top: 22px;
  border: none;
}

/* ---------- 待支付 ---------- */
.pending-slip {
  max-width: 640px;
  margin: 0 auto;
  padding: 8px;
}

.pending-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 18px;
}

.pending-product {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 14px;
  background: var(--surface);
  border: 1px solid var(--line);
}

.pending-name {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.pending-amount {
  margin: 0;
  font-size: 26px;
  font-weight: 600;
  color: var(--cyan-soft);
}

.countdown {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding: 12px 20px;
  border-radius: 12px;
  background: rgba(251, 191, 36, 0.08);
  border: 1px solid rgba(251, 191, 36, 0.35);
}

.countdown-label {
  color: var(--amber);
  font-size: 13.5px;
}

.countdown-value {
  font-size: 19px;
  font-weight: 600;
  color: var(--amber);
  font-variant-numeric: tabular-nums;
}

.pending-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px 18px;
  margin: 18px 0 0;
}

.pending-meta div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 8px;
  border-bottom: 1px dashed var(--line);
  font-size: 13.5px;
}

.pending-meta dt {
  color: var(--mute);
  white-space: nowrap;
  flex-shrink: 0;
}

.pending-meta dd {
  margin: 0;
  color: var(--ink-soft);
  text-align: right;
  min-width: 0;
  word-break: break-all;
}

.pay-main {
  min-width: 220px;
}
</style>
