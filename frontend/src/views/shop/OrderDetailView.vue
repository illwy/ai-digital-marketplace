<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import CardSecretPanel from '../../components/shop/CardSecretPanel.vue'
import { cancelOrder, createAfterSale, fetchDeliveries, fetchOrder, payOrder, sandboxTopup } from '../../api/shop'
import { readApiError } from '../../api/http'
import { useWalletStore } from '../../stores/wallet'
import { deliveryStatusLabel, orderAftersaleLabel, payStatusLabel } from '../../utils/labels'
import { formatFen } from '../../utils/money'
import { expireCountdown } from '../../utils/time'
import type { DeliveryView, OrderView } from '../../types/api'

const route = useRoute()
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
  <el-card v-if="order && issued" class="voucher">
    <p class="issued-kicker">发卡凭证</p>
    <h2 class="page-title">{{ order.productName }}</h2>
    <p class="issued-copy">订单号 {{ order.orderNo }} · {{ formatFen(order.amountFen) }}</p>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <CardSecretPanel v-if="delivery?.content" :content="delivery.content" hint="请立即复制并自行保存。" />
    <p v-else class="page-error">卡密加载失败，请到「卡密」页查看。</p>
    <RouterLink class="issued-link" to="/deliveries">我的卡密</RouterLink>
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
  <el-card v-else-if="order" class="pending-slip">
    <h2 class="page-title">订单 {{ order.orderNo }}</h2>
    <p>商品：{{ order.productName }}</p>
    <p>金额：{{ formatFen(order.amountFen) }}</p>
    <p>支付状态：{{ payStatusLabel(order.payStatus) }}</p>
    <p>交付状态：{{ deliveryStatusLabel(order.deliveryStatus) }}</p>
    <p>售后状态：{{ orderAftersaleLabel(order.aftersaleStatus) }}</p>
    <p v-if="canPay && remain">剩余支付时间：{{ remain }}</p>
    <p v-if="wallet">钱包余额：{{ formatFen(wallet.balanceFen) }}</p>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <div v-if="canPay" class="actions">
      <el-button type="primary" size="large" :loading="paying" @click="pay('SANDBOX')">确认付款并出卡</el-button>
      <el-button :loading="paying" @click="pay('WALLET')">钱包支付</el-button>
      <el-button @click="topup">钱包充值 10 元</el-button>
      <el-button @click="onCancel">取消订单</el-button>
    </div>
  </el-card>
  <el-skeleton v-else-if="loading" :rows="6" animated />
  <el-empty v-else :description="errorMessage || '订单不存在'" />
</template>

<style scoped>
.page-title {
  margin-top: 0;
}

.page-error {
  color: var(--el-color-danger);
}

.actions,
.aftersale {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}

.aftersale-btn {
  align-self: flex-start;
}

.aftersale-hint {
  margin-top: 16px;
  color: var(--mute);
}

.voucher {
  position: relative;
  overflow: visible !important;
  background: var(--ticket) !important;
  color: var(--ticket-ink);
  border: 0 !important;
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

.voucher .page-title {
  font-family: "Noto Serif SC", serif;
}

.voucher .issued-copy,
.voucher .issued-link,
.voucher .aftersale-hint {
  color: var(--ticket-mute);
}

.issued-kicker {
  margin: 0 0 4px;
  color: #9a3b16;
  font-size: 13px;
  letter-spacing: 0.12em;
}

.issued-copy {
  margin: 0;
  color: var(--mute);
}

.issued-link {
  display: inline-block;
  margin-top: 12px;
}

.aftersale-fold {
  margin-top: 20px;
  border: none;
}

.pending-slip {
  background: var(--ticket) !important;
  color: var(--ticket-ink);
  border: 0 !important;
}

.pending-slip .page-title {
  font-family: "Noto Serif SC", serif;
}
</style>
