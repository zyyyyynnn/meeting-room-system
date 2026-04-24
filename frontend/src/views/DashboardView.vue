<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RefreshRight } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { getRequestErrorMessage } from '../api/http'
import { apiDashboardStats } from '../api/mrs'
import type {
  DashboardHeatmapBucket,
  DashboardMetric,
  DashboardRiskDistributionItem,
  DashboardStatsResponse,
  DashboardTaskItem,
  DashboardTrendPoint,
} from '../api/types'
import PageStatusPanel from '../components/PageStatusPanel.vue'
import TypedText from '../components/TypedText.vue'
import { authStore } from '../store/auth'

type ViewState = 'loading' | 'ready' | 'error'
function fallbackRoleLabel() {
  if (authStore.state.role === 'SUPER_ADMIN') return '超级管理员'
  if (authStore.state.role === 'ADMIN') return '管理员'
  return '用户'
}

function currentUserName() {
  return authStore.state.username?.trim() || fallbackRoleLabel()
}

function createEmptyDashboardStats(): DashboardStatsResponse {
  return {
    adminView: authStore.isAdmin.value,
    welcome: {
      roleLabel: fallbackRoleLabel(),
      message: `Welcome，${currentUserName()}！`,
    },
    taskSummary: {
      title: authStore.isAdmin.value ? '今日待办与异常' : '我的今日主控',
      subtitle: authStore.isAdmin.value
        ? '聚合审批、维护、会议节奏与资源变化，帮助你更快完成今天的运营判断。'
        : '聚合个人预约、提醒与资源信号，帮助你更快完成今天的预约判断。',
      items: [],
    },
    resourceSnapshot: {
      title: authStore.isAdmin.value ? '资源态势驾驶舱' : '轻量资源态势',
      subtitle: authStore.isAdmin.value
        ? '围绕可用性、负载与维护影响做快速判断。'
        : '保留必要的全局资源信息，帮助你判断什么时间更容易预约。',
      metrics: [],
    },
    todayHeatmap: [],
    weeklyTrend: [],
    riskDistribution: [],
    quickLinkContext: [],
  }
}

const router = useRouter()
const loading = ref(false)
const viewState = ref<ViewState>('loading')
const statusMessage = ref('')
const hasLoadedOnce = ref(false)
const stats = ref<DashboardStatsResponse>(createEmptyDashboardStats())

const isAdminView = computed(() => stats.value.adminView)
const showBlockingState = computed(() => !hasLoadedOnce.value && viewState.value === 'error')
const showInlineError = computed(() => hasLoadedOnce.value && viewState.value === 'error')
const stateTitle = computed(() => '运营看板暂时不可用')
const stateDescription = computed(() => statusMessage.value || '当前无法获取运营看板数据，请稍后重试。')

const welcomeTexts = computed(() => {
  const name = currentUserName()
  return [
    `Welcome，${name}！`,
    `${name}，欢迎回到运营中枢！`,
    isAdminView.value ? `准备开始今天的调度，${name}！` : `准备开始今天的会议，${name}！`,
  ]
})
const taskItems = computed<DashboardTaskItem[]>(() => {
  const items = stats.value.taskSummary?.items ?? []
  if (!isAdminView.value) {
    return items.filter((item) => item.key !== 'maintenance-alerts')
  }
  return items.filter((item) => item.key !== 'notifications')
})
const resourceMetrics = computed<DashboardMetric[]>(() => {
  const metrics = stats.value.resourceSnapshot?.metrics ?? []
  if (!isAdminView.value) {
    return metrics.filter((item) => item.key !== 'quiet-slots')
  }
  return metrics.filter((item) => item.key !== 'pressure' && item.label !== '负载压力')
})
const heatmapBuckets = computed<DashboardHeatmapBucket[]>(() => stats.value.todayHeatmap ?? [])
const trendPoints = computed<DashboardTrendPoint[]>(() => stats.value.weeklyTrend ?? [])
const riskItems = computed<DashboardRiskDistributionItem[]>(() => stats.value.riskDistribution ?? [])
const heatPeakBucket = computed(() => {
  const buckets = heatmapBuckets.value
  if (!buckets.length) return null
  return [...buckets].sort((left, right) => right.occupancyPercent - left.occupancyPercent)[0]
})
const trendMax = computed(() => Math.max(1, ...trendPoints.value.map((item) => item.reservationCount)))
const riskMax = computed(() => Math.max(1, ...riskItems.value.map((item) => item.value)))
const riskTitle = computed(() => (isAdminView.value ? '风险与资源分布' : '预约提示'))
const trendSummary = computed(() => {
  const items = trendPoints.value
  const totalReservations = items.reduce((sum, item) => sum + item.reservationCount, 0)
  const totalPending = items.reduce((sum, item) => sum + item.pendingCount, 0)
  const totalRisk = items.reduce((sum, item) => sum + item.riskCount, 0)
  const peakPoint = [...items].sort((left, right) => right.reservationCount - left.reservationCount)[0] ?? null
  return {
    totalReservations,
    totalPending,
    totalRisk,
    peakDayLabel: peakPoint?.label ?? '--',
  }
})

