<script setup lang="ts">
import CopyButton from './CopyButton.vue'

defineProps<{
  content: string
  hint?: string
}>()
</script>

<template>
  <section class="secret">
    <span class="secret-notch secret-notch-left" aria-hidden="true" />
    <span class="secret-notch secret-notch-right" aria-hidden="true" />
    <header class="secret-head">
      <p class="secret-label">卡密</p>
      <CopyButton :text="content" primary />
    </header>
    <pre class="secret-body">{{ content }}</pre>
    <footer class="secret-foot">
      <p class="secret-hint">{{ hint || '发卡后请自行保存，离开页面仍可在「卡密」里查看。' }}</p>
    </footer>
  </section>
</template>

<style scoped>
.secret {
  position: relative;
  margin-top: 16px;
  background: #1a120c;
  border: 1px solid #5a3a24;
}

.secret-notch {
  position: absolute;
  top: 22px;
  z-index: 1;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--ticket, var(--paper));
}

.secret-notch-left {
  left: -8px;
}

.secret-notch-right {
  right: -8px;
}

.secret-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 12px;
  padding: 12px 16px 0;
}

.secret-label {
  margin: 0;
  font-size: 12px;
  color: var(--copper);
}

.secret-body {
  margin: 10px 16px 0;
  padding: 14px;
  background: #0f0c0a;
  color: #f6e7d4;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 16px;
  letter-spacing: 0.02em;
  user-select: all;
}

.secret-foot {
  padding: 12px 16px 14px;
}

.secret-hint {
  margin: 0;
  color: var(--mute);
  font-size: 13px;
  line-height: 1.5;
}
</style>
