<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    tone?: 'violet' | 'mute' | 'warn' | 'ok' | 'danger' | 'cyan'
  }>(),
  { tone: 'violet' },
)
</script>

<template>
  <span class="foil-badge" :class="`is-${tone}`">{{ label }}</span>
</template>

<style scoped>
.foil-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  border: 1px solid currentColor;
  background: color-mix(in srgb, currentColor 10%, transparent);
  font-size: 11.5px;
  font-weight: 500;
  line-height: 1.6;
  white-space: nowrap;
}

.is-violet {
  color: var(--violet-soft);
}

.is-copper,
.is-cyan {
  color: var(--cyan-soft);
}

.is-mute {
  color: var(--mute);
}

.is-warn {
  color: var(--amber);
}

.is-ok {
  color: var(--green);
}

.is-danger {
  color: var(--red);
}
</style>
