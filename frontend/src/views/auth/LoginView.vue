<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoginForm from '../../components/auth/LoginForm.vue'
import { readApiError } from '../../api/http'
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
    errorMessage.value = readApiError(error).message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card class="auth-card">
    <h2 class="auth-title">登录</h2>
    <LoginForm :loading="loading" :error-message="errorMessage" @submit="onSubmit" />
    <p class="auth-switch">
      没有账号？
      <RouterLink to="/register">去注册</RouterLink>
    </p>
  </el-card>
</template>

<style scoped>
.auth-card {
  max-width: 420px;
  margin: 48px auto;
}

.auth-title {
  margin: 0 0 16px;
  font-size: 20px;
}

.auth-switch {
  margin-top: 16px;
  color: #909399;
}
</style>
