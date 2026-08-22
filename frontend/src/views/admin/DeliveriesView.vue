<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { motion } from 'motion-v'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchAdminDeliveries, remarkAdminDelivery } from '../../api/admin'
import { readApiError } from '../../api/http'
import FoilBadge from '../../components/shop/FoilBadge.vue'
import { deliveryStatusLabel } from '../../utils/labels'
import { formatDateTime } from '../../utils/time'
import type { DeliveryView } from '../../types/api'

const route = useRoute()
const items = ref<DeliveryView[]>([])
const orderId = ref(typeof route.query.orderId === 'string' ? route.query.orderId : '')
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchAdminDeliveries({
      orderId: orderId.value ? Number(orderId.value) : undefined,
      page: page.value,
    })
    items.value = data.data
    total.value = data.pagination.totalItems
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

function rowClass({ row }: { row: DeliveryView }): string {
  return row.status === 'FAILED' ? 'is-failed' : ''
}

function isDismissed(error: unknown): boolean {
  return error === 'cancel' || error === 'close'
}

async function remark(row: DeliveryView): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt(`交付记录 #${row.id} 备注`, '填写备注', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValue: row.remark ?? '',
      inputPlaceholder: '处理说明',
    })
    await remarkAdminDelivery(row.id, value.trim())
    ElMessage.success('已保存备注')
    await load()
  } catch (error) {
    if (isDismissed(error)) {
      return
    }
    errorMessage.value = readApiError(error).message
  }
}

onMounted(load)
</script>

<template>
  <motion.div
    class="page"
    :initial="{ opacity: 0, y: 24 }"
    :animate="{ opacity: 1, y: 0 }"
    :transition="{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }"
  >
    <header class="page-head">
      <div>
        <h2 class="page-title">交付记录</h2>
        <p class="page-desc">出卡流水与卡密内容，失败单可通过备注跟进。</p>
      </div>
    </header>

    <div class="filter-bar glass-panel">
      <el-form inline class="page-filters" @submit.prevent="search">
        <el-form-item label="订单 ID">
          <el-input v-model="orderId" clearable placeholder="全部" style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
      class="page-alert"
    />

    <div class="table-card glass-panel">
      <el-table v-loading="loading" :data="items" :row-class-name="rowClass">
        <el-table-column label="ID" width="80">
          <template #default="{ row }">
            <span class="font-mono">{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="140">
          <template #default="{ row }">{{ row.productName || '-' }}</template>
        </el-table-column>
        <el-table-column label="订单" min-width="180">
          <template #default="{ row }">
            <span class="font-mono">{{ row.orderNo || row.orderId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="90">
          <template #default="{ row }">
            <span class="font-mono">{{ row.inventoryId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <FoilBadge
              :label="deliveryStatusLabel(row.status)"
              :tone="row.status === 'WAITING' ? 'warn' : row.status === 'DELIVERED' ? 'ok' : 'danger'"
            />
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="200">
          <template #default="{ row }">
            <span class="masked font-mono">{{ row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" />
        <el-table-column label="交付时间" min-width="160">
          <template #default="{ row }">
            <span class="font-mono">{{ formatDateTime(row.deliveredAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="remark(row)">备注</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无交付记录" />
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
    </div>
  </motion.div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 24px;
  letter-spacing: 0.02em;
}

.page-desc {
  margin: 4px 0 0;
  color: var(--mute);
  font-size: 13.5px;
}

.filter-bar {
  padding: 6px 16px 0;
}

.filter-bar :deep(.el-form-item) {
  margin-bottom: 6px;
}

.page-alert {
  border-radius: 12px;
}

.table-card {
  overflow: hidden;
}

.table-card :deep(.el-table::before) {
  display: none;
}

.page-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-bottom: 16px;
}

.masked {
  white-space: pre-wrap;
  word-break: break-all;
}

:deep(.is-failed) {
  --el-table-tr-bg-color: rgba(251, 113, 133, 0.08);
}
</style>
