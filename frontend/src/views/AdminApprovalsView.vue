<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Reservation } from '../api/types'
import { apiAdminAuditLogs, apiApprove, apiPending, apiRecentReviewed, apiReject, apiRevokeReview } from '../api/mrs'
import { authStore } from '../store/auth'

const loading = ref(false)
const list = ref<Reservation[]>([])
const reviewed = ref<Reservation[]>([])
const auditLogs = ref<string[]>([])

const isSuperAdmin = computed(() => authStore.isSuperAdmin.value)

const stats = computed(() => ({
  total: list.value.length,
  today: list.value.filter((x) => String(x?.startTime ?? '').slice(0, 10) === new Date().toISOString().slice(0, 10)).length,
  rooms: new Set(list.value.map((x) => x?.roomId)).size,
}))

async function reload() {
  loading.value = true
  try {
    const [resp, reviewedResp, auditResp] = await Promise.all([apiPending(), apiRecentReviewed(), apiAdminAuditLogs()])
    if (resp.code !== 0) return ElMessage.error(resp.message)
    list.value = Array.isArray(resp.data) ? resp.data : []
    reviewed.value = reviewedResp.code === 0 && Array.isArray(reviewedResp.data) ? reviewedResp.data : []
    auditLogs.value = auditResp.code === 0 && Array.isArray(auditResp.data) ? auditResp.data : []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载审批列表失败')
    list.value = []
    reviewed.value = []
    auditLogs.value = []
  } finally {
    loading.value = false
  }
}

async function approve(row: Reservation) {
  const { value } = await ElMessageBox.prompt('审批备注（可空）', '批准', { inputValue: '' })
  const resp = await apiApprove(row.id, value)
  if (resp.code !== 0) return ElMessage.error(resp.message)
  ElMessage.success('已批准')
  await reload()
}

async function reject(row: Reservation) {
  const { value } = await ElMessageBox.prompt('驳回原因', '驳回', { inputValue: '' })
  const resp = await apiReject(row.id, value)
  if (resp.code !== 0) return ElMessage.error(resp.message)
  ElMessage.success('已驳回')
  await reload()
}

async function revoke(row: Reservation) {
  await ElMessageBox.confirm(`确认撤销预约 #${row.id} 的审批结果并回退到待审批吗？`, '撤销审批', { type: 'warning' })
  const resp = await apiRevokeReview(row.id)
  if (resp.code !== 0) return ElMessage.error(resp.message)
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
        <p class="page-subtitle">管理员负责审批待处理预约；超级管理员可撤销历史审批结果并重审。</p>
      </div>
      <el-button :loading="loading" @click="reload">刷新</el-button>
    </section>

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

      <el-table class="approvals-table" :data="list" v-loading="loading" style="width: 100%" :max-height="520">
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
        <el-table-column label="操作" width="190" align="right" header-align="right" fixed="right">
          <template #default="{ row }">
            <div class="row-actions row-actions--right">
              <el-button size="small" type="primary" class="btn-key-solid" @click="approve(row)">批准</el-button>
              <el-dropdown trigger="click">
                <el-button size="small" type="default">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="reject(row)">驳回申请</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="cursor-card table-card" v-if="isSuperAdmin">
      <div class="section-head">
        <div class="section-title">最近已审批记录（超级管理员）</div>
        <div class="section-desc">可将已通过/已驳回预约撤销为待审批，用于重新审查。</div>
      </div>

      <el-table class="approvals-table approvals-table--reviewed" :data="reviewed" v-loading="loading" style="width: 100%" table-layout="fixed" :max-height="420">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="status" label="审批结果" width="120" />
        <el-table-column prop="roomName" label="会议室" min-width="220" />
        <el-table-column prop="username" label="申请人" min-width="180" />
        <el-table-column prop="approvedAt" label="审批时间" min-width="220" />
        <el-table-column label="操作" width="170" align="right" header-align="right" fixed="right">
          <template #default="{ row }">
            <div class="row-actions row-actions--right">
              <el-button size="small" type="primary" class="btn-key-solid" @click="revoke(row)">撤销审批</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="cursor-card table-card">
      <div class="section-head">
        <div class="section-title">全局审计日志</div>
        <div class="section-desc">追踪预约创建、取消、审批等关键行为。</div>
      </div>
      <div v-if="auditLogs.length" class="logs-wrap">
        <div class="log-line" v-for="(l, idx) in auditLogs" :key="`log-${idx}`">{{ l }}</div>
      </div>
      <div v-else class="empty-state">暂无全局审计日志。后续审批、撤销等关键动作会在这里自动沉淀。</div>
    </section>
  </div>
</template>

<style scoped>
:deep(.approvals-table .el-table__row) {
  height: 60px;
}

:deep(.approvals-table .el-table__cell) {
  padding-top: 14px;
  padding-bottom: 14px;
}

:deep(.approvals-table .el-table__body-wrapper) {
  overflow-x: auto;
}

:deep(.approvals-table--reviewed colgroup col:last-child) {
  width: 190px !important;
}

.stats-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.row-actions {
  display: flex;
  gap: 8px;
}

.row-actions--right {
  justify-content: flex-end;
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

.logs-wrap {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.log-line {
  font-size: 12px;
  color: var(--text-muted);
  padding: 6px 8px;
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  background: var(--bg-soft);
}

.muted {
  color: var(--text-muted);
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
