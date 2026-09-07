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
      <div class="aurora-orb aurora-orb--violet"></div>
      <div class="aurora-orb aurora-orb--cyan"></div>
      <div class="aurora-orb aurora-orb--pink"></div>
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
      <RouterView v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>
    <footer class="shop-footer">
      <span class="shop-footer-line"></span>
      钥市 · 虚拟库存 · 一单一件 · 出卡即复制
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
  opacity: 0.5;
  animation: aurora-drift 22s var(--ease-move) infinite;
}

.aurora-orb--violet {
  width: 620px;
  height: 620px;
  top: -220px;
  left: -120px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.55), transparent 65%);
}

.aurora-orb--cyan {
  width: 520px;
  height: 520px;
  top: 8%;
  right: -180px;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.4), transparent 65%);
  animation-delay: -7s;
  animation-duration: 28s;
}

.aurora-orb--pink {
  width: 420px;
  height: 420px;
  bottom: -160px;
  left: 32%;
  background: radial-gradient(circle, rgba(244, 114, 182, 0.28), transparent 65%);
  animation-delay: -14s;
  animation-duration: 34s;
}

.aurora-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 216, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 216, 0.05) 1px, transparent 1px);
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
  background: rgba(5, 6, 13, 0.72);
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
  color: #fff;
  font-size: 21px;
  font-weight: 700;
  border-radius: 12px;
  box-shadow: var(--glow-violet);
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
  color: #fff;
  background: rgba(139, 92, 246, 0.18);
  box-shadow: inset 0 0 0 1px rgba(139, 92, 246, 0.4);
}

.shop-wallet {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-left: 10px;
  padding: 6px 13px;
  border-radius: 999px;
  border: 1px solid rgba(34, 211, 238, 0.35);
  background: rgba(34, 211, 238, 0.08);
  color: var(--cyan-soft);
  font-family: var(--font-mono);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.shop-wallet-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--cyan);
  box-shadow: var(--glow-cyan);
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
  background: var(--grad-primary);
  background-size: 180% 180%;
  animation: grad-shift 6s ease infinite;
  color: #fff;
  text-decoration: none;
  font-weight: 600;
  font-size: 14px;
  border-radius: 999px;
  box-shadow: var(--glow-violet);
  transition: transform 0.2s var(--ease-out), box-shadow 0.25s var(--ease-out);
}

.shop-nav-cta:hover {
  transform: translateY(-1px);
  box-shadow: 0 0 36px rgba(139, 92, 246, 0.55);
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
