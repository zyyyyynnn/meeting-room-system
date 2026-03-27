<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import type { ConflictSuggestion, Reservation, Room, TimeSlice } from '../api/types'
import { apiCalendar, apiCreateReservation, apiCreateWeeklyBatch, apiReservationSuggestions, apiRoomDayOccupancy, apiRooms } from '../api/mrs'

const rooms = ref<Room[]>([])
const roomId = ref<number | null>(null)
const loading = ref(false)
const submitting = ref(false)
const events = ref<any[]>([])
const daySlices = ref<TimeSlice[]>([])
const suggestions = ref<ConflictSuggestion | null>(null)

const canUseRoom = computed(() => !!roomId.value)
const currentRoom = computed(() => rooms.value.find((x) => x.id === roomId.value) ?? null)

const stats = computed(() => {
  const total = events.value.length
  const approved = events.value.filter((e) => e.extendedProps?.status === 'APPROVED').length
  const pending = events.value.filter((e) => e.extendedProps?.status === 'PENDING').length
  const today = events.value.filter((e) => {
    const d = new Date(e.start)
    const n = new Date()
    return d.getFullYear() === n.getFullYear() && d.getMonth() === n.getMonth() && d.getDate() === n.getDate()
  }).length
  return { total, approved, pending, today }
})

const calendarOptions = computed(() => ({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: 'timeGridWeek',
  selectable: true,
  selectMirror: true,
  snapDuration: '00:30:00',
  slotDuration: '00:30:00',
  slotLabelInterval: '01:00:00',
  events: events.value,
  height: 'auto',
  contentHeight: 'auto',
  expandRows: true,
  stickyHeaderDates: true,
  slotMinTime: '08:00:00',
  slotMaxTime: '18:00:00',
  allDaySlot: false,
  nowIndicator: false,
  datesSet: (arg: any) => loadCalendar(arg.start, arg.end),
  select: onSelect,
}))

async function loadRooms() {
  try {
    const resp = await apiRooms()
    if (resp.code !== 0) return ElMessage.error(resp.message)
    rooms.value = Array.isArray(resp.data) ? resp.data : []
    if (!roomId.value && rooms.value.length > 0) roomId.value = rooms.value[0].id
  } catch (error: any) {
    ElMessage.error(error?.message || '加载会议室失败')
    rooms.value = []
  }
}

function toEvent(r: Reservation) {
  const status = String((r as any)?.status ?? 'PENDING')
  const color =
    status === 'APPROVED' ? '#2f3f38' : status === 'PENDING' ? '#6e685f' : status === 'REJECTED' ? '#65423d' : '#5f5f5f'
  const start = (r as any)?.startTime
  const end = (r as any)?.endTime
  return {
    id: Number((r as any)?.id ?? 0),
    title: `${String((r as any)?.roomName ?? '会议室')} (${status})`,
    start,
    end,
    backgroundColor: color,
    borderColor: color,
    extendedProps: { ...r, status },
  }
}

