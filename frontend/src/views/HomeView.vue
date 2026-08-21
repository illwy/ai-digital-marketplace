<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAppStore } from '../stores/app'
import { http } from '../api/http'

const app = useAppStore()
const ping = ref('检查中…')

onMounted(async () => {
  try {
    const { data } = await http.get('/ping')
    ping.value = `API ${data.app} / MySQL ${data.mysql} / Redis ${data.redis}`
  } catch {
    ping.value = '后端未启动（可先只跑前端）'
  }
})
</script>

<template>
  <el-container class="home">
    <el-header>
      <h1>{{ app.title }}</h1>
    </el-header>
    <el-main>
      <el-result icon="info" title="基础设施已初始化" sub-title="当前未开发业务功能。">
        <template #extra>
          <el-tag>{{ ping }}</el-tag>
        </template>
      </el-result>
    </el-main>
  </el-container>
</template>

<style scoped>
.home {
  min-height: 100vh;
}

.el-header h1 {
  margin: 16px 0;
  font-size: 20px;
}
</style>
