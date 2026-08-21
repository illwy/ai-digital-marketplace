<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { cancelOrder, createAfterSale, fetchOrder, fetchWallet, payOrder, sandboxTopup } from '../../api/shop'
import { readApiError } from '../../api/http'
import { formatFen } from '../../utils/money'
import type { OrderView, WalletView } from '../../types/api'

const route = useRoute()
const order = ref<OrderView | null>(null)
const wallet = ref<WalletView | null>(null)
const errorMessage = ref('')
const paying = ref(false)
const reason = ref('')

const orderId = computed(() => Number(route.params.id))
const canPay = computed(() => order.value?.payStatus === 'PENDING')

async function load(): Promise<void> {
  errorMessage.value = ''
  try {
    const [orderRes, walletRes] = await Promise.all([fetchOrder(orderId.value), fetchWallet()])
    order.value = orderRes.data.data
    wallet.value = walletRes.data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
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
    wallet.value = data.data
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
  try {
    await createAfterSale(orderId.value, reason.value)
    reason.value = ''
    await load()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
}

onMounted(load)
watch(orderId, load)
</script>

<template>
  <el-card v-if="order">
    <h2 class="page-title">订单 {{ order.orderNo }}</h2>
    <p>商品：{{ order.productName }}</p>
    <p>金额：{{ formatFen(order.amountFen) }}</p>
    <p>支付状态：{{ order.payStatus }}</p>
    <p>交付状态：{{ order.deliveryStatus }}</p>
    <p v-if="wallet">钱包余额：{{ formatFen(wallet.balanceFen) }}</p>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <div v-if="canPay" class="actions">
      <el-button @click="topup">沙箱充值 10 元</el-button>
      <el-button type="primary" :loading="paying" @click="pay('WALLET')">钱包支付</el-button>
      <el-button :loading="paying" @click="pay('SANDBOX')">沙箱支付</el-button>
      <el-button @click="onCancel">取消订单</el-button>
    </div>
    <div v-if="order.payStatus === 'PAID'" class="aftersale">
      <el-input v-model="reason" type="textarea" placeholder="售后原因" />
      <el-button class="aftersale-btn" @click="submitAfterSale">提交售后</el-button>
      <RouterLink to="/deliveries">查看已购资源</RouterLink>
    </div>
  </el-card>
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
</style>
