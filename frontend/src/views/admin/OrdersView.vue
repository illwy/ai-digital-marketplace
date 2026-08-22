<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cancelAdminOrder,
  fetchAdminDeliveries,
  fetchAdminOrder,
  fetchAdminOrders,
  fetchInventory,
} from '../../api/admin'
import { readApiError } from '../../api/http'
import {
  deliveryStatusLabel,
  deliveryStatusTagType,
  inventoryStatusLabel,
  inventoryStatusTagType,
  orderAftersaleLabel,
  payStatusLabel,
  payStatusTagType,
} from '../../utils/labels'
import { formatFen } from '../../utils/money'
import { formatDateTime } from '../../utils/time'
import type { DeliveryView, InventoryView, OrderView } from '../../types/api'

const route = useRoute()
const items = ref<OrderView[]>([])
const payStatus = ref(typeof route.query.payStatus === 'string' ? route.query.payStatus : '')
const deliveryStatus = ref(typeof route.query.deliveryStatus === 'string' ? route.query.deliveryStatus : '')
const orderNo = ref(typeof route.query.orderNo === 'string' ? route.query.orderNo : '')
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')
const detailVisible = ref(false)
const detail = ref<OrderView | null>(null)
const relatedInventory = ref<InventoryView | null>(null)
const relatedDelivery = ref<DeliveryView | null>(null)

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchAdminOrders({
      payStatus: payStatus.value || undefined,
      deliveryStatus: deliveryStatus.value || undefined,
      orderNo: orderNo.value.trim() || undefined,
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

async function openDetail(id: number): Promise<void> {
  try {
    const { data } = await fetchAdminOrder(id)
    detail.value = data.data
    relatedInventory.value = null
    relatedDelivery.value = null
    const [invRes, delRes] = await Promise.all([
      fetchInventory({ orderId: id, page: 1 }),
      fetchAdminDeliveries({ orderId: id, page: 1 }),
    ])
    relatedInventory.value = invRes.data.data[0] ?? null
    relatedDelivery.value = delRes.data.data[0] ?? null
    detailVisible.value = true
  } catch (error) {
    errorMessage.value = readApiError(error).message
  }
}

async function cancel(row: OrderView): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认关闭未支付订单 ${row.orderNo}？`, '确认', {
      type: 'warning',
      confirmButtonText: '关闭',
      cancelButtonText: '取消',
    })
    await cancelAdminOrder(row.id)
    ElMessage.success('已关闭订单')
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
      <h2 class="page-title">订单</h2>
    </div>
    <el-form inline class="page-filters" @submit.prevent="search">
      <el-form-item label="支付状态">
        <el-select v-model="payStatus" clearable placeholder="全部" style="width: 150px">
          <el-option label="待支付" value="PENDING" />
          <el-option label="已支付" value="PAID" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="已过期" value="EXPIRED" />
        </el-select>
      </el-form-item>
      <el-form-item label="交付状态">
        <el-select v-model="deliveryStatus" clearable placeholder="全部" style="width: 150px">
          <el-option label="待交付" value="WAITING" />
          <el-option label="已交付" value="DELIVERED" />
          <el-option label="失败" value="FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item label="订单号">
        <el-input v-model="orderNo" clearable placeholder="ORD…" style="width: 200px" @keyup.enter="search" />
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
      <el-table-column prop="orderNo" label="订单号" min-width="160" />
      <el-table-column prop="userId" label="用户" width="80" />
      <el-table-column prop="productName" label="商品" min-width="140" />
      <el-table-column label="金额" width="110">
        <template #default="{ row }">{{ formatFen(row.amountFen) }}</template>
      </el-table-column>
      <el-table-column label="支付" width="110">
        <template #default="{ row }">
          <el-tag :type="payStatusTagType(row.payStatus)" size="small">
            {{ payStatusLabel(row.payStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="交付" width="110">
        <template #default="{ row }">
          <el-tag :type="deliveryStatusTagType(row.deliveryStatus)" size="small">
            {{ deliveryStatusLabel(row.deliveryStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="售后" width="110">
        <template #default="{ row }">{{ orderAftersaleLabel(row.aftersaleStatus) }}</template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="160">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="openDetail(row.id)">详情</el-button>
          <el-button v-if="row.payStatus === 'PENDING'" text type="danger" @click="cancel(row)">关闭</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无订单" />
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
  <el-dialog v-model="detailVisible" title="订单详情" width="560px">
    <template v-if="detail">
      <p>订单号：{{ detail.orderNo }}</p>
      <p>用户：{{ detail.userId }}</p>
      <p>商品：{{ detail.productName }}</p>
      <p>金额：{{ formatFen(detail.amountFen) }}</p>
      <p>支付：{{ payStatusLabel(detail.payStatus) }}</p>
      <p>交付：{{ deliveryStatusLabel(detail.deliveryStatus) }}</p>
      <p>售后：{{ orderAftersaleLabel(detail.aftersaleStatus) }}</p>
      <p>支付时间：{{ detail.paidAt ? formatDateTime(detail.paidAt) : '未支付' }}</p>
      <p v-if="relatedInventory">
        库存：#{{ relatedInventory.id }}
        <el-tag :type="inventoryStatusTagType(relatedInventory.status)" size="small">
          {{ inventoryStatusLabel(relatedInventory.status) }}
        </el-tag>
        <RouterLink class="detail-link" :to="{ path: '/admin/inventory', query: { productId: String(relatedInventory.productId) } }">
          去库存
        </RouterLink>
      </p>
      <p v-else>库存：{{ detail.inventoryId ?? '无' }}</p>
      <p v-if="relatedDelivery">
        卡密：{{ relatedDelivery.content }}
        <RouterLink class="detail-link" :to="{ path: '/admin/deliveries', query: { orderId: String(detail.id) } }">
          去交付
        </RouterLink>
      </p>
      <p v-else>卡密：尚未交付</p>
    </template>
  </el-dialog>
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

.detail-link {
  margin-left: 8px;
}
</style>
