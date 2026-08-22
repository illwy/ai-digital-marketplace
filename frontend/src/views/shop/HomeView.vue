<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { motion } from 'motion-v'
import AnnouncementList from '../../components/shop/AnnouncementList.vue'
import ProductList from '../../components/shop/ProductList.vue'
import { fetchAnnouncements, fetchCategories, fetchProducts } from '../../api/shop'
import { readApiError } from '../../api/http'
import type { AnnouncementView, CategoryView, ProductView } from '../../types/api'

const announcements = ref<AnnouncementView[]>([])
const categories = ref<CategoryView[]>([])
const products = ref<ProductView[]>([])
const categoryId = ref<number>(0)
const page = ref(1)
const pageSize = 12
const totalItems = ref(0)
const loading = ref(false)
const errorMessage = ref('')

async function loadProducts(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchProducts({
      categoryId: categoryId.value || undefined,
      page: page.value,
      pageSize,
    })
    products.value = data.data
    totalItems.value = data.pagination.totalItems
  } catch (error) {
    errorMessage.value = readApiError(error).message
  } finally {
    loading.value = false
  }
}

async function onCategoryChange(): Promise<void> {
  page.value = 1
  await loadProducts()
}

async function selectCategory(id: number): Promise<void> {
  categoryId.value = id
  await onCategoryChange()
}

async function onPageChange(next: number): Promise<void> {
  page.value = next
  await loadProducts()
}

onMounted(async () => {
  try {
    const [ann, cats] = await Promise.all([fetchAnnouncements(), fetchCategories()])
    announcements.value = ann.data.data
    categories.value = cats.data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
  await loadProducts()
})
</script>

<template>
  <section class="home">
    <div class="hero">
      <div class="hero-copy-block">
        <motion.p
          class="hero-kicker"
          :initial="{ opacity: 0, y: 16 }"
          :animate="{ opacity: 1, y: 0 }"
          :transition="{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }"
        >
          AI 数字商品 · 自动发卡
        </motion.p>
        <motion.h1
          class="hero-title"
          :initial="{ opacity: 0, y: 24 }"
          :animate="{ opacity: 1, y: 0 }"
          :transition="{ duration: 0.6, delay: 0.08, ease: [0.22, 1, 0.36, 1] }"
        >
          柜台上的<span class="grad-text">一张票</span>，<br />付款后立刻到你手里。
        </motion.h1>
        <motion.p
          class="hero-copy"
          :initial="{ opacity: 0, y: 20 }"
          :animate="{ opacity: 1, y: 0 }"
          :transition="{ duration: 0.55, delay: 0.18, ease: [0.22, 1, 0.36, 1] }"
        >
          账号、卡密、额度都是虚拟库存。一单一件，确认付款后当场出卡，可复制，可再查。
        </motion.p>
        <motion.div
          class="hero-actions"
          :initial="{ opacity: 0, y: 20 }"
          :animate="{ opacity: 1, y: 0 }"
          :transition="{ duration: 0.55, delay: 0.26, ease: [0.22, 1, 0.36, 1] }"
        >
          <a class="hero-cta" href="#shelf">开始选卡</a>
          <RouterLink class="hero-cta hero-cta--ghost" to="/register">注册购买</RouterLink>
        </motion.div>
      </div>

      <!-- 悬浮全息凭证 -->
      <motion.div
        class="hero-card-stage"
        :initial="{ opacity: 0, y: 32, rotate: 4 }"
        :animate="{ opacity: 1, y: 0, rotate: 0 }"
        :transition="{ duration: 0.8, delay: 0.2, ease: [0.22, 1, 0.36, 1] }"
      >
        <div class="hero-holo-card">
          <div class="hero-holo-shine"></div>
          <header>
            <span class="hero-holo-chip"></span>
            <span class="hero-holo-label">DELIVERED</span>
          </header>
          <p class="hero-holo-name">ChatGPT Plus 月卡</p>
          <p class="hero-holo-secret">gpt-plus-demo-••••</p>
          <footer>
            <span>订单已交付</span>
            <span class="hero-holo-ok">✓ 可复制</span>
          </footer>
        </div>
      </motion.div>
    </div>

    <ol class="hero-steps">
      <li v-for="(step, index) in ['选卡', '下单', '出密']" :key="step" class="hero-step">
        <span class="hero-step-num font-display">{{ String(index + 1).padStart(2, '0') }}</span>
        <strong>{{ step }}</strong>
        <small>{{ ['看库存和类型', '锁住这一张', '复制并自己保存'][index] }}</small>
      </li>
    </ol>

    <AnnouncementList :items="announcements" />

    <div id="shelf" class="home-toolbar" role="radiogroup" aria-label="卡种">
      <button
        type="button"
        class="chip"
        role="radio"
        :aria-checked="categoryId === 0"
        :class="{ 'is-on': categoryId === 0 }"
        @click="selectCategory(0)"
      >
        全部卡种
      </button>
      <button
        v-for="item in categories"
        :key="item.id"
        type="button"
        class="chip"
        role="radio"
        :aria-checked="categoryId === item.id"
        :class="{ 'is-on': categoryId === item.id }"
        @click="selectCategory(item.id)"
      >
        {{ item.name }}
      </button>
    </div>
    <p v-if="errorMessage" class="home-error">{{ errorMessage }}</p>
    <el-skeleton v-if="loading" :rows="4" animated />
    <template v-else>
      <ProductList v-if="!errorMessage || products.length" :products="products" />
      <el-pagination
        v-if="totalItems > 0"
        class="home-pagination"
        background
        layout="prev, pager, next, total"
        :page-size="pageSize"
        :current-page="page"
        :total="totalItems"
        @current-change="onPageChange"
      />
    </template>
  </section>
</template>

<style scoped>
.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.65fr);
  gap: 40px 56px;
  align-items: center;
  margin-bottom: 30px;
  padding: 28px 0 8px;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 18px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid rgba(139, 92, 246, 0.4);
  background: rgba(139, 92, 246, 0.1);
  color: var(--violet-soft);
  font-size: 12px;
  letter-spacing: 0.22em;
}

