import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const title = ref('AI 数字商品自动售卖平台')
  return { title }
})
