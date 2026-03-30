<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getRequestErrorMessage } from '../api/http'
import { apiUpdateUserEnabled, apiUpdateUserRole, apiUserList } from '../api/mrs'
import type { UserAccount } from '../api/types'
import PageStatusPanel from '../components/PageStatusPanel.vue'
import { authStore } from '../store/auth'

type ViewState = 'loading' | 'ready' | 'error'

const loading = ref(false)
const viewState = ref<ViewState>('loading')
const statusMessage = ref('')
const hasLoadedOnce = ref(false)
const users = ref<UserAccount[]>([])

const filters = reactive({
  keyword: '',
  role: 'ALL' as 'ALL' | 'SUPER_ADMIN' | 'ADMIN' | 'USER',
  enabled: 'ALL' as 'ALL' | 'ENABLED' | 'DISABLED',
})

const myUserId = computed(() => authStore.state.userId)

const filteredUsers = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return users.value.filter((user) => {
    const hitKeyword = !keyword || String(user.username ?? '').toLowerCase().includes(keyword)
    const hitRole = filters.role === 'ALL' || user.role === filters.role
    const hitEnabled =
      filters.enabled === 'ALL' ||
      (filters.enabled === 'ENABLED' && user.enabled) ||
      (filters.enabled === 'DISABLED' && !user.enabled)
    return hitKeyword && hitRole && hitEnabled
  })
})

const showBlockingState = computed(() => !hasLoadedOnce.value && viewState.value !== 'ready')
const showInlineError = computed(() => hasLoadedOnce.value && viewState.value === 'error')
const stateTitle = computed(() => (viewState.value === 'loading' ? '正在加载用户与权限数据' : '用户与权限数据暂时不可用'))
const stateDescription = computed(() => {
  if (viewState.value === 'loading') {
    return '正在同步账号角色和启用状态，请稍候。'
  }
  return statusMessage.value || '当前无法获取用户列表，请稍后重试。'
})
const emptyUserMessage = computed(() => {
  if (users.value.length) {
    return '当前筛选条件下暂无匹配账号，请调整筛选条件后再试。'
  }
  return '当前暂无用户数据，后续注册或导入的账号会显示在这里。'
})

function isSelf(row: UserAccount) {
  return row.id === myUserId.value
}

function canEditRole(row: UserAccount) {
  if (isSelf(row)) return false
  if (row.role === 'SUPER_ADMIN' && !authStore.isSuperAdmin.value) return false
  return true
}

function canEditEnabled(row: UserAccount) {
  if (isSelf(row)) return false
  if (row.role === 'SUPER_ADMIN' && !authStore.isSuperAdmin.value) return false
  return true
}

function enabledLabel(enabled: boolean) {
  return enabled ? '已启用' : '已停用'
}

async function reload() {
  const preserveContent = hasLoadedOnce.value
  loading.value = true
  statusMessage.value = ''

  if (!preserveContent) {
    viewState.value = 'loading'
  }

  try {
    const resp = await apiUserList()
    if (resp.code !== 0) {
      viewState.value = 'error'
      statusMessage.value = resp.message || '加载用户列表失败'
      if (!preserveContent) {
        users.value = []
      }
      return
    }
    users.value = Array.isArray(resp.data) ? resp.data : []
    hasLoadedOnce.value = true
    viewState.value = 'ready'
  } catch (error) {
    viewState.value = 'error'
    statusMessage.value = getRequestErrorMessage(error, '加载用户列表失败')
    if (!preserveContent) {
      users.value = []
    }
  } finally {
    loading.value = false
  }
}

async function onRoleChange(row: UserAccount, role: 'SUPER_ADMIN' | 'ADMIN' | 'USER') {
  if (!canEditRole(row)) {
    ElMessage.warning('该账号角色不允许当前操作')
    return
  }
  if (role === 'SUPER_ADMIN' && !authStore.isSuperAdmin.value) {
    ElMessage.warning('仅超级管理员可授予超级管理员权限')
    return
  }
  const resp = await apiUpdateUserRole(row.id, role)
  if (resp.code !== 0) {
    ElMessage.error(resp.message)
    await reload()
    return
  }
  ElMessage.success('角色已更新')
  await reload()
}

async function onEnabledChange(row: UserAccount, enabled: boolean) {
  if (!canEditEnabled(row)) {
    ElMessage.warning('该账号状态不允许当前操作')
    return
  }
  const resp = await apiUpdateUserEnabled(row.id, enabled)
  if (resp.code !== 0) {
    ElMessage.error(resp.message)
    await reload()
    return
  }
  ElMessage.success('状态已更新')
  await reload()
}

function handleRoleChange(row: UserAccount, value: string) {
  if (value === 'SUPER_ADMIN' || value === 'ADMIN' || value === 'USER') {
    onRoleChange(row, value)
  }
}

function handleEnabledChange(row: UserAccount, value: boolean | string | number) {
  onEnabledChange(row, Boolean(value))
}

function resetFilters() {
  filters.keyword = ''
  filters.role = 'ALL'
  filters.enabled = 'ALL'
}

onMounted(reload)
</script>

