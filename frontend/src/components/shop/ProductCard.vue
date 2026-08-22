<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { ProductView } from '../../types/api'
import { deliveryTypeLabel } from '../../utils/labels'
import { formatFen } from '../../utils/money'
import FoilBadge from './FoilBadge.vue'

const props = defineProps<{
  product: ProductView
}>()

const coverFailed = ref(false)
const soldOut = computed(() => props.product.availableCount < 1)
const stockText = computed(() => (soldOut.value ? '售罄' : `库存 ${props.product.availableCount}`))
const ctaText = computed(() => (soldOut.value ? '售罄' : '立即购买'))

watch(
  () => props.product.coverUrl,
  () => {
    coverFailed.value = false
  },
)
</script>

<template>
  <RouterLink class="product-link" :to="`/products/${product.id}`">
    <span class="ticket-dash" aria-hidden="true" />
    <article class="ticket" :class="{ 'is-sold-out': soldOut }">
      <div class="ticket-cover">
        <img
          v-if="product.coverUrl && !coverFailed"
          class="ticket-cover-img"
          :src="product.coverUrl"
          alt=""
          @error="coverFailed = true"
        />
        <div v-else class="ticket-cover-fallback" />
        <FoilBadge class="ticket-type" :label="deliveryTypeLabel(product.deliveryType)" />
      </div>
      <div class="ticket-perf" aria-hidden="true">
        <span class="ticket-notch ticket-notch-left" />
        <span class="ticket-notch ticket-notch-right" />
      </div>
      <div class="ticket-body">
        <h3 class="ticket-name">{{ product.name }}</h3>
        <p class="ticket-stock">{{ stockText }}</p>
      </div>
      <div class="ticket-actions">
        <p class="ticket-price">{{ formatFen(product.priceFen) }}</p>
        <span class="ticket-cta">{{ ctaText }}</span>
      </div>
    </article>
  </RouterLink>
</template>

<style scoped>
.product-link {
  position: relative;
  display: block;
  height: 100%;
  color: inherit;
  text-decoration: none;
}

.product-link:focus-visible {
  outline: 2px solid var(--copper);
  outline-offset: 4px;
}

.ticket-dash {
  position: absolute;
  inset: 0;
  border: 1.5px dashed var(--copper);
  pointer-events: none;
}

.ticket {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--ticket);
  color: var(--ticket-ink);
  transition: transform 180ms cubic-bezier(0.16, 1, 0.3, 1);
}

.product-link:hover .ticket,
.product-link:focus-visible .ticket {
  transform: translate(-6px, -6px);
}

.ticket.is-sold-out {
  filter: grayscale(0.55);
  opacity: 0.78;
}

.ticket-cover {
  position: relative;
  height: 148px;
  overflow: hidden;
  background: #2a211c;
}

.ticket-cover-img,
.ticket-cover-fallback {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ticket-cover-fallback {
  background: linear-gradient(145deg, #3a2a22, #c45c28);
}

.ticket-type {
  position: absolute;
  left: 10px;
  bottom: 10px;
  color: #f3c9a0;
  background: rgba(17, 12, 9, 0.78);
  border-color: color-mix(in srgb, var(--copper) 70%, #f3c9a0);
}

.ticket-perf {
  position: relative;
  height: 18px;
  background: var(--ticket);
}

.ticket-perf::after {
  content: "";
  position: absolute;
  left: 18px;
  right: 18px;
  top: 50%;
  border-top: 1.5px dashed #cbb8a6;
}

.ticket-notch {
  position: absolute;
  top: 50%;
  z-index: 1;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--paper);
  transform: translateY(-50%);
  box-shadow: 0 0 0 2px var(--paper);
}

.ticket-notch-left {
  left: -8px;
}

.ticket-notch-right {
  right: -8px;
}

.ticket-body {
  flex: 1;
  padding: 4px 14px 10px;
}

.ticket-name {
  margin: 0 0 8px;
  font-family: "Noto Serif SC", serif;
  font-size: 17px;
  line-height: 1.35;
  text-wrap: pretty;
}

.ticket-stock {
  margin: 0;
  color: var(--ticket-mute);
  font-size: 12px;
}

.ticket-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 14px 14px;
}

.ticket-price {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: #9a3b16;
}

.ticket-cta {
  padding: 5px 10px;
  background: var(--copper);
  color: #1a120c;
  font-size: 12px;
  font-weight: 700;
}

.ticket.is-sold-out .ticket-cta {
  background: #c4b6a8;
}
</style>
