<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import RegisterForm from '../../components/auth/RegisterForm.vue'
import { formatApiError } from '../../api/http'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const errorMessage = ref('')

async function onSubmit(payload: { username: string; password: string; nickname: string }): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    await auth.register(payload.username, payload.password, payload.nickname || undefined)
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
  <el-card class="auth-card">
    <h2 class="auth-title">注册</h2>
    <RegisterForm :loading="loading" :error-message="errorMessage" @submit="onSubmit" />
    <p class="auth-switch">
      已有账号？
      <RouterLink :to="{ path: '/login', query: route.query.redirect ? { redirect: String(route.query.redirect) } : {} }">
        去登录
      </RouterLink>
    </p>
  </el-card>
</template>

<style scoped>
.auth-card {
  position: relative;
  max-width: 420px;
  margin: 56px auto;
  overflow: visible !important;
  background: var(--ticket) !important;
  color: var(--ticket-ink);
  border: 0 !important;
}

.auth-card::before,
.auth-card::after {
  content: "";
  position: absolute;
  left: -9px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--paper);
}

.auth-card::before {
  top: 36px;
}

.auth-card::after {
  bottom: 36px;
}

.auth-title {
  margin: 0 0 16px;
  font-family: "Noto Serif SC", serif;
  font-size: 24px;
}

.auth-switch {
  margin-top: 16px;
  color: var(--ticket-mute);
}
</style>
