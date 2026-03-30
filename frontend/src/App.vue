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
    <transition name="app-shell" mode="out-in" appear>
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
  transition:
    opacity 520ms var(--motion-ease-emphasis),
    transform 520ms var(--motion-ease-emphasis);
}

.app-shell-leave-active {
  transition:
    opacity 240ms var(--motion-ease-out),
    transform 240ms var(--motion-ease-out);
}

.app-shell-enter-from,
.app-shell-leave-to {
  opacity: 0;
  transform: translate3d(0, 16px, 0);
}

.app-shell-enter-to,
.app-shell-leave-from {
  opacity: 1;
  transform: translate3d(0, 0, 0);
}
</style>
