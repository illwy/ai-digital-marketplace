<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { ProductView } from '../../types/api'
import { deliveryTypeLabel } from '../../utils/labels'
import { formatFen } from '../../utils/money'

const props = defineProps<{
  product: ProductView
}>()

const coverFailed = ref(false)
const soldOut = computed(() => props.product.availableCount < 1)
const stockText = computed(() => (soldOut.value ? '已售罄' : `现货 ${props.product.availableCount} 件`))
const lowStock = computed(() => !soldOut.value && props.product.availableCount <= 3)

// 指针跟随的 3D 倾斜 + 光斑
const tiltStyle = ref<Record<string, string>>({})
const glareStyle = ref<Record<string, string>>({})
let frame = 0

function onPointerMove(event: PointerEvent): void {
  const card = event.currentTarget as HTMLElement
  const rect = card.getBoundingClientRect()
  const px = (event.clientX - rect.left) / rect.width
  const py = (event.clientY - rect.top) / rect.height
  cancelAnimationFrame(frame)
  frame = requestAnimationFrame(() => {
    tiltStyle.value = {
      transform: `perspective(900px) rotateX(${(0.5 - py) * 7}deg) rotateY(${(px - 0.5) * 9}deg) translateY(-4px)`,
    }
    glareStyle.value = {
      opacity: '1',
      background: `radial-gradient(circle at ${px * 100}% ${py * 100}%, rgba(255,255,255,0.16), transparent 55%)`,
    }
  })
}

function onPointerLeave(): void {
  cancelAnimationFrame(frame)
  tiltStyle.value = { transform: '' }
  glareStyle.value = { opacity: '0' }
}

watch(
  () => props.product.coverUrl,
  () => {
    coverFailed.value = false
  },
)
</script>

<template>
  <RouterLink class="product-link" :to="`/products/${product.id}`">
    <article
      class="holo-card"
      :class="{ 'is-sold-out': soldOut }"
      :style="tiltStyle"
      @pointermove="onPointerMove"
      @pointerleave="onPointerLeave"
    >
      <span class="holo-glare" :style="glareStyle" aria-hidden="true"></span>
      <span class="holo-edge" aria-hidden="true"></span>

      <div class="holo-cover">
        <img
          v-if="product.coverUrl && !coverFailed"
          class="holo-cover-img"
          :src="product.coverUrl"
          alt=""
          @error="coverFailed = true"
        />
        <div v-else class="holo-cover-fallback">
          <span class="font-display">AI</span>
        </div>
        <span v-if="soldOut" class="holo-soldout font-display">SOLD OUT</span>
        <span class="holo-type">{{ deliveryTypeLabel(product.deliveryType) }}</span>
      </div>

      <div class="holo-body">
        <h3 class="holo-name">{{ product.name }}</h3>
        <p class="holo-stock" :class="{ 'is-low': lowStock }">
          <span class="holo-stock-dot"></span>{{ stockText }}
        </p>
        <div class="holo-actions">
          <p class="holo-price font-mono">{{ formatFen(product.priceFen) }}</p>
          <span class="holo-cta">{{ soldOut ? '到货提醒' : '立即购买' }} →</span>
        </div>
      </div>
    </article>
  </RouterLink>
</template>

<style scoped>
.product-link {
  display: block;
  height: 100%;
  color: inherit;
  text-decoration: none;
  border-radius: 20px;
}

.product-link:focus-visible {
  outline: 2px solid var(--gold);
  outline-offset: 4px;
}

.holo-card {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  border-radius: 20px;
  background: var(--bg-raised);
  border: 1px solid var(--line);
  transform-style: preserve-3d;
  transition: transform 0.35s var(--ease-out), border-color 0.3s ease, box-shadow 0.35s ease;
  will-change: transform;
}

@media (hover: hover) and (pointer: fine) {
  .product-link:hover .holo-card,
  .product-link:focus-visible .holo-card {
    border-color: rgba(18, 179, 154, 0.55);
  box-shadow: 0 18px 40px rgba(22, 48, 43, 0.10), var(--glow-gold);
  }
}

/* 指针光斑 */
.holo-glare {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.3s ease;
}

/* 流动渐变描边 */
.holo-edge {
  position: absolute;
  inset: -1px;
  z-index: 1;
  border-radius: inherit;
  padding: 1px;
  background: var(--grad-foil);
  background-size: 220% 220%;
  -webkit-mask:
    linear-gradient(#fff 0 0) content-box,
    linear-gradient(#fff 0 0);
  mask:
    linear-gradient(#fff 0 0) content-box,
    linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  opacity: 0.7;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.product-link:hover .holo-edge {
  opacity: 1;
  animation: grad-shift 4s ease infinite;
  background-size: 220% 220%;
}

.holo-card.is-sold-out {
  filter: grayscale(0.8);
  opacity: 0.72;
}

/* ---------- 封面 ---------- */
.holo-cover {
  position: relative;
  height: 168px;
  overflow: hidden;
  background: #f3eee6;
}

.holo-cover-img,
.holo-cover-fallback {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s var(--ease-out);
}

.product-link:hover .holo-cover-img {
  transform: scale(1.06);
}

.holo-cover-fallback {
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 22% 18%, rgba(255, 93, 74, 0.38), transparent 55%),
    radial-gradient(circle at 78% 20%, rgba(18, 179, 154, 0.40), transparent 58%),
    radial-gradient(circle at 70% 82%, rgba(47, 143, 219, 0.38), transparent 55%),
    var(--bg-raised);
}

.holo-cover-fallback span {
  font-size: 44px;
  font-weight: 800;
  color: rgba(11, 125, 111, 0.28);
}

.holo-soldout {
  position: absolute;
  inset: auto 14px 14px auto;
  padding: 5px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid var(--line-strong);
  color: var(--mute);
  font-size: 11px;
  letter-spacing: 0.16em;
}

.holo-type {
  position: absolute;
  left: 12px;
  bottom: 12px;
  padding: 4px 11px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(18, 179, 154, 0.5);
  backdrop-filter: blur(8px);
  color: var(--gold-soft);
  font-size: 12px;
}

/* ---------- 内容 ---------- */
.holo-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px 18px 18px;
}

.holo-name {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.4;
  text-wrap: pretty;
}

.holo-stock {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0;
  color: var(--mute);
  font-size: 12.5px;
}

.holo-stock-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--green);
  box-shadow: 0 0 10px rgba(52, 211, 153, 0.7);
}

.holo-stock.is-low {
  color: var(--amber);
}

.holo-stock.is-low .holo-stock-dot {
  background: var(--amber);
  box-shadow: 0 0 10px rgba(251, 191, 36, 0.7);
}

.holo-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 6px;
  border-top: 1px solid var(--line);
}

.holo-price {
  margin: 0;
  font-size: 21px;
  font-weight: 600;
  color: var(--coral);
}

.holo-cta {
  padding: 7px 13px;
  border-radius: 999px;
  background: var(--grad-cta);
  color: var(--on-accent);
  font-size: 12.5px;
  font-weight: 600;
  transition: box-shadow 0.25s var(--ease-out), transform 0.25s var(--ease-out);
}

.product-link:hover .holo-cta {
  box-shadow: var(--glow-gold);
  transform: translateX(2px);
}

.holo-card.is-sold-out .holo-cta {
  background: var(--surface-strong);
  color: var(--mute);
}
</style>
