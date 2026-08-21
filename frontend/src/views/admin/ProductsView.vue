<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { fetchAdminCategories, fetchAdminProducts, saveProduct } from '../../api/admin'
import { readApiError } from '../../api/http'
import { formatFen } from '../../utils/money'
import type { CategoryView, ProductView } from '../../types/api'

const products = ref<ProductView[]>([])
const categories = ref<CategoryView[]>([])
const errorMessage = ref('')
const form = reactive({
  id: 0,
  categoryId: 0,
  name: '',
  description: '',
  priceFen: 100,
  deliveryType: 'LICENSE',
  status: 'ON_SALE',
})

async function load(): Promise<void> {
  const [prod, cats] = await Promise.all([fetchAdminProducts({ page: 1 }), fetchAdminCategories()])
  products.value = prod.data.data
  categories.value = cats.data.data
  if (!form.categoryId && categories.value[0]) {
    form.categoryId = categories.value[0].id
  }
}

async function submit(): Promise<void> {
  try {
    await saveProduct(
      {
        categoryId: form.categoryId,
        name: form.name,
        description: form.description,
        priceFen: form.priceFen,
        deliveryType: form.deliveryType,
        status: form.status,
      },
      form.id || undefined,
    )
    form.id = 0
    form.name = ''
    await load()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
}

function edit(item: ProductView): void {
  form.id = item.id
  form.categoryId = item.categoryId
  form.name = item.name
  form.description = item.description ?? ''
  form.priceFen = item.priceFen
  form.deliveryType = item.deliveryType
  form.status = item.status
}

onMounted(async () => {
  try {
    await load()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
})
</script>

<template>
  <el-card>
    <h2 class="page-title">商品</h2>
    <el-form label-width="80px">
      <el-form-item label="分类">
        <el-select v-model="form.categoryId">
          <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="名称">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="价格(分)">
        <el-input-number v-model="form.priceFen" :min="0" />
      </el-form-item>
      <el-form-item label="交付">
        <el-select v-model="form.deliveryType">
          <el-option label="LICENSE" value="LICENSE" />
          <el-option label="ACCOUNT" value="ACCOUNT" />
          <el-option label="TOKEN" value="TOKEN" />
          <el-option label="TEXT" value="TEXT" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="在售" value="ON_SALE" />
          <el-option label="下架" value="OFF_SALE" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" />
      </el-form-item>
      <el-button type="primary" @click="submit">保存</el-button>
    </el-form>
    <p v-if="errorMessage" class="page-error">{{ errorMessage }}</p>
    <el-table :data="products">
      <el-table-column prop="name" label="名称" />
      <el-table-column label="价格">
        <template #default="{ row }">{{ formatFen(row.priceFen) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="availableCount" label="可售" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button text @click="edit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>
.page-title {
  margin-top: 0;
}

.page-error {
  color: var(--el-color-danger);
}
</style>
