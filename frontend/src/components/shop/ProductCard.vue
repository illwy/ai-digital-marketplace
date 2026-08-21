<script setup lang="ts">
import type { ProductView } from '../../types/api'
import { formatFen } from '../../utils/money'

defineProps<{
  product: ProductView
}>()
</script>

<template>
  <el-card class="product-card" shadow="hover">
    <h3 class="product-name">{{ product.name }}</h3>
    <p class="product-price">{{ formatFen(product.priceFen) }}</p>
    <p class="product-stock">可售 {{ product.availableCount }}</p>
    <RouterLink :to="`/products/${product.id}`">查看详情</RouterLink>
  </el-card>
</template>

<style scoped>
.product-card {
  height: 100%;
}

.product-name {
  margin: 0 0 8px;
  font-size: 16px;
}

.product-price {
  margin: 0;
  color: #f56c6c;
  font-weight: 700;
}

.product-stock {
  color: #909399;
  font-size: 13px;
}
</style>
