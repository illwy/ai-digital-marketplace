<script setup lang="ts">
import type { ProductView } from '../../types/api'
import ProductCard from './ProductCard.vue'

defineProps<{
  products: ProductView[]
}>()
</script>

<template>
  <el-row v-if="products.length" :gutter="16">
    <el-col v-for="product in products" :key="product.id" :xs="24" :sm="12" :md="8">
      <ProductCard :product="product" />
    </el-col>
  </el-row>
  <el-empty v-else description="暂无在售商品" />
</template>
