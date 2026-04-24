<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RefreshRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { getRequestErrorMessage } from '../api/http'
import type { Room } from '../api/types'
import { apiAddRoomMaintenance, apiCreateRoom, apiDeleteRoom, apiRooms, apiUpdateRoom, apiUpdateRoomStatus } from '../api/mrs'
import PageStatusPanel from '../components/PageStatusPanel.vue'
import { authStore } from '../store/auth'

type ViewState = 'loading' | 'ready' | 'error'

const rooms = ref<Room[]>([])
const loading = ref(false)
const viewState = ref<ViewState>('loading')
const statusMessage = ref('')
const hasLoadedOnce = ref(false)
const dialogOpen = ref(false)
const editing = ref<Room | null>(null)
const route = useRoute()

const EQUIPMENT_LABELS = {
  projector: '投影仪',
  display: '显示屏',
  camera: '摄像头',
  microphone: '麦克风',
  speaker: '音响',
  wirelessCast: '无线投屏',
} as const

const baseEquipmentOptions = Object.values(EQUIPMENT_LABELS)
const equipmentOptions = ref<string[]>([...baseEquipmentOptions])
const equipmentSortOrder: Map<string, number> = new Map(baseEquipmentOptions.map((item, index) => [item, index]))

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

function resetMaintenanceForm(room?: Room | null) {
  maintenanceForm.roomId = room?.id ?? 0
  maintenanceForm.status = room?.status ?? 'AVAILABLE'
  maintenanceForm.startTime = ''
  maintenanceForm.endTime = ''
  maintenanceForm.reason = ''
}

const filters = reactive({
  keyword: '',
  capacity: 'ALL' as 'ALL' | 'SMALL' | 'MEDIUM' | 'LARGE',
  status: 'ALL' as 'ALL' | 'AVAILABLE' | 'MAINTENANCE' | 'DISABLED',
  equipment: 'ALL' as string,
})

function applyRouteFilters() {
  const status = String(route.query.status ?? '').toUpperCase()
  filters.status = ['ALL', 'AVAILABLE', 'MAINTENANCE', 'DISABLED'].includes(status)
    ? (status as typeof filters.status)
    : 'ALL'
  filters.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
}

const isAdmin = computed(() => authStore.isAdmin.value)
const showBlockingState = computed(() => !hasLoadedOnce.value && viewState.value === 'error')
const showInlineError = computed(() => hasLoadedOnce.value && viewState.value === 'error')
const stateTitle = computed(() => '会议室数据暂时不可用')
const stateDescription = computed(() => statusMessage.value || '当前无法获取会议室列表，请稍后重试。')