async function loadCalendar(rangeStart: Date, rangeEnd: Date) {
  if (!roomId.value) return
  loading.value = true
  try {
    const start = fmt(rangeStart)
    const end = fmt(rangeEnd)
    const resp = await apiCalendar(roomId.value, start, end)
    if (resp.code !== 0) return ElMessage.error(resp.message)
    const raw = Array.isArray(resp.data)
      ? resp.data
      : Array.isArray((resp.data as any)?.list)
        ? (resp.data as any).list
        : []
    events.value = raw.map(toEvent)

    const day = `${rangeStart.getFullYear()}-${String(rangeStart.getMonth() + 1).padStart(2, '0')}-${String(rangeStart.getDate()).padStart(2, '0')}`
    const occResp = await apiRoomDayOccupancy(roomId.value, day)
    if (occResp.code === 0) {
      daySlices.value = Array.isArray(occResp.data?.slices) ? occResp.data.slices : []
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载日历失败')
    events.value = []
    daySlices.value = []
  } finally {
    loading.value = false
  }
}

function fmt(d: Date) {
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(
    d.getMinutes(),
  )}:${pad(d.getSeconds())}`
}

function withinBusinessHours(start: Date, end: Date) {
  const s = start.getHours() + start.getMinutes() / 60
  const e = end.getHours() + end.getMinutes() / 60
  return s >= 8 && e <= 18
}

function hasConflict(start: Date, end: Date) {
  return events.value.some((e) => {
    const s = new Date(e.start)
    const t = new Date(e.end)
    return start < t && end > s
  })
}

async function onSelect(arg: any) {
  if (!roomId.value) return
  if (submitting.value) return ElMessage.warning('预约提交中，请稍候')

  const start = new Date(arg.start)
  const end = new Date(arg.end)
  const hours = (end.getTime() - start.getTime()) / 3600000

  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return ElMessage.warning('时间格式异常，请重试')
  if (hours <= 0) return ElMessage.warning('请选择有效时间段')
  if (!withinBusinessHours(start, end)) return ElMessage.warning('仅支持 08:00 - 18:00 的预约时段')
  const startMinute = start.getMinutes()
  const endMinute = end.getMinutes()
  if (!([0, 30].includes(startMinute) && [0, 30].includes(endMinute))) {
    return ElMessage.warning('当前按半小时预约，请选择 00 或 30 分钟起止时间')
  }
  if (hours > 4) return ElMessage.warning('单次预约时长不超过 4 小时')
  if (hasConflict(start, end)) {
    const suggestResp = await apiReservationSuggestions(roomId.value, fmt(start), fmt(end))
    if (suggestResp.code === 0) {
      suggestions.value = suggestResp.data
    }
    return ElMessage.error('所选时间段与现有排期冲突，请参考下方替代推荐')
  }

  submitting.value = true
  try {
    const room = rooms.value.find((x) => x.id === roomId.value)
    const resp = await apiCreateReservation({
      roomId: roomId.value,
      startTime: fmt(start),
      endTime: fmt(end),
      reason: `在线预约（${room?.name ?? ''}）`,
    })
    if (resp.code !== 0) return ElMessage.error(resp.message)

    const newStatus = (resp.data as any)?.status
    ElMessage.success(newStatus === 'PENDING' ? '已提交申请，等待审批' : '预约成功')
    await loadCalendar(arg.view.activeStart, arg.view.activeEnd)
  } catch (error: any) {
    ElMessage.error(error?.message || '预约提交失败')
  } finally {
    submitting.value = false
    arg?.view?.calendar?.unselect?.()
  }
}

function onRoomChange() {
  const start = new Date()
  start.setDate(start.getDate() - 7)
  const end = new Date()
  end.setDate(end.getDate() + 7)
  loadCalendar(start, end)
}

onMounted(loadRooms)
</script>

<template>
  <div class="page-wrap">
    <section class="page-hero cursor-card">
      <div>
        <h2 class="page-title">会议室预约日历</h2>
        <p class="page-subtitle">按时间轴查看会议室占用，拖拽即可快速发起预约申请，审批结果实时同步到日历。</p>
      </div>
      <div class="hero-actions">
        <el-select v-model="roomId" placeholder="选择会议室" style="width: 260px" @change="onRoomChange">
          <el-option v-for="r in rooms" :key="r.id" :label="`${r.name}（容量 ${r.capacity}）`" :value="r.id" />
        </el-select>
        <el-button :loading="loading" @click="loadRooms">刷新会议室</el-button>
      </div>
    </section>

    <section class="stats-grid calendar-stats-grid">
      <article class="stat-card cursor-card tone-total">
        <div class="stat-label">预约总数</div>
        <div class="stat-value">{{ stats.total }}</div>
      </article>
      <article class="stat-card cursor-card tone-approved">
        <div class="stat-label">已批准</div>
        <div class="stat-value">{{ stats.approved }}</div>
      </article>
      <article class="stat-card cursor-card tone-pending">
        <div class="stat-label">待审批</div>
        <div class="stat-value">{{ stats.pending }}</div>
      </article>
      <article class="stat-card cursor-card tone-today">
        <div class="stat-label">今日安排</div>
        <div class="stat-value">{{ stats.today }}</div>
      </article>
    </section>

    <section class="calendar-main cursor-card">
      <div class="main-header">
        <div>
          <div class="main-title">排期总览</div>
          <div class="main-subtitle">{{ currentRoom ? `当前会议室：${currentRoom.name}` : '请选择会议室后查看可预约时间段' }}</div>
        </div>
        <div class="legend-row">
          <span class="legend-item"><i class="dot approved" />已批准</span>
          <span class="legend-item"><i class="dot pending" />待审批</span>
          <span class="legend-item"><i class="dot rejected" />已拒绝</span>
        </div>
      </div>

      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="操作说明：支持 08:00 - 18:00 半小时预约（00/30 分）；单次不超过 4 小时，冲突时段会自动拦截并给出替代建议。"
        class="calendar-tip"
      />

      <div v-if="!canUseRoom" class="empty-state">
        当前暂无可用会议室。<br />
        你可以先前往“会议室管理”创建或启用会议室，再回来完成预约。
      </div>
      <div v-else class="calendar-wrap" v-loading="loading">
        <full-calendar :options="calendarOptions" />
      </div>
    </section>

    <section class="cursor-card table-card" v-if="suggestions && suggestions.alternatives?.length">
      <div class="section-head">
        <div class="section-title">冲突替代推荐</div>
        <div class="section-desc">{{ suggestions.conflictMessage }}</div>
      </div>
      <el-table class="calendar-suggestion-table" :data="suggestions.alternatives" style="width: 100%" :max-height="420">
        <el-table-column prop="roomName" label="会议室" width="180" />
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="endTime" label="结束时间" width="180" />
        <el-table-column prop="tip" label="推荐说明" />
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.calendar-main {
  border-color: rgba(38, 38, 38, 0.1);
  background: rgba(255, 255, 255, 0.44);
}

:deep(.calendar-suggestion-table .el-table__row) {
  height: 60px;
}

:deep(.calendar-suggestion-table .el-table__cell) {
  padding-top: 14px;
  padding-bottom: 14px;
}

.main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.main-title {
  color: var(--text-main);
  font-size: 15px;
  font-weight: 600;
}

.main-subtitle {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
}

.legend-row {
  display: flex;
  gap: 12px;
  color: var(--text-muted);
  font-size: 12px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot.approved { background: #5d7f71; }
.dot.pending { background: #c4904d; }
.dot.rejected { background: #ae594f; }

.calendar-tip {
  margin-bottom: 12px;
}

.tone-total,
.tone-approved,
.tone-pending,
.tone-today {
  background: rgba(255, 255, 255, 0.42);
  border-color: var(--line-soft);
}

.calendar-wrap {
  overflow-x: auto;
}

:deep(.fc .fc-timegrid-slot-label-cushion),
:deep(.fc .fc-col-header-cell-cushion) {
  color: var(--text-muted);
  font-size: 12px;
}

:deep(.fc .fc-day-today) {
  background: rgba(31, 31, 31, 0.06) !important;
}

:deep(.fc .fc-scrollgrid),
:deep(.fc .fc-timegrid-slot),
:deep(.fc .fc-col-header-cell),
:deep(.fc .fc-timegrid-axis),
:deep(.fc-theme-standard td),
:deep(.fc-theme-standard th) {
  border-color: var(--line-soft) !important;
}

:deep(.fc) {
  min-width: 900px;
}

:deep(.fc .fc-toolbar) {
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px !important;
}

:deep(.fc .fc-toolbar-title) {
  color: var(--text-main);
  font-size: 16px;
  font-weight: 650;
}

:deep(.fc .fc-button) {
  background: rgba(255, 255, 255, 0.03) !important;
  border-color: var(--line-soft) !important;
  color: var(--text-main) !important;
  box-shadow: none !important;
  padding: 0.42em 0.72em !important;
  border-radius: 8px !important;
}

:deep(.fc .fc-button:hover) {
  background: rgba(255, 255, 255, 0.1) !important;
  border-color: var(--line-strong) !important;
}

:deep(.fc .fc-button.fc-button-active),
:deep(.fc .fc-button:focus-visible) {
  background: var(--accent) !important;
  border-color: var(--accent) !important;
}

:deep(.fc .fc-timegrid-now-indicator-line) {
  border-color: rgba(69, 116, 98, 0.8);
}

:deep(.fc .fc-timegrid-now-indicator-arrow) {
  border-color: rgba(69, 116, 98, 0.8);
}

:deep(.fc .fc-event) {
  border-radius: 7px;
  padding: 4px 6px;
  font-size: 11px;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  box-shadow: 0 4px 10px rgba(20, 18, 16, 0.16);
}

:deep(.fc .fc-event-title) {
  font-weight: 600;
}

:deep(.fc .fc-timegrid-slot) {
  height: 38px !important;
}


.calendar-stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 980px) {
  .calendar-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .main-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .calendar-stats-grid {
    grid-template-columns: 1fr;
  }

  :deep(.fc) {
    min-width: 800px;
  }
}
</style>
