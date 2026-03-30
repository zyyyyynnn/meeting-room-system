<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRequestErrorMessage } from '../api/http'
import { apiOverviewStats } from '../api/mrs'
import type { OverviewStats } from '../api/types'
import PageStatusPanel from '../components/PageStatusPanel.vue'
import { authStore } from '../store/auth'

type ViewState = 'loading' | 'ready' | 'error'

function createEmptyStats(): OverviewStats {
  return {
    totalRooms: 0,
    totalUsers: 0,
    todayReservations: 0,
    myUpcomingReservations: 0,
    pendingApprovals: 0,
    userBreakdown: {
      normalUsers: 0,
      adminUsers: 0,
      superAdminUsers: 0,
      disabledUsers: 0,
    },
  }
}

const loading = ref(false)
const viewState = ref<ViewState>('loading')
const statusMessage = ref('')
const hasLoadedOnce = ref(false)
const stats = ref<OverviewStats>(createEmptyStats())

const router = useRouter()

const quickActions = [
  {
    title: '发起预约',
    desc: '进入日历选择时间段并快速提交预约',
    to: '/calendar',
  },
  {
    title: '我的预约',
    desc: '查看即将开始和历史预约安排',
    to: '/mine',
  },
  {
    title: '浏览会议室',
    desc: '查看会议室容量与设备配置',
    to: '/rooms',
  },
]

const userStructureItems = computed(() => [
  { label: '普通用户', value: stats.value.userBreakdown.normalUsers },
  { label: '管理员', value: stats.value.userBreakdown.adminUsers },
  { label: '超级管理员', value: stats.value.userBreakdown.superAdminUsers },
  { label: '停用账号', value: stats.value.userBreakdown.disabledUsers },
])

const isNormalUser = computed(() => authStore.state.role === 'USER')
const showBlockingState = computed(() => !hasLoadedOnce.value && viewState.value !== 'ready')
const showInlineError = computed(() => hasLoadedOnce.value && viewState.value === 'error')
const stateTitle = computed(() => (viewState.value === 'loading' ? '正在加载看板数据' : '看板数据暂时不可用'))
const stateDescription = computed(() => {
  if (viewState.value === 'loading') {
    return '正在汇总预约、会议室与审批概览，请稍候。'
  }
  return statusMessage.value || '当前无法获取看板数据，请稍后重试。'
})

function go(to: string) {
  router.push(to)
}

async function reload() {
  const preserveContent = hasLoadedOnce.value
  loading.value = true
  statusMessage.value = ''

  if (!preserveContent) {
    viewState.value = 'loading'
  }

  try {
    const resp = await apiOverviewStats()
    if (resp.code !== 0) {
      viewState.value = 'error'
      statusMessage.value = resp.message || '看板数据暂时不可用'
      if (!preserveContent) {
        stats.value = createEmptyStats()
      }
      return
    }

    stats.value = resp.data
    hasLoadedOnce.value = true
    viewState.value = 'ready'
    statusMessage.value = ''
  } catch (error) {
    viewState.value = 'error'
    statusMessage.value = getRequestErrorMessage(error, '看板数据加载失败，请稍后重试。')
    if (!preserveContent) {
      stats.value = createEmptyStats()
    }
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>

<template>
  <div class="page-wrap dashboard-wrap">
    <section class="page-hero dashboard-hero cursor-card">
      <div>
        <h2 class="page-title">运营看板</h2>
        <p class="page-subtitle">以克制的秩序呈现今日工作全貌，聚焦真正重要的信息。</p>
      </div>
      <el-button type="primary" class="btn-key-solid" :loading="loading" @click="reload">刷新</el-button>
    </section>

    <PageStatusPanel
      v-if="showBlockingState"
      :tone="viewState === 'loading' ? 'loading' : 'danger'"
      :title="stateTitle"
      :description="stateDescription"
      :action-text="viewState === 'error' ? '重新加载' : ''"
      @action="reload"
    />

    <template v-else>
      <PageStatusPanel
        v-if="showInlineError"
        tone="warning"
        title="已保留上次加载的数据"
        :description="statusMessage"
        action-text="重新加载"
        @action="reload"
      />

      <template v-if="isNormalUser">
        <section class="stats-grid normal-stats-grid">
          <article class="stat-card cursor-card compact-card tone-mine">
            <div class="k">我的待进行会议</div>
            <div class="v">{{ stats.myUpcomingReservations }}</div>
          </article>
          <article class="stat-card cursor-card compact-card tone-today">
            <div class="k">今日预约总量</div>
            <div class="v">{{ stats.todayReservations }}</div>
          </article>
          <article class="stat-card cursor-card compact-card tone-room">
            <div class="k">可预约会议室</div>
            <div class="v">{{ stats.totalRooms }}</div>
          </article>
        </section>

        <section class="cursor-card quick-actions-card">
          <div class="section-head">
            <div class="section-title">快捷入口</div>
          </div>
          <div class="quick-actions-grid">
            <article
              class="quick-action"
              role="button"
              tabindex="0"
              v-for="item in quickActions"
              :key="item.to"
              @click="go(item.to)"
              @keydown.enter.prevent="go(item.to)"
              @keydown.space.prevent="go(item.to)"
            >
              <div class="qa-title">{{ item.title }}</div>
              <div class="qa-desc">{{ item.desc }}</div>
              <div class="qa-footer">
                <el-button size="small" type="primary" class="btn-key-solid" @click.stop="go(item.to)">进入</el-button>
              </div>
            </article>
          </div>
        </section>
      </template>

      <template v-else>
        <section class="stats-grid admin-kpi-grid">
          <article class="stat-card cursor-card compact-card tone-room">
            <div class="k">会议室总量</div>
            <div class="v">{{ stats.totalRooms }}</div>
          </article>
          <article class="stat-card cursor-card compact-card tone-today">
            <div class="k">今日预约</div>
            <div class="v">{{ stats.todayReservations }}</div>
          </article>
          <article class="stat-card cursor-card compact-card tone-mine">
            <div class="k">我的待进行会议</div>
            <div class="v">{{ stats.myUpcomingReservations }}</div>
          </article>
          <article class="stat-card cursor-card compact-card tone-pending">
            <div class="k">待审批预约</div>
            <div class="v">{{ stats.pendingApprovals }}</div>
          </article>
        </section>

        <section class="cursor-card quick-actions-card">
          <div class="section-head">
            <div class="section-title">快捷入口</div>
          </div>
          <div class="quick-actions-grid">
            <article
              class="quick-action"
              role="button"
              tabindex="0"
              v-for="item in quickActions"
              :key="item.to"
              @click="go(item.to)"
              @keydown.enter.prevent="go(item.to)"
              @keydown.space.prevent="go(item.to)"
            >
              <div class="qa-title">{{ item.title }}</div>
              <div class="qa-desc">{{ item.desc }}</div>
              <div class="qa-footer">
                <el-button size="small" type="primary" class="btn-key-solid" @click.stop="go(item.to)">进入</el-button>
              </div>
            </article>
          </div>
        </section>

        <section class="cursor-card table-card user-structure-card">
          <div class="section-head">
            <div class="section-title">用户结构</div>
          </div>
          <div class="user-breakdown-grid">
            <div class="breakdown-item" v-for="item in userStructureItems" :key="item.label">
              <span class="label">{{ item.label }}</span>
              <span class="num">{{ item.value }}</span>
            </div>
          </div>
        </section>
      </template>
    </template>
  </div>
</template>

<style scoped>
.dashboard-wrap {
  gap: 22px;
}

.dashboard-hero {
  min-height: 118px;
}

.admin-kpi-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.normal-stats-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.left-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.compact-card {
  min-height: 136px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 18px;
}

.user-structure-card {
  padding: 20px;
}

.tone-users {
  background: linear-gradient(180deg, rgba(112, 128, 144, 0.15), rgba(112, 128, 144, 0.05));
  border-color: rgba(112, 128, 144, 0.22);
}

.user-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--line-soft);
}

