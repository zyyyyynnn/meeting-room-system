<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  Calendar,
  DataBoard,
  Files,
  HomeFilled,
  Key,
  Operation,
  SwitchButton,
} from '@element-plus/icons-vue'
import { apiNotifications } from '../api/mrs'
import AuthMeshLogo from '../components/AuthMeshLogo.vue'
import { authStore } from '../store/auth'

const router = useRouter()
const route = useRoute()

const active = computed(() => route.path)
const noticeDrawerOpen = ref(false)
const notifications = ref<string[]>([])
const noticeLoading = ref(false)
const blockedNoticeMarkers = ['示例数据已生成', '本周已补充预约', '界面联调', '运营看板、审批与趋势图层']
const meetingNoticeMarkers = ['预约', '会议', '审批', '维护', '会议室', '房间', '参会']

const noticeCount = computed(() => notifications.value.length)
const noticeStats = computed(() => {
  const items = notifications.value
  const reservation = items.filter((item) => /预约|会议/.test(item)).length
  const approval = items.filter((item) => /审批|通过|驳回|拒绝/.test(item)).length
  return {
    total: items.length,
    reservation,
    approval,
    system: Math.max(0, items.length - reservation - approval),
  }
})
const latestNotice = computed(() => notifications.value[0] ?? '暂无通知，新的预约提醒、审批回执和系统消息会显示在这里。')

function shouldDisplayNotice(item: string) {
  const text = String(item ?? '').trim()
  if (!text) return false
  if (blockedNoticeMarkers.some((marker) => text.includes(marker))) {
    return false
  }
  return meetingNoticeMarkers.some((marker) => text.includes(marker))
}

function sanitizeNotifications(items: string[]) {
  return items.filter(shouldDisplayNotice)
}

async function openNotifications() {
  noticeDrawerOpen.value = true
  noticeLoading.value = true
  try {
    const resp = await apiNotifications()
    if (resp.code !== 0) {
      ElMessage.error(resp.message)
      return
    }
    notifications.value = sanitizeNotifications(Array.isArray(resp.data) ? resp.data : [])
  } finally {
    noticeLoading.value = false
  }
}

function closeNotifications() {
  noticeDrawerOpen.value = false
  if (route.query.panel === 'notifications') {
    const { panel, ...rest } = route.query
    router.replace({ path: route.path, query: rest })
  }
}

watch(
  () => route.query.panel,
  (panel) => {
    if (panel === 'notifications') {
      void openNotifications()
    }
  },
  { immediate: true },
)

function logout() {
  authStore.clear()
  router.push('/login')
}
</script>

