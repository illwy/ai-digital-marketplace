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
    <!-- 极光氛围层 -->
    <div class="aurora" aria-hidden="true">
      <div class="aurora-orb aurora-orb--gold"></div>
      <div class="aurora-orb aurora-orb--azure"></div>
      <div class="aurora-orb aurora-orb--coral"></div>
      <div class="aurora-grid"></div>
    </div>

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
          <RouterLink class="shop-nav-link" to="/node-subscriptions">节点</RouterLink>
          <RouterLink class="shop-nav-link" to="/after-sales">售后</RouterLink>
          <RouterLink v-if="auth.isAdmin" class="shop-nav-link" to="/admin">后台</RouterLink>
          <span v-if="wallet" class="shop-wallet">
            <span class="shop-wallet-dot"></span>
            {{ formatFen(wallet.balanceFen) }}
          </span>
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
      <RouterView v-slot="{ Component, route: viewRoute }">
        <component :is="Component" v-if="Component" :key="viewRoute.fullPath" />
      </RouterView>
    </main>
    <footer class="shop-footer">
      <span class="shop-footer-line"></span>
      <div class="shop-footer-content">
        <span>钥市 · 虚拟库存 · 一单一件 · 出卡即复制</span>
        <a
          class="shop-footer-icp"
          href="https://beian.miit.gov.cn/"
          target="_blank"
          rel="noopener noreferrer"
          >豫ICP备2026043820号-1</a
        >
      </div>
      <span class="shop-footer-line"></span>
    </footer>
  </div>
</template>

<style scoped>
.shop-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  position: relative;
}

/* ---------- 极光氛围 ---------- */
.aurora {
  position: fixed;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.aurora-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.58;
  animation: aurora-drift 22s var(--ease-move) infinite;
}

.aurora-orb--gold {
  width: 620px;
  height: 620px;
  top: -220px;
  left: -120px;
  background: radial-gradient(circle, rgba(18, 179, 154, 0.55), transparent 65%);
}

.aurora-orb--azure {
  width: 520px;
  height: 520px;
  top: 8%;
  right: -180px;
  background: radial-gradient(circle, rgba(47, 143, 219, 0.4), transparent 65%);
  animation-delay: -7s;
  animation-duration: 28s;
}

.aurora-orb--coral {
  width: 420px;
  height: 420px;
  bottom: -160px;
  left: 32%;
  background: radial-gradient(circle, rgba(255, 179, 138, 0.55), transparent 65%);
  animation-delay: -14s;
  animation-duration: 34s;
}

.aurora-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(26, 36, 48, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(26, 36, 48, 0.045) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(ellipse 90% 60% at 50% 0%, #000 30%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse 90% 60% at 50% 0%, #000 30%, transparent 75%);
}

.shop-header,
.shop-main,
.shop-footer {
  position: relative;
  z-index: 1;
}

/* ---------- 玻璃导航 ---------- */
.shop-header {
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px 28px;
  padding: 14px 32px;
  border-bottom: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
}

.shop-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: var(--ink);
}

.shop-brand-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  background: var(--grad-primary);
  background-size: 200% 200%;
  animation: grad-shift 6s ease infinite;
  color: var(--on-accent);
  font-size: 21px;
  font-weight: 700;
  border-radius: 12px;
  box-shadow: var(--glow-gold);
}

.shop-brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.shop-brand-text strong {
  font-size: 19px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.shop-brand-text small {
  color: var(--mute);
  font-size: 12px;
}

.shop-nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 6px;
}

.shop-nav-link {
  position: relative;
  padding: 7px 13px;
  border-radius: 999px;
  color: var(--ink-soft);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.shop-nav-link:hover {
  color: var(--ink);
  background: var(--surface-strong);
}

.shop-nav-link.router-link-active,
.shop-nav-link.is-active {
  color: var(--gold-soft);
  background: rgba(18, 179, 154, 0.18);
  box-shadow: inset 0 0 0 1px rgba(18, 179, 154, 0.4);
}

.shop-wallet {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-left: 10px;
  padding: 6px 13px;
  border-radius: 999px;
  border: 1px solid rgba(47, 143, 219, 0.35);
  background: rgba(47, 143, 219, 0.08);
  color: var(--azure-soft);
  font-family: var(--font-mono);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.shop-wallet-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--azure);
  box-shadow: var(--glow-azure);
  animation: pulse-glow 2.4s ease infinite;
}

.shop-user {
  padding: 0 6px;
  color: var(--mute);
  font-size: 13px;
}

.shop-text-btn {
  border: 1px solid transparent;
  background: transparent;
  color: var(--mute);
  cursor: pointer;
  padding: 6px 11px;
  border-radius: 999px;
  font-size: 13px;
  transition: all 0.2s ease;
}

.shop-text-btn:hover {
  color: var(--red);
  background: rgba(251, 113, 133, 0.08);
}

.shop-nav-cta {
  padding: 8px 18px;
  margin-left: 6px;
  background: var(--grad-cta);
  background-size: 180% 180%;
  animation: grad-shift 6s ease infinite;
  color: var(--on-accent);
  text-decoration: none;
  font-weight: 600;
  font-size: 14px;
  border-radius: 999px;
  box-shadow: var(--glow-coral);
  transition: transform 0.2s var(--ease-out), box-shadow 0.25s var(--ease-out);
}

.shop-nav-cta:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 28px rgba(255, 93, 74, 0.32);
}

/* ---------- 主区 ---------- */
.shop-main {
  flex: 1;
  width: 100%;
  max-width: 1160px;
  margin: 0 auto;
  padding: 32px 20px 80px;
}

/* 路由过渡 */
.page-enter-active {
  transition: opacity 0.3s var(--ease-out), transform 0.3s var(--ease-out);
}
.page-leave-active {
  transition: opacity 0.18s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(14px);
}
.page-leave-to {
  opacity: 0;
}

/* ---------- 页脚 ---------- */
.shop-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  padding: 26px;
  color: var(--mute);
  font-size: 12px;
  letter-spacing: 0.14em;
}

.shop-footer-line {
  width: 56px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--line-strong));
}

.shop-footer-line:last-child {
  transform: scaleX(-1);
}

.shop-footer-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.shop-footer-icp {
  color: var(--mute);
  text-decoration: none;
  letter-spacing: 0.08em;
  transition: color 0.2s ease;
}

.shop-footer-icp:hover {
  color: var(--gold-soft);
}

@media (max-width: 720px) {
  .shop-header {
    padding: 12px 16px;
  }

  .shop-brand-text small {
    display: none;
  }

  .shop-user {
    display: none;
  }
}
</style>
