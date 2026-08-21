<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import RegisterForm from '../../components/auth/RegisterForm.vue'
import { readApiError } from '../../api/http'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')

async function onSubmit(payload: { username: string; password: string; nickname: string }): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    await auth.register(payload.username, payload.password, payload.nickname || undefined)
    await router.replace('/')
  } catch (error) {
    errorMessage.value = readApiError(error).message
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
      <RouterLink to="/login">去登录</RouterLink>
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
