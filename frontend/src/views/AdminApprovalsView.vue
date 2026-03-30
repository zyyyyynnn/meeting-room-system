<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRequestErrorMessage } from '../api/http'
import type { Reservation } from '../api/types'
import { apiAdminAuditLogs, apiApprove, apiPending, apiRecentReviewed, apiReject, apiRevokeReview } from '../api/mrs'
import PageStatusPanel from '../components/PageStatusPanel.vue'
import { authStore } from '../store/auth'

type ViewState = 'loading' | 'ready' | 'error'

const loading = ref(false)
const viewState = ref<ViewState>('loading')
const statusMessage = ref('')
const partialMessage = ref('')
const hasLoadedOnce = ref(false)
const list = ref<Reservation[]>([])
const reviewed = ref<Reservation[]>([])
const auditLogs = ref<string[]>([])

const isSuperAdmin = computed(() => authStore.isSuperAdmin.value)
const showBlockingState = computed(() => !hasLoadedOnce.value && viewState.value !== 'ready')
const showInlineError = computed(() => hasLoadedOnce.value && viewState.value === 'error')
const showPartialWarning = computed(() => hasLoadedOnce.value && viewState.value === 'ready' && !!partialMessage.value)
const stateTitle = computed(() => (viewState.value === 'loading' ? '正在加载审批数据' : '审批数据暂时不可用'))
const stateDescription = computed(() => {
  if (viewState.value === 'loading') {
    return '正在同步待审批列表、已审批记录与审计日志，请稍候。'
  }
  return statusMessage.value || '当前无法获取审批数据，请稍后重试。'
})

const stats = computed(() => ({
  total: list.value.length,
  today: list.value.filter((x) => String(x?.startTime ?? '').slice(0, 10) === new Date().toISOString().slice(0, 10)).length,
  rooms: new Set(list.value.map((x) => x?.roomId)).size,
}))

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
    confirmButtonClass: 'btn-key-solid confirm-btn-force',
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
    confirmButtonClass: 'btn-key-solid confirm-btn-force',
  })
  const resp = await apiRevokeReview(row.id)
  if (resp.code !== 0) {
    ElMessage.error(resp.message)
    return
  }
  ElMessage.success('已撤销审批结果')
  await reload()
}

onMounted(reload)
</script>

<template>
  <div class="page-wrap">
    <section class="page-hero cursor-card">
      <div>
        <h2 class="page-title">预约审批</h2>
        <p class="page-subtitle">管理员负责审批待处理预约；超级管理员可撤销历史审批结果并重新审核。</p>
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

      <section class="cursor-card table-card">
        <div class="section-head">
          <div class="section-title">待审批列表</div>
          <div class="section-desc">请根据会议用途与冲突情况进行批准或驳回。</div>
        </div>

        <el-table v-if="list.length" class="approvals-table" :data="list" style="width: 100%" :max-height="520">
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="roomId" label="会议室ID" width="110" />
          <el-table-column prop="userId" label="用户ID" width="110" />
          <el-table-column prop="startTime" label="开始" min-width="220" />
          <el-table-column prop="endTime" label="结束" min-width="220" />
          <el-table-column prop="reason" label="原因/备注">
            <template #default="{ row }">
              <span>{{ row.reason || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column class-name="action-col" label="操作" width="210" align="right" header-align="center">
            <template #default="{ row }">
              <div class="row-actions row-actions--right">
                <el-button size="small" type="primary" class="btn-key-soft" @click="approve(row)">批准</el-button>
                <el-button size="small" type="primary" class="btn-key-solid" @click="reject(row)">驳回</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-state">当前没有待审批预约，新的申请提交后会自动出现在这里。</div>
      </section>

      <section class="cursor-card table-card" v-if="isSuperAdmin">
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
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="status" label="审批结果" width="120" />
          <el-table-column prop="roomName" label="会议室" min-width="220" />
          <el-table-column prop="username" label="申请人" min-width="180" />
          <el-table-column prop="approvedAt" label="审批时间" min-width="220" />
          <el-table-column class-name="action-col reviewed-action-col" label="操作" width="190" align="center" header-align="center">
            <template #default="{ row }">
              <div class="row-actions row-actions--right">
                <el-button size="small" type="primary" class="btn-key-solid" @click="revoke(row)">撤销审批</el-button>
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
  width: 190px !important;
}

:deep(.approvals-table--reviewed .reviewed-action-col .cell) {
  display: flex;
  justify-content: center;
}

.stats-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.stats-grid .stat-card {
  min-height: 126px;
}

:deep(.approvals-table .el-table__body .cell) {
  line-height: 1.66;
}

.row-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  row-gap: 6px;
}

.row-actions--right {
  justify-content: flex-end;
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
  background: linear-gradient(180deg, var(--nested-surface-top), rgba(245, 248, 251, 0.92));
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
  }

  .row-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
