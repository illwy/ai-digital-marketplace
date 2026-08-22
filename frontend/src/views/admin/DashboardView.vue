<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAdminOverview } from '../../api/admin'
import { readApiError } from '../../api/http'
import type { AdminOverviewView } from '../../types/api'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const overview = ref<AdminOverviewView | null>(null)

const cards: { key: keyof AdminOverviewView; title: string; path: string; query?: Record<string, string> }[] = [
  { key: 'userCount', title: '用户', path: '/admin/users' },
  { key: 'productOnSaleCount', title: '在售商品', path: '/admin/products', query: { status: 'ON_SALE' } },
  { key: 'availableInventoryCount', title: '可用库存', path: '/admin/inventory', query: { status: 'AVAILABLE' } },
  { key: 'pendingOrderCount', title: '待支付订单', path: '/admin/orders', query: { payStatus: 'PENDING' } },
  { key: 'paidOrderCount', title: '已支付订单', path: '/admin/orders', query: { payStatus: 'PAID' } },
  { key: 'openAfterSaleCount', title: '待处理售后', path: '/admin/after-sales', query: { status: 'OPEN_ACTIVE' } },
  { key: 'enabledAnnouncementCount', title: '启用公告', path: '/admin/announcements' },
]

async function load(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const { data } = await fetchAdminOverview()
    overview.value = data.data
  } catch (error) {
    errorMessage.value = readApiError(error).message
  } finally {
    loading.value = false
  }
}

function go(path: string, query?: Record<string, string>): void {
  void router.push({ path, query })
}

onMounted(load)
</script>

<template>
  <el-card v-loading="loading">
    <h2 class="dash-title">运营概览</h2>
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
      class="dash-alert"
    />
    <el-empty v-else-if="!overview && !loading" description="暂无概览数据" />
    <el-row v-else-if="overview" :gutter="16">
      <el-col v-for="card in cards" :key="card.key" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card class="stat-card" shadow="hover" @click="go(card.path, card.query)">
          <el-statistic :title="card.title" :value="overview[card.key]" />
        </el-card>
      </el-col>
    </el-row>
    <div v-if="overview" class="demo-path">
      <h3 class="demo-title">演示路径</h3>
      <p class="demo-copy">买家下单拿卡密之后，后台按这三条走：补货、查单、处理售后。</p>
      <div class="demo-actions">
        <el-button type="primary" @click="go('/admin/inventory')">去补货</el-button>
        <el-button @click="go('/admin/orders', { payStatus: 'PAID' })">已支付订单</el-button>
        <el-button @click="go('/admin/deliveries')">交付记录</el-button>
        <el-button @click="go('/admin/after-sales', { status: 'OPEN_ACTIVE' })">待处理售后</el-button>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.dash-title {
  margin: 0 0 16px;
  font-size: 18px;
}

.dash-alert {
  margin-bottom: 12px;
}

.stat-card {
  margin-bottom: 16px;
  cursor: pointer;
}

.stat-card:hover {
  border-color: var(--el-color-primary-light-5);
}

.demo-path {
  margin-top: 8px;
  padding-top: 8px;
}

.demo-title {
  margin: 0 0 8px;
  font-size: 16px;
}

.demo-copy {
  margin: 0 0 12px;
  color: #606266;
}

.demo-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
