<template>
  <div class="agent-avatar-wrap" :style="{ width: size + 'px', height: size + 'px' }">
    <div class="agent-avatar-inner" :style="{ fontSize: size * 0.5 + 'px' }">
      {{ emoji }}
    </div>
  </div>
</template>

<script setup>
defineProps({
  emoji: { type: String, default: '🤖' },
  size: { type: Number, default: 48 }
})
</script>

<style scoped>
.agent-avatar-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
}
.agent-avatar-inner {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #4F46E5 0%, #06B6D4 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.35);
}
</style>
