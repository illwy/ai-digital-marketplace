<script setup lang="ts">
import { onMounted, ref } from 'vue'
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
        <h1 class="hero-title">柜台上的一张票，<br />付款后立刻到你手里。</h1>
        <p class="hero-copy">账号、卡密、额度都是虚拟库存。一单一件，确认付款后当场出卡，可复制，可再查。</p>
      </div>
      <ol class="hero-steps">
        <li><span>选卡</span>看库存和类型</li>
        <li><span>下单</span>锁住这一张</li>
        <li><span>出密</span>复制并自己保存</li>
      </ol>
    </div>
    <AnnouncementList :items="announcements" />
    <div class="home-toolbar" role="radiogroup" aria-label="卡种">
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
  grid-template-columns: minmax(0, 1.4fr) minmax(220px, 0.7fr);
  gap: 32px 48px;
  align-items: end;
  margin-bottom: 36px;
  padding: 12px 0 8px;
}

.hero-title {
  margin: 0 0 16px;
  font-family: "Noto Serif SC", serif;
  font-size: clamp(32px, 5vw, 52px);
  font-weight: 700;
  line-height: 1.18;
  text-wrap: balance;
  letter-spacing: -0.02em;
}

.hero-copy {
  max-width: 36rem;
  margin: 0;
  color: var(--mute);
  line-height: 1.75;
  font-size: 15px;
}

.hero-steps {
  position: relative;
  margin: 0;
  padding: 18px 22px 18px 28px;
  list-style: none;
  background: var(--ticket);
  color: var(--ticket-ink);
}

.hero-steps::before,
.hero-steps::after {
  content: "";
  position: absolute;
  left: -9px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--paper);
}

.hero-steps::before {
  top: 18%;
}

.hero-steps::after {
  bottom: 18%;
}

.hero-steps li {
  display: grid;
  grid-template-columns: 3.5em 1fr;
  gap: 10px;
  padding: 10px 0;
  color: var(--ticket-mute);
  font-size: 14px;
}

.hero-steps li + li {
  border-top: 1.5px dashed #cbb8a6;
}

.hero-steps span {
  color: #9a3b16;
  font-family: "Noto Serif SC", serif;
  font-weight: 700;
}

.home-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 8px 0 22px;
}

.chip {
  padding: 6px 12px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  color: var(--mute);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.chip.is-on {
  border-color: var(--copper);
  background: color-mix(in srgb, var(--copper) 16%, transparent);
  color: var(--copper-deep);
}

.chip:focus-visible {
  outline: 2px solid var(--copper);
  outline-offset: 2px;
}

.home-error {
  color: #e48a78;
}

.home-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 28px;
}

@media (max-width: 800px) {
  .hero {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}
</style>
