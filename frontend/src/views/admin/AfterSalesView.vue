<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchAdminAfterSales, handleAfterSale } from '../../api/admin'
import { readApiError } from '../../api/http'
import { ticketStatusLabel, ticketStatusTagType } from '../../utils/labels'
import { formatDateTime } from '../../utils/time'
import type { AfterSaleView } from '../../types/api'

const route = useRoute()
const items = ref<AfterSaleView[]>([])
const status = ref(typeof route.query.status === 'string' ? route.query.status : '')
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchAdminAfterSales({
      status: status.value || undefined,
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

function isDismissed(error: unknown): boolean {
  return error === 'cancel' || error === 'close'
}

async function markProcessing(row: AfterSaleView): Promise<void> {
  try {
    await ElMessageBox.confirm(`将售后单 #${row.id} 标记为处理中？`, '确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await handleAfterSale(row.id, 'PROCESSING', row.adminReply ?? '')
    ElMessage.success('已标记为处理中')
    await load()
  } catch (error) {
    if (isDismissed(error)) {
      return
    }
    errorMessage.value = readApiError(error).message
  }
}

async function closeTicket(row: AfterSaleView): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt(`关闭售后单 #${row.id}`, '关闭售后', {
      confirmButtonText: '确定关闭',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValue: row.adminReply ?? '',
      inputPlaceholder: '处理说明',
      inputValidator: (val: string) => (val && val.trim() ? true : '请填写处理说明'),
    })
    await handleAfterSale(row.id, 'CLOSED', value.trim())
    ElMessage.success('已关闭售后')
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
      <h2 class="page-title">售后</h2>
    </div>
    <el-form inline class="page-filters" @submit.prevent="search">
      <el-form-item label="状态">
        <el-select v-model="status" clearable placeholder="全部" style="width: 170px">
          <el-option label="待处理+处理中" value="OPEN_ACTIVE" />
          <el-option label="待处理" value="OPEN" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
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
    <el-table v-loading="loading" :data="items">
      <el-table-column prop="id" label="单号" width="80" />
      <el-table-column label="商品" min-width="140">
        <template #default="{ row }">{{ row.productName || '-' }}</template>
      </el-table-column>
      <el-table-column label="订单" min-width="180">
        <template #default="{ row }">
          <RouterLink :to="{ path: '/admin/orders', query: { orderNo: row.orderNo } }">
            {{ row.orderNo || `#${row.orderId}` }}
          </RouterLink>
        </template>
      </el-table-column>
      <el-table-column prop="userId" label="用户" width="80" />
      <el-table-column prop="reason" label="原因" min-width="180" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="ticketStatusTagType(row.status)" size="small">
            {{ ticketStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="adminReply" label="回复" min-width="160" />
      <el-table-column label="创建时间" min-width="160">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'OPEN'" text type="primary" @click="markProcessing(row)">处理中</el-button>
          <el-button v-if="row.status !== 'CLOSED'" text type="danger" @click="closeTicket(row)">关闭</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无售后单" />
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
</style>