.user-breakdown-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(120px, 1fr));
  gap: 10px;
  flex: 1;
}

.breakdown-item {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--nested-surface-border);
  border-radius: calc(var(--radius-unified) + 2px);
  padding: 14px 14px 13px;
  background:
    linear-gradient(180deg, var(--nested-surface-top), var(--nested-surface-bottom)),
    var(--bg-card);
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-height: 84px;
  justify-content: center;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.breakdown-item::after {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  top: 0;
  height: 1px;
  background: linear-gradient(90deg, var(--nested-surface-rule), transparent);
}

.breakdown-item .label {
  font-size: 12px;
  color: var(--text-muted);
}

.breakdown-item .num {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
  line-height: 1.1;
}

.quick-actions-card {
  padding: 20px;
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.quick-action {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--nested-surface-border);
  border-radius: calc(var(--radius-unified) + 2px);
  background:
    linear-gradient(180deg, var(--nested-surface-top), var(--nested-surface-bottom)),
    var(--bg-card);
  color: var(--text-main);
  text-align: left;
  padding: 16px;
  min-height: 146px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    var(--nested-surface-shadow);
  transition:
    transform var(--motion-fast) var(--motion-ease-emphasis),
    box-shadow var(--motion-base) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out),
    background-color var(--motion-fast) var(--motion-ease-out);
}

.quick-action::after {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 0;
  height: 2px;
  border-radius: 999px;
  background: rgba(78, 86, 96, 0.3);
  transform: scaleX(0);
  transform-origin: center;
  transition: transform var(--motion-base) var(--motion-ease-emphasis);
}

.quick-action:hover {
  transform: translate3d(0, -2px, 0);
  border-color: rgba(31, 31, 31, 0.16);
  background:
    linear-gradient(180deg, rgba(252, 254, 255, 0.96), var(--nested-surface-bottom-strong)),
    var(--bg-card);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.76),
    0 10px 22px rgba(20, 24, 28, 0.08);
}

.quick-action:hover::after {
  transform: scaleX(1);
}

.quick-action:active {
  transform: translate3d(0, 0, 0) scale(0.99);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.7),
    0 4px 10px rgba(20, 24, 28, 0.08);
}

.quick-action:focus-visible {
  outline: none;
  border-color: rgba(31, 31, 31, 0.42);
  box-shadow: 0 0 0 3px var(--focus-ring);
}

.quick-action:focus-visible::after {
  transform: scaleX(1);
}

.qa-footer {
  margin-top: auto;
  padding-top: 6px;
}

.qa-title {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.42;
}

.qa-desc {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.68;
}

@media (max-width: 1180px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .user-card {
    min-height: auto;
  }
}

@media (max-width: 980px) {
  .normal-stats-grid {
    grid-template-columns: 1fr;
  }

  .quick-actions-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .left-grid {
    grid-template-columns: 1fr;
  }

  .compact-card {
    min-height: 112px;
  }

  .user-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }

  .user-breakdown-grid {
    grid-template-columns: 1fr;
  }
}
</style>
