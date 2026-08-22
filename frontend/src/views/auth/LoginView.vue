<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { motion } from 'motion-v'
import LoginForm from '../../components/auth/LoginForm.vue'
import { formatApiError } from '../../api/http'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const errorMessage = ref('')

async function onSubmit(payload: { username: string; password: string }): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    await auth.login(payload.username, payload.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (error) {
    errorMessage.value = formatApiError(error)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <motion.div
    class="auth-wrap"
    :initial="{ opacity: 0, y: 24 }"
    :animate="{ opacity: 1, y: 0 }"
    :transition="{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }"
  >
    <el-card class="auth-card">
      <p class="auth-eyebrow font-mono">KEY MARKET</p>
      <h2 class="auth-title font-display"><span class="grad-text">登录</span>钥市</h2>
      <p class="auth-sub">欢迎回来，继续你的数字收藏之旅。</p>
      <LoginForm :loading="loading" :error-message="errorMessage" @submit="onSubmit" />
      <p class="auth-switch">
        没有账号？
        <RouterLink
          :to="{ path: '/register', query: route.query.redirect ? { redirect: String(route.query.redirect) } : {} }"
        >
          去注册
        </RouterLink>
      </p>
    </el-card>
  </motion.div>
</template>

<style scoped>
.auth-wrap {
  display: flex;
  justify-content: center;
  padding: 56px 16px;
}

.auth-card {
  position: relative;
  width: 100%;
  max-width: 420px;
  padding: 10px 6px;
  border-radius: 20px;
  overflow: hidden !important;
  background:
    linear-gradient(165deg, rgba(139, 92, 246, 0.1), rgba(34, 211, 238, 0.06) 60%),
    var(--bg-raised) !important;
  border: 1px solid var(--line-strong) !important;
  box-shadow: 0 26px 80px rgba(0, 0, 0, 0.55), var(--glow-violet), inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.auth-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--grad-primary);
  opacity: 0.9;
}

.auth-eyebrow {
  margin: 0 0 6px;
  color: var(--mute);
  font-size: 11px;
  letter-spacing: 0.35em;
}

.auth-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 800;
  color: var(--ink);
}

.auth-title .grad-text {
  margin-right: 8px;
}

.auth-sub {
  margin: 0 0 22px;
  color: var(--mute);
  font-size: 13.5px;
}

.auth-card :deep(.el-form-item) {
  margin-bottom: 22px;
}

.auth-card :deep(.el-button.el-button--primary) {
  margin-top: 6px;
}

.auth-switch {
  margin: 18px 0 0;
  color: var(--mute);
  font-size: 13.5px;
}
</style>
