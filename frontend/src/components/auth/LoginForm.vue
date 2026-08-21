<script setup lang="ts">
import { reactive } from 'vue'

const props = defineProps<{
  loading: boolean
  errorMessage: string
}>()

const emit = defineEmits<{
  submit: [payload: { username: string; password: string }]
}>()

const form = reactive({
  username: '',
  password: '',
})

function onSubmit(): void {
  emit('submit', { username: form.username.trim(), password: form.password })
}
</script>

<template>
  <el-form class="auth-form" label-position="top" @submit.prevent="onSubmit">
    <el-form-item label="用户名">
      <el-input v-model="form.username" autocomplete="username" maxlength="32" />
    </el-form-item>
    <el-form-item label="密码">
      <el-input v-model="form.password" type="password" show-password autocomplete="current-password" maxlength="64" />
    </el-form-item>
    <p v-if="props.errorMessage" class="auth-form-error">{{ props.errorMessage }}</p>
    <el-button type="primary" native-type="submit" :loading="props.loading" class="auth-form-submit">
      登录
    </el-button>
  </el-form>
</template>

<style scoped>
.auth-form-error {
  color: var(--el-color-danger);
  margin: 0 0 12px;
  font-size: 13px;
}

.auth-form-submit {
  width: 100%;
}
</style>
