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
  border: 1px solid rgba(38, 38, 38, 0.1);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(243, 246, 249, 0.82)),
    var(--bg-card);
  box-shadow:
    0 14px 26px rgba(20, 24, 28, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.status-panel__icon {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-unified);
  display: grid;
  place-items: center;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(242, 245, 248, 0.82));
  color: var(--text-main);
  font-size: 20px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.status-panel__body {
  min-width: 0;
}

.status-panel__title {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: 1.25;
  color: var(--text-main);
}

.status-panel__desc {
  margin-top: 4px;
  max-width: 62ch;
  font-size: 14px;
  line-height: 1.68;
  color: var(--text-muted);
}

.status-panel--info {
  border-color: rgba(64, 72, 80, 0.16);
}

.status-panel--warning {
  border-color: rgba(112, 98, 78, 0.18);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(245, 243, 238, 0.82)),
    var(--bg-card);
}

.status-panel--warning .status-panel__icon {
  color: #665847;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(243, 239, 232, 0.86));
}

.status-panel--danger {
  border-color: rgba(113, 88, 90, 0.18);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(245, 240, 241, 0.84)),
    var(--bg-card);
}

.status-panel--danger .status-panel__icon {
  color: #6a5052;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(243, 234, 235, 0.88));
}

.status-panel--loading {
  border-color: rgba(64, 72, 80, 0.16);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(243, 246, 249, 0.84)),
    var(--bg-card);
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