<template>
  <div class="page-wrap">
    <section class="page-hero cursor-card">
      <div>
        <h2 class="page-title">用户与权限管理</h2>
        <p class="page-subtitle">集中管理账号角色与启用状态，确保系统权限边界清晰可控。</p>
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
        title="已保留上次同步的用户数据"
        :description="statusMessage"
        action-text="重新加载"
        @action="reload"
      />

      <section class="stats-grid user-stats-grid">
        <article class="stat-card cursor-card">
          <div class="k">用户总数</div>
          <div class="v">{{ users.length }}</div>
        </article>
        <article class="stat-card cursor-card">
          <div class="k">超级管理员</div>
          <div class="v">{{ users.filter((x) => x.role === 'SUPER_ADMIN').length }}</div>
        </article>
        <article class="stat-card cursor-card">
          <div class="k">管理员</div>
          <div class="v">{{ users.filter((x) => x.role === 'ADMIN').length }}</div>
        </article>
        <article class="stat-card cursor-card">
          <div class="k">已停用</div>
          <div class="v">{{ users.filter((x) => !x.enabled).length }}</div>
        </article>
      </section>

      <section class="cursor-card table-card user-table-card">
        <div class="list-toolbar user-list-toolbar">
          <div class="section-head section-head--compact">
            <div class="section-title">账号列表</div>
            <div class="section-desc">统一查看账号信息，并直接调整角色与启用状态。</div>
          </div>
          <div class="user-filters">
            <el-input v-model="filters.keyword" clearable placeholder="搜索用户名" class="user-filter-keyword" />
            <el-select v-model="filters.role" class="user-filter-select">
              <el-option label="全部角色" value="ALL" />
              <el-option label="普通用户" value="USER" />
              <el-option label="管理员" value="ADMIN" />
              <el-option label="超级管理员" value="SUPER_ADMIN" />
            </el-select>
            <el-select v-model="filters.enabled" class="user-filter-select">
              <el-option label="全部状态" value="ALL" />
              <el-option label="已启用" value="ENABLED" />
              <el-option label="已停用" value="DISABLED" />
            </el-select>
            <el-button type="primary" class="user-filters__reset btn-key-soft" @click="resetFilters">重置筛选</el-button>
          </div>
        </div>

        <el-table v-if="filteredUsers.length" class="users-table" :data="filteredUsers" style="width: 100%" :max-height="560">
          <el-table-column label="账号" min-width="220">
            <template #default="{ row }">
              <div class="user-cell">
                <div class="user-cell__head">
                  <div class="user-cell__name" :title="row.username">{{ row.username }}</div>
                  <el-tag v-if="isSelf(row)" type="info" effect="plain" size="small">当前账号</el-tag>
                </div>
                <div class="user-cell__meta">账号 ID {{ row.id }}</div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="角色" min-width="240">
            <template #default="{ row }">
              <div class="user-role-cell">
                <el-select class="user-row-select" :model-value="row.role" :disabled="!canEditRole(row)" @change="handleRoleChange(row, $event)">
                  <el-option label="普通用户" value="USER" />
                  <el-option label="管理员" value="ADMIN" />
                  <el-option v-if="authStore.isSuperAdmin.value" label="超级管理员" value="SUPER_ADMIN" />
                </el-select>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="启用状态" min-width="220">
            <template #default="{ row }">
              <div class="user-enabled-cell">
                <div class="user-enabled-head">
                  <span class="user-status-text" :class="{ 'is-disabled': !row.enabled }">{{ enabledLabel(row.enabled) }}</span>
                  <el-switch :model-value="row.enabled" :disabled="!canEditEnabled(row)" @change="handleEnabledChange(row, $event)" />
                </div>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-state">{{ emptyUserMessage }}</div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.user-stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.user-table-card {
  padding-top: 20px;
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

.user-list-toolbar .section-head {
  max-width: min(38ch, 100%);
}

.user-filters {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: nowrap;
  margin-left: auto;
}

.user-filter-keyword {
  width: 228px;
}

.user-filter-select {
  width: 148px;
}

.user-filters__reset {
  min-width: 92px;
  height: var(--control-height);
  padding: 0 12px;
  border-radius: var(--radius-unified);
  flex: 0 0 auto;
}

:deep(.user-filters .el-input__wrapper),
:deep(.user-filters .el-select__wrapper) {
  min-height: var(--control-height);
  height: var(--control-height);
  border-radius: var(--radius-unified);
}

:deep(.user-filters .el-input__inner),
:deep(.user-filters .el-select__selected-item),
:deep(.user-filters .el-select__placeholder) {
  font-size: 14px;
  line-height: 1.2;
}

.users-table :deep(.el-table__cell) {
  vertical-align: top;
}

.user-cell,
.user-role-cell,
.user-enabled-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.user-cell__head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  width: 100%;
  min-width: 0;
}

.user-cell__name {
  color: var(--text-main);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.35;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-cell__meta {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.5;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.user-row-select {
  width: 176px;
}

.user-enabled-head {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 172px;
  min-height: var(--control-height);
  white-space: nowrap;
}

.user-status-text {
  color: var(--text-main);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.3;
  white-space: nowrap;
}

.user-status-text.is-disabled {
  color: var(--text-muted);
}

@media (max-width: 1180px) {
  .user-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .list-toolbar {
    align-items: flex-start;
    margin-bottom: 12px;
  }

  .user-filters {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
    margin-left: 0;
  }

  .user-filter-keyword {
    width: min(260px, 100%);
  }

  .user-filter-select {
    width: 136px;
  }
}

@media (max-width: 640px) {
  .user-stats-grid {
    grid-template-columns: 1fr;
  }

  .user-filters {
    gap: 8px;
  }

  .user-filter-keyword,
  .user-filter-select,
  .user-filters__reset {
    width: 100%;
    min-width: 0;
  }

  .user-row-select {
    width: 100%;
  }

  .user-enabled-head {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
