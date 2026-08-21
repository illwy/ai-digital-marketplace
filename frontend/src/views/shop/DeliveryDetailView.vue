<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchDelivery } from '../../api/shop'
import { readApiError } from '../../api/http'
import type { DeliveryView } from '../../types/api'

const route = useRoute()
const item = ref<DeliveryView | null>(null)
const errorMessage = ref('')
const id = computed(() => Number(route.params.id))

async function load(): Promise<void> {
  try {
    const { data } = await fetchDelivery(id.value)
    item.value = data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
}

onMounted(load)
watch(id, load)
</script>

<template>
  <el-card v-if="item">
    <h2 class="page-title">交付内容</h2>
    <p>状态：{{ item.status }}</p>
    <pre class="content">{{ item.content }}</pre>
  </el-card>
  <el-empty v-else :description="errorMessage || '加载中'" />
</template>

<style scoped>
.page-title {
  margin-top: 0;
}

.content {
  background: #f5f7fa;
  padding: 16px;
  white-space: pre-wrap;
}
</style>
