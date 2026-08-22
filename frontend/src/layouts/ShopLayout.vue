<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { watch } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useWalletStore } from '../stores/wallet'
import { formatFen } from '../utils/money'

const auth = useAuthStore()
const walletStore = useWalletStore()
const { wallet } = storeToRefs(walletStore)
const router = useRouter()

async function onLogout(): Promise<void> {
  await auth.logout()
  walletStore.clear()
  await router.push('/')
}

watch(
  () => auth.isAuthenticated,
  async (loggedIn) => {
    if (loggedIn) {
      await walletStore.refresh()
    } else {
      walletStore.clear()
    }
  },
  { immediate: true },
)
</script>

<template>
  <div class="shop-shell">
    <header class="shop-header">
      <RouterLink class="shop-brand" to="/">
        <span class="shop-brand-mark">钥</span>
        <span class="shop-brand-text">
          <strong>钥市</strong>
          <small>付款后立刻出卡</small>
        </span>
      </RouterLink>
      <nav class="shop-nav">
        <RouterLink class="shop-nav-link" to="/" active-class="" exact-active-class="is-active">货架</RouterLink>
        <template v-if="auth.isAuthenticated">
          <RouterLink class="shop-nav-link" to="/orders">订单</RouterLink>
          <RouterLink class="shop-nav-link" to="/deliveries">卡密</RouterLink>
          <RouterLink class="shop-nav-link" to="/after-sales">售后</RouterLink>
          <RouterLink v-if="auth.isAdmin" class="shop-nav-link" to="/admin">后台</RouterLink>
          <span v-if="wallet" class="shop-wallet">{{ formatFen(wallet.balanceFen) }}</span>
          <span class="shop-user">{{ auth.displayName }}</span>
          <button class="shop-text-btn" type="button" @click="onLogout">退出</button>
        </template>
        <template v-else>
          <RouterLink class="shop-nav-link" to="/login">登录</RouterLink>
          <RouterLink class="shop-nav-cta" to="/register">注册购买</RouterLink>
        </template>
      </nav>
    </header>
    <main class="shop-main">
      <RouterView />
    </main>
    <footer class="shop-footer">钥市 · 虚拟库存 · 一单一件 · 出卡即复制</footer>
  </div>
</template>

<style scoped>
.shop-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(900px 420px at 80% -10%, rgba(224, 122, 58, 0.18), transparent 55%),
    radial-gradient(700px 380px at 0% 100%, rgba(90, 42, 22, 0.45), transparent 50%),
    var(--paper);
}

.shop-shell::before {
  content: "";
  position: fixed;
  inset: 0;
  pointer-events: none;
  opacity: 0.045;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='80' height='80'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.8' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='80' height='80' filter='url(%23n)' opacity='.55'/%3E%3C/svg%3E");
  z-index: 1;
}

.shop-header,
.shop-main,
.shop-footer {
  position: relative;
  z-index: 2;
}

.shop-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px 28px;
  padding: 20px 32px;
  border-bottom: 1px solid var(--line);
  background: rgba(17, 14, 12, 0.86);
  backdrop-filter: blur(10px);
}

.shop-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: var(--ink);
}

.shop-brand-mark {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  background: var(--copper);
  color: #1a120c;
  font-family: "Noto Serif SC", serif;
  font-size: 20px;
  font-weight: 700;
}

.shop-brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.shop-brand-text strong {
  font-family: "Noto Serif SC", serif;
  font-size: 20px;
  font-weight: 700;
}

.shop-brand-text small {
  color: var(--mute);
  font-size: 12px;
}

.shop-nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 18px;
}

.shop-nav-link {
  color: var(--mute);
  text-decoration: none;
  font-size: 14px;
}

.shop-nav-link.router-link-active,
.shop-nav-link.is-active {
  color: var(--ink);
}

.shop-nav-cta {
  padding: 8px 14px;
  background: var(--copper);
  color: #1a120c;
  text-decoration: none;
  font-weight: 700;
}

.shop-wallet,
.shop-user {
  font-size: 13px;
}

.shop-wallet {
  color: var(--copper-deep);
  font-variant-numeric: tabular-nums;
}

.shop-text-btn {
  border: 0;
  background: transparent;
  color: var(--mute);
  cursor: pointer;
}

.shop-main {
  flex: 1;
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding: 28px 20px 64px;
}

.shop-footer {
  padding: 20px;
  text-align: center;
  color: var(--mute);
  font-size: 12px;
  letter-spacing: 0.08em;
}
</style>
