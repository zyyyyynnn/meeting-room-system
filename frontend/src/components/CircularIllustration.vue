<script setup lang="ts">
withDefaults(
  defineProps<{
    mode?: 'auth' | 'app'
    variant?: 'calendar' | 'rooms' | 'approvals' | 'default'
  }>(),
  {
    mode: 'auth',
    variant: 'default',
  },
)
</script>

<template>
  <div class="ci-wrap" :class="[`is-${mode}`, `v-${variant}`]" aria-hidden="true">
    <svg viewBox="0 0 420 420" class="ci-svg" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="ringMain" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#262b29" stop-opacity="0.42" />
          <stop offset="100%" stop-color="#5e8072" stop-opacity="0.26" />
        </linearGradient>
        <linearGradient id="ringSub" x1="0" y1="1" x2="1" y2="0">
          <stop offset="0%" stop-color="#1f2422" stop-opacity="0.36" />
          <stop offset="100%" stop-color="#739487" stop-opacity="0.2" />
        </linearGradient>
      </defs>

      <circle cx="210" cy="210" r="168" class="base" />
      <circle cx="210" cy="210" r="142" class="ring" stroke="url(#ringMain)" />
      <circle cx="210" cy="210" r="108" class="ring" stroke="url(#ringSub)" />

      <g v-if="variant === 'calendar'">
        <circle cx="210" cy="210" r="74" class="core" />
        <circle cx="210" cy="210" r="46" class="sub-ring" />
        <circle cx="286" cy="132" r="20" class="dot dot-strong" />
        <circle cx="130" cy="282" r="16" class="dot" />
        <path class="orbit" d="M88 205c0-68 54-123 122-123" />
        <path class="orbit" d="M332 216c0 68-54 123-122 123" />
      </g>

      <g v-else-if="variant === 'rooms'">
        <circle cx="210" cy="210" r="66" class="core" />
        <circle cx="158" cy="206" r="24" class="dot" />
        <circle cx="212" cy="206" r="24" class="dot dot-strong" />
        <circle cx="266" cy="206" r="24" class="dot" />
        <circle cx="184" cy="264" r="18" class="dot" />
        <circle cx="240" cy="264" r="18" class="dot" />
        <path class="orbit" d="M120 152a110 110 0 0 1 180 0" />
      </g>

      <g v-else-if="variant === 'approvals'">
        <circle cx="210" cy="210" r="70" class="core" />
        <circle cx="210" cy="188" r="18" class="dot dot-strong" />
        <circle cx="184" cy="235" r="14" class="dot" />
        <circle cx="236" cy="235" r="14" class="dot" />
        <path class="orbit" d="M148 212l42 36 78-90" />
        <circle cx="300" cy="140" r="14" class="dot" />
      </g>

      <g v-else>
        <circle cx="210" cy="210" r="72" class="core" />
        <circle cx="286" cy="126" r="22" class="dot dot-strong" />
        <circle cx="126" cy="286" r="18" class="dot" />
        <circle cx="314" cy="268" r="12" class="dot" />
        <path class="orbit" d="M84 205c0-69 56-125 125-125" />
        <path class="orbit" d="M336 215c0 69-56 125-125 125" />
      </g>
    </svg>
  </div>
</template>

<style scoped>
.ci-wrap {
  width: min(440px, 100%);
  aspect-ratio: 1 / 1;
  border-radius: 18px;
  border: 1px solid rgba(24, 28, 27, 0.28);
  background: radial-gradient(circle at 28% 26%, rgba(94, 128, 114, 0.18) 0, rgba(28, 33, 31, 0.08) 44%),
    rgba(245, 241, 233, 0.96);
  display: grid;
  place-items: center;
}

.ci-svg {
  width: 84%;
  height: 84%;
}

.base {
  fill: rgba(22, 26, 25, 0.2);
}

.ring {
  fill: none;
  stroke-width: 2.8;
}

.sub-ring {
  fill: none;
  stroke: rgba(26, 31, 29, 0.56);
  stroke-width: 2.1;
}

.core {
  fill: rgba(24, 29, 27, 0.3);
  stroke: rgba(94, 128, 114, 0.48);
  stroke-width: 2;
}

.dot {
  fill: rgba(23, 27, 26, 0.32);
  stroke: rgba(23, 27, 26, 0.62);
  stroke-width: 1.6;
}

.dot-strong {
  fill: rgba(94, 128, 114, 0.3);
  stroke: rgba(94, 128, 114, 0.56);
}

.orbit {
  fill: none;
  stroke: rgba(22, 27, 25, 0.62);
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-dasharray: 6 8;
}

.is-app {
  width: min(220px, 100%);
}
</style>