<template>
  <div class="workspace-shell cursor-layout">
    <header class="workspace-top">
      <div class="top-brand">
        <div class="brand-mark">
          <AuthMeshLogo />
        </div>
        <div>
          <div class="brand-title">会议室预约</div>
          <div class="brand-sub">Meeting Room System</div>
        </div>
      </div>

      <nav class="top-nav">
        <button class="nav-chip" :class="{ active: active.startsWith('/dashboard') }" @click="$router.push('/dashboard')">
          <el-icon><DataBoard /></el-icon>
          <span>运营看板</span>
        </button>
        <button class="nav-chip" :class="{ active: active.startsWith('/calendar') }" @click="$router.push('/calendar')">
          <el-icon><Calendar /></el-icon>
          <span>会议预约</span>
        </button>
        <button class="nav-chip" :class="{ active: active.startsWith('/mine') }" @click="$router.push('/mine')">
          <el-icon><Files /></el-icon>
          <span>我的预约</span>
        </button>
        <button class="nav-chip" :class="{ active: active.startsWith('/rooms') }" @click="$router.push('/rooms')">
          <el-icon><HomeFilled /></el-icon>
          <span>会议室</span>
        </button>
        <button
          v-if="authStore.isAdmin.value"
          class="nav-chip"
          :class="{ active: active.startsWith('/admin/approvals') }"
          @click="$router.push('/admin/approvals')"
        >
          <el-icon><Operation /></el-icon>
          <span>预约审批</span>
        </button>
        <button
          v-if="authStore.isAdmin.value"
          class="nav-chip"
          :class="{ active: active.startsWith('/admin/users') }"
          @click="$router.push('/admin/users')"
        >
          <el-icon><Key /></el-icon>
          <span>用户管理</span>
        </button>
      </nav>

      <div class="top-actions">
        <div class="header-user">
          <span class="user-name">{{ authStore.state.username }}</span>
          <span class="user-role">
            {{
              authStore.state.role === 'SUPER_ADMIN'
                ? '超级管理员'
                : authStore.state.role === 'ADMIN'
                  ? '管理员'
                  : '普通用户'
            }}
          </span>
        </div>
        <el-button class="logout-btn" @click="logout" text>
          <el-icon><SwitchButton /></el-icon>
          <span>退出</span>
        </el-button>
      </div>
    </header>

    <button class="notice-float-btn" type="button" @click="openNotifications" aria-label="打开通知抽屉">
      <el-icon class="notice-float-btn__icon"><Bell /></el-icon>
      <span class="notice-float-btn__text">通知</span>
      <span v-if="noticeCount" class="notice-float-btn__badge">{{ noticeCount }}</span>
    </button>

    <el-drawer v-model="noticeDrawerOpen" title="通知" size="min(460px, 92vw)" @close="closeNotifications">
      <div v-loading="noticeLoading" class="notice-drawer">
        <section class="notice-drawer__hero">
          <div class="notice-drawer__eyebrow">Notification Hub</div>
          <div class="notice-drawer__title">通知中心</div>
          <div class="notice-drawer__count">{{ noticeStats.total }}</div>
          <div class="notice-drawer__metrics">
            <span class="notice-drawer__metric">预约 {{ noticeStats.reservation }}</span>
            <span class="notice-drawer__metric">审批 {{ noticeStats.approval }}</span>
            <span class="notice-drawer__metric">系统 {{ noticeStats.system }}</span>
          </div>
          <div class="notice-drawer__latest">{{ latestNotice }}</div>
        </section>

        <section class="notice-panel-head">
          <div>
            <div class="notice-panel-title">最近通知</div>
            <div class="notice-panel-subtitle">预约状态、审批结果和系统消息统一沉淀在这里，随时从右侧拉出查看。</div>
          </div>
          <div class="notice-panel-count">{{ noticeCount }} 条</div>
        </section>

        <el-empty v-if="!notifications.length && !noticeLoading" description="暂无通知" />

        <div v-else class="notice-items">
          <article class="notice-item" :class="{ 'is-latest': idx === 0 }" v-for="(item, idx) in notifications" :key="`notice-${idx}`">
            <span class="notice-item__index">{{ idx + 1 }}</span>
            <div class="notice-item__body">
              <div class="notice-item__head">
                <span class="notice-item__badge">{{ idx === 0 ? '最新' : '记录' }}</span>
              </div>
              <span class="notice-item__text">{{ item }}</span>
            </div>
          </article>
        </div>
      </div>
    </el-drawer>

    <div class="workspace-body">
      <main class="workspace-main cursor-main-panel">
        <router-view v-slot="{ Component, route: childRoute }">
          <keep-alive>
            <component :is="Component" :key="childRoute.path" class="workspace-page" />
          </keep-alive>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.workspace-shell {
  min-height: 100vh;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  background: transparent;
}

.workspace-shell::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background:
    radial-gradient(circle at 14% 20%, var(--auth-shell-accent-1) 0, transparent 40%),
    radial-gradient(circle at 86% 80%, var(--auth-shell-accent-2) 0, transparent 44%),
    linear-gradient(165deg, var(--auth-shell-glow), transparent 56%);
}

