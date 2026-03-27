<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import {
  Bell,
  Calendar,
  DataBoard,
  Files,
  HomeFilled,
  House,
  Key,
  Operation,
  SwitchButton,
} from '@element-plus/icons-vue'
import { apiNotifications } from '../api/mrs'
import { authStore } from '../store/auth'

const router = useRouter()
const route = useRoute()

const active = computed(() => route.path)
const noticeDrawerOpen = ref(false)
const notifications = ref<string[]>([])
const noticeLoading = ref(false)

async function openNotifications() {
  noticeDrawerOpen.value = true
  noticeLoading.value = true
  try {
    const resp = await apiNotifications()
    if (resp.code !== 0) return ElMessage.error(resp.message)
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
  <div class="workspace-shell">
    <header class="workspace-top">
      <div class="top-brand">
        <div class="brand-mark"><el-icon><House /></el-icon></div>
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
          <span class="user-role">{{ authStore.state.role === 'SUPER_ADMIN' ? '超级管理员' : authStore.state.role === 'ADMIN' ? '管理员' : '普通用户' }}</span>
        </div>
        <el-button class="logout-btn" @click="logout" text>
          <el-icon><SwitchButton /></el-icon>
          <span>退出</span>
        </el-button>
      </div>
    </header>

    <el-button class="notice-float-btn" @click="openNotifications">
      <el-icon><Bell /></el-icon>
      <span>通知</span>
    </el-button>

    <el-drawer v-model="noticeDrawerOpen" title="通知中心" size="min(420px, 92vw)">
      <div v-loading="noticeLoading" class="notice-list">
        <el-empty v-if="!notifications.length && !noticeLoading" description="暂无通知" />
        <div v-else class="notice-items">
          <div class="notice-item" v-for="(n, idx) in notifications" :key="`notice-${idx}`">{{ n }}</div>
        </div>
      </div>
    </el-drawer>

    <div class="workspace-body">
      <main class="workspace-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.workspace-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-base);
}

.workspace-top {
  height: 76px;
  padding: 0 30px;
  border-bottom: 1px solid var(--line-soft);
  background: var(--bg-elevated);
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 18px;
  position: sticky;
  top: 0;
  z-index: 20;
}

.top-brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: grid;
  place-items: center;
  border: 1px solid var(--line-soft);
  background: var(--bg-card);
}

.brand-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-main);
}

.brand-sub {
  font-size: 12px;
  color: var(--text-weak);
}

.top-slot {
  min-width: 0;
}

.notice-float-btn {
  position: fixed;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 30;
  height: 40px;
  min-width: 96px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: var(--bg-card);
  color: var(--text-main);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.notice-list {
  min-height: 220px;
}

.notice-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notice-item {
  border: 1px solid var(--line-soft);
  border-radius: 10px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.42);
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.65;
}

.top-nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  overflow-x: auto;
  padding: 6px;
}

.nav-chip {
  height: 40px;
  min-width: 126px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: var(--bg-card);
  color: var(--text-muted);
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
}

.nav-chip.active {
  background: var(--accent);
  color: var(--bg-card-strong);
  border-color: var(--accent);
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  background: var(--bg-card);
}

.user-name {
  font-size: 13px;
  font-weight: 600;
}

.user-role {
  font-size: 11px;
  color: var(--text-weak);
}

.logout-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--line-soft);
  background: var(--bg-card);
}

.workspace-body {
  flex: 1;
  min-height: 0;
  padding: 0 24px 24px;
}

.workspace-main {
  min-width: 0;
  overflow: auto;
  padding: 24px 0 0;
}

.rail-progress .rail-list {
  margin: 8px 0 0;
  padding-left: 16px;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.7;
}

.workspace-main :deep(.page-wrap) {
  max-width: 1120px;
  margin: 0 auto;
}

@media (max-width: 1100px) {
  .workspace-top {
    padding: 0 16px;
    gap: 10px;
  }

  .top-actions {
    gap: 8px;
  }

  .header-user {
    padding: 5px 8px;
  }
}
</style>
