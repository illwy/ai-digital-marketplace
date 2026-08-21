<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchAdminPing } from '../../api/auth'
import { readApiError } from '../../api/http'

const status = ref('检查中…')

onMounted(async () => {
  try {
    const { data } = await fetchAdminPing()
    status.value = `管理接口正常，当前管理员 ${data.data.username}`
  } catch (error) {
    status.value = readApiError(error).message
  }
})
</script>

<template>
  <el-card>
    <h2 class="dash-title">运营概览</h2>
    <p>{{ status }}</p>
  </el-card>
</template>

<style scoped>
.dash-title {
  margin: 0 0 12px;
  font-size: 18px;
}
</style>
