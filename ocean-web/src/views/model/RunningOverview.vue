<template>
  <div v-if="versions.length > 0" style="margin-bottom: 28px;">
    <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px;">
      <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #22c55e;"></span>
      <span style="font-size: 10px; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.1em;">运行概览</span>
      <span style="font-size: 12px; color: #999;">{{ versions.length }} 个版本运行中</span>
    </div>
    <div style="display: flex; flex-wrap: wrap; gap: 10px;">
      <div
        v-for="v in versions"
        :key="v.versionId"
        style="display: flex; align-items: center; gap: 10px; border: 1px solid #e8e8e8; padding: 8px 14px; font-size: 13px;"
      >
        <span style="font-weight: 600; color: var(--color-text);">{{ v.modelName }}</span>
        <span style="background: #e0e7ff; color: #4f46e5; padding: 1px 7px; font-size: 11px; font-weight: 500;">{{ v.versionLabel }}</span>
        <span style="color: #22c55e; font-size: 12px;">运行中</span>
        <a class="editorial-link" style="color: #ef4444;" @click="$emit('stop', v)">停止</a>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  versions: { type: Array, default: () => [] }
})

defineEmits(['stop'])
</script>