.workspace-top {
  height: 76px;
  padding: 0 30px;
  border-bottom: 1px solid var(--line-soft);
  background:
    linear-gradient(145deg, var(--surface-hero-top) 0%, var(--surface-hero-bottom) 100%),
    var(--bg-card);
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  grid-template-areas: 'brand nav actions';
  align-items: center;
  gap: 18px;
  position: sticky;
  top: 0;
  z-index: 20;
  box-shadow:
    0 6px 14px rgba(20, 24, 28, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.workspace-top::after {
  content: '';
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: -1px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(78, 86, 96, 0.22), transparent);
}

.top-brand {
  grid-area: brand;
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-unified);
  display: grid;
  place-items: center;
  border: 1px solid var(--line-soft);
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 22%, rgba(255, 255, 255, 0.56), transparent 48%),
    linear-gradient(150deg, rgba(94, 108, 122, 0.12), rgba(255, 255, 255, 0.42));
}

.brand-mark :deep(.auth-mesh-logo) {
  --logo-size: 30px;
  border: none;
  box-shadow: none;
}

.brand-title {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--text-main);
  white-space: nowrap;
}

.brand-sub {
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--text-weak);
  white-space: nowrap;
}

.top-nav {
  grid-area: nav;
  min-width: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 6px;
  scrollbar-width: thin;
  scrollbar-color: rgba(78, 86, 96, 0.28) transparent;
}

.top-nav::-webkit-scrollbar {
  height: 6px;
}

.top-nav::-webkit-scrollbar-track {
  background: transparent;
}

.top-nav::-webkit-scrollbar-thumb {
  background: rgba(78, 86, 96, 0.28);
  border-radius: 999px;
}

.nav-chip {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.02em;
  height: 40px;
  min-width: 126px;
  border-radius: var(--radius-unified);
  border: 1px solid var(--line-soft);
  background: var(--bg-card);
  color: var(--text-muted);
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition:
    transform var(--motion-feedback) var(--motion-ease-out),
    box-shadow var(--motion-hover) var(--motion-ease-out),
    border-color var(--motion-feedback) var(--motion-ease-out),
    background-color var(--motion-feedback) var(--motion-ease-out),
    color var(--motion-feedback) var(--motion-ease-out);
  scroll-snap-align: start;
}

.nav-chip::after {
  content: '';
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 6px;
  height: 2px;
  border-radius: 999px;
  background: currentColor;
  transform: scaleX(0);
  transform-origin: center;
  transition: transform var(--motion-hover) var(--motion-ease-out);
  opacity: 0.82;
}

.nav-chip:hover {
  transform: translate3d(0, -0.5px, 0);
  border-color: var(--line-strong);
  background: rgba(245, 245, 245, 0.92);
  box-shadow: 0 3px 8px rgba(20, 24, 28, 0.06);
}

.nav-chip:active {
  transform: translate3d(0, 0, 0) scale(0.99);
  box-shadow: 0 2px 5px rgba(20, 24, 28, 0.06);
}

.nav-chip.active {
  background: var(--accent);
  color: var(--bg-card-strong);
  border-color: var(--accent);
  box-shadow: 0 6px 14px rgba(20, 24, 28, 0.12);
}

.nav-chip.active::after {
  transform: scaleX(1);
}

.top-actions {
  grid-area: actions;
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 10px;
  justify-self: end;
}

.header-user {
  font-family: var(--font-display);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 12px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-unified);
  background: var(--bg-card);
  box-sizing: border-box;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.user-role {
  font-size: 13px;
  font-weight: 500;
  letter-spacing: -0.01em;
  color: var(--text-weak);
}

.logout-btn {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.02em;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 12px;
  border-radius: var(--radius-unified);
  border: 1px solid var(--line-soft);
  background: var(--bg-card);
  box-sizing: border-box;
  transition:
    transform var(--motion-feedback) var(--motion-ease-out),
    box-shadow var(--motion-hover) var(--motion-ease-out),
    border-color var(--motion-feedback) var(--motion-ease-out),
    background-color var(--motion-feedback) var(--motion-ease-out),
    color var(--motion-feedback) var(--motion-ease-out);
}

