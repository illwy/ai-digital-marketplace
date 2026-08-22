<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    tone?: 'copper' | 'mute' | 'warn' | 'ok' | 'danger'
  }>(),
  { tone: 'copper' },
)
</script>

<template>
  <span class="foil-badge" :class="`is-${tone}`">{{ label }}</span>
</template>

<style scoped>
.foil-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 9px;
  border: 1px solid currentColor;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1.5;
  white-space: nowrap;
}

.is-copper {
  color: var(--copper);
  background: color-mix(in srgb, var(--copper) 12%, transparent);
}

.is-mute {
  color: var(--ticket-mute, var(--mute));
  background: transparent;
}

.is-warn {
  color: #c47a2a;
  background: color-mix(in srgb, #c47a2a 12%, transparent);
}

.is-ok {
  color: #3f7a4a;
  background: color-mix(in srgb, #3f7a4a 12%, transparent);
}

.is-danger {
  color: #b54a3a;
  background: color-mix(in srgb, #b54a3a 12%, transparent);
}
</style>
