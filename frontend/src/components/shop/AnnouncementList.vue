<script setup lang="ts">
import type { AnnouncementView } from '../../types/api'

defineProps<{
  items: AnnouncementView[]
}>()
</script>

<template>
  <div v-if="items.length" class="announcement-list">
    <article v-for="item in items" :key="item.id" class="announcement">
      <span class="announcement-bell" aria-hidden="true">📣</span>
      <div class="announcement-copy">
        <h3 class="announcement-title">{{ item.title }}</h3>
        <p class="announcement-body">{{ item.body }}</p>
      </div>
    </article>
  </div>
</template>

<style scoped>
.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 30px;
}

.announcement {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 13px;
  padding: 15px 18px;
  border-radius: 14px;
  background: linear-gradient(120deg, rgba(47, 143, 219, 0.08), rgba(18, 179, 154, 0.06));
  border: 1px solid rgba(47, 143, 219, 0.22);
}

.announcement-bell {
  font-size: 16px;
  line-height: 1.4;
}

.announcement-copy {
  min-width: 0;
}

.announcement-title {
  margin: 0 0 5px;
  font-size: 14px;
  color: var(--azure-soft);
}

.announcement-body {
  margin: 0;
  color: var(--ink-soft);
  line-height: 1.7;
  white-space: pre-wrap;
}
</style>