.logout-btn:hover {
  transform: translate3d(0, -0.5px, 0);
  border-color: var(--line-strong);
  background: rgba(245, 245, 245, 0.92);
  box-shadow: 0 3px 8px rgba(20, 24, 28, 0.06);
}

.logout-btn:active {
  transform: translate3d(0, 0, 0) scale(0.99);
  box-shadow: 0 2px 5px rgba(20, 24, 28, 0.06);
}

.nav-chip:focus-visible,
.logout-btn:focus-visible,
.notice-float-btn:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px var(--focus-ring);
}

.notice-float-btn {
  --notice-float-offset: -50%;
  position: fixed;
  right: 16px;
  top: 50%;
  transform: translate3d(0, var(--notice-float-offset), 0);
  z-index: 30;
  min-width: 64px;
  height: 54px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-unified);
  background:
    linear-gradient(180deg, var(--surface-nested-top), var(--surface-nested-bottom)),
    var(--bg-card);
  box-shadow:
    var(--surface-nested-shadow),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  color: var(--text-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
  transition:
    transform var(--motion-feedback) var(--motion-ease-out),
    box-shadow var(--motion-hover) var(--motion-ease-out),
    border-color var(--motion-feedback) var(--motion-ease-out),
    background-color var(--motion-feedback) var(--motion-ease-out),
    color var(--motion-feedback) var(--motion-ease-out);
}

.notice-float-btn:hover {
  transform: translate3d(0, calc(var(--notice-float-offset) - 0.5px), 0);
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.08);
  border-color: var(--line-strong);
  background:
    linear-gradient(180deg, rgba(252, 252, 252, 0.98), rgba(239, 239, 239, 0.92)),
    var(--bg-card);
  color: var(--text-main);
}

.notice-float-btn:active {
  transform: translate3d(0, var(--notice-float-offset), 0) scale(0.985);
  box-shadow: 0 3px 8px rgba(15, 23, 42, 0.08);
}

.notice-float-btn__icon {
  font-size: 18px;
  color: currentColor;
}

.notice-float-btn__text {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: 1;
}

.notice-float-btn__badge {
  position: absolute;
  right: -4px;
  top: -4px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: var(--radius-unified);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--accent);
  color: var(--bg-card-strong);
  font-size: 11px;
  font-weight: 700;
}

.notice-drawer {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notice-drawer__hero {
  border: 1px solid var(--surface-hero-border);
  border-radius: calc(var(--radius-unified) + 4px);
  padding: 20px;
  background:
    radial-gradient(circle at 16% 18%, rgba(255, 255, 255, 0.58), transparent 34%),
    linear-gradient(180deg, var(--surface-hero-top), var(--surface-hero-bottom));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.76),
    var(--surface-hero-shadow);
}

.notice-drawer__eyebrow {
  font-size: 11px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--text-weak);
}

.notice-drawer__title {
  margin-top: 6px;
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.03em;
  color: var(--text-main);
}

.notice-drawer__count {
  margin-top: 10px;
  font-family: var(--font-numeric);
  font-size: 38px;
  font-weight: 700;
  line-height: 1;
  color: var(--text-main);
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum' 1;
}

.notice-drawer__metrics {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.notice-drawer__metric {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--surface-nested-border);
  background: linear-gradient(180deg, var(--surface-nested-top), var(--surface-nested-bottom));
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.notice-drawer__latest {
  margin-top: 12px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.74;
}

.notice-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 58px;
  padding: 14px 16px;
  border: 1px solid var(--surface-nested-border);
  border-radius: calc(var(--radius-unified) + 2px);
  background:
    linear-gradient(180deg, var(--surface-nested-top), var(--surface-nested-bottom)),
    var(--bg-card);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.notice-panel-title {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--text-main);
}

.notice-panel-subtitle {
  display: none;
}

