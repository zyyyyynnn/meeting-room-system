<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const appRouteKey = computed(() => {
  const topLevelPath = route.matched[0]?.path
  if (!topLevelPath || topLevelPath === '/') return '/'
  return topLevelPath
})
</script>

<template>
  <router-view v-slot="{ Component }">
    <transition name="app-shell" mode="out-in">
      <div :key="appRouteKey" class="app-route-shell">
        <component :is="Component" />
      </div>
    </transition>
  </router-view>
</template>

<style scoped>
.app-route-shell {
  width: 100%;
  min-height: 100%;
  height: 100%;
}

.app-shell-enter-active {
  transition: opacity 140ms var(--motion-ease-out);
  will-change: opacity;
}

.app-shell-leave-active {
  transition: opacity 100ms var(--motion-ease-out);
  will-change: opacity;
}

.app-shell-enter-from,
.app-shell-leave-to {
  opacity: 0;
}

.app-shell-enter-to,
.app-shell-leave-from {
  opacity: 1;
}
</style>
