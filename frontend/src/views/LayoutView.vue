<script setup lang="ts">
import { computed, ref } from 'vue'
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

const noticeCount = computed(() => notifications.value.length)

async function openNotifications() {
  noticeDrawerOpen.value = true
  noticeLoading.value = true
  try {
    const resp = await apiNotifications()
    if (resp.code !== 0) {
      ElMessage.error(resp.message)
      return
    }
    notifications.value = Array.isArray(resp.data) ? resp.data : []
  } finally {
    noticeLoading.value = false
  }
}

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

    <button class="notice-float-btn" type="button" @click="openNotifications" aria-label="Open notifications">
      <el-icon class="notice-float-btn__icon"><Bell /></el-icon>
      <span v-if="noticeCount" class="notice-float-btn__badge">{{ noticeCount }}</span>
    </button>

    <el-drawer v-model="noticeDrawerOpen" title="通知中心" size="min(440px, 92vw)">
      <div v-loading="noticeLoading" class="notice-list">
        <div class="notice-panel-head">
          <div>
            <div class="notice-panel-title">最新提醒</div>
            <div class="notice-panel-subtitle">预约状态、审批结果和系统消息会集中展示在这里。</div>
          </div>
          <div class="notice-panel-count">{{ noticeCount }} 条</div>
        </div>

        <el-empty v-if="!notifications.length && !noticeLoading" description="暂无通知" />

        <div v-else class="notice-items">
          <article class="notice-item" v-for="(item, idx) in notifications" :key="`notice-${idx}`">
            <span class="notice-item__index">{{ idx + 1 }}</span>
            <span class="notice-item__text">{{ item }}</span>
          </article>
        </div>
      </div>
    </el-drawer>

    <div class="workspace-body">
      <main class="workspace-main cursor-main-panel">
        <router-view v-slot="{ Component, route: childRoute }">
          <transition name="workspace-view" mode="out-in" appear>
            <div :key="childRoute.fullPath" class="workspace-page">
              <component :is="Component" />
            </div>
          </transition>
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
    linear-gradient(145deg, rgba(255, 255, 255, 0.88) 0%, rgba(245, 248, 250, 0.8) 52%, rgba(238, 242, 245, 0.72) 100%),
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
    transform var(--motion-fast) var(--motion-ease-emphasis),
    box-shadow var(--motion-base) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out),
    background-color var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
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
  transition: transform var(--motion-base) var(--motion-ease-emphasis);
  opacity: 0.82;
}

.nav-chip:hover {
  transform: translate3d(0, -2px, 0);
  border-color: var(--line-strong);
  box-shadow: 0 5px 12px rgba(20, 24, 28, 0.08);
}

.nav-chip:active {
  transform: translate3d(0, 0, 0) scale(0.99);
  box-shadow: 0 2px 6px rgba(20, 24, 28, 0.08);
}

.nav-chip.active {
  background: var(--accent);
  color: var(--bg-card-strong);
  border-color: var(--accent);
  box-shadow: 0 8px 18px rgba(20, 24, 28, 0.16);
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
    transform var(--motion-fast) var(--motion-ease-emphasis),
    box-shadow var(--motion-base) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out),
    background-color var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.logout-btn:hover {
  transform: translate3d(0, -2px, 0);
  border-color: var(--line-strong);
  box-shadow: 0 5px 12px rgba(20, 24, 28, 0.08);
}

.logout-btn:active {
  transform: translate3d(0, 0, 0) scale(0.99);
  box-shadow: 0 2px 6px rgba(20, 24, 28, 0.08);
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
  width: 54px;
  height: 54px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-unified);
  background: var(--bg-card);
  box-shadow:
    0 10px 24px rgba(15, 23, 42, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  color: var(--text-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition:
    transform var(--motion-fast) var(--motion-ease-emphasis),
    box-shadow var(--motion-base) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out),
    background-color var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.notice-float-btn:hover {
  transform: translate3d(-2px, calc(var(--notice-float-offset) - 2px), 0);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.14);
  border-color: var(--accent);
  background: var(--accent);
  color: var(--bg-card-strong);
}

.notice-float-btn:active {
  transform: translate3d(0, var(--notice-float-offset), 0) scale(0.985);
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.12);
}

.notice-float-btn__icon {
  font-size: 21px;
  color: currentColor;
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

.notice-list {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.notice-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--nested-surface-border);
  border-radius: calc(var(--radius-unified) + 2px);
  background:
    linear-gradient(180deg, var(--nested-surface-top), var(--nested-surface-bottom)),
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
  margin-top: 4px;
  font-size: 14px;
  line-height: 1.68;
  color: var(--text-weak);
}

.notice-panel-count {
  padding: 6px 10px;
  border-radius: calc(var(--radius-unified) + 2px);
  border: 1px solid var(--nested-surface-border);
  background: linear-gradient(180deg, var(--nested-surface-top), var(--nested-surface-bottom));
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.notice-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice-item {
  position: relative;
  display: grid;
  grid-template-columns: 34px 1fr;
  gap: 14px;
  align-items: flex-start;
  border: 1px solid var(--nested-surface-border);
  border-radius: calc(var(--radius-unified) + 2px);
  padding: 14px 15px 14px 14px;
  background:
    linear-gradient(180deg, var(--nested-surface-top), var(--nested-surface-bottom)),
    var(--bg-card);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
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
  border: 1px solid var(--nested-surface-border);
  background: linear-gradient(180deg, var(--nested-surface-top), var(--nested-surface-bottom));
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
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
  overscroll-behavior: contain;
  padding: 24px 24px 0;
  scroll-behavior: smooth;
}

.workspace-page {
  min-height: 100%;
}

.workspace-view-enter-active {
  transition:
    opacity 380ms var(--motion-ease-emphasis),
    transform 380ms var(--motion-ease-emphasis);
}

.workspace-view-leave-active {
  transition:
    opacity 220ms var(--motion-ease-out),
    transform 220ms var(--motion-ease-out);
}

.workspace-view-enter-from,
.workspace-view-leave-to {
  opacity: 0;
  transform: translate3d(0, 14px, 0);
}

.workspace-view-enter-to,
.workspace-view-leave-from {
  opacity: 1;
  transform: translate3d(0, 0, 0);
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
    width: 48px;
    height: 48px;
  }

  .notice-float-btn__icon {
    font-size: 19px;
  }
}
</style>