.hero-kicker::before {
  content: "";
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--violet);
  box-shadow: var(--glow-violet);
}

.hero-title {
  margin: 0 0 18px;
  font-size: clamp(30px, 4.6vw, 50px);
  font-weight: 800;
  line-height: 1.22;
  letter-spacing: -0.01em;
  text-wrap: balance;
}

.hero-copy {
  max-width: 34rem;
  margin: 0 0 26px;
  color: var(--ink-soft);
  line-height: 1.8;
  font-size: 15px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero-cta {
  display: inline-flex;
  align-items: center;
  padding: 12px 28px;
  border-radius: 999px;
  background: var(--grad-primary);
  background-size: 180% 180%;
  animation: grad-shift 6s ease infinite;
  color: #fff;
  text-decoration: none;
  font-weight: 600;
  font-size: 15px;
  box-shadow: var(--glow-violet);
  transition: transform 0.25s var(--ease-out), box-shadow 0.25s var(--ease-out);
}

.hero-cta:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 44px rgba(139, 92, 246, 0.55);
}

.hero-cta--ghost {
  background: transparent;
  border: 1px solid var(--line-strong);
  box-shadow: none;
  color: var(--ink-soft);
  animation: none;
}

.hero-cta--ghost:hover {
  border-color: rgba(34, 211, 238, 0.55);
  color: var(--cyan-soft);
  box-shadow: none;
}

/* ---------- 悬浮全息凭证 ---------- */
.hero-card-stage {
  perspective: 900px;
}

.hero-holo-card {
  position: relative;
  overflow: hidden;
  padding: 26px 26px 22px;
  border-radius: 22px;
  background:
    linear-gradient(160deg, rgba(139, 92, 246, 0.22), rgba(34, 211, 238, 0.12) 60%, rgba(244, 114, 182, 0.14)),
    var(--bg-raised);
  border: 1px solid rgba(148, 163, 216, 0.25);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.12);
  animation: float-soft 7s ease-in-out infinite;
  transform: rotate(-3deg);
}

.hero-holo-shine {
  position: absolute;
  top: -30%;
  bottom: -30%;
  width: 45%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.14), transparent);
  animation: shimmer-sweep 3.6s var(--ease-move) infinite;
  pointer-events: none;
}

.hero-holo-card header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22px;
}

.hero-holo-chip {
  width: 38px;
  height: 28px;
  border-radius: 7px;
  background: var(--grad-primary);
  opacity: 0.9;
}

.hero-holo-label {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.18em;
  color: var(--green);
}

.hero-holo-name {
  margin: 0 0 10px;
  font-size: 17px;
  font-weight: 700;
  color: var(--ink);
}

.hero-holo-secret {
  margin: 0 0 24px;
  padding: 10px 14px;
  border-radius: 10px;
  background: rgba(5, 6, 13, 0.6);
  border: 1px dashed var(--line-strong);
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--cyan-soft);
}

.hero-holo-card footer {
  display: flex;
  justify-content: space-between;
  color: var(--mute);
  font-size: 13px;
}

.hero-holo-ok {
  color: var(--green);
  font-weight: 600;
}

/* ---------- 三步流程（真实顺序，编号承载信息） ---------- */
.hero-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin: 0 0 34px;
  padding: 0;
  list-style: none;
}

.hero-step {
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto;
  column-gap: 14px;
  align-items: center;
  padding: 16px 18px;
  border-radius: 16px;
  background: var(--surface);
  border: 1px solid var(--line);
  backdrop-filter: blur(12px);
  transition: transform 0.25s var(--ease-out), border-color 0.25s ease, box-shadow 0.25s ease;
}

.hero-step:hover {
  transform: translateY(-3px);
  border-color: rgba(139, 92, 246, 0.45);
  box-shadow: var(--glow-violet);
}

.hero-step-num {
  grid-row: span 2;
  font-size: 22px;
  font-weight: 800;
  background: var(--grad-text);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-step strong {
  font-size: 15px;
  color: var(--ink);
}

.hero-step small {
  color: var(--mute);
  font-size: 12.5px;
}

/* ---------- 分类筛选 ---------- */
.home-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0 0 24px;
  scroll-margin-top: 90px;
}

.chip {
  padding: 7px 16px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  color: var(--mute);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s var(--ease-out);
}

.chip:hover {
  color: var(--ink);
  border-color: var(--line-strong);
  transform: translateY(-1px);
}

.chip.is-on {
  border-color: transparent;
  background: var(--grad-primary);
  color: #fff;
  box-shadow: var(--glow-violet);
}

.chip:focus-visible {
  outline: 2px solid var(--violet);
  outline-offset: 2px;
}

.home-error {
  color: var(--red);
}

.home-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 28px;
}

@media (max-width: 800px) {
  .hero {
    grid-template-columns: 1fr;
    gap: 28px;
    padding-top: 8px;
  }

  .hero-card-stage {
    max-width: 360px;
  }

  .hero-steps {
    grid-template-columns: 1fr;
  }
}
</style>
