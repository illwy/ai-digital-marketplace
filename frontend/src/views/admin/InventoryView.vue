<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchAdminProducts,
  fetchInventory,
  fetchInventoryStats,
  importInventory,
  invalidateInventory,
} from '../../api/admin'
import { readApiError } from '../../api/http'
import { inventoryStatusLabel, inventoryStatusTagType } from '../../utils/labels'
import type { InventoryStatsView, InventoryView, ProductView } from '../../types/api'

const route = useRoute()
const products = ref<ProductView[]>([])
const items = ref<InventoryView[]>([])
const productId = ref<number | undefined>(
  typeof route.query.productId === 'string' && route.query.productId
    ? Number(route.query.productId)
    : undefined,
)
const status = ref(typeof route.query.status === 'string' ? route.query.status : '')
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const importing = ref(false)
const dialogVisible = ref(false)
const importProductId = ref<number | undefined>(undefined)
const bulk = ref('')
const remark = ref('')
const errorMessage = ref('')
const stats = ref<InventoryStatsView | null>(null)

function productName(id: number): string {
  return products.value.find((item) => item.id === id)?.name ?? String(id)
}

async function loadProducts(): Promise<void> {
  const { data } = await fetchAdminProducts({ page: 1, pageSize: 100 })
  products.value = data.data
}

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchInventory({
      productId: productId.value,
      status: status.value || undefined,
      page: page.value,
    })
    items.value = data.data
    total.value = data.pagination.totalItems
    if (productId.value) {
      const statsRes = await fetchInventoryStats(productId.value)
      stats.value = statsRes.data.data
    } else {
      stats.value = null
    }
  } catch (error) {
    errorMessage.value = readApiError(error).message
  } finally {
    loading.value = false
  }
}

function search(): void {
  page.value = 1
  void load()
}

async function onPageChange(next: number): Promise<void> {
  page.value = next
  await load()
}

function openImport(): void {
  importProductId.value = productId.value ?? products.value[0]?.id
  bulk.value = ''
  remark.value = '演示补货'
  dialogVisible.value = true
}

function fillDemoKeys(): void {
  const stamp = Date.now().toString().slice(-6)
  bulk.value = [1, 2, 3].map((n) => `DEMO-REST-${stamp}${n}`).join('\n')
}

async function onImport(): Promise<void> {
  if (!importProductId.value) {
    ElMessage.warning('请选择商品')
    return
  }
  const contents = bulk.value
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
  if (!contents.length) {
    ElMessage.warning('请输入库存内容')
    return
  }
  importing.value = true
  try {
    await importInventory(importProductId.value, contents, remark.value.trim() || undefined)
    ElMessage.success(`已导入 ${contents.length} 条，前台可售件数会立刻增加`)
    dialogVisible.value = false
    productId.value = importProductId.value
    status.value = 'AVAILABLE'
    page.value = 1
    await load()
  } catch (error) {
    ElMessage.error(readApiError(error).message)
  } finally {
    importing.value = false
  }
}

function isDismissed(error: unknown): boolean {
  return error === 'cancel' || error === 'close'
}

async function invalidate(row: InventoryView): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认作废库存项 #${row.id}？`, '确认', {
      type: 'warning',
      confirmButtonText: '作废',
      cancelButtonText: '取消',
    })
    await invalidateInventory(row.id)
    ElMessage.success('已作废')
    await load()
  } catch (error) {
    if (isDismissed(error)) {
      return
    }
    errorMessage.value = readApiError(error).message
  }
}

onMounted(async () => {
  try {
    await loadProducts()
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
  await load()
})
</script>

<template>
  <el-card>
    <div class="page-header">
      <h2 class="page-title">虚拟库存</h2>
      <el-button type="primary" @click="openImport">导入</el-button>
    </div>
    <el-form inline class="page-filters" @submit.prevent="search">
      <el-form-item label="商品">
        <el-select v-model="productId" clearable filterable placeholder="全部商品" style="width: 220px">
          <el-option v-for="item in products" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="status" clearable placeholder="全部" style="width: 140px">
          <el-option label="可用" value="AVAILABLE" />
          <el-option label="锁定" value="LOCKED" />
          <el-option label="已售" value="SOLD" />
          <el-option label="作废" value="INVALID" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
      </el-form-item>
    </el-form>
    <p v-if="stats" class="page-stats">
      可用 {{ stats.available }} · 锁定 {{ stats.locked }} · 已售 {{ stats.sold }} · 作废 {{ stats.invalid }}
    </p>
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
      class="page-alert"
    />
    <el-table v-loading="loading" :data="items">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="商品" min-width="120">
        <template #default="{ row }">{{ productName(row.productId) }}</template>
      </el-table-column>
      <el-table-column label="掩码内容" min-width="200">
        <template #default="{ row }">
          <span class="masked">{{ row.maskedContent }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="inventoryStatusTagType(row.status)" size="small">
            {{ inventoryStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="orderId" label="订单" width="90" />
      <el-table-column prop="remark" label="备注" min-width="120" />
      <el-table-column prop="createdAt" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'AVAILABLE'" text type="danger" @click="invalidate(row)">作废</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无库存" />
      </template>
    </el-table>
    <div v-if="total > 0" class="page-pagination">
      <el-pagination
        :current-page="page"
        :page-size="20"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="onPageChange"
      />
    </div>
  </el-card>

  <el-dialog v-model="dialogVisible" title="导入库存" width="560px" destroy-on-close>
    <el-form label-width="80px">
      <el-form-item label="商品">
        <el-select v-model="importProductId" filterable placeholder="选择商品" style="width: 100%">
          <el-option v-for="item in products" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="内容">
        <el-input v-model="bulk" type="textarea" :rows="8" placeholder="每行一条卡密，导入后前台立刻可买" />
        <el-button class="demo-fill" text type="primary" @click="fillDemoKeys">填入 3 条演示卡密</el-button>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="remark" maxlength="255" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="importing" @click="onImport">导入</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.page-title {
  margin: 0;
  font-size: 18px;
}

.page-filters {
  margin-bottom: 4px;
}

.page-stats {
  margin: 0 0 12px;
  color: #606266;
}

.page-alert {
  margin-bottom: 12px;
}

.page-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.masked {
  white-space: pre-wrap;
  word-break: break-all;
}

.demo-fill {
  margin-top: 4px;
  padding: 0;
}
</style>
