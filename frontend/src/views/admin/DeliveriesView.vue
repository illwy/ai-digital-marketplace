<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchAdminDeliveries, remarkAdminDelivery } from '../../api/admin'
import { readApiError } from '../../api/http'
import { deliveryStatusLabel, deliveryStatusTagType } from '../../utils/labels'
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
  <el-card>
    <div class="page-header">
      <h2 class="page-title">交付记录</h2>
    </div>
    <el-form inline class="page-filters" @submit.prevent="search">
      <el-form-item label="订单 ID">
        <el-input v-model="orderId" clearable placeholder="全部" style="width: 140px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit">查询</el-button>
      </el-form-item>
    </el-form>
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
      class="page-alert"
    />
    <el-table v-loading="loading" :data="items" :row-class-name="rowClass">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="商品" min-width="140">
        <template #default="{ row }">{{ row.productName || '-' }}</template>
      </el-table-column>
      <el-table-column label="订单" min-width="180">
        <template #default="{ row }">{{ row.orderNo || row.orderId }}</template>
      </el-table-column>
      <el-table-column prop="inventoryId" label="库存" width="90" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="deliveryStatusTagType(row.status)" size="small">
            {{ deliveryStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="内容" min-width="200">
        <template #default="{ row }">
          <span class="masked">{{ row.content }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" />
      <el-table-column label="交付时间" min-width="160">
        <template #default="{ row }">{{ formatDateTime(row.deliveredAt) }}</template>
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
  </el-card>
</template>

<style scoped>
.page-header {
  margin-bottom: 12px;
}

.page-title {
  margin: 0;
  font-size: 18px;
}

.page-filters {
  margin-bottom: 4px;
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

:deep(.is-failed) {
  --el-table-tr-bg-color: var(--el-color-danger-light-9);
}
</style>
