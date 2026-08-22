<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { motion } from 'motion-v'
import { useRouter } from 'vue-router'
import { fetchAdminOverview } from '../../api/admin'
import { readApiError } from '../../api/http'
import type { AdminOverviewView } from '../../types/api'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const overview = ref<AdminOverviewView | null>(null)

const cards: {
  key: keyof AdminOverviewView
  title: string
  path: string
  query?: Record<string, string>
  tone: 'violet' | 'cyan' | 'green' | 'amber' | 'danger' | 'mute'
}[] = [
  { key: 'userCount', title: '用户', path: '/admin/users', tone: 'violet' },
  { key: 'productOnSaleCount', title: '在售商品', path: '/admin/products', query: { status: 'ON_SALE' }, tone: 'cyan' },
  { key: 'availableInventoryCount', title: '可用库存', path: '/admin/inventory', query: { status: 'AVAILABLE' }, tone: 'green' },
  { key: 'pendingOrderCount', title: '待支付订单', path: '/admin/orders', query: { payStatus: 'PENDING' }, tone: 'amber' },
  { key: 'paidOrderCount', title: '已支付订单', path: '/admin/orders', query: { payStatus: 'PAID' }, tone: 'cyan' },
  { key: 'openAfterSaleCount', title: '待处理售后', path: '/admin/after-sales', query: { status: 'OPEN_ACTIVE' }, tone: 'danger' },
  { key: 'enabledAnnouncementCount', title: '启用公告', path: '/admin/announcements', tone: 'mute' },
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
  <motion.div
    class="dash"
    :initial="{ opacity: 0, y: 24 }"
    :animate="{ opacity: 1, y: 0 }"
    :transition="{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }"
  >
    <header class="dash-head">
      <h2 class="dash-title">运营概览</h2>
      <p class="dash-desc">实时盘一下用户、商品、库存、订单、售后与公告的水位。</p>
    </header>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
      class="dash-alert"
    />
    <el-empty v-else-if="!overview && !loading" description="暂无概览数据" />

    <div v-else-if="overview" v-loading="loading" class="stat-grid">
      <div
        v-for="card in cards"
        :key="card.key"
        class="stat-card glass-panel"
        :class="`is-${card.tone}`"
        role="button"
        tabindex="0"
        @click="go(card.path, card.query)"
        @keyup.enter="go(card.path, card.query)"
      >
        <span class="stat-dot"></span>
        <span class="stat-label">{{ card.title }}</span>
        <span class="stat-value font-mono">{{ overview[card.key] }}</span>
        <span class="stat-go">查看 →</span>
      </div>
    </div>

    <div v-if="overview" class="demo-path glass-panel">
      <h3 class="demo-title">演示路径</h3>
      <p class="demo-copy">买家下单拿卡密之后，后台按这三条走：补货、查单、处理售后。</p>
      <div class="demo-actions">
        <el-button type="primary" @click="go('/admin/inventory')">去补货</el-button>
        <el-button @click="go('/admin/orders', { payStatus: 'PAID' })">已支付订单</el-button>
        <el-button @click="go('/admin/deliveries')">交付记录</el-button>
        <el-button @click="go('/admin/after-sales', { status: 'OPEN_ACTIVE' })">待处理售后</el-button>
      </div>
    </div>
  </motion.div>
</template>

<style scoped>
.dash {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dash-head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dash-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 24px;
  letter-spacing: 0.02em;
}

.dash-desc {
  margin: 0;
  color: var(--mute);
  font-size: 13.5px;
}

.dash-alert {
  border-radius: 12px;
}

/* ---------- 统计卡 ---------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.stat-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 20px;
  cursor: pointer;
  overflow: hidden;
  transition:
    transform 0.25s var(--ease-out),
    box-shadow 0.25s var(--ease-out),
    border-color 0.25s var(--ease-out);
}

.stat-card:hover,
.stat-card:focus-visible {
  transform: translateY(-3px);
  outline: none;
}

.stat-dot {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  margin-bottom: 4px;
  background: color-mix(in srgb, currentColor 18%, transparent);
  border: 1px solid currentColor;
}

.stat-label {
  color: var(--mute);
  font-size: 13px;
  letter-spacing: 0.04em;
}

.stat-value {
  font-size: 32px;
  line-height: 1.1;
  font-weight: 700;
  color: var(--ink);
}

.stat-go {
  align-self: flex-end;
  color: currentColor;
  font-size: 12px;
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.2s var(--ease-out), transform 0.2s var(--ease-out);
}

.stat-card:hover .stat-go {
  opacity: 1;
  transform: translateX(0);
}

.is-violet {
  color: var(--violet-soft);
}
.is-cyan {
  color: var(--cyan-soft);
}
.is-green {
  color: var(--green);
}
.is-amber {
  color: var(--amber);
}
.is-danger {
  color: var(--pink);
}
.is-mute {
  color: var(--mute);
}

.is-violet:hover,
.is-violet:focus-visible {
  border-color: rgba(139, 92, 246, 0.55);
  box-shadow: var(--glow-violet);
}
.is-cyan:hover,
.is-cyan:focus-visible {
  border-color: rgba(34, 211, 238, 0.5);
  box-shadow: var(--glow-cyan);
}
.is-green:hover,
.is-green:focus-visible {
  border-color: rgba(52, 211, 153, 0.5);
  box-shadow: 0 0 24px rgba(52, 211, 153, 0.28);
}
.is-amber:hover,
.is-amber:focus-visible {
  border-color: rgba(251, 191, 36, 0.5);
  box-shadow: 0 0 24px rgba(251, 191, 36, 0.26);
}
.is-danger:hover,
.is-danger:focus-visible {
  border-color: rgba(244, 114, 182, 0.5);
  box-shadow: 0 0 24px rgba(244, 114, 182, 0.28);
}
.is-mute:hover,
.is-mute:focus-visible {
  border-color: var(--line-strong);
  box-shadow: 0 0 20px rgba(139, 147, 178, 0.22);
}

/* ---------- 演示路径 ---------- */
.demo-path {
  padding: 22px 24px;
}

.demo-title {
  margin: 0 0 8px;
  font-family: var(--font-display);
  font-size: 16px;
}

.demo-copy {
  margin: 0 0 14px;
  color: var(--ink-soft);
  font-size: 13.5px;
}

.demo-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