function shortBucketLabel(label: string) {
  return String(label ?? '').split('-')[0] || label
}

function heatScale(bucket: DashboardHeatmapBucket) {
  return `scaleY(${Math.max(0.1, bucket.occupancyPercent / 100)})`
}

function trendHeight(value: number) {
  return `${Math.max(14, Math.round((value / trendMax.value) * 100))}%`
}

function riskWidth(value: number) {
  return `${Math.max(10, Math.round((value / riskMax.value) * 100))}%`
}

function goToTarget(to: string, query?: Record<string, string>) {
  router.push({ path: to, query })
}

function handleTaskItemClick(item: DashboardTaskItem) {
  if (item.key === 'notifications') {
    router.push({ path: '/dashboard', query: { panel: 'notifications' } })
    return
  }
  goToTarget(item.to, item.query)
}

async function reload() {
  const preserveContent = hasLoadedOnce.value
  loading.value = true
  statusMessage.value = ''

  if (!preserveContent) {
    viewState.value = 'loading'
  }

  try {
    const resp = await apiDashboardStats()
    if (resp.code !== 0) {
      viewState.value = 'error'
      statusMessage.value = resp.message || '运营看板暂时不可用'
      if (!preserveContent) {
        stats.value = createEmptyDashboardStats()
      }
      return
    }

    stats.value = resp.data ?? createEmptyDashboardStats()
    hasLoadedOnce.value = true
    viewState.value = 'ready'
    statusMessage.value = ''
  } catch (error) {
    viewState.value = 'error'
    statusMessage.value = getRequestErrorMessage(error, '运营看板加载失败，请稍后重试。')
    if (!preserveContent) {
      stats.value = createEmptyDashboardStats()
    }
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>

<template>
  <div class="page-wrap dashboard-wrap">
    <section class="page-hero cursor-card command-hero">
      <div class="page-hero__copy dashboard-hero-copy">
        <div class="command-hero__eyebrow">Command Center</div>
        <div class="page-title-row dashboard-title-row">
          <h2 class="page-title">运营看板</h2>
        </div>
        <div class="dashboard-hero-typing">
          <TypedText
            :texts="welcomeTexts"
            :typing-speed="48"
            :deleting-speed="28"
            :pause-duration="2400"
            :initial-delay="120"
            :loop="true"
            :hide-cursor-while-typing="true"
          />
        </div>
      </div>
      <div class="hero-actions dashboard-hero-actions">
        <el-button
          type="primary"
          class="btn-key-solid page-refresh-btn"
          :icon="RefreshRight"
          :loading="loading"
          circle
          title="刷新"
          aria-label="刷新运营看板"
          @click="reload"
        />
      </div>
    </section>

    <PageStatusPanel
      v-if="showBlockingState"
      tone="danger"
      :title="stateTitle"
      :description="stateDescription"
      action-text="重新加载"
      @action="reload"
    />

    <template v-else>
      <PageStatusPanel
        v-if="showInlineError"
        tone="warning"
        title="已保留上次成功加载的看板数据"
        :description="statusMessage"
        action-text="重新加载"
        @action="reload"
      />

      <section class="cursor-card stage-card">
        <div class="stage-grid">
          <section class="stage-panel stage-panel--tasks">
            <div class="stage-panel__head">
              <div class="stage-panel__kicker">Primary Focus</div>
              <div class="stage-panel__title">{{ stats.taskSummary.title }}</div>
            </div>

            <div class="task-list">
              <button
                v-for="item in taskItems"
                :key="item.key"
                type="button"
                class="task-item"
                :class="`tone-${item.tone}`"
                @click="handleTaskItemClick(item)"
              >
                <div class="task-item__top">
                  <span class="task-item__label">{{ item.label }}</span>
                  <span class="task-item__value">{{ item.value }}</span>
                </div>
              </button>
            </div>
          </section>

          <section class="stage-panel stage-panel--resources">
            <div class="stage-panel__head">
              <div class="stage-panel__kicker">Snapshot</div>
              <div class="stage-panel__title">{{ stats.resourceSnapshot.title }}</div>
            </div>

            <div class="metric-grid">
              <article v-for="metric in resourceMetrics" :key="metric.key" class="metric-card" :class="`tone-${metric.tone}`">
                <div class="metric-card__label">{{ metric.label }}</div>
                <div class="metric-card__value">{{ metric.value }}</div>
              </article>
            </div>
          </section>
        </div>
      </section>

      <section class="cursor-card chart-card chart-card--heatmap">
        <div class="section-head chart-card__head">
          <div>
            <div class="section-title">今日时段热度</div>
          </div>
          <div class="heatmap-summary">
            <div class="heatmap-summary__value">{{ heatPeakBucket?.label || '今日较平稳' }}</div>
          </div>
        </div>

        <div class="heatmap-grid">
          <div v-for="bucket in heatmapBuckets" :key="bucket.label" class="heat-column" :class="`is-${bucket.load}`">
            <div class="heat-column__count">{{ bucket.reservationCount }}</div>
            <div class="heat-column__track">
              <div class="heat-column__fill" :style="{ transform: heatScale(bucket) }"></div>
            </div>
            <div class="heat-column__label">{{ shortBucketLabel(bucket.label) }}</div>
          </div>
        </div>

        <div class="chart-legend">
          <span class="chart-legend__item"><i class="legend-dot is-low"></i> 低压</span>
          <span class="chart-legend__item"><i class="legend-dot is-medium"></i> 中压</span>
          <span class="chart-legend__item"><i class="legend-dot is-high"></i> 高压</span>
        </div>
      </section>

      <section class="dashboard-lower">
        <section class="cursor-card chart-card chart-card--trend">
          <div class="section-head chart-card__head">
            <div>
              <div class="section-title">本周趋势</div>
            </div>
          </div>

          <div class="trend-summary-grid">
            <article class="trend-summary-card">
              <div class="trend-summary-card__label">本周预约</div>
              <div class="trend-summary-card__value">{{ trendSummary.totalReservations }}</div>
            </article>
            <article class="trend-summary-card">
              <div class="trend-summary-card__label">待审批</div>
              <div class="trend-summary-card__value">{{ trendSummary.totalPending }}</div>
            </article>
            <article class="trend-summary-card">
              <div class="trend-summary-card__label">最繁忙日</div>
              <div class="trend-summary-card__value">{{ trendSummary.peakDayLabel }}</div>
            </article>
          </div>

          <div class="trend-grid">
            <div v-for="point in trendPoints" :key="point.day" class="trend-column">
              <div class="trend-column__bars">
                <div class="trend-column__bar trend-column__bar--total" :style="{ height: trendHeight(point.reservationCount) }"></div>
                <div class="trend-column__bar trend-column__bar--pending" :style="{ height: trendHeight(point.pendingCount) }"></div>
              </div>
              <div class="trend-column__meta">{{ point.reservationCount }}</div>
              <div class="trend-column__label">{{ point.label }}</div>
            </div>
          </div>

          <div class="chart-legend chart-legend--trend">
            <span class="chart-legend__item"><i class="legend-pill legend-pill--total"></i> 预约总量</span>
            <span class="chart-legend__item"><i class="legend-pill legend-pill--pending"></i> 待审批</span>
            <span class="chart-legend__item">高风险日累计 {{ trendSummary.totalRisk }}</span>
          </div>
        </section>

        <section class="cursor-card chart-card chart-card--risk">
          <div class="section-head chart-card__head">
            <div>
              <div class="section-title">{{ riskTitle }}</div>
            </div>
          </div>

          <div class="risk-list">
            <article v-for="item in riskItems" :key="item.key" class="risk-item" :class="`tone-${item.tone}`">
              <div class="risk-item__top">
                <span class="risk-item__label">{{ item.label }}</span>
                <span class="risk-item__value">{{ item.value }}</span>
              </div>
              <div class="risk-item__bar">
                <span class="risk-item__fill" :style="{ width: riskWidth(item.value) }"></span>
              </div>
            </article>
          </div>
        </section>
      </section>
    </template>
  </div>
</template>

<style scoped>
.dashboard-wrap {
  --dashboard-compact-card-height: 116px;
  --dashboard-chart-head-height: 52px;
  gap: 16px;
}

.command-hero {
  min-height: clamp(188px, 19vw, 220px);
}

.dashboard-hero-copy {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.command-hero__eyebrow {
  margin-bottom: 2px;
  font-size: 11px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--text-weak);
}

.dashboard-title-row {
  align-items: center;
}

.dashboard-hero-typing {
  min-height: 1.28em;
  font-family: var(--font-display);
  font-size: var(--type-section);
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: 1.28;
  color: var(--text-main);
}

.dashboard-hero-typing :deep(.typed-text) {
  max-width: min(100%, 34ch);
}

.dashboard-hero-actions {
  align-self: center;
}

.metric-card,
.risk-item,
.trend-summary-card {
  border: 1px solid var(--surface-nested-border);
  border-radius: calc(var(--radius-unified) + 2px);
  padding: 14px;
  background: linear-gradient(180deg, var(--surface-nested-top), var(--surface-nested-bottom));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.metric-card,
.risk-item,
.trend-summary-card,
.task-item {
  min-height: var(--dashboard-compact-card-height);
  height: 100%;
}

.metric-card,
.risk-item,
.trend-summary-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 14px;
}

.task-item__label,
.metric-card__label,
.risk-item__label,
.trend-summary-card__label {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.5;
}

.metric-card__value,
.trend-summary-card__value {
  margin-top: auto;
  font-family: var(--font-numeric);
  font-size: clamp(1.28rem, 1.14rem + 0.38vw, 1.68rem);
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.06;
  color: var(--text-main);
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum' 1;
}

.task-item__value,
.risk-item__value {
  font-family: var(--font-numeric);
  font-size: clamp(1.28rem, 1.14rem + 0.38vw, 1.68rem);
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.06;
  color: var(--text-main);
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum' 1;
}

.stage-card,
.chart-card {
  padding: var(--panel-card-padding);
}

.stage-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(0, 1fr);
  gap: 16px;
}

.stage-panel {
  border: 1px solid var(--surface-nested-border);
  border-radius: calc(var(--radius-unified) + 2px);
  padding: 18px;
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(180deg, var(--surface-nested-top), var(--surface-nested-bottom)),
    var(--bg-card);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    0 1px 2px rgba(20, 24, 28, 0.03);
}

.stage-panel__head {
  min-height: 56px;
  margin-bottom: 14px;
}

.stage-panel__kicker {
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--text-weak);
}

