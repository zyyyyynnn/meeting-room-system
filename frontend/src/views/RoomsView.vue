<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Room } from '../api/types'
import { apiAddRoomMaintenance, apiCreateRoom, apiDeleteRoom, apiRooms, apiUpdateRoom, apiUpdateRoomStatus } from '../api/mrs'
import { authStore } from '../store/auth'

const rooms = ref<Room[]>([])
const loading = ref(false)
const dialogOpen = ref(false)
const editing = ref<Room | null>(null)
const maintenanceOpen = ref(false)
const filterDrawerOpen = ref(false)
const baseEquipmentOptions = [
  '投影仪',
  '显示屏',
  '摄像头',
  '麦克风',
  '音响',
  '无线投屏',
]

const equipmentOptions = ref<string[]>([...baseEquipmentOptions])

const form = reactive({
  name: '',
  capacity: 10,
  equipment: [] as string[],
})

const maintenanceForm = reactive({
  roomId: 0,
  status: 'AVAILABLE' as 'AVAILABLE' | 'MAINTENANCE' | 'DISABLED',
  startTime: '',
  endTime: '',
  reason: '',
})

const filters = reactive({
  keyword: '',
  capacity: 'ALL' as 'ALL' | 'SMALL' | 'MEDIUM' | 'LARGE',
  equipment: '' as string | null,
})

const isAdmin = computed(() => authStore.isAdmin.value)

const equipmentAliasMap: Record<string, string> = {
  '鎶曞奖浠': '投影仪',
  '鐧芥澘': '白板',
  '鐢佃?': '电视',
  '瑙嗛?浼氳?': '视频会议',
}

function normalizeText(input: unknown) {
  const raw = typeof input === 'string' ? input : ''
  if (!raw) return ''

  const cleaned = raw
    .replace(/[\uFFFD]/g, '')
    .replace(/[\u0000-\u001F]/g, '')
    .trim()

  if (!cleaned) return ''
  return equipmentAliasMap[cleaned] ?? cleaned
}

const viewRooms = computed(() => {
  const normalized = rooms.value.map((r) => ({
    ...r,
    name: normalizeText(r.name) || `会议室-${r.id}`,
    equipment: Array.isArray(r.equipment)
      ? r.equipment.map((x) => normalizeText(x)).filter((x) => !!x && x.toLowerCase() !== 'nodata')
      : [],
  }))

  return normalized.filter((r) => {
    const kw = filters.keyword.trim().toLowerCase()
    const kwOk = !kw || r.name.toLowerCase().includes(kw)

    const cap = r.capacity ?? 0
    const capOk =
      filters.capacity === 'ALL' ||
      (filters.capacity === 'SMALL' && cap >= 2 && cap <= 6) ||
      (filters.capacity === 'MEDIUM' && cap >= 7 && cap <= 10) ||
      (filters.capacity === 'LARGE' && cap > 10)

    const eq = (filters.equipment ?? '').trim()
    const eqOk = !eq || r.equipment.includes(eq)

    return kwOk && capOk && eqOk
  })
})

const stats = computed(() => ({
  total: viewRooms.value.length,
  available: viewRooms.value.filter((r) => r.status === 'AVAILABLE').length,
  maintenance: viewRooms.value.filter((r) => r.status === 'MAINTENANCE').length,
  disabled: viewRooms.value.filter((r) => r.status === 'DISABLED').length,
  equipmentCount: viewRooms.value.reduce((acc, r) => acc + (r.equipment?.length ?? 0), 0),
}))

function dedupeAndSortEquipment(items: string[]) {
  return Array.from(new Set(items.map((x) => normalizeText(x)).filter(Boolean))).sort((a, b) => a.localeCompare(b, 'zh-Hans-CN'))
}

function syncEquipmentOptions() {
  const dynamic: string[] = []
  viewRooms.value.forEach((r) => {
    r.equipment.forEach((e) => dynamic.push(e))
  })
  equipmentOptions.value = dedupeAndSortEquipment([...baseEquipmentOptions, ...dynamic])
}

