<script setup lang="ts">
import type { ProductView } from '../../types/api'
import ProductCard from './ProductCard.vue'

defineProps<{
  products: ProductView[]
}>()
</script>

<template>
  <div v-if="products.length" class="product-grid">
    <ProductCard v-for="product in products" :key="product.id" :product="product" />
  </div>
  <el-empty v-else description="这个卡种暂时没货" />
</template>

<style scoped>
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 22px;
  padding: 6px 6px 2px 0;
}
</style>
