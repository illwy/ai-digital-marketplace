<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchNodeSubscriptions, fetchOrders } from '../../api/shop'
import { readApiError } from '../../api/http'
import { nodeSubscriptionStatusLabel } from '../../utils/labels'
import { formatDateTime } from '../../utils/time'
import type { NodeSubscriptionView, OrderView } from '../../types/api'

const PROCESSING_STATUSES = new Set(['PROVISIONING', 'PENDING'])

const items = ref<NodeSubscriptionView[]>([])
const pendingOrders = ref<OrderView[]>([])
const loading = ref(false)
const refreshing = ref(false)
const errorMessage = ref('')

const activeCount = computed(() => items.value.filter((item) => item.status === 'ACTIVE').length)
const processingCount = computed(() => {
  const fromSubscriptions = items.value.filter((item) => PROCESSING_STATUSES.has(item.status)).length
  return fromSubscriptions + pendingOrders.value.length
})
const hasContent = computed(() => items.value.length > 0 || pendingOrders.value.length > 0)

let refreshTimer: number | undefined
let loadSeq = 0
let alive = true

function statusClass(status: string | null | undefined): string {
  return status ? 'status-' + status.toLowerCase() : ''
}

function trafficLabel(bytes: number | null | undefined): string {
  if (bytes == null || Number.isNaN(Number(bytes))) return '—'
  return Math.round(Number(bytes) / 1073741824) + ' GB'
}

function shouldPoll(): boolean {
  return alive && !document.hidden && processingCount.value > 0 && !loading.value && !refreshing.value
}

function stopPolling(): void {
  if (refreshTimer !== undefined) {
    window.clearInterval(refreshTimer)
    refreshTimer = undefined
  }
}

function startPolling(): void {
  stopPolling()
  if (processingCount.value <= 0) return
  refreshTimer = window.setInterval(() => {
    if (shouldPoll()) void load({ silent: true })
  }, 15000)
}

async function load(options: { silent?: boolean } = {}): Promise<void> {
  const seq = ++loadSeq
  const silent = Boolean(options.silent)
  if (silent || hasContent.value) {
    refreshing.value = true
  } else {
    loading.value = true
  }
  if (!silent) errorMessage.value = ''
  try {
    const [subsResult, ordersResult] = await Promise.allSettled([
      fetchNodeSubscriptions(),
      fetchOrders(1),
    ])
    if (!alive || seq !== loadSeq) return

    if (ordersResult.status === 'fulfilled') {
      const orders = ordersResult.value.data.data
      const orderList = Array.isArray(orders) ? orders : []
      pendingOrders.value = orderList.filter(
        (order) => order.payStatus === 'PAID' && order.deliveryStatus === 'PROVISIONING',
      )
    }

    if (subsResult.status === 'fulfilled') {
      const subs = subsResult.value.data.data
      items.value = Array.isArray(subs) ? subs : []
      errorMessage.value = ''
    } else {
      errorMessage.value = readApiError(subsResult.reason).message
    }
  } catch (error) {
    if (!alive || seq !== loadSeq) return
    errorMessage.value = readApiError(error).message
  } finally {
    if (seq === loadSeq) {
      loading.value = false
      refreshing.value = false
      if (alive) startPolling()
    }
  }
}

async function copy(value: string, label: string): Promise<void> {
  if (!value) return
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success(label + '已复制')
  } catch {
    ElMessage.error('复制失败，请手动选择内容')
  }
}

function onVisibility(): void {
  if (document.hidden) return
  if (processingCount.value > 0) void load({ silent: true })
}

onMounted(() => {
  alive = true
  void load()
  document.addEventListener('visibilitychange', onVisibility)
})

