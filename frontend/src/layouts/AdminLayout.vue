<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menus = [
  { path: '/admin', label: '概览' },
  { path: '/admin/users', label: '用户' },
  { path: '/admin/categories', label: '分类' },
  { path: '/admin/products', label: '商品' },
  { path: '/admin/inventory', label: '库存' },
  { path: '/admin/orders', label: '订单' },
  { path: '/admin/deliveries', label: '交付' },
  { path: '/admin/announcements', label: '公告' },
  { path: '/admin/after-sales', label: '售后' },
]

const active = computed(() => route.path)

async function onLogout(): Promise<void> {
  await auth.logout()
  await router.push('/login')
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside width="220px" class="admin-aside">
      <div class="admin-brand">钥市运营后台</div>
      <el-menu :key="active" :default-active="active" router>
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          {{ item.label }}
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <span>{{ auth.displayName }}</span>
        <div class="admin-header-actions">
          <el-button text @click="router.push('/')">返回商城</el-button>
          <el-button text @click="onLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="admin-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-shell {
  min-height: 100vh;
  background: #f5f7fa;
}

.admin-main {
  padding: 20px 24px 32px;
}

.admin-aside {
  background: #fff;
  border-right: 1px solid #ebeef5;
}

.admin-brand {
  padding: 20px 16px;
  font-weight: 700;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.admin-header-actions {
  display: flex;
  gap: 8px;
}
</style>
