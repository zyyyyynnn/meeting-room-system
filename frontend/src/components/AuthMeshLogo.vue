<script setup lang="ts">
import { createElement } from 'react'
import { MeshGradient } from '@paper-design/shaders-react'
import { createRoot } from 'react-dom/client'
import { onBeforeUnmount, onMounted, ref } from 'vue'

const mountEl = ref<HTMLElement | null>(null)
let root: ReturnType<typeof createRoot> | null = null
let resizeObserver: ResizeObserver | null = null

function renderMesh() {
  if (!root) return
  root.render(
    createElement(MeshGradient, {
      speed: 0.86,
      scale: 1,
      distortion: 0.7,
      swirl: 0.44,
      colors: ['#222222', '#5C5C5C', '#A8A8A8', '#EFEFEF'],
      style: {
        borderRadius: '9999px',
        width: '100%',
        height: '100%',
      },
    }),
  )
}

onMounted(() => {
  if (!mountEl.value) return
  root = createRoot(mountEl.value)
  renderMesh()

  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      renderMesh()
    })
    resizeObserver.observe(mountEl.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  root?.unmount()
  root = null
})
</script>

<template>
  <div class="auth-mesh-logo" aria-hidden="true">
    <div ref="mountEl" class="auth-mesh-logo__shader" />
  </div>
</template>

<style scoped>
.auth-mesh-logo {
  --logo-size: clamp(150px, 22vw, 220px);
  width: var(--logo-size);
  height: var(--logo-size);
  border-radius: 9999px;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(88, 88, 88, 0.22);
  box-shadow:
    0 16px 34px rgba(20, 24, 28, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.62);
}

.auth-mesh-logo__shader {
  width: 100%;
  height: 100%;
  border-radius: inherit;
  overflow: hidden;
}
</style>
