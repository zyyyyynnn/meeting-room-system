<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getRequestErrorMessage } from '../api/http'
import { apiNotifications } from '../api/mrs'
import PageStatusPanel from '../components/PageStatusPanel.vue'

type ViewState = 'loading' | 'ready' | 'error'

const loading = ref(false)
const viewState = ref<ViewState>('loading')
const statusMessage = ref('')
const hasLoadedOnce = ref(false)
const list = ref<string[]>([])

const stats = computed(() => {
  const items = list.value
  return {
    total: items.length,
    reservation: items.filter((item) => /预约|会议/.test(item)).length,
    approval: items.filter((item) => /审批|通过|驳回|拒绝/.test(item)).length,
  }
})

const showBlockingState = computed(() => !hasLoadedOnce.value && viewState.value !== 'ready')
const showInlineError = computed(() => hasLoadedOnce.value && viewState.value === 'error')
const stateTitle = computed(() => (viewState.value === 'loading' ? '正在加载通知中心' : '通知中心暂时不可用'))
const stateDescription = computed(() => {
  if (viewState.value === 'loading') {
    return '正在同步预约提醒、审批回执与系统消息，请稍候。'
  }
  return statusMessage.value || '当前无法获取通知列表，请稍后重试。'
})

async function reload() {
  const preserveContent = hasLoadedOnce.value
  loading.value = true
  statusMessage.value = ''

  if (!preserveContent) {
    viewState.value = 'loading'
  }

  try {
    const resp = await apiNotifications()
    if (resp.code !== 0) {
      viewState.value = 'error'
      statusMessage.value = resp.message || '加载通知失败'
      if (!preserveContent) {
        list.value = []
      }
      return
    }

    list.value = Array.isArray(resp.data) ? resp.data : []
    hasLoadedOnce.value = true
    viewState.value = 'ready'
  } catch (error) {
    viewState.value = 'error'
    statusMessage.value = getRequestErrorMessage(error, '加载通知失败')
    if (!preserveContent) {
      list.value = []
    }
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>

<template>
  <div class="page-wrap">
    <section class="page-hero cursor-card">
      <div>
        <h2 class="page-title">通知中心</h2>
        <p class="page-subtitle">集中查看预约提醒、审批结果与系统消息，减少你在多个页面之间来回切换。</p>
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
        title="已保留上次同步的通知"
        :description="statusMessage"
        action-text="重新加载"
        @action="reload"
      />

      <section class="stats-grid notification-stats-grid">
        <article class="stat-card cursor-card tone-total">
          <div class="stat-label">通知总数</div>
          <div class="stat-value">{{ stats.total }}</div>
        </article>
        <article class="stat-card cursor-card tone-reservation">
          <div class="stat-label">预约相关</div>
          <div class="stat-value">{{ stats.reservation }}</div>
        </article>
        <article class="stat-card cursor-card tone-approval">
          <div class="stat-label">审批回执</div>
          <div class="stat-value">{{ stats.approval }}</div>
        </article>
      </section>

      <section class="cursor-card table-card">
        <div class="section-head notice-head">
          <div>
            <div class="section-title">最近通知</div>
            <div class="section-desc">按时间倒序展示最近消息，用于快速跟踪状态变化。</div>
          </div>
          <div class="notice-summary-pill">{{ list.length }} 条</div>
        </div>

        <div v-if="list.length" class="notice-list">
          <article class="notice-item" :class="{ 'is-latest': idx === 0 }" v-for="(item, idx) in list" :key="`notice-${idx}`">
            <span class="notice-index">{{ idx + 1 }}</span>
            <div class="notice-body">
              <div class="notice-item__head">
                <span class="notice-badge">{{ idx === 0 ? '最新' : '记录' }}</span>
              </div>
              <p class="notice-text">{{ item }}</p>
            </div>
          </article>
        </div>
        <div v-else class="empty-state">
          暂无通知，后续预约提醒、审批结果和系统消息会显示在这里。
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.notification-stats-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.tone-total,
.tone-reservation,
.tone-approval {
  min-height: 126px;
}

.notice-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.notice-summary-pill {
  flex: 0 0 auto;
  min-width: 72px;
  padding: 8px 12px;
  border-radius: calc(var(--radius-unified) + 2px);
  border: 1px solid rgba(121, 102, 82, 0.22);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(247, 242, 234, 0.72));
  color: var(--text-main);
  font-size: 12px;
  font-weight: 700;
  text-align: center;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice-item {
  position: relative;
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 15px;
  align-items: flex-start;
  border: 1px solid color-mix(in oklab, var(--line-soft), #a89478 14%);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(248, 244, 237, 0.66));
  border-radius: calc(var(--radius-unified) + 2px);
  padding: 15px 16px 15px 14px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.74),
    0 8px 18px rgba(25, 21, 17, 0.05);
}

.notice-item::after {
  content: '';
  position: absolute;
  left: 18px;
  right: 18px;
  top: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(122, 104, 82, 0.22), transparent);
}

.notice-item.is-latest {
  border-color: rgba(131, 112, 89, 0.26);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.84), rgba(247, 242, 234, 0.74));
}

.notice-index {
  width: 36px;
  height: 36px;
  border-radius: calc(var(--radius-unified) + 2px);
  border: 1px solid rgba(121, 102, 82, 0.22);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.86), rgba(247, 242, 234, 0.76));
  display: grid;
  place-items: center;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.notice-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notice-item__head {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 24px;
}

.notice-badge {
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

.notice-text {
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.72;
}

@media (max-width: 900px) {
  .notification-stats-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .notice-head {
    flex-direction: column;
    align-items: stretch;
  }

  .notice-summary-pill {
    width: 100%;
  }

  .notice-item {
    grid-template-columns: 1fr;
  }

  .notice-index {
    width: 32px;
    height: 32px;
  }
}
</style>