.stage-panel__title {
  margin-top: 5px;
  font-family: var(--font-display);
  font-size: clamp(1.24rem, 1.1rem + 0.34vw, 1.5rem);
  font-weight: 600;
  letter-spacing: -0.03em;
  color: var(--text-main);
  line-height: 1.12;
}

.task-list,
.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  flex: 1;
  grid-auto-rows: minmax(var(--dashboard-compact-card-height), 1fr);
  align-items: stretch;
}

.task-list > .task-item:last-child:nth-child(odd) {
  grid-column: 1 / -1;
  width: min(100%, calc(50% - 6px));
  justify-self: center;
}

.task-item {
  border: 1px solid var(--surface-nested-border);
  border-radius: calc(var(--radius-unified) + 2px);
  background: linear-gradient(180deg, var(--surface-nested-top), var(--surface-nested-bottom));
  padding: 14px;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  cursor: pointer;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.task-item__top {
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
}

.chart-card__head {
  align-items: center;
  min-height: var(--dashboard-chart-head-height);
}

.heatmap-summary {
  min-width: 140px;
  min-height: var(--dashboard-chart-head-height);
  padding: 10px 14px;
  border-radius: calc(var(--radius-unified) + 2px);
  border: 1px solid var(--surface-nested-border);
  background: linear-gradient(180deg, var(--surface-nested-top), var(--surface-nested-bottom));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.heatmap-summary__value {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--text-main);
}

.heatmap-grid {
  display: grid;
  grid-template-columns: repeat(10, minmax(0, 1fr));
  gap: 10px;
  align-items: end;
}

.heat-column {
  min-width: 0;
}

.heat-column__count,
.trend-column__meta {
  margin-bottom: 8px;
  text-align: center;
  color: var(--text-muted);
  font-family: var(--font-numeric);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum' 1;
}

.heat-column__track,
.trend-column__bars {
  border-radius: calc(var(--radius-unified) + 2px);
  border: 1px solid var(--surface-nested-border);
  background: linear-gradient(180deg, rgba(251, 251, 251, 0.94), rgba(236, 236, 236, 0.9));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.heat-column__track {
  height: 168px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 10px 0;
}

.heat-column__fill {
  width: min(34px, 72%);
  height: 100%;
  border-radius: 999px;
  background: var(--tone-neutral-fill);
  transform-origin: bottom;
  transition: transform var(--motion-hover) var(--motion-ease-out);
}

.heat-column.is-low .heat-column__fill {
  background: var(--tone-info-fill);
}

.heat-column.is-medium .heat-column__fill {
  background: var(--tone-warning-fill);
}

.heat-column.is-high .heat-column__fill {
  background: var(--tone-danger-fill);
}

.heat-column__label,
.trend-column__label {
  margin-top: 8px;
  text-align: center;
  font-size: 12px;
  color: var(--text-weak);
}

.chart-legend {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.chart-legend__item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.legend-dot.is-low {
  background: var(--tone-info-fill);
}

.legend-dot.is-medium {
  background: var(--tone-warning-fill);
}

.legend-dot.is-high {
  background: var(--tone-danger-fill);
}

.legend-pill {
  width: 14px;
  height: 8px;
  border-radius: 999px;
}

.legend-pill--total {
  background: var(--tone-info-fill);
}

.legend-pill--pending {
  background: var(--tone-warning-fill);
}

.dashboard-lower {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(0, 0.92fr);
  gap: 16px;
  align-items: stretch;
}

.chart-card--trend,
.chart-card--risk {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.trend-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
  grid-auto-rows: 1fr;
  align-items: stretch;
}

.trend-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
  align-items: stretch;
  align-content: stretch;
  flex: 1;
  min-height: 0;
}

.trend-column {
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.trend-column__bars {
  flex: 1;
  min-height: 166px;
  height: auto;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 8px;
  padding: 14px 8px;
}

.trend-column__bar {
  width: min(22px, 42%);
  min-height: 14px;
  border-radius: 999px;
}

.trend-column__bar--total {
  background: var(--tone-info-fill);
}

.trend-column__bar--pending {
  background: var(--tone-warning-fill);
}

.risk-list {
  display: grid;
  gap: 10px;
  flex: 1;
  align-content: stretch;
  grid-auto-rows: 1fr;
}

.risk-item__top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.risk-item__bar {
  height: 8px;
  border-radius: 999px;
  overflow: hidden;
  background: var(--tone-neutral-bg);
}

.risk-item__fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--tone-neutral-fill);
}

.tone-accent,
.tone-total {
  border-color: var(--tone-info-border);
}

.tone-warning,
.tone-pending {
  border-color: var(--tone-warning-border);
}

.tone-danger,
.tone-rejected {
  border-color: var(--tone-danger-border);
}

.tone-info,
.tone-neutral {
  border-color: var(--tone-info-border);
}

.tone-success,
.tone-approved,
.tone-available {
  border-color: var(--tone-success-border);
}

.risk-item.tone-warning .risk-item__fill,
.trend-summary-card.tone-warning .risk-item__fill,
.metric-card.tone-warning .risk-item__fill {
  background: var(--tone-warning-fill);
}

.risk-item.tone-danger .risk-item__fill,
.risk-item.tone-rejected .risk-item__fill {
  background: var(--tone-danger-fill);
}

.risk-item.tone-success .risk-item__fill,
.risk-item.tone-approved .risk-item__fill,
.risk-item.tone-available .risk-item__fill {
  background: var(--tone-success-fill);
}

.risk-item.tone-accent .risk-item__fill,
.risk-item.tone-total .risk-item__fill,
.risk-item.tone-info .risk-item__fill,
.risk-item.tone-neutral .risk-item__fill {
  background: var(--tone-info-fill);
}

button:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px var(--focus-ring);
}

@media (max-width: 1240px) {
  .stage-grid,
  .dashboard-lower {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .task-list,
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .task-list > .task-item:last-child:nth-child(odd) {
    grid-column: auto;
    width: 100%;
  }

  .trend-summary-grid {
    grid-template-columns: 1fr;
  }

  .heatmap-grid {
    gap: 8px;
  }

  .trend-grid {
    gap: 8px;
  }
}

@media (max-width: 760px) {
  .dashboard-hero-actions {
    align-self: flex-start;
  }

  .heatmap-grid {
    grid-template-columns: repeat(5, minmax(0, 1fr));
    row-gap: 14px;
  }

  .trend-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .heatmap-grid,
  .trend-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
