<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRequestErrorMessage } from '../api/http'
import type { Reservation } from '../api/types'
import { apiCancelReservation, apiDeleteReservation, apiMyAuditLogs, apiMyRecent } from '../api/mrs'
import PageStatusPanel from '../components/PageStatusPanel.vue'

type ReservationLike = Partial<Reservation> & { reservationId?: number }
type ReservationEnvelope = { list?: ReservationLike[]; records?: ReservationLike[] }
type ViewState = 'loading' | 'ready' | 'error'

const loading = ref(false)
const viewState = ref<ViewState>('loading')
const statusMessage = ref('')
const partialMessage = ref('')
const hasLoadedOnce = ref(false)
const list = ref<Reservation[]>([])
const auditLogs = ref<string[]>([])

const filters = reactive({
  keyword: '',
  status: 'ALL' as 'ALL' | 'APPROVED' | 'PENDING' | 'REJECTED' | 'CANCELLED',
})

function normalizeReservation(item: ReservationLike): Reservation {
  return {
    id: Number(item.id ?? item.reservationId ?? 0),
    userId: Number(item.userId ?? 0),
    username: String(item.username ?? ''),
    roomId: Number(item.roomId ?? 0),
    roomName: String(item.roomName ?? '未命名会议室'),
    startTime: String(item.startTime ?? ''),
    endTime: String(item.endTime ?? ''),
    status: String(item.status ?? 'PENDING'),
    reason: item.reason ? String(item.reason) : '',
    adminComment: item.adminComment ? String(item.adminComment) : '',
    approvedBy: item.approvedBy ? Number(item.approvedBy) : undefined,
    approvedAt: item.approvedAt ? String(item.approvedAt) : undefined,
  }
}

function extractReservations(data: unknown): ReservationLike[] {
  if (Array.isArray(data)) {
    return data as ReservationLike[]
  }

  if (data && typeof data === 'object') {
    const envelope = data as ReservationEnvelope
    if (Array.isArray(envelope.list)) return envelope.list
    if (Array.isArray(envelope.records)) return envelope.records
  }

  return []
}

const stats = computed(() => ({
  total: list.value.length,
  approved: list.value.filter((x) => x.status === 'APPROVED').length,
  pending: list.value.filter((x) => x.status === 'PENDING').length,
  cancelled: list.value.filter((x) => x.status === 'CANCELLED').length,
}))

const filteredList = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return list.value.filter((item) => {
    const hitKeyword =
      !keyword ||
      item.roomName.toLowerCase().includes(keyword) ||
      String(item.reason ?? '').toLowerCase().includes(keyword) ||
      String(item.adminComment ?? '').toLowerCase().includes(keyword)
    const hitStatus = filters.status === 'ALL' || item.status === filters.status
    return hitKeyword && hitStatus
  })
})

const showBlockingState = computed(() => !hasLoadedOnce.value && viewState.value !== 'ready')
const showInlineError = computed(() => hasLoadedOnce.value && viewState.value === 'error')
const showPartialWarning = computed(() => hasLoadedOnce.value && viewState.value === 'ready' && !!partialMessage.value)
const stateTitle = computed(() => (viewState.value === 'loading' ? '正在加载我的预约' : '我的预约暂时不可用'))
const stateDescription = computed(() => {
  if (viewState.value === 'loading') {
    return '正在同步最近预约与审计日志，请稍候。'
  }
  return statusMessage.value || '当前无法获取预约记录，请稍后重试。'
})
const emptyListMessage = computed(() => {
  if (list.value.length) {
    return '当前筛选条件下暂无匹配的预约记录，请调整筛选条件后再试。'
  }
  return '最近暂无预约记录。创建新的会议预约后，这里会自动同步显示。'
})

