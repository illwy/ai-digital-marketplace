<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchAdminProducts, fetchInventory, importInventory, invalidateInventory } from '../../api/admin'
import { readApiError } from '../../api/http'
import type { InventoryView, ProductView } from '../../types/api'

const products = ref<ProductView[]>([])
const items = ref<InventoryView[]>([])
const productId = ref<number | undefined>(undefined)
const bulk = ref('')
const errorMessage = ref('')

async function load(): Promise<void> {
  const { data } = await fetchInventory({ productId: productId.value, page: 1 })
  items.value = data.data
}

async function onImport(): Promise<void> {
  if (!productId.value) {
    errorMessage.value = '请选择商品'
    return
  }
  const contents = bulk.value.split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  try {
    await importInventory(productId.value, contents)
    bulk.value = ''
    await load()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
}

async function invalidate(id: number): Promise<void> {
  try {
    await invalidateInventory(id)
    await load()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
}

onMounted(async () => {
  try {
    const { data } = await fetchAdminProducts({ page: 1 })
    products.value = data.data
    productId.value = products.value[0]?.id
    await load()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
})
</script>

<template>
  <el-card>
    <h2 class="page-title">虚拟库存</h2>
    <el-select v-model="productId" placeholder="商品" @change="load">
      <el-option v-for="item in products" :key="item.id" :label="item.name" :value="item.id" />
    </el-select>
    <el-input v-model="bulk" type="textarea" class="bulk" placeholder="每行一条库存内容" />
    <el-button type="primary" @click="onImport">导入</el-button>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <el-table :data="items">
      <el-table-column prop="id" label="ID" />
      <el-table-column prop="maskedContent" label="内容" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button v-if="row.status === 'AVAILABLE'" text @click="invalidate(row.id)">作废</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>
.page-title {
  margin-top: 0;
}

.bulk {
  margin: 12px 0;
}

.page-error {
  color: var(--el-color-danger);
}
</style>