onUnmounted(() => {
  alive = false
  stopPolling()
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>

<template>
  <section class="page-shell">
    <div class="page-heading">
      <div>
        <p class="eyebrow">NODE ACCESS</p>
        <h1 class="page-title font-display">我的<span class="grad-text">节点</span></h1>
        <p class="page-desc">支付成功后，2S-UI 会在后台自动开通；开通期间这里会显示处理中，并自动刷新进度。</p>
      </div>
      <el-button :loading="loading || refreshing" @click="load()">刷新数据</el-button>
    </div>
    <div v-if="errorMessage" class="subscription-error" role="alert">
      <span>{{ errorMessage }}</span>
      <el-button text type="primary" @click="load()">重新加载</el-button>
    </div>
    <div v-if="hasContent" class="subscription-summary">
      <div class="summary-item"><strong>{{ activeCount }}</strong><span>正常订阅</span></div>
      <div class="summary-item"><strong>{{ processingCount }}</strong><span>开通中</span></div>
      <div class="summary-item"><strong>{{ items.length + pendingOrders.length }}</strong><span>全部订阅</span></div>
    </div>
    <el-skeleton v-if="loading && !hasContent" :rows="8" animated />
    <el-empty v-else-if="!hasContent && !errorMessage" description="还没有节点订阅，购买节点商品后会显示在这里">
      <RouterLink to="/">
        <el-button type="primary">去选节点商品</el-button>
      </RouterLink>
    </el-empty>
    <div v-else-if="hasContent" class="node-grid">
      <el-card v-for="order in pendingOrders" :key="'pending-' + order.id" class="node-card is-pending" shadow="never">
        <div class="node-card-head">
          <div>
            <span class="node-status status-provisioning">开通中</span>
            <h2>{{ order.productName }}</h2>
          </div>
          <span class="node-expiry">到期 开通完成后显示</span>
        </div>
        <p class="node-meta">订单 {{ order.orderNo }} · 后台正在向 2S-UI 开通，大约每 15 秒自动刷新</p>
        <div class="node-url">
          <code>等待 2S-UI 返回订阅链接</code>
        </div>
        <RouterLink class="order-link" :to="'/orders/' + order.id">查看订单 →</RouterLink>
      </el-card>
      <el-card v-for="item in items" :key="item.id" class="node-card" shadow="never">
        <div class="node-card-head">
          <div>
            <span class="node-status" :class="statusClass(item.status)">{{ nodeSubscriptionStatusLabel(item.status) }}</span>
            <h2>{{ item.clientName }}</h2>
          </div>
          <span class="node-expiry">到期 {{ formatDateTime(item.expiresAt) || '待开通后显示' }}</span>
        </div>
        <p class="node-meta">流量 {{ trafficLabel(item.trafficBytes) }} · 设备 {{ item.deviceLimit || '不限' }}</p>
        <div class="node-url">
          <code>{{ item.subscriptionUrl || '等待 2S-UI 返回订阅链接' }}</code>
          <el-button
            v-if="item.subscriptionUrl"
            text
            type="primary"
            :aria-label="'复制' + (item.clientName || '节点') + '的订阅地址'"
            @click="copy(item.subscriptionUrl, '订阅地址')"
          >复制</el-button>
        </div>
        <details v-if="item.clashConfig">
          <summary>查看协议链接（Clash/Mihomo 可转换）</summary>
          <pre>{{ item.clashConfig }}</pre>
          <el-button class="copy-config" text type="primary" @click="copy(item.clashConfig, '配置内容')">复制配置</el-button>
        </details>
      </el-card>
    </div>
  </section>
</template>

<style scoped>
.page-shell { display: flex; flex-direction: column; gap: 4px; }
.page-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.eyebrow { margin: 0 0 8px; color: var(--azure-soft); font-size: 12px; letter-spacing: 0.16em; }
.page-title { margin: 0; font-size: 28px; }
.page-desc { margin: 8px 0 0; color: var(--mute); font-size: 14px; max-width: 46rem; }
.node-grid { display: grid; gap: 16px; }
.subscription-summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin: 22px 0 16px; }
.summary-item { padding: 16px 18px; border: 1px solid var(--line); border-radius: 14px; background: var(--surface); }
.summary-item strong { display: block; color: var(--ink); font: 700 24px var(--font-mono); }
.summary-item span { color: var(--mute); font-size: 12px; }
.subscription-error { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 18px 0; padding: 12px 14px; border: 1px solid rgba(225, 29, 72, .24); border-radius: 10px; color: var(--red); background: rgba(225, 29, 72, .06); }
.node-card { border-color: var(--line); background: rgba(12, 15, 28, .72); }
.node-card.is-pending { border-color: rgba(232, 163, 23, .35); }
.node-card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.node-card h2 { margin: 8px 0 0; font-size: 18px; }
.node-status { color: var(--azure-soft); font-size: 12px; letter-spacing: .08em; }
.status-active { color: var(--green); }
.status-provisioning, .status-pending { color: var(--amber); }
.status-expired, .status-failed { color: var(--red); }
.node-expiry, .node-meta { color: var(--mute); font-size: 13px; }
.node-url { display: flex; align-items: center; gap: 10px; margin-top: 16px; }
.node-url code { overflow: hidden; flex: 1; min-width: 0; padding: 10px; border: 1px solid var(--line); border-radius: 8px; color: var(--ink); white-space: nowrap; text-overflow: ellipsis; }
.order-link { display: inline-block; margin-top: 12px; color: var(--azure-soft); text-decoration: none; font-size: 13px; }
.order-link:hover { text-decoration: underline; }
details { margin-top: 14px; color: var(--mute); font-size: 13px; }
pre { max-height: 180px; overflow: auto; margin-top: 8px; padding: 10px; border-radius: 8px; background: rgba(0, 0, 0, .25); white-space: pre-wrap; word-break: break-all; }
.copy-config { margin-top: 8px; }
@media (max-width: 620px) {
  .page-heading { flex-direction: column; }
  .subscription-summary { grid-template-columns: 1fr; }
  .subscription-error { align-items: flex-start; flex-direction: column; }
  .node-card-head { flex-direction: column; gap: 8px; }
  .node-url { align-items: stretch; flex-direction: column; }
}
</style>