async function reload() {
  const preserveContent = hasLoadedOnce.value
  loading.value = true
  statusMessage.value = ''
  partialMessage.value = ''

  if (!preserveContent) {
    viewState.value = 'loading'
  }

  try {
    const [recentResp, auditResp] = await Promise.all([apiMyRecent(), apiMyAuditLogs()])

    if (recentResp.code !== 0) {
      viewState.value = 'error'
      statusMessage.value = recentResp.message || '加载我的预约失败'
      if (!preserveContent) {
        list.value = []
        auditLogs.value = []
      }
      return
    }

    const raw = extractReservations(recentResp.data)

    list.value = raw.map((x: ReservationLike) => normalizeReservation(x)).filter((x: Reservation) => x.id > 0)
    auditLogs.value = auditResp.code === 0 && Array.isArray(auditResp.data) ? auditResp.data : []
    partialMessage.value = auditResp.code !== 0 ? String(auditResp.message || '审计日志刷新失败') : ''
    hasLoadedOnce.value = true
    viewState.value = 'ready'
  } catch (error) {
    viewState.value = 'error'
    statusMessage.value = getRequestErrorMessage(error, '加载我的预约失败')
    if (!preserveContent) {
      list.value = []
      auditLogs.value = []
    }
  } finally {
    loading.value = false
  }
}

function statusType(status: string) {
  if (status === 'APPROVED') return 'success'
  if (status === 'PENDING') return 'warning'
  if (status === 'REJECTED') return 'danger'
  return 'info'
}

function statusText(status: string) {
  if (status === 'APPROVED') return '已批准'
  if (status === 'PENDING') return '待审批'
  if (status === 'REJECTED') return '已拒绝'
  if (status === 'CANCELLED') return '已取消'
  return status || '未知'
}

async function cancel(r: Reservation) {
  if (!r?.id) {
    ElMessage.warning('无效预约记录，无法取消')
    return
  }

  await ElMessageBox.confirm('确认取消该预约？', '提示', {
    type: 'warning',
    cancelButtonText: '取消',
    confirmButtonText: '确定',
    cancelButtonClass: 'btn-key-soft cancel-btn-force',
    confirmButtonClass: 'btn-key-solid confirm-btn-force',
  })

  const resp = await apiCancelReservation(r.id)
  if (resp.code !== 0) {
    ElMessage.error(resp.message)
    return
  }

  ElMessage.success('已取消')
  await reload()
}

async function removeReservation(r: Reservation) {
  if (!r?.id) {
    ElMessage.warning('无效预约记录，无法删除')
    return
  }

  await ElMessageBox.confirm('确认删除该预约记录？删除后不可恢复。', '提示', {
    type: 'warning',
    cancelButtonText: '取消',
    confirmButtonText: '确定',
    cancelButtonClass: 'btn-key-soft cancel-btn-force',
    confirmButtonClass: 'btn-key-solid confirm-btn-force',
  })

  const resp = await apiDeleteReservation(r.id)
  if (resp.code !== 0) {
    ElMessage.error(resp.message)
    return
  }

  ElMessage.success('已删除')
  await reload()
}

function resetFilters() {
  filters.keyword = ''
  filters.status = 'ALL'
}

onMounted(reload)
</script>

