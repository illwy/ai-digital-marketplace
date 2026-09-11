<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { motion } from 'motion-v'
import CopyButton from './CopyButton.vue'

const props = defineProps<{
  content: string
  hint?: string
}>()

// 出卡扫描动画：内容挂载后扫一遍光
const scanned = ref(false)

onMounted(() => {
  requestAnimationFrame(() => {
    scanned.value = true
  })
})
</script>

<template>
  <motion.section
    class="secret"
    :initial="{ opacity: 0, y: 18, scale: 0.98 }"
    :animate="{ opacity: 1, y: 0, scale: 1 }"
    :transition="{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }"
  >
    <header class="secret-head">
      <p class="secret-label">
        <span class="secret-pulse"></span>
        卡密已交付
      </p>
      <CopyButton :text="content" primary />
    </header>

    <div class="secret-body-wrap">
      <pre
        class="secret-body"
        :class="{ 'is-scanned': scanned }">{{ content }}</pre>
      <span v-if="scanned" class="secret-scanline" aria-hidden="true"></span>
    </div>

    <footer class="secret-foot">
      <p class="secret-hint">{{ hint || '发卡后请自行保存，离开页面仍可在「卡密」里查看。' }}</p>
    </footer>
  </motion.section>
</template>

<style scoped>
.secret {
  position: relative;
  margin-top: 16px;
  border-radius: 18px;
  background:
    linear-gradient(160deg, rgba(47, 143, 219, 0.1), rgba(18, 179, 154, 0.12) 55%, rgba(11, 125, 109, 0.08)),
    var(--bg-raised);
  border: 1px solid rgba(47, 143, 219, 0.35);
  box-shadow: 0 16px 40px rgba(22, 48, 43, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.9);
  overflow: hidden;
}

.secret-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 12px;
  padding: 15px 18px 0;
}

.secret-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--azure-soft);
}

.secret-pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--green);
  box-shadow: 0 0 12px rgba(52, 211, 153, 0.9);
  animation: pulse-glow 2s ease infinite;
}

.secret-body-wrap {
  position: relative;
  margin: 12px 18px 0;
  overflow: hidden;
  border-radius: 12px;
}

.secret-body {
  position: relative;
  z-index: 1;
  margin: 0;
  padding: 16px 18px;
  background: #eef8f5;
  color: var(--ink);
  white-space: pre-wrap;
  word-break: break-all;
  font-family: var(--font-mono);
  font-size: 16px;
  letter-spacing: 0.03em;
  user-select: all;
}

/* 扫描光：出卡瞬间从上到下掠过一次 */
.secret-scanline {
  position: absolute;
  left: 0;
  right: 0;
  top: -20%;
  height: 42%;
  z-index: 2;
  pointer-events: none;
  background: linear-gradient(
    to bottom,
    transparent,
    rgba(125, 188, 232, 0.28) 45%,
    rgba(255, 255, 255, 0.35) 50%,
    rgba(125, 188, 232, 0.28) 55%,
    transparent
  );
  animation: secret-scan 1.4s var(--ease-move) forwards;
}

@keyframes secret-scan {
  from {
    transform: translateY(-30%);
    opacity: 1;
  }
  to {
    transform: translateY(320%);
    opacity: 0;
  }
}

.secret-foot {
  padding: 13px 18px 16px;
}

.secret-hint {
  margin: 0;
  color: var(--mute);
  font-size: 13px;
  line-height: 1.55;
}
</style>
