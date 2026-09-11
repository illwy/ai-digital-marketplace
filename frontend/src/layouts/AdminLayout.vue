<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
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
  <div class="admin-shell">
    <div class="admin-aurora" aria-hidden="true">
      <span class="admin-orb admin-orb--gold"></span>
      <span class="admin-orb admin-orb--azure"></span>
    </div>

    <aside class="admin-aside">
      <div class="admin-brand">
        <span class="admin-brand-mark">钥</span>
        <span class="admin-brand-text">
          <strong>钥市后台</strong>
          <small>运营控制台</small>
        </span>
      </div>
      <nav class="admin-nav">
        <RouterLink
          v-for="item in menus"
          :key="item.path"
          class="admin-nav-link"
          :class="{ 'is-active': active === item.path }"
          :to="item.path"
        >
          <span class="admin-nav-indicator"></span>
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="admin-aside-foot">
        <span class="admin-foot-dot"></span>
        深空流光 · 运营版
      </div>
    </aside>

    <div class="admin-body">
      <header class="admin-header">
        <div class="admin-header-title">运营控制台</div>
        <div class="admin-header-actions">
          <span class="admin-user">
            <span class="admin-user-avatar">{{ auth.displayName.slice(0, 1) }}</span>
            {{ auth.displayName }}
          </span>
          <el-button text @click="router.push('/')">返回商城</el-button>
          <el-button text @click="onLogout">退出</el-button>
        </div>
      </header>
      <main class="admin-main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: flex;
  background: var(--bg);
  position: relative;
}

/* ---------- 氛围光 ---------- */
.admin-aurora {
  position: fixed;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.admin-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.4;
  animation: aurora-drift 22s var(--ease-move) infinite;
}

.admin-orb--gold {
  width: 520px;
  height: 520px;
  top: -200px;
  left: -140px;
  background: radial-gradient(circle, rgba(18, 179, 154, 0.5), transparent 65%);
}

.admin-orb--azure {
  width: 460px;
  height: 460px;
  bottom: -180px;
  right: -120px;
  background: radial-gradient(circle, rgba(47, 143, 219, 0.35), transparent 65%);
  animation-delay: -11s;
}

/* ---------- 侧边栏 ---------- */
.admin-aside {
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  width: 232px;
  flex-shrink: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 20px 18px;
}

.admin-brand-mark {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  background: var(--grad-primary);
  background-size: 200% 200%;
  animation: grad-shift 6s ease infinite;
  color: var(--on-accent);
  font-size: 20px;
  font-weight: 700;
  border-radius: 12px;
  box-shadow: var(--glow-gold);
}

.admin-brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.admin-brand-text strong {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.admin-brand-text small {
  color: var(--mute);
  font-size: 11.5px;
}

.admin-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 12px;
  overflow-y: auto;
}

.admin-nav-link {
  position: relative;
  display: flex;
  align-items: center;
  padding: 10px 14px;
  border-radius: 10px;
  color: var(--mute);
  font-size: 14px;
  text-decoration: none;
  transition: color 0.2s var(--ease-out), background-color 0.2s var(--ease-out);
}

.admin-nav-link:hover {
  color: var(--ink);
  background: var(--surface);
}

.admin-nav-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  width: 3px;
  height: 0;
  border-radius: 999px;
  background: var(--grad-primary);
  transform: translateY(-50%);
  transition: height 0.25s var(--ease-out), box-shadow 0.25s var(--ease-out);
}

.admin-nav-link.is-active {
  color: var(--ink);
  background: rgba(18, 179, 154, 0.14);
}

.admin-nav-link.is-active .admin-nav-indicator {
  height: 18px;
  box-shadow: var(--glow-gold);
}

.admin-aside-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  border-top: 1px solid var(--line);
  color: var(--mute);
  font-size: 11.5px;
  letter-spacing: 0.06em;
}

.admin-foot-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--green);
  box-shadow: 0 0 10px rgba(52, 211, 153, 0.7);
}

/* ---------- 右侧主体 ---------- */
.admin-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: calc(var(--z-sticky) - 1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 32px;
  border-bottom: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
}

.admin-header-title {
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 0.12em;
  color: var(--ink-soft);
}

.admin-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-right: 8px;
  color: var(--ink-soft);
  font-size: 13.5px;
}

.admin-user-avatar {
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--grad-primary);
  color: var(--on-accent);
  font-size: 12px;
  font-weight: 700;
}

.admin-main {
  flex: 1;
  padding: 28px 32px 40px;
}

@media (max-width: 900px) {
  .admin-aside {
    width: 76px;
  }

  .admin-brand-text,
  .admin-aside-foot {
    display: none;
  }

  .admin-nav-link {
    justify-content: center;
    padding: 10px;
  }

  .admin-nav-indicator {
    display: none;
  }

  .admin-main {
    padding: 20px 16px 32px;
  }

  .admin-header {
    padding: 12px 16px;
  }
}
</style>