<template>
  <div class="page-wrap">
    <section class="page-hero cursor-card">
      <div>
        <h2 class="page-title">我的预约</h2>
        <p class="page-subtitle">查看近 30 天预约记录，跟进审批进度并按需取消预约。</p>
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
        title="已保留上次同步的预约数据"
        :description="statusMessage"
        action-text="重新加载"
        @action="reload"
      />

      <PageStatusPanel
        v-else-if="showPartialWarning"
        tone="info"
        title="预约数据已更新，日志尚未完全刷新"
        :description="partialMessage"
        action-text="重新加载"
        @action="reload"
      />

      <section class="stats-grid reservation-stats-grid">
        <article class="stat-card cursor-card">
          <div class="k">预约总数</div>
          <div class="v">{{ stats.total }}</div>
        </article>
        <article class="stat-card cursor-card">
          <div class="k">已批准</div>
          <div class="v">{{ stats.approved }}</div>
        </article>
        <article class="stat-card cursor-card">
          <div class="k">待审批</div>
          <div class="v">{{ stats.pending }}</div>
        </article>
        <article class="stat-card cursor-card">
          <div class="k">已取消</div>
          <div class="v">{{ stats.cancelled }}</div>
        </article>
      </section>

      <section class="cursor-card table-card">
        <div class="list-toolbar">
          <div class="section-head section-head--compact">
            <div class="section-title">预约列表</div>
            <div class="section-desc">按时间倒序展示最近记录。</div>
          </div>
          <div class="reservation-filters">
            <el-input v-model="filters.keyword" clearable placeholder="搜索会议室或备注" class="reservation-filter-keyword" />
            <el-select v-model="filters.status" class="reservation-filter-select">
              <el-option label="全部状态" value="ALL" />
              <el-option label="已批准" value="APPROVED" />
              <el-option label="待审批" value="PENDING" />
              <el-option label="已拒绝" value="REJECTED" />
              <el-option label="已取消" value="CANCELLED" />
            </el-select>
            <el-button type="primary" class="btn-key-soft reservation-filters__reset" @click="resetFilters">重置筛选</el-button>
          </div>
        </div>

        <el-table v-if="filteredList.length" class="reservation-table" :data="filteredList" style="width: 100%">
          <el-table-column prop="roomName" label="会议室" width="180" />
          <el-table-column prop="startTime" label="开始" width="180" />
          <el-table-column prop="endTime" label="结束" width="180" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" :class="{ 'tag-pending': row.status === 'PENDING' }" effect="plain">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因/备注">
            <template #default="{ row }">
              <span>{{ row.reason || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column class-name="action-col" label="操作" width="200" align="right" header-align="center">
            <template #default="{ row }">
              <div class="row-actions row-actions--right">
                <el-button
                  size="small"
                  type="primary"
                  class="btn-key-soft"
                  @click="cancel(row)"
                  :disabled="row.status === 'CANCELLED' || row.status === 'REJECTED'"
                >
                  取消
                </el-button>
                <el-button size="small" type="primary" class="btn-key-solid" @click="removeReservation(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-state">{{ emptyListMessage }}</div>
      </section>

      <section class="cursor-card table-card">
        <div class="section-head">
          <div class="section-title">审计日志</div>
          <div class="section-desc">记录预约创建、取消、删除、审批等关键动作。</div>
        </div>
        <div v-if="auditLogs.length" class="logs-wrap">
          <div v-for="(log, idx) in auditLogs" :key="`${idx}-${log}`" class="log-line">{{ log }}</div>
        </div>
        <div v-else class="empty-state">暂无日志。你可以进入“审批中心”查看跨页面日志聚合。</div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.reservation-stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.list-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.section-head--compact {
  margin-bottom: 0;
}

.reservation-filters {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: nowrap;
  margin-left: auto;
}

.reservation-filter-keyword {
  width: 228px;
}

.reservation-filter-select {
  width: 148px;
}

.reservation-filters__reset {
  min-width: 92px;
  height: var(--control-height);
  padding: 0 12px;
  border-radius: var(--radius-unified);
}

:deep(.reservation-filters .el-input__wrapper),
:deep(.reservation-filters .el-select__wrapper) {
  min-height: var(--control-height);
  height: var(--control-height);
  border-radius: var(--radius-unified);
}

:deep(.reservation-filters .el-input__inner),
:deep(.reservation-filters .el-select__selected-item),
:deep(.reservation-filters .el-select__placeholder) {
  font-size: 14px;
  line-height: 1.2;
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

.tag-pending {
  color: #525861 !important;
  border-color: rgba(78, 86, 96, 0.3) !important;
  background: rgba(93, 102, 112, 0.12) !important;
}

@media (max-width: 980px) {
  .reservation-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .list-toolbar {
    align-items: flex-start;
    margin-bottom: 12px;
  }

  .reservation-filters {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
    margin-left: 0;
  }

  .reservation-filter-keyword {
    width: min(260px, 100%);
  }

  .reservation-filter-select {
    width: 136px;
  }

  .row-actions {
    flex-wrap: wrap;
    justify-content: flex-end;
  }
}

@media (max-width: 640px) {
  .reservation-stats-grid {
    grid-template-columns: 1fr;
  }

  .reservation-filter-keyword,
  .reservation-filter-select {
    width: 100%;
  }

  .row-actions {
    width: 100%;
    justify-content: stretch;
  }

  .row-actions :deep(.el-button) {
    flex: 1;
    min-width: 0;
  }
}
</style>
