<script setup lang="ts">
import type { AnnouncementView } from '../../types/api'

defineProps<{
  items: AnnouncementView[]
}>()
</script>

<template>
  <el-alert
    v-for="item in items"
    :key="item.id"
    class="announcement"
    :title="item.title"
    :description="item.body"
    type="info"
    show-icon
    :closable="false"
  />
</template>

<style scoped>
.announcement {
  margin-bottom: 12px;
}
</style>