const equipmentAliasMap: Record<string, string> = {
  投影设备: EQUIPMENT_LABELS.projector,
  投影机: EQUIPMENT_LABELS.projector,
  电视: EQUIPMENT_LABELS.display,
  白板电视: EQUIPMENT_LABELS.display,
  视频会议: EQUIPMENT_LABELS.camera,
  麦克风阵列: EQUIPMENT_LABELS.microphone,
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

function normalizeEquipmentLabel(input: unknown) {
  const value = normalizeText(input)
  if (!value) return ''

  const lower = value.toLowerCase()
  if (/projector|[投影]/.test(lower)) return EQUIPMENT_LABELS.projector
  if (/display|screen|[显示电视]/.test(lower)) return EQUIPMENT_LABELS.display
  if (/camera|[摄像视频]/.test(lower)) return EQUIPMENT_LABELS.camera
  if (/microphone|mic|[麦克风]/.test(lower)) return EQUIPMENT_LABELS.microphone
  if (/speaker|audio|[音响]/.test(lower)) return EQUIPMENT_LABELS.speaker
  if (/wireless|cast|[投屏无线]/.test(lower)) return EQUIPMENT_LABELS.wirelessCast

  if ((baseEquipmentOptions as readonly string[]).includes(value)) return value
  return ''
}

function normalizeEquipmentForForm(input: unknown) {
  const value = normalizeEquipmentLabel(input)
  if (!value) return ''
  if (value.toLowerCase() === 'nodata') return ''
  return value
}

const viewRooms = computed(() => {
  const normalized = rooms.value.map((room) => ({
    ...room,
    name: normalizeText(room.name) || `会议室 ${room.id}`,
    equipment: Array.isArray(room.equipment)
      ? room.equipment.map((item) => normalizeEquipmentForForm(item)).filter(Boolean)
      : [],
  }))

  return normalized.filter((room) => {
    const keyword = filters.keyword.trim().toLowerCase()
    const keywordMatched = !keyword || room.name.toLowerCase().includes(keyword)

    const capacity = room.capacity ?? 0
    const capacityMatched =
      filters.capacity === 'ALL' ||
      (filters.capacity === 'SMALL' && capacity >= 2 && capacity <= 6) ||
      (filters.capacity === 'MEDIUM' && capacity >= 7 && capacity <= 10) ||
      (filters.capacity === 'LARGE' && capacity > 10)

    const equipment = (filters.equipment ?? '').trim()
    const equipmentMatched = equipment === 'ALL' || !equipment || room.equipment.includes(equipment)
    const statusMatched = filters.status === 'ALL' || (room.status ?? 'AVAILABLE') === filters.status

    return keywordMatched && capacityMatched && equipmentMatched && statusMatched
  })
})

const stats = computed(() => ({
  total: viewRooms.value.length,
  available: viewRooms.value.filter((room) => room.status === 'AVAILABLE').length,
  maintenance: viewRooms.value.filter((room) => room.status === 'MAINTENANCE').length,
  disabled: viewRooms.value.filter((room) => room.status === 'DISABLED').length,
  equipmentCount: equipmentOptions.value.length,
}))

const emptyRoomMessage = computed(() => {
  if (rooms.value.length) {
    return '当前筛选条件下暂无匹配的会议室，请调整筛选条件后再试。'
  }
  return isAdmin.value
    ? '当前还没有会议室数据。你可以先创建会议室，再回来继续维护设备与状态。'
    : '当前暂无可展示的会议室信息，请稍后再试。'
})

function dedupeAndSortEquipment(items: string[]) {
  const normalized = Array.from(new Set(items.map((item) => normalizeEquipmentForForm(item)).filter(Boolean)))
  return normalized.sort((a, b) => (equipmentSortOrder.get(a) ?? Number.MAX_SAFE_INTEGER) - (equipmentSortOrder.get(b) ?? Number.MAX_SAFE_INTEGER))
}

function syncEquipmentOptions() {
  equipmentOptions.value = [...baseEquipmentOptions]
}

async function reload() {
  const preserveContent = hasLoadedOnce.value
  loading.value = true
  statusMessage.value = ''

  if (!preserveContent) {
    viewState.value = 'loading'
  }

  try {
    const resp = await apiRooms()
    if (resp.code !== 0) {
      viewState.value = 'error'
      statusMessage.value = resp.message || '加载会议室失败'
      if (!preserveContent) {
        rooms.value = []
        equipmentOptions.value = [...baseEquipmentOptions]
      }
      return
    }
    rooms.value = Array.isArray(resp.data) ? resp.data : []
    syncEquipmentOptions()
    hasLoadedOnce.value = true
    viewState.value = 'ready'
  } catch (error) {
    viewState.value = 'error'
    statusMessage.value = getRequestErrorMessage(error, '加载会议室失败')
    if (!preserveContent) {
      rooms.value = []
      equipmentOptions.value = [...baseEquipmentOptions]
    }
  } finally {
    loading.value = false
  }
}

function resetAdvancedFilters() {
  filters.capacity = 'ALL'
  filters.status = 'ALL'
  filters.equipment = 'ALL'
}

function openCreate() {
  editing.value = null
  form.name = ''
  form.capacity = 10
  form.equipment = []
  resetMaintenanceForm(null)
  dialogOpen.value = true
}

function openEdit(room: Room) {
  editing.value = room
  form.name = normalizeText(room.name)
  form.capacity = room.capacity
  form.equipment = Array.isArray(room.equipment)
    ? dedupeAndSortEquipment(room.equipment.filter((item) => normalizeText(item).toLowerCase() !== 'nodata'))
    : []
  resetMaintenanceForm(room)
  dialogOpen.value = true
}

async function save() {
  const editId = editing.value?.id ?? 0
  const isEditing = Boolean(editing.value)
  const payload = {
    name: normalizeText(form.name),
    capacity: form.capacity,
    equipment: dedupeAndSortEquipment(form.equipment.filter((item) => normalizeText(item).toLowerCase() !== 'nodata')),
  }

  if (!payload.name) {
    ElMessage.warning('请输入会议室名称')
    return
  }

  const resp = editing.value ? await apiUpdateRoom(editing.value.id, payload) : await apiCreateRoom(payload)
  if (resp.code !== 0) {
    ElMessage.error(resp.message)
    return
  }

  if (isEditing && isAdmin.value) {
    const { status, startTime, endTime, reason } = maintenanceForm
    const statusResp = await apiUpdateRoomStatus(editId, status)
    if (statusResp.code !== 0) {
      ElMessage.error(statusResp.message)
      return
    }

    if (startTime && endTime) {
      const maintenanceResp = await apiAddRoomMaintenance(editId, { startTime, endTime, reason })
      if (maintenanceResp.code !== 0) {
        ElMessage.error(maintenanceResp.message)
        return
      }
    }
  }

  ElMessage.success('保存成功')
  dialogOpen.value = false
  await reload()
}

async function del(room: Room) {
  await ElMessageBox.confirm(`确认删除会议室「${normalizeText(room.name) || room.name}」？`, '提示', {
    type: 'warning',
    cancelButtonText: '取消',
    confirmButtonText: '确定',
    cancelButtonClass: 'btn-key-soft cancel-btn-force',
    confirmButtonClass: 'btn-danger-soft confirm-btn-force',
  })
  const resp = await apiDeleteRoom(room.id)
  if (resp.code !== 0) {
    ElMessage.error(resp.message)
    return
  }

  ElMessage.success('已删除会议室')
  await reload()
}

onMounted(reload)

watch(
  () => route.query,
  () => {
    applyRouteFilters()
  },
  { immediate: true },
)
</script>

<template>
  <div class="page-wrap">
    <section class="page-hero cursor-card">
      <div class="page-hero__copy">
        <div class="page-title-row">
          <h2 class="page-title">会议室管理</h2>
        </div>
        <p class="page-subtitle">统一维护会议室容量与设备信息，确保预约资源清晰可控。</p>
      </div>
      <div class="hero-actions">
        <el-button v-if="isAdmin" type="primary" class="btn-key-soft" @click="openCreate">新增会议室</el-button>
        <el-button
          type="primary"
          class="btn-key-solid page-refresh-btn"
          :icon="RefreshRight"
          :loading="loading"
          circle
          title="刷新"
          aria-label="刷新会议室管理"
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
        title="已保留上次同步的会议室数据"
        :description="statusMessage"
        action-text="重新加载"
        @action="reload"
      />

      <section class="stats-grid room-stats-grid">
        <article class="stat-card cursor-card tone-total"><div class="k">会议室总数</div><div class="v">{{ stats.total }}</div></article>
        <article class="stat-card cursor-card tone-available"><div class="k">可用中</div><div class="v">{{ stats.available }}</div></article>
        <article class="stat-card cursor-card tone-maintenance"><div class="k">维护中</div><div class="v">{{ stats.maintenance }}</div></article>
        <article class="stat-card cursor-card tone-disabled"><div class="k">已停用</div><div class="v">{{ stats.disabled }}</div></article>
        <article class="stat-card cursor-card tone-equipment"><div class="k">设备条目数</div><div class="v">{{ stats.equipmentCount }}</div></article>
      </section>

      <section class="cursor-card table-card room-table-card">
        <div class="section-head room-head toolbar-row">
          <div class="section-title">会议室列表</div>
          <div class="room-filters room-filters--inline filter-bar">
            <el-input v-model="filters.keyword" clearable placeholder="搜索会议室" class="filter-inline-keyword" />
            <el-select v-model="filters.capacity" class="filter-inline-select">
              <el-option label="全部容量" value="ALL" />
              <el-option label="2-6 人" value="SMALL" />
              <el-option label="7-10 人" value="MEDIUM" />
              <el-option label="10 人以上" value="LARGE" />
            </el-select>
            <el-select v-model="filters.status" class="filter-inline-select">
              <el-option label="全部状态" value="ALL" />
              <el-option label="可用" value="AVAILABLE" />
              <el-option label="维护中" value="MAINTENANCE" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
            <el-select
              v-model="filters.equipment"
              class="filter-inline-select filter-inline-select--equipment"
              :popper-class="'filter-equipment-popper'"
            >
              <el-option label="全部设备" value="ALL" />
              <el-option v-for="item in equipmentOptions" :key="`f-${item}`" :label="item" :value="item" />
            </el-select>
            <el-button type="primary" class="room-filters__reset btn-key-soft" @click="resetAdvancedFilters">重置筛选</el-button>
          </div>
        </div>

        <el-table v-if="viewRooms.length" class="rooms-table" :data="viewRooms" style="width: 100%" :max-height="560">
          <el-table-column prop="name" label="名称" width="168" />
          <el-table-column prop="capacity" label="容量" width="92" />
          <el-table-column label="设备" min-width="260">
            <template #default="{ row }">
              <div v-if="row.equipment?.length" class="tags-wrap">
                <el-tag v-for="item in row.equipment" :key="item" effect="plain">{{ item }}</el-tag>
              </div>
              <span v-else class="empty-inline">暂无设备配置，可在编辑中补充。</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="112">
            <template #default="{ row }">
              <el-tag :type="row.status === 'AVAILABLE' ? 'success' : row.status === 'MAINTENANCE' ? 'warning' : 'danger'" effect="plain">
                {{ row.status === 'AVAILABLE' ? '可用' : row.status === 'MAINTENANCE' ? '维护中' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isAdmin" class-name="action-col" label="操作" width="184" align="right" header-align="center">
            <template #default="{ row }">
              <div class="row-actions row-actions--right">
                <el-button size="small" type="primary" class="btn-key-soft" @click="openEdit(row)">编辑</el-button>
                <el-button size="small" type="primary" class="btn-danger-soft" @click="del(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-state">{{ emptyRoomMessage }}</div>
      </section>
    </template>
  </div>

  <el-dialog v-model="dialogOpen" :title="editing ? '编辑会议室 / 状态维护' : '新增会议室'" width="min(560px, 92vw)">
    <el-form class="dialog-form-stack" label-position="top">
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
          :reserve-keyword="false"
          placeholder="请选择设备"
          no-data-text="暂无可选设备"
        >
          <el-option v-for="item in equipmentOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>

      <template v-if="editing && isAdmin">
        <el-form-item label="会议室状态">
          <el-select v-model="maintenanceForm.status">
            <el-option label="可用" value="AVAILABLE" />
            <el-option label="维护中" value="MAINTENANCE" />
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
      </template>
    </el-form>

    <template #footer>
      <div class="dialog-footer-bar">
        <el-button type="primary" class="btn-key-soft" @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" class="btn-key-solid" @click="save">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
:deep(.dialog-form-stack .el-form-item) {
  margin-bottom: 16px;
}

:deep(.dialog-form-stack .el-form-item:last-child) {
  margin-bottom: 0;
}

:deep(.dialog-form-stack .el-form-item__label) {
  padding-bottom: 0;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  line-height: 1.35;
}

:deep(.dialog-form-stack .el-input-number),
:deep(.dialog-form-stack .el-select),
:deep(.dialog-form-stack .el-date-editor.el-input),
:deep(.dialog-form-stack .el-date-editor.el-input__wrapper) {
  width: 100%;
}

:deep(.dialog-form-stack .el-textarea__inner) {
  min-height: 92px;
  line-height: 1.72;
}

.dialog-footer-bar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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

.room-stats-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.room-stats-grid .stat-card {
  min-height: var(--stat-card-min-height);
}

.room-table-card {
  padding-top: var(--panel-card-padding);
}

.room-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 0;
}

.room-head .section-title {
  flex: 1 1 auto;
}

.room-filters {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: nowrap;
}

.room-filters--inline {
  margin-left: auto;
}

.room-filters__reset {
  min-width: 92px;
  height: var(--control-height);
  padding: 0 12px;
  font-size: 14px;
  border-radius: var(--radius-unified);
  flex: 0 0 auto;
}

.filter-inline-keyword {
  width: 220px;
}

.filter-inline-select {
  width: 148px;
}

.filter-inline-select--equipment {
  width: 164px;
}

:deep(.room-filters .el-input__wrapper),
:deep(.room-filters .el-select__wrapper) {
  min-height: var(--control-height);
  height: var(--control-height);
  border-radius: var(--radius-unified);
}

:deep(.room-filters .el-input__inner),
:deep(.room-filters .el-select__selected-item),
:deep(.room-filters .el-select__placeholder) {
  font-size: 14px;
  line-height: 1.2;
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

  .room-filters {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .room-filters--inline {
    margin-left: 0;
  }

  .filter-inline-keyword {
    width: min(260px, 100%);
  }

  .filter-inline-select {
    width: 132px;
  }

  .filter-inline-select--equipment {
    width: 148px;
  }

  .row-actions--right {
    justify-content: flex-start;
  }
}

@media (max-width: 680px) {
  .room-stats-grid {
    grid-template-columns: 1fr;
  }

  .room-filters {
    padding: var(--filter-bar-padding-block) var(--filter-bar-padding-inline);
  }

  .filter-inline-keyword,
  .filter-inline-select,
  .filter-inline-select--equipment,
  .room-filters__reset {
    width: calc(50% - 5px);
    min-width: 0;
  }

  .room-filters {
    gap: 8px;
  }
}

@media (max-width: 520px) {
  .filter-inline-keyword,
  .filter-inline-select,
  .filter-inline-select--equipment,
  .room-filters__reset {
    width: 100%;
  }

  .row-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .row-actions :deep(.el-button) {
    width: 100%;
  }

  .dialog-footer-bar {
    width: 100%;
    flex-direction: column-reverse;
  }

  .dialog-footer-bar :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
