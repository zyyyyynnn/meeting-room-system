<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { RefreshRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { getRequestErrorMessage } from '../api/http'
import type { Reservation } from '../api/types'
import { apiAdminAuditLogs, apiApprove, apiPending, apiRecentReviewed, apiReject, apiRevokeReview } from '../api/mrs'
import PageStatusPanel from '../components/PageStatusPanel.vue'
import { authStore } from '../store/auth'

type ViewState = 'loading' | 'ready' | 'error'

const route = useRoute()
const loading = ref(false)
const viewState = ref<ViewState>('loading')
const statusMessage = ref('')
const partialMessage = ref('')
const hasLoadedOnce = ref(false)
const list = ref<Reservation[]>([])
const reviewed = ref<Reservation[]>([])
const auditLogs = ref<string[]>([])
const pendingSectionRef = ref<HTMLElement | null>(null)
const reviewedSectionRef = ref<HTMLElement | null>(null)

const isSuperAdmin = computed(() => authStore.isSuperAdmin.value)
const showBlockingState = computed(() => !hasLoadedOnce.value && viewState.value === 'error')
const showInlineError = computed(() => hasLoadedOnce.value && viewState.value === 'error')
const showPartialWarning = computed(() => hasLoadedOnce.value && viewState.value === 'ready' && !!partialMessage.value)
const focusHint = computed(() => {
  const focus = String(route.query.focus ?? '').toLowerCase()
  if (focus === 'reviewed') return '已按看板上下文定位到最近审批记录'
  if (focus === 'pending') return '已按看板上下文定位到待审批队列'
  return ''
})
const stateTitle = computed(() => '审批数据暂时不可用')
const stateDescription = computed(() => statusMessage.value || '当前无法获取审批数据，请稍后重试。')

const stats = computed(() => ({
  total: list.value.length,
  today: list.value.filter((x) => String(x?.startTime ?? '').slice(0, 10) === new Date().toISOString().slice(0, 10)).length,
  rooms: new Set(list.value.map((x) => x?.roomId)).size,
}))

function scrollToFocusedSection() {
  const focus = String(route.query.focus ?? '').toLowerCase()
  if (focus === 'reviewed' && isSuperAdmin.value) {
    reviewedSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    return
  }
  if (focus === 'pending') {
    pendingSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

async function reload() {
  const preserveContent = hasLoadedOnce.value
  loading.value = true
  statusMessage.value = ''
  partialMessage.value = ''

  if (!preserveContent) {
    viewState.value = 'loading'
  }

  try {
    const [resp, reviewedResp, auditResp] = await Promise.all([apiPending(), apiRecentReviewed(), apiAdminAuditLogs()])
    if (resp.code !== 0) {
      viewState.value = 'error'
      statusMessage.value = resp.message || '加载审批列表失败'
      if (!preserveContent) {
        list.value = []
        reviewed.value = []
        auditLogs.value = []
      }
      return
    }

    list.value = Array.isArray(resp.data) ? resp.data : []
    reviewed.value = reviewedResp.code === 0 && Array.isArray(reviewedResp.data) ? reviewedResp.data : []
    auditLogs.value = auditResp.code === 0 && Array.isArray(auditResp.data) ? auditResp.data : []

    const warningMessages = [reviewedResp, auditResp]
      .filter((item) => item.code !== 0 && typeof item.message === 'string' && item.message.trim())
      .map((item) => item.message.trim())

    partialMessage.value = warningMessages.join('；')
    hasLoadedOnce.value = true
    viewState.value = 'ready'
    void nextTick(() => {
      scrollToFocusedSection()
    })
  } catch (error) {
    viewState.value = 'error'
    statusMessage.value = getRequestErrorMessage(error, '加载审批列表失败，请稍后重试。')
    if (!preserveContent) {
      list.value = []
      reviewed.value = []
      auditLogs.value = []
    }
  } finally {
    loading.value = false
  }
}

async function approve(row: Reservation) {
  const { value } = await ElMessageBox.prompt('审批备注（可空）', '批准', {
    inputValue: '',
    cancelButtonText: '取消',
    confirmButtonText: '确定',
    cancelButtonClass: 'btn-key-soft cancel-btn-force',
    confirmButtonClass: 'btn-key-solid confirm-btn-force',
  })
  const resp = await apiApprove(row.id, value)
  if (resp.code !== 0) {
    ElMessage.error(resp.message)
    return
  }
  ElMessage.success('已批准')
  await reload()
}

async function reject(row: Reservation) {
  const { value } = await ElMessageBox.prompt('驳回原因', '驳回', {
    inputValue: '',
    cancelButtonText: '取消',
    confirmButtonText: '确定',
    cancelButtonClass: 'btn-key-soft cancel-btn-force',
    confirmButtonClass: 'btn-danger-soft confirm-btn-force',
  })
  const resp = await apiReject(row.id, value)
  if (resp.code !== 0) {
    ElMessage.error(resp.message)
    return
  }
  ElMessage.success('已驳回')
  await reload()
}

async function revoke(row: Reservation) {
  await ElMessageBox.confirm(`确认撤销预约 #${row.id} 的审批结果并回退到待审批吗？`, '撤销审批', {
    type: 'warning',
    cancelButtonText: '取消',
    confirmButtonText: '确定',
    cancelButtonClass: 'btn-key-soft cancel-btn-force',
    confirmButtonClass: 'btn-danger-soft confirm-btn-force',
  })
  const resp = await apiRevokeReview(row.id)
  if (resp.code !== 0) {
    ElMessage.error(resp.message)
    return
  }
  ElMessage.success('已撤销审批结果')
  await reload()
}

watch(
  () => route.query.focus,
  () => {
    void nextTick(() => {
      scrollToFocusedSection()
    })
  },
  { immediate: true },
)

onMounted(reload)
</script>

<template>
  <div class="page-wrap">
    <section class="page-hero cursor-card">
      <div class="page-hero__copy">
        <div class="page-title-row">
          <h2 class="page-title">预约审批</h2>
        </div>
        <p class="page-subtitle">管理员负责审批待处理预约；超级管理员可撤销历史审批结果并重新审核。</p>
      </div>
      <div class="hero-actions">
        <el-button
          type="primary"
          class="btn-key-solid page-refresh-btn"
          :icon="RefreshRight"
          :loading="loading"
          circle
          title="刷新"
          aria-label="刷新预约审批"
          @click="reload"
        />
      </div>
    </section>

    <div v-if="focusHint" class="focus-pill focus-pill--block status-strip">{{ focusHint }}</div>

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
        title="已保留上次成功加载的审批数据"
        :description="statusMessage"
        action-text="重新加载"
        @action="reload"
      />

      <PageStatusPanel
        v-else-if="showPartialWarning"
        tone="info"
        title="部分辅助信息未完全刷新"
        :description="partialMessage"
        action-text="重新加载"
        @action="reload"
      />

      <section class="stats-grid">
        <article class="stat-card cursor-card"><div class="k">待审批总数</div><div class="v">{{ stats.total }}</div></article>
        <article class="stat-card cursor-card"><div class="k">今日开始会议</div><div class="v">{{ stats.today }}</div></article>
        <article class="stat-card cursor-card"><div class="k">涉及会议室</div><div class="v">{{ stats.rooms }}</div></article>
      </section>

      <section ref="pendingSectionRef" class="cursor-card table-card">
        <div class="section-head">
          <div class="section-title">待审批列表</div>
          <div class="section-desc">请根据会议用途与冲突情况进行批准或驳回。</div>
        </div>

        <el-table v-if="list.length" class="approvals-table" :data="list" style="width: 100%" :max-height="520">
          <el-table-column prop="id" label="ID" width="76" />
          <el-table-column label="申请信息" min-width="230">
            <template #default="{ row }">
              <div class="reservation-cell">
                <div class="reservation-cell__title">{{ row.roomName || `会议室 #${row.roomId}` }}</div>
                <div class="reservation-cell__meta">申请人 {{ row.username || `用户 #${row.userId}` }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="startTime" label="开始时间" min-width="188" />
          <el-table-column prop="endTime" label="结束时间" min-width="188" />
          <el-table-column class-name="action-col" label="操作" width="164" align="right" header-align="center">
            <template #default="{ row }">
              <div class="row-actions row-actions--right row-actions--inline">
                <el-button size="small" type="primary" class="btn-key-soft" @click="approve(row)">批准</el-button>
                <el-button size="small" type="primary" class="btn-danger-soft" @click="reject(row)">驳回</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-state">当前没有待审批预约，新的申请提交后会自动出现在这里。</div>
      </section>

      <section ref="reviewedSectionRef" class="cursor-card table-card" v-if="isSuperAdmin">
        <div class="section-head">
          <div class="section-title">最近已审批记录（超级管理员）</div>
          <div class="section-desc">可将已通过/已驳回预约撤销为待审批，用于重新审查。</div>
        </div>

        <el-table
          v-if="reviewed.length"
          class="approvals-table approvals-table--reviewed"
          :data="reviewed"
          style="width: 100%"
          table-layout="fixed"
          :max-height="420"
        >
          <el-table-column prop="id" label="ID" width="76" />
          <el-table-column prop="status" label="审批结果" width="112" />
          <el-table-column prop="roomName" label="会议室" min-width="200" />
          <el-table-column prop="username" label="申请人" min-width="160" />
          <el-table-column prop="approvedAt" label="审批时间" min-width="200" />
          <el-table-column class-name="action-col reviewed-action-col" label="操作" width="156" align="center" header-align="center">
            <template #default="{ row }">
              <div class="row-actions row-actions--right">
                <el-button size="small" type="primary" class="btn-danger-soft" @click="revoke(row)">撤销审批</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-state">最近暂无可撤销的审批记录，新的审批结果会在这里保留一段时间。</div>
      </section>

      <section class="cursor-card table-card">
        <div class="section-head">
          <div class="section-title">全局审计日志</div>
          <div class="section-desc">追踪预约创建、取消、审批等关键行为。</div>
        </div>
        <div v-if="auditLogs.length" class="logs-wrap">
          <div class="log-line" v-for="(item, idx) in auditLogs" :key="`log-${idx}`">{{ item }}</div>
        </div>
        <div v-else class="empty-state">暂无全局审计日志。后续审批、撤销等关键动作会在这里自动沉淀。</div>
      </section>
    </template>
  </div>
</template>

<style scoped>
:deep(.approvals-table .el-table__body-wrapper) {
  overflow-x: auto;
}

:deep(.approvals-table--reviewed colgroup col:last-child) {
  width: 156px !important;
}

:deep(.approvals-table--reviewed .reviewed-action-col .cell) {
  display: flex;
  justify-content: center;
}

.stats-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.stats-grid .stat-card {
  min-height: var(--stat-card-min-height);
}

.table-card .section-desc {
  display: none;
}

:deep(.approvals-table .el-table__body .cell) {
  line-height: 1.66;
}

.row-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  row-gap: 6px;
}

.row-actions--right {
  justify-content: flex-end;
  flex-wrap: nowrap;
}

.row-actions--inline {
  flex-wrap: nowrap;
  gap: 6px;
}

.row-actions--inline :deep(.el-button) {
  min-width: 64px;
}

.reservation-cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.reservation-cell__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  line-height: 1.42;
}

.reservation-cell__meta {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.56;
}

.focus-pill {
  max-width: min(100%, 70ch);
}

.focus-pill--block {
  margin-top: -2px;
  margin-bottom: 12px;
}

.approvals-table--reviewed .row-actions--right {
  width: 100%;
  justify-content: center;
}

.logs-wrap {
  counter-reset: audit-log;
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.log-line {
  position: relative;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.72;
  padding: 12px 14px 12px 48px;
  border: 1px solid var(--nested-surface-border);
  border-radius: calc(var(--radius-unified) + 2px);
  background: linear-gradient(180deg, var(--nested-surface-top), var(--nested-surface-bottom));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.log-line::before {
  counter-increment: audit-log;
  content: counter(audit-log, decimal-leading-zero);
  position: absolute;
  left: 14px;
  top: 12px;
  width: 24px;
  height: 24px;
  border-radius: 999px;
  border: 1px solid var(--nested-surface-border);
  background: linear-gradient(180deg, var(--nested-surface-top), rgba(242, 242, 242, 0.92));
  display: grid;
  place-items: center;
  color: var(--text-muted);
  font-size: 10px;
  font-weight: 700;
}

.log-line::after {
  content: '';
  position: absolute;
  left: 25px;
  top: 42px;
  bottom: -10px;
  width: 1px;
  background: linear-gradient(180deg, var(--nested-surface-rule), transparent);
}

.log-line:last-child::after {
  display: none;
}

.muted {
  color: var(--text-muted);
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .row-actions--right {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .row-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .row-actions :deep(.el-button) {
    width: 100%;
  }

  .row-actions--inline {
    width: auto;
  }

  .row-actions--inline :deep(.el-button) {
    width: auto;
    flex: 0 0 auto;
  }
}
</style>
