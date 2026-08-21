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
const loading = ref(false)
const errorMessage = ref('')

async function loadProducts(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchProducts({
      categoryId: categoryId.value || undefined,
      page: 1,
      pageSize: 12,
    })
    products.value = data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
  } finally {
    loading.value = false
  }
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
    <AnnouncementList :items="announcements" />
    <div class="home-toolbar">
      <el-radio-group v-model="categoryId" @change="loadProducts">
        <el-radio-button :value="0">全部</el-radio-button>
        <el-radio-button v-for="item in categories" :key="item.id" :value="item.id">
          {{ item.name }}
        </el-radio-button>
      </el-radio-group>
    </div>
    <p v-if="errorMessage" class="home-error">{{ errorMessage }}</p>
    <el-skeleton v-if="loading" :rows="4" animated />
    <ProductList v-else :products="products" />
  </section>
</template>

<style scoped>
.home-toolbar {
  margin: 16px 0;
}

.home-error {
  color: var(--el-color-danger);
}
</style>
