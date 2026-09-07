<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchNodeSubscriptions } from '../../api/shop'
import { readApiError } from '../../api/http'
import type { NodeSubscriptionView } from '../../types/api'

const items = ref<NodeSubscriptionView[]>([])
const loading = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    const { data } = await fetchNodeSubscriptions()
    items.value = data.data
  } catch (error) {
    ElMessage.error(readApiError(error).message)
  } finally {
    loading.value = false
  }
}

function copy(value: string): void {
  if (!value) return
  void navigator.clipboard.writeText(value).then(() => ElMessage.success('订阅地址已复制'))
}

onMounted(load)
</script>

<template>
  <section class="page-shell">
    <div class="page-heading">
      <div>
        <p class="eyebrow">NODE ACCESS</p>
        <h1 class="page-title font-display">我的<span class="grad-text">节点</span></h1>
        <p class="page-desc">支付成功后，2S-UI 会在后台自动开通；开通期间状态会显示为处理中。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>
    <el-empty v-if="!loading && !items.length" description="还没有节点订阅，购买节点商品后会显示在这里" />
    <div v-else class="node-grid">
      <el-card v-for="item in items" :key="item.id" class="node-card" shadow="never">
        <div class="node-card-head">
          <div>
            <span class="node-status">{{ item.status }}</span>
            <h2>{{ item.clientName }}</h2>
          </div>
          <span class="node-expiry">到期 {{ item.expiresAt?.replace('T', ' ') }}</span>
        </div>
        <p class="node-meta">流量 {{ Math.round(item.trafficBytes / 1073741824) }} GB · 设备 {{ item.deviceLimit || '不限' }}</p>
        <div class="node-url">
          <code>{{ item.subscriptionUrl || '等待 2S-UI 返回订阅链接' }}</code>
          <el-button v-if="item.subscriptionUrl" text type="primary" @click="copy(item.subscriptionUrl)">复制</el-button>
        </div>
        <details v-if="item.clashConfig">
          <summary>查看协议链接（Clash/Mihomo 可转换）</summary>
          <pre>{{ item.clashConfig }}</pre>
        </details>
      </el-card>
    </div>
  </section>
</template>

<style scoped>
.node-grid { display: grid; gap: 16px; }
.node-card { border-color: var(--line); background: rgba(12, 15, 28, .72); }
.node-card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.node-card h2 { margin: 8px 0 0; font-size: 18px; }
.node-status { color: var(--cyan); font-size: 12px; letter-spacing: .08em; }
.node-expiry, .node-meta { color: var(--mute); font-size: 13px; }
.node-url { display: flex; align-items: center; gap: 10px; margin-top: 16px; }
.node-url code { overflow: hidden; flex: 1; padding: 10px; border: 1px solid var(--line); border-radius: 8px; color: var(--ink); white-space: nowrap; text-overflow: ellipsis; }
details { margin-top: 14px; color: var(--mute); font-size: 13px; }
pre { max-height: 180px; overflow: auto; margin-top: 8px; padding: 10px; border-radius: 8px; background: rgba(0, 0, 0, .25); white-space: pre-wrap; word-break: break-all; }
</style>
