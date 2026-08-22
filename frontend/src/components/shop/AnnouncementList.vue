<script setup lang="ts">
import type { AnnouncementView } from '../../types/api'
import FoilBadge from './FoilBadge.vue'

defineProps<{
  items: AnnouncementView[]
}>()
</script>

<template>
  <div v-if="items.length" class="announcement-list">
    <article v-for="item in items" :key="item.id" class="announcement">
      <FoilBadge label="通告" />
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
  margin-bottom: 8px;
}

.announcement {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  background: var(--panel);
  border: 1px solid var(--line);
}

.announcement-copy {
  min-width: 0;
}

.announcement-title {
  margin: 0 0 6px;
  font-size: 14px;
  color: var(--ink);
}

.announcement-body {
  margin: 0;
  color: var(--mute);
  line-height: 1.65;
  white-space: pre-wrap;
}
</style>
