<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

async function onLogout(): Promise<void> {
  await auth.logout()
  await router.push('/')
}
</script>

<template>
  <el-container class="shop-shell">
    <el-header class="shop-header">
      <RouterLink class="shop-brand" to="/">数字商品商城</RouterLink>
      <nav class="shop-nav">
        <RouterLink to="/">商品</RouterLink>
        <template v-if="auth.isAuthenticated">
          <RouterLink to="/orders">我的订单</RouterLink>
          <RouterLink to="/deliveries">已购资源</RouterLink>
          <RouterLink to="/after-sales">售后</RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin">后台</RouterLink>
          <span class="shop-user">{{ auth.displayName }}</span>
          <el-button text @click="onLogout">退出</el-button>
        </template>
        <template v-else>
          <RouterLink to="/login">登录</RouterLink>
          <RouterLink to="/register">注册</RouterLink>
        </template>
      </nav>
    </el-header>
    <el-main class="shop-main">
      <RouterView />
    </el-main>
  </el-container>
</template>

<style scoped>
.shop-shell {
  min-height: 100vh;
  background: #f5f7fa;
}

.shop-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.shop-brand {
  font-weight: 700;
  color: #303133;
  text-decoration: none;
}

.shop-nav {
  display: flex;
  align-items: center;
  gap: 16px;
}

.shop-nav a {
  color: #606266;
  text-decoration: none;
}

.shop-nav a.router-link-active {
  color: #409eff;
}

.shop-user {
  color: #303133;
}

.shop-main {
  max-width: 1080px;
  width: 100%;
  margin: 0 auto;
}
</style>