.notice-panel-count {
  padding: 6px 10px;
  border-radius: calc(var(--radius-unified) + 2px);
  border: 1px solid var(--surface-nested-border);
  background: linear-gradient(180deg, var(--surface-nested-top), var(--surface-nested-bottom));
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.notice-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-item {
  position: relative;
  display: grid;
  grid-template-columns: 34px 1fr;
  gap: 12px;
  align-items: flex-start;
  border: 1px solid var(--surface-nested-border);
  border-radius: calc(var(--radius-unified) + 2px);
  padding: 14px 16px 14px 14px;
  background:
    linear-gradient(180deg, var(--surface-nested-top), var(--surface-nested-bottom)),
    var(--bg-card);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.notice-item.is-latest {
  border-color: rgba(55, 63, 72, 0.14);
  background: linear-gradient(180deg, rgba(252, 252, 252, 0.96), var(--nested-surface-bottom-strong));
}

.notice-item::after {
  content: '';
  position: absolute;
  left: 18px;
  right: 18px;
  top: 0;
  height: 1px;
  background: linear-gradient(90deg, var(--nested-surface-rule), transparent);
}

.notice-item__index {
  width: 34px;
  height: 34px;
  border-radius: calc(var(--radius-unified) + 2px);
  display: grid;
  place-items: center;
  border: 1px solid var(--surface-nested-border);
  background: linear-gradient(180deg, var(--surface-nested-top), var(--surface-nested-bottom));
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.notice-item__body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notice-item__head {
  display: flex;
  align-items: center;
  min-height: 24px;
}

.notice-item__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(31, 31, 31, 0.06);
  border: 1px solid rgba(31, 31, 31, 0.1);
  color: var(--text-main);
  font-size: 11px;
  font-weight: 700;
}

.notice-item__text {
  color: var(--text-muted);
  font-size: 14px;
  line-height: 1.72;
}

.workspace-body {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
  padding: 0 0 24px;
  position: relative;
  z-index: 1;
}

.workspace-main {
  flex: 1;
  min-height: 0;
  min-width: 0;
  position: relative;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-gutter: stable;
  overscroll-behavior: contain;
  padding: 24px 24px 0;
  scroll-behavior: auto;
}

.workspace-page {
  min-height: 100%;
}

.workspace-main :deep(.page-wrap) {
  max-width: 1120px;
  margin: 0 auto;
}

@media (max-width: 1260px) {
  .workspace-top {
    padding: 0 16px;
    gap: 10px;
  }

  .top-nav {
    justify-content: flex-start;
    scroll-snap-type: x proximity;
  }

  .nav-chip {
    min-width: 116px;
    padding: 0 12px;
  }
}

@media (max-width: 980px) {
  .workspace-top {
    height: auto;
    padding: 10px 12px 12px;
    grid-template-columns: 1fr auto;
    grid-template-areas:
      'brand actions'
      'nav nav';
    align-items: start;
    gap: 10px;
  }

  .top-brand {
    min-width: 0;
  }

  .top-actions {
    justify-self: end;
  }

  .top-nav {
    width: 100%;
    padding: 2px 2px 6px;
  }

  .workspace-body {
    padding: 0 0 16px;
  }

  .workspace-main {
    padding: 14px 12px 0;
  }
}

@media (max-width: 1100px) {
  .top-actions {
    gap: 8px;
  }

  .header-user {
    padding: 5px 8px;
  }

  .top-nav {
    justify-content: flex-start;
  }
}

@media (max-width: 760px) {
  .brand-sub {
    display: none;
  }

  .nav-chip {
    height: 36px;
    min-width: 104px;
    font-size: 13px;
  }

  .nav-chip span {
    white-space: nowrap;
  }

  .header-user {
    max-width: 150px;
    overflow: hidden;
  }

  .user-name {
    display: inline-block;
    max-width: 82px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .user-role {
    display: none;
  }
}

@media (max-width: 820px) {
  .notice-float-btn {
    right: 10px;
    min-width: 52px;
    height: 48px;
  }

  .notice-float-btn__icon {
    font-size: 17px;
  }

  .notice-float-btn__text {
    font-size: 12px;
  }
}
</style>
