<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    texts: string[]
    typingSpeed?: number
    deletingSpeed?: number
    pauseDuration?: number
    initialDelay?: number
    loop?: boolean
    showCursor?: boolean
    hideCursorWhileTyping?: boolean
    cursorCharacter?: string
  }>(),
  {
    typingSpeed: 58,
    deletingSpeed: 34,
    pauseDuration: 1400,
    initialDelay: 160,
    loop: false,
    showCursor: true,
    hideCursorWhileTyping: false,
    cursorCharacter: '|',
  },
)

const renderedText = ref('')
const currentIndex = ref(0)
const currentCharIndex = ref(0)
const isDeleting = ref(false)
const prefersReducedMotion = ref(false)

let timeoutId: ReturnType<typeof window.setTimeout> | null = null

const safeTexts = computed(() => props.texts.filter((item) => typeof item === 'string' && item.trim()))
const currentText = computed(() => safeTexts.value[currentIndex.value] ?? '')
const shouldHideCursor = computed(() => {
  if (!props.showCursor) return true
  if (!props.hideCursorWhileTyping) return false
  return currentText.value !== renderedText.value || isDeleting.value
})

function clearTimer() {
  if (timeoutId !== null) {
    window.clearTimeout(timeoutId)
    timeoutId = null
  }
}

function schedule(delay: number, callback: () => void) {
  clearTimer()
  timeoutId = window.setTimeout(callback, delay)
}

function resetMachine() {
  clearTimer()
  renderedText.value = ''
  currentIndex.value = 0
  currentCharIndex.value = 0
  isDeleting.value = false
}

function runTypeMachine() {
  const texts = safeTexts.value
  if (!texts.length) {
    renderedText.value = ''
    return
  }

  if (prefersReducedMotion.value) {
    renderedText.value = texts[0]
    return
  }

  const target = texts[currentIndex.value] ?? ''
  if (!target) {
    renderedText.value = ''
    return
  }

  if (!isDeleting.value) {
    if (currentCharIndex.value < target.length) {
      schedule(props.typingSpeed, () => {
        currentCharIndex.value += 1
        renderedText.value = target.slice(0, currentCharIndex.value)
        runTypeMachine()
      })
      return
    }

    const hasNext = currentIndex.value < texts.length - 1
    if (!props.loop && !hasNext) {
      renderedText.value = target
      return
    }

    schedule(props.pauseDuration, () => {
      isDeleting.value = true
      runTypeMachine()
    })
    return
  }

  if (renderedText.value.length > 0) {
    schedule(props.deletingSpeed, () => {
      renderedText.value = renderedText.value.slice(0, -1)
      currentCharIndex.value = renderedText.value.length
      runTypeMachine()
    })
    return
  }

  isDeleting.value = false
  currentIndex.value = (currentIndex.value + 1) % texts.length
  currentCharIndex.value = 0
  runTypeMachine()
}

function detectReducedMotion() {
  prefersReducedMotion.value =
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

onMounted(() => {
  detectReducedMotion()
  if (!safeTexts.value.length) return
  if (prefersReducedMotion.value) {
    renderedText.value = safeTexts.value[0]
    return
  }
  schedule(props.initialDelay, () => {
    runTypeMachine()
  })
})

watch(
  () => props.texts,
  () => {
    resetMachine()
    detectReducedMotion()
    if (safeTexts.value.length === 0) return
    if (prefersReducedMotion.value) {
      renderedText.value = safeTexts.value[0]
      return
    }
    schedule(props.initialDelay, () => {
      runTypeMachine()
    })
  },
  { deep: true },
)

onBeforeUnmount(() => {
  clearTimer()
})
</script>

<template>
  <span class="typed-text" aria-live="polite">
    <span class="typed-text__content">{{ renderedText }}</span>
    <span v-if="showCursor" class="typed-text__cursor" :class="{ 'is-hidden': shouldHideCursor }">
      {{ cursorCharacter }}
    </span>
  </span>
</template>

<style scoped>
.typed-text {
  --typed-cursor-gap: 0.08em;
  display: inline-flex;
  align-items: center;
  min-height: 1.15em;
  line-height: inherit;
  max-width: 100%;
  gap: var(--typed-cursor-gap);
  vertical-align: middle;
}

.typed-text__content {
  display: inline-block;
  min-width: 0;
  white-space: pre-wrap;
  text-align: inherit;
}

.typed-text__cursor {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  line-height: 1;
  user-select: none;
  pointer-events: none;
  animation: typedCursorBlink 1.35s steps(1, end) infinite;
}

.typed-text__cursor.is-hidden {
  opacity: 0;
}

@keyframes typedCursorBlink {
  0%,
  48% {
    opacity: 1;
  }

  49%,
  100% {
    opacity: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .typed-text__cursor {
    animation: none;
  }
}
</style>
