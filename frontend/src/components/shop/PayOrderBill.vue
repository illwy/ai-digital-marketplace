<script setup lang="ts">
import { computed, shallowRef, watch } from 'vue'
import type { OrderView, ProductView } from '../../types/api'
import { deliveryStatusLabel, deliveryTypeLabel, payStatusLabel } from '../../utils/labels'
import { formatFen } from '../../utils/money'
import { formatDateTime } from '../../utils/time'
import FoilBadge from './FoilBadge.vue'

const props = defineProps<{
  order: OrderView
  product: ProductView | null
  remain: string
  buyerName: string
}>()

const coverFailed = shallowRef(false)
const deliveryType = computed(() =>
  props.product ? deliveryTypeLabel(props.product.deliveryType) : '数字商品',
)
const coverUrl = computed(() => props.product?.coverUrl ?? null)

watch(coverUrl, () => {
  coverFailed.value = false
})
</script>

<template>
  <section class="bill glass-panel">
    <header class="bill-head">
      <div class="bill-brand">
        <span class="bill-mark">钥</span>
        <div>
          <p class="bill-kicker">钥市收银台</p>
          <h1 class="bill-title">确认付款</h1>
        </div>
      </div>
      <FoilBadge :label="payStatusLabel(order.payStatus)" :tone="order.payStatus === 'PENDING' ? 'warn' : 'ok'" />
    </header>

    <div class="bill-product">
      <div class="bill-cover">
        <img
          v-if="coverUrl && !coverFailed"
          class="bill-cover-img"
          :src="coverUrl"
          alt=""
          @error="coverFailed = true"
        />
        <span v-else class="bill-cover-fallback font-display">AI</span>
      </div>
      <div class="bill-product-copy">
        <FoilBadge :label="deliveryType" tone="azure" />
        <p class="bill-product-name">{{ order.productName }}</p>
        <p class="bill-product-sub">数字商品 · 一单一件 · 付款后立刻出卡</p>
      </div>
      <p class="bill-product-price font-mono">{{ formatFen(order.amountFen) }}</p>
    </div>

    <dl class="bill-meta">
      <div class="bill-meta-row">
        <dt>收款商户</dt>
        <dd>钥市</dd>
      </div>
      <div class="bill-meta-row">
        <dt>付款买家</dt>
        <dd>{{ buyerName || '已登录买家' }}</dd>
      </div>
      <div class="bill-meta-row">
        <dt>商户订单号</dt>
        <dd class="font-mono">{{ order.orderNo }}</dd>
      </div>
      <div class="bill-meta-row">
        <dt>商品编号</dt>
        <dd class="font-mono">#{{ order.productId }}</dd>
      </div>
      <div class="bill-meta-row">
        <dt>下单时间</dt>
        <dd class="font-mono">{{ formatDateTime(order.createdAt) }}</dd>
      </div>
      <div class="bill-meta-row">
        <dt>支付截止</dt>
        <dd class="font-mono">{{ formatDateTime(order.expireAt) || '—' }}</dd>
      </div>
      <div class="bill-meta-row">
        <dt>交付方式</dt>
        <dd>{{ deliveryType }}自动发放</dd>
      </div>
      <div class="bill-meta-row">
        <dt>交付状态</dt>
        <dd>{{ deliveryStatusLabel(order.deliveryStatus) }}</dd>
      </div>
    </dl>

    <div v-if="remain" class="bill-countdown">
      <span>剩余支付时间</span>
      <strong class="font-mono">{{ remain }}</strong>
    </div>

    <ul class="bill-lines">
      <li>
        <span>商品金额</span>
        <span class="font-mono">{{ formatFen(order.amountFen) }}</span>
      </li>
      <li>
        <span>数量</span>
        <span class="font-mono">1 件</span>
      </li>
      <li>
        <span>物流运费</span>
        <span class="font-mono">¥0.00</span>
      </li>
      <li>
        <span>优惠减免</span>
        <span class="font-mono">¥0.00</span>
      </li>
      <li class="bill-lines-total">
        <span>应付金额</span>
        <span class="font-mono">{{ formatFen(order.amountFen) }}</span>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.bill {
  padding: 24px 26px 26px;
}

.bill-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.bill-brand {
  display: flex;
  gap: 12px;
  align-items: center;
}

.bill-mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--grad-primary);
  color: var(--on-accent);
  font-weight: 800;
}

.bill-kicker {
  margin: 0 0 4px;
  color: var(--gold-soft);
  font-size: 12px;
  letter-spacing: 0.16em;
}

.bill-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
}

.bill-product {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 14px;
  border-radius: 14px;
  background: var(--surface-strong);
  border: 1px solid var(--line);
}

.bill-cover {
  width: 72px;
  height: 72px;
  overflow: hidden;
  border-radius: 12px;
  background: #ffffff;
}

.bill-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.bill-cover-fallback {
  display: grid;
  place-items: center;
  height: 100%;
  color: var(--azure-soft);
}

.bill-product-name {
  margin: 8px 0 4px;
  font-weight: 700;
  line-height: 1.4;
}

.bill-product-sub,
.bill-kicker {
  color: var(--mute);
}

.bill-product-sub {
  margin: 0;
  font-size: 12.5px;
}

.bill-product-price {
  margin: 0;
  font-size: 20px;
  color: var(--azure-soft);
  font-weight: 700;
}

.bill-meta {
  display: grid;
  gap: 0;
  margin: 18px 0 0;
}

.bill-meta-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 9px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 13.5px;
}

.bill-meta-row dt {
  color: var(--mute);
  flex-shrink: 0;
}

.bill-meta-row dd {
  margin: 0;
  color: var(--ink-soft);
  text-align: right;
  word-break: break-all;
}

.bill-countdown {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(251, 191, 36, 0.08);
  border: 1px solid rgba(251, 191, 36, 0.35);
  color: var(--amber);
}

.bill-countdown strong {
  font-size: 18px;
  font-variant-numeric: tabular-nums;
}

.bill-lines {
  list-style: none;
  margin: 16px 0 0;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(125, 188, 232, 0.05);
  border: 1px solid var(--line);
}

.bill-lines li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 0;
  color: var(--ink-soft);
  font-size: 13.5px;
}

.bill-lines-total {
  margin-top: 6px;
  padding-top: 12px !important;
  border-top: 1px solid var(--line);
  color: var(--ink) !important;
  font-weight: 700;
  font-size: 16px !important;
}

.bill-lines-total span:last-child {
  color: var(--azure-soft);
  font-size: 22px;
}

@media (max-width: 720px) {
  .bill-product {
    grid-template-columns: 64px minmax(0, 1fr);
  }

  .bill-product-price {
    grid-column: 1 / -1;
    text-align: right;
  }
}
</style>
