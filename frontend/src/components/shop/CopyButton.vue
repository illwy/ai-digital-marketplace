<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  text: string
  primary?: boolean
}>()

const copying = ref(false)

async function copy(): Promise<void> {
  if (!props.text) {
    return
  }
  copying.value = true
  try {
    await navigator.clipboard.writeText(props.text)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败，请手动选择文字')
  } finally {
    copying.value = false
  }
}
</script>

<template>
  <el-button
    :type="primary ? 'primary' : 'default'"
    size="small"
    :loading="copying"
    :disabled="!text"
    @click.stop="copy"
  >
    复制卡密
  </el-button>
</template>
