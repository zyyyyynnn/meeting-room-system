<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiOverviewStats } from '../api/mrs'
import type { OverviewStats } from '../api/types'
import { authStore } from '../store/auth'

const loading = ref(false)
const stats = ref<OverviewStats>({
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
})

const router = useRouter()

const quickActions = [
  {
    title: '发起预约',
    desc: '进入日历选择时间段快速预约',
    to: '/calendar',
  },
  {
    title: '查看我的预约',
    desc: '确认即将开始的会议安排',
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

function go(to: string) {
  router.push(to)
}

const isNormalUser = computed(() => authStore.state.role === 'USER')

async function reload() {
  loading.value = true
  try {
    const resp = await apiOverviewStats()
    if (resp.code !== 0) {
      ElMessage.error(resp.message)
      return
    }
    stats.value = resp.data
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
      <el-button type="primary" class="btn-key-solid" :loading="loading" @click="reload">刷新数据</el-button>
    </section>

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
          <div class="section-desc">为普通用户保留高频操作，减少无关信息干扰。</div>
        </div>
        <div class="quick-actions-grid">
          <button class="quick-action" v-for="item in quickActions" :key="item.to" @click="go(item.to)">
            <div class="qa-title">{{ item.title }}</div>
            <div class="qa-desc">{{ item.desc }}</div>
            <div class="qa-footer">
              <el-button size="small" type="primary" class="btn-key-solid" @click.stop="go(item.to)">进入</el-button>
            </div>
          </button>
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
          <div class="section-desc">优先展示高频管理动作，减少路径跳转成本。</div>
        </div>
        <div class="quick-actions-grid">
          <button class="quick-action" v-for="item in quickActions" :key="item.to" @click="go(item.to)">
            <div class="qa-title">{{ item.title }}</div>
            <div class="qa-desc">{{ item.desc }}</div>
            <div class="qa-footer">
              <el-button size="small" type="primary" class="btn-key-solid" @click.stop="go(item.to)">进入</el-button>
            </div>
          </button>
        </div>
      </section>

      <section class="cursor-card table-card user-structure-card">
        <div class="section-head">
          <div class="section-title">用户结构</div>
          <div class="section-desc">系统用户总数 {{ stats.totalUsers }}，便于跟踪权限结构健康度。</div>
        </div>
        <div class="user-breakdown-grid">
          <div class="breakdown-item" v-for="item in userStructureItems" :key="item.label">
            <span class="label">{{ item.label }}</span>
            <span class="num">{{ item.value }}</span>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.dashboard-wrap {
  gap: 22px;
}

.dashboard-hero {
  border: 1px solid var(--line-soft);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.6), rgba(255, 255, 255, 0.3));
}

.btn-key-solid {
  --el-button-bg-color: var(--accent) !important;
  --el-button-border-color: var(--accent) !important;
  --el-button-text-color: var(--bg-card-strong) !important;
  --el-button-hover-bg-color: var(--accent-strong) !important;
  --el-button-hover-border-color: var(--accent-strong) !important;
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
  min-height: 132px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 16px 18px;
}

.user-structure-card {
  padding: 20px;
}

.tone-room,
.tone-today,
.tone-mine,
.tone-pending {
  background: rgba(255, 255, 255, 0.42);
  border-color: var(--line-soft);
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
  padding-bottom: 10px;
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
  border: 1px solid var(--line-soft);
  border-radius: 10px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.45);
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-height: 84px;
  justify-content: center;
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
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.48);
  color: var(--text-main);
  text-align: left;
  padding: 14px 16px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quick-action:hover {
  border-color: rgba(63, 83, 103, 0.35);
  background: rgba(63, 83, 103, 0.08);
}

.qa-footer {
  margin-top: 4px;
}

.btn-key-solid {
  --el-button-bg-color: var(--accent) !important;
  --el-button-border-color: var(--accent) !important;
  --el-button-text-color: #f7f9fc !important;
  --el-button-hover-bg-color: var(--accent-strong) !important;
  --el-button-hover-border-color: var(--accent-strong) !important;
  --el-button-active-bg-color: #2e4356 !important;
  --el-button-active-border-color: #2e4356 !important;
}

.qa-title {
  font-size: 16px;
  font-weight: 700;
}

.qa-desc {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
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
