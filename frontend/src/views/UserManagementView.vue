<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiUpdateUserEnabled, apiUpdateUserRole, apiUserList } from '../api/mrs'
import type { UserAccount } from '../api/types'
import { authStore } from '../store/auth'

const loading = ref(false)
const users = ref<UserAccount[]>([])

const myUserId = computed(() => authStore.state.userId)

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

async function reload() {
  loading.value = true
  try {
    const resp = await apiUserList()
    if (resp.code !== 0) {
      ElMessage.error(resp.message)
      return
    }
    users.value = resp.data ?? []
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

onMounted(reload)
</script>

<template>
  <div class="page-wrap">
    <section class="page-hero cursor-card">
      <div>
        <h2 class="page-title">用户与权限管理</h2>
        <p class="page-subtitle">集中管理账号角色与启用状态，确保系统权限边界清晰可控。</p>
      </div>
      <el-button :loading="loading" @click="reload">刷新</el-button>
    </section>

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

    <section class="cursor-card table-card">
      <div class="section-head">
        <div class="section-title">账号列表</div>
        <div class="section-desc">仅具备权限的管理员可编辑角色与启用状态。</div>
      </div>
      <el-table class="users-table" :data="users" v-loading="loading" style="width: 100%" :max-height="560">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="180" />
        <el-table-column label="角色" width="240">
          <template #default="{ row }">
            <el-select
              :model-value="row.role"
              style="width: 180px"
              :disabled="!canEditRole(row)"
              @change="(v) => onRoleChange(row, v)"
            >
              <el-option label="普通用户" value="USER" />
              <el-option label="管理员" value="ADMIN" />
              <el-option v-if="authStore.isSuperAdmin.value" label="超级管理员" value="SUPER_ADMIN" />
            </el-select>
            <div class="hint" v-if="isSelf(row)">当前登录账号不可修改自身角色</div>
          </template>
        </el-table-column>
        <el-table-column label="启用状态" width="220">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled" :disabled="!canEditEnabled(row)" @change="(v) => onEnabledChange(row, !!v)" />
            <div class="hint" v-if="isSelf(row)">当前登录账号不可停用自身</div>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.user-stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

:deep(.users-table .el-table__row) {
  height: 60px;
}

:deep(.users-table .el-table__cell) {
  padding-top: 14px;
  padding-bottom: 14px;
}

.hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

@media (max-width: 980px) {
  .user-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .user-stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