async function reload() {
  loading.value = true
  try {
    const resp = await apiRooms()
    if (resp.code !== 0) return ElMessage.error(resp.message)
    rooms.value = Array.isArray(resp.data) ? resp.data : []
    syncEquipmentOptions()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.name = ''
  form.capacity = 10
  form.equipment = []
  dialogOpen.value = true
}

function openEdit(r: Room) {
  editing.value = r
  form.name = normalizeText(r.name)
  form.capacity = r.capacity
  form.equipment = Array.isArray(r.equipment)
    ? dedupeAndSortEquipment(r.equipment.filter((x) => normalizeText(x).toLowerCase() !== 'nodata'))
    : []
  dialogOpen.value = true
}

async function save() {
  const payload = {
    name: normalizeText(form.name),
    capacity: form.capacity,
    equipment: dedupeAndSortEquipment(form.equipment.filter((x) => normalizeText(x).toLowerCase() !== 'nodata')),
  }
  if (!payload.name) return ElMessage.warning('请输入会议室名称')

  const resp = editing.value ? await apiUpdateRoom(editing.value.id, payload) : await apiCreateRoom(payload)
  if (resp.code !== 0) return ElMessage.error(resp.message)
  ElMessage.success('保存成功')
  dialogOpen.value = false
  await reload()
}

async function del(r: Room) {
  await ElMessageBox.confirm(`确认删除会议室「${r.name}」？`, '提示', { type: 'warning' })
  const resp = await apiDeleteRoom(r.id)
  if (resp.code !== 0) return ElMessage.error(resp.message)
  ElMessage.success('已删除')
  await reload()
}

function openMaintenance(r: Room) {
  maintenanceForm.roomId = r.id
  maintenanceForm.status = r.status ?? 'AVAILABLE'
  maintenanceForm.startTime = ''
  maintenanceForm.endTime = ''
  maintenanceForm.reason = ''
  maintenanceOpen.value = true
}

async function saveMaintenance() {
  const { roomId, status, startTime, endTime, reason } = maintenanceForm
  const statusResp = await apiUpdateRoomStatus(roomId, status)
  if (statusResp.code !== 0) return ElMessage.error(statusResp.message)

  if (startTime && endTime) {
    const maintenanceResp = await apiAddRoomMaintenance(roomId, { startTime, endTime, reason })
    if (maintenanceResp.code !== 0) return ElMessage.error(maintenanceResp.message)
  }

  ElMessage.success('会议室状态/维护已更新')
  maintenanceOpen.value = false
  await reload()
}

onMounted(reload)
</script>

<template>
  <div class="page-wrap">
    <section class="page-hero cursor-card">
      <div>
        <h2 class="page-title">会议室管理</h2>
        <p class="page-subtitle">统一维护会议室容量与设备信息，确保预约资源清晰可控。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="reload">刷新</el-button>
        <el-button v-if="isAdmin" type="primary" class="theme-primary-btn" @click="openCreate">新增会议室</el-button>
      </div>
    </section>

    <section class="stats-grid room-stats-grid">
      <article class="stat-card cursor-card tone-total"><div class="k">会议室总数</div><div class="v">{{ stats.total }}</div></article>
      <article class="stat-card cursor-card tone-available"><div class="k">可用中</div><div class="v">{{ stats.available }}</div></article>
      <article class="stat-card cursor-card tone-maintenance"><div class="k">维修中</div><div class="v">{{ stats.maintenance }}</div></article>
      <article class="stat-card cursor-card tone-disabled"><div class="k">已停用</div><div class="v">{{ stats.disabled }}</div></article>
      <article class="stat-card cursor-card tone-equipment"><div class="k">设备条目数</div><div class="v">{{ stats.equipmentCount }}</div></article>
    </section>

    <section class="cursor-card table-card room-table-card">
      <div class="section-head room-head">
        <div>
          <div class="section-title">会议室列表</div>
          <div class="section-desc">普通用户仅可查看，管理员可新增、编辑、维护状态和删除。</div>
        </div>
        <div class="room-toolbar-actions">
          <el-input v-model="filters.keyword" clearable placeholder="按会议室名称搜索" class="filter-inline-keyword" />
          <el-button type="default" @click="filterDrawerOpen = true">高级筛选</el-button>
        </div>
      </div>

      <el-table class="rooms-table" :data="viewRooms" v-loading="loading" style="width: 100%" :max-height="560">
        <el-table-column prop="name" label="名称" width="180" />
        <el-table-column prop="capacity" label="容量" width="100" />
        <el-table-column label="设备">
          <template #default="{ row }">
            <div v-if="row.equipment?.length" class="tags-wrap">
              <el-tag v-for="e in row.equipment" :key="e" effect="plain">{{ e }}</el-tag>
            </div>
            <span v-else class="empty-inline">暂无设备配置，可在编辑中补充。</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'AVAILABLE' ? 'success' : row.status === 'MAINTENANCE' ? 'warning' : 'danger'" effect="plain">
              {{ row.status === 'AVAILABLE' ? '可用' : row.status === 'MAINTENANCE' ? '维修中' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isAdmin" label="操作" width="190" align="right" header-align="right" fixed="right">
          <template #default="{ row }">
            <div class="row-actions row-actions--right">
              <el-button size="small" type="primary" class="btn-key-solid" @click="openEdit(row)">编辑</el-button>
              <el-dropdown trigger="click">
                <el-button size="small" type="default">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="openMaintenance(row)">状态/维护</el-dropdown-item>
                    <el-dropdown-item @click="del(row)">删除会议室</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>

  <el-drawer
    v-model="filterDrawerOpen"
    title="高级筛选"
    size="min(420px, 92vw)"
    :with-header="true"
  >
    <div class="drawer-filters">
      <el-form label-position="top">
        <el-form-item label="容量区间">
          <el-select v-model="filters.capacity" style="width: 100%">
            <el-option label="全部容量" value="ALL" />
            <el-option label="2-6 人" value="SMALL" />
            <el-option label="7-10 人" value="MEDIUM" />
            <el-option label="10 人以上" value="LARGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备类型">
          <el-select
            v-model="filters.equipment"
            clearable
            filterable
            placeholder="按设备筛选"
            style="width: 100%"
            :popper-class="'filter-equipment-popper'"
          >
            <el-option v-for="x in equipmentOptions" :key="`f-${x}`" :label="x" :value="x" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>
  </el-drawer>

  <el-dialog v-model="dialogOpen" :title="editing ? '编辑会议室' : '新增会议室'" width="min(560px, 92vw)">
    <el-form label-width="90px">
      <el-form-item label="名称">
        <el-input v-model="form.name" placeholder="请输入会议室名称" />
      </el-form-item>
      <el-form-item label="容量">
        <el-input-number v-model="form.capacity" :min="1" />
      </el-form-item>
      <el-form-item label="设备">
        <el-select
          v-model="form.equipment"
          multiple
          filterable
          allow-create
          default-first-option
          :reserve-keyword="false"
          style="width: 100%"
          placeholder="请选择或输入设备"
          no-data-text="可直接输入设备后回车创建"
        >
          <el-option v-for="x in equipmentOptions" :key="x" :label="x" :value="x" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogOpen = false">取消</el-button>
      <el-button type="primary" class="theme-primary-btn" @click="save">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="maintenanceOpen" title="会议室状态与维护" width="min(560px, 92vw)">
    <el-form label-width="110px">
      <el-form-item label="会议室状态">
        <el-select v-model="maintenanceForm.status" style="width: 220px">
          <el-option label="可用" value="AVAILABLE" />
          <el-option label="维修中" value="MAINTENANCE" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item label="维护开始">
        <el-date-picker v-model="maintenanceForm.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="可选" />
      </el-form-item>
      <el-form-item label="维护结束">
        <el-date-picker v-model="maintenanceForm.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="可选" />
      </el-form-item>
      <el-form-item label="维护原因">
        <el-input v-model="maintenanceForm.reason" placeholder="如：投影设备检修" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="maintenanceOpen = false">取消</el-button>
      <el-button type="primary" class="theme-primary-btn" @click="saveMaintenance">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.btn-key-solid {
  --el-button-bg-color: var(--accent) !important;
  --el-button-border-color: var(--accent) !important;
  --el-button-text-color: var(--bg-card-strong) !important;
  --el-button-hover-bg-color: var(--accent-strong) !important;
  --el-button-hover-border-color: var(--accent-strong) !important;
}

.row-actions {
  display: flex;
  gap: 8px;
}

.row-actions--right {
  justify-content: flex-end;
}

:deep(.rooms-table .el-table__row) {
  height: 60px;
}

:deep(.rooms-table .el-table__cell) {
  padding-top: 14px;
  padding-bottom: 14px;
}

.room-stats-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.room-table-card {
  padding-top: 20px;
}

.room-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-inline-keyword {
  width: min(360px, 60vw);
}

.drawer-filters {
  padding: 4px 2px;
}

.room-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.tone-total,
.tone-available,
.tone-maintenance,
.tone-disabled,
.tone-equipment {
  background: rgba(255, 255, 255, 0.42);
  border-color: var(--line-soft);
}

.tags-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.empty-inline {
  color: var(--text-weak);
  font-size: 12px;
}

:deep(.filter-equipment-popper .el-select-dropdown__item) {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1400px) {
  .room-stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .room-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .room-stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
