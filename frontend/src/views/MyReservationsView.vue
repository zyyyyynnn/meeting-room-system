<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Reservation } from '../api/types'
import { apiCancelReservation, apiDeleteReservation, apiMyAuditLogs, apiMyRecent } from '../api/mrs'

type ReservationLike = Partial<Reservation> & { reservationId?: number }

const loading = ref(false)
const list = ref<Reservation[]>([])
const auditLogs = ref<string[]>([])

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

const stats = computed(() => ({
  total: list.value.length,
  approved: list.value.filter((x) => x.status === 'APPROVED').length,
  pending: list.value.filter((x) => x.status === 'PENDING').length,
  cancelled: list.value.filter((x) => x.status === 'CANCELLED').length,
}))

async function reload() {
  loading.value = true
  try {
    const [recentResp, auditResp] = await Promise.all([
      apiMyRecent(),
      apiMyAuditLogs(),
    ])

    if (recentResp.code !== 0) return ElMessage.error(recentResp.message)

    const raw = Array.isArray(recentResp.data)
      ? recentResp.data
      : Array.isArray((recentResp.data as any)?.list)
        ? (recentResp.data as any).list
        : Array.isArray((recentResp.data as any)?.records)
          ? (recentResp.data as any).records
          : []

    list.value = raw.map((x: ReservationLike) => normalizeReservation(x)).filter((x: Reservation) => x.id > 0)
    auditLogs.value = auditResp.code === 0 && Array.isArray(auditResp.data) ? auditResp.data : []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载我的预约失败')
    list.value = []
    auditLogs.value = []
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
  if (!r?.id) return ElMessage.warning('无效预约记录，无法取消')
  await ElMessageBox.confirm('确认取消该预约？', '提示', { type: 'warning' })
  const resp = await apiCancelReservation(r.id)
  if (resp.code !== 0) return ElMessage.error(resp.message)
  ElMessage.success('已取消')
  await reload()
}

async function removeReservation(r: Reservation) {
  if (!r?.id) return ElMessage.warning('无效预约记录，无法删除')
  await ElMessageBox.confirm('确认删除该预约记录？删除后不可恢复。', '提示', { type: 'warning' })
  const resp = await apiDeleteReservation(r.id)
  if (resp.code !== 0) return ElMessage.error(resp.message)
  ElMessage.success('已删除')
  await reload()
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
      <el-button :loading="loading" @click="reload">刷新数据</el-button>
    </section>

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
          <div class="section-desc">按时间倒序展示最近记录</div>
        </div>
      </div>

      <el-table class="reservation-table" :data="list" v-loading="loading" style="width: 100%">
        <el-table-column prop="roomName" label="会议室" width="180" />
        <el-table-column prop="startTime" label="开始" width="180" />
        <el-table-column prop="endTime" label="结束" width="180" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag
              :type="statusType(row.status)"
              :class="{ 'tag-pending': row.status === 'PENDING' }"
              effect="plain"
            >
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因/备注">
          <template #default="{ row }">
            <span>{{ row.reason || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="right" header-align="right" fixed="right">
          <template #default="{ row }">
            <div class="row-actions row-actions--right">
              <el-button
                size="small"
                type="default"
                class="btn-cancel-soft"
                @click="cancel(row)"
                :disabled="row.status === 'CANCELLED' || row.status === 'REJECTED'"
              >
                取消
              </el-button>
              <el-dropdown trigger="click">
                <el-button size="small" type="default">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="removeReservation(row)">删除预约</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="cursor-card table-card">
      <div class="section-head">
        <div class="section-title">审计日志</div>
        <div class="section-desc">记录预约创建、取消、删除、审批等关键动作</div>
      </div>
      <el-empty description="暂无日志。你可以进入“审批中心”查看跨页面日志聚合。" />
    </section>
  </div>
</template>

<style scoped>
.reservation-stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.list-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 20px;
}

.section-head--compact {
  margin-bottom: 0;
}

.reservation-notice {
  width: min(460px, 52%);
  margin: 0;
  align-self: stretch;
  border-radius: var(--radius-sm);
}

.row-actions {
  display: flex;
  gap: 8px;
}

.row-actions--right {
  justify-content: flex-end;
}

:deep(.reservation-table .el-table__row) {
  height: 60px;
}

:deep(.reservation-table .el-table__cell) {
  padding-top: 14px;
  padding-bottom: 14px;
}

.logs-wrap {
  margin: 10px 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.logs-wrap--notice {
  margin: 8px 0 0;
}

.log-line {
  font-size: 12px;
  color: var(--text-muted);
  padding: 6px 8px;
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  background: var(--bg-soft);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag-pending {
  color: #5a492f !important;
  border-color: rgba(138, 108, 62, 0.5) !important;
  background: rgba(168, 131, 74, 0.22) !important;
}

.btn-cancel-soft {
  --el-button-bg-color: rgba(255, 255, 255, 0.88) !important;
  --el-button-border-color: rgba(255, 255, 255, 0.98) !important;
  --el-button-text-color: #4f5a54 !important;
  --el-button-hover-bg-color: rgba(255, 255, 255, 0.96) !important;
  --el-button-hover-border-color: rgba(255, 255, 255, 0.98) !important;
  --el-button-active-bg-color: rgba(244, 245, 243, 0.94) !important;
  --el-button-active-border-color: rgba(233, 236, 232, 0.98) !important;
}

@media (max-width: 980px) {
  .reservation-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .list-toolbar {
    flex-direction: column;
    margin-bottom: 12px;
  }
}

@media (max-width: 640px) {
  .reservation-stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
