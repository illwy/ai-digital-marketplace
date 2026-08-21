<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createOrder, fetchProduct } from '../../api/shop'
import { readApiError } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import { formatFen } from '../../utils/money'
import type { ProductView } from '../../types/api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const product = ref<ProductView | null>(null)
const errorMessage = ref('')
const loading = ref(false)
const buying = ref(false)

const productId = computed(() => Number(route.params.id))

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchProduct(productId.value)
    product.value = data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
  } finally {
    loading.value = false
  }
}

async function buy(): Promise<void> {
  if (!auth.isAuthenticated) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  buying.value = true
  errorMessage.value = ''
  try {
    const { data } = await createOrder(productId.value)
    await router.push(`/orders/${data.data.id}`)
  } catch (error) {
    errorMessage.value = readApiError(error).message
  } finally {
    buying.value = false
  }
}

onMounted(load)
watch(productId, load)
</script>

<template>
  <el-card v-if="product" class="detail">
    <h2 class="detail-title">{{ product.name }}</h2>
    <p class="detail-price">{{ formatFen(product.priceFen) }}</p>
    <p>交付类型：{{ product.deliveryType }}</p>
    <p>可售件数：{{ product.availableCount }}</p>
    <p class="detail-desc">{{ product.description || '暂无描述' }}</p>
    <p v-if="errorMessage" class="detail-error">{{ errorMessage }}</p>
    <el-button type="primary" :loading="buying" :disabled="product.availableCount < 1" @click="buy">
      立即购买
    </el-button>
  </el-card>
  <el-skeleton v-else-if="loading" :rows="6" animated />
  <el-empty v-else :description="errorMessage || '商品不存在'" />
</template>

<style scoped>
.detail-title {
  margin-top: 0;
}

.detail-price {
  color: #f56c6c;
  font-size: 24px;
  font-weight: 700;
}

.detail-desc {
  color: #606266;
  white-space: pre-wrap;
}

.detail-error {
  color: var(--el-color-danger);
}
</style>
