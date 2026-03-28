<script setup lang="ts">
import { computed } from 'vue'
import { CircleCloseFilled, InfoFilled, Loading, WarningFilled } from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    tone?: 'info' | 'warning' | 'danger' | 'loading'
    title: string
    description?: string
    actionText?: string
  }>(),
  {
    tone: 'info',
    description: '',
    actionText: '',
  },
)

const emit = defineEmits<{
  (e: 'action'): void
}>()

const iconComponent = computed(() => {
  if (props.tone === 'loading') return Loading
  if (props.tone === 'warning') return WarningFilled
  if (props.tone === 'danger') return CircleCloseFilled
  return InfoFilled
})
</script>

<template>
  <section class="status-panel cursor-card" :class="`status-panel--${tone}`" role="status" :aria-live="tone === 'danger' ? 'assertive' : 'polite'">
    <div class="status-panel__icon">
      <el-icon :class="{ 'is-spinning': tone === 'loading' }">
        <component :is="iconComponent" />
      </el-icon>
    </div>
    <div class="status-panel__body">
      <div class="status-panel__title">{{ title }}</div>
      <div v-if="description" class="status-panel__desc">{{ description }}</div>
    </div>
    <el-button v-if="actionText" type="primary" class="btn-key-solid status-panel__action" @click="emit('action')">
      {{ actionText }}
    </el-button>
  </section>
</template>

<style scoped>
.status-panel {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-radius: calc(var(--radius-unified) + 2px);
  border: 1px solid var(--line-soft);
  background: rgba(255, 255, 255, 0.62);
}

.status-panel__icon {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-unified);
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.76);
  color: var(--text-main);
  font-size: 20px;
}

.status-panel__body {
  min-width: 0;
}

.status-panel__title {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
  color: var(--text-main);
}

.status-panel__desc {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.66;
  color: var(--text-muted);
}

.status-panel--info {
  border-color: rgba(31, 31, 31, 0.14);
}

.status-panel--warning {
  border-color: rgba(111, 86, 43, 0.24);
  background: rgba(255, 250, 242, 0.72);
}

.status-panel--warning .status-panel__icon {
  color: #6a4f22;
  background: rgba(255, 246, 227, 0.88);
}

.status-panel--danger {
  border-color: rgba(110, 57, 57, 0.22);
  background: rgba(255, 246, 245, 0.78);
}

.status-panel--danger .status-panel__icon {
  color: #7c3a3a;
  background: rgba(255, 237, 235, 0.92);
}

.status-panel--loading {
  border-color: rgba(31, 31, 31, 0.14);
  background: rgba(250, 248, 243, 0.82);
}

.status-panel--loading .status-panel__icon {
  color: var(--accent);
}

.is-spinning {
  animation: panelSpin 1s linear infinite;
}

@keyframes panelSpin {
  100% {
    transform: rotate(360deg);
  }
}

@media (max-width: 760px) {
  .status-panel {
    grid-template-columns: auto 1fr;
  }

  .status-panel__action {
    grid-column: 1 / -1;
    width: 100%;
  }
}
</style>
