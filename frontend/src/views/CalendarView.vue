<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import interactionPlugin from '@fullcalendar/interaction'
import timeGridPlugin from '@fullcalendar/timegrid'
import type { DateSelectArg, DatesSetArg, EventContentArg, EventInput } from '@fullcalendar/core'
import { useRoute } from 'vue-router'
import { getRequestErrorMessage } from '../api/http'
import { apiCalendar, apiCreateReservation, apiReservationSuggestions, apiRooms } from '../api/mrs'
import type { ConflictSuggestion, Reservation, Room } from '../api/types'
import PageStatusPanel from '../components/PageStatusPanel.vue'

type ViewState = 'loading' | 'ready' | 'error'
type EventTone = 'approved' | 'pending' | 'rejected' | 'muted'
type ReservationEnvelope = {
  list?: Reservation[]
  records?: Reservation[]
}
type CalendarEventMeta = Reservation & {
  roomName: string
  statusLabel: string
  statusTone: EventTone
}
type CalendarEventInput = EventInput & {
  id: string
  title: string
  start: string
  end: string
  backgroundColor: string
  borderColor: string
  textColor: string
  extendedProps: CalendarEventMeta
}

const route = useRoute()
const rooms = ref<Room[]>([])
const roomId = ref<number | null>(null)
const loading = ref(false)
const submitting = ref(false)
const viewState = ref<ViewState>('loading')
const statusMessage = ref('')
const hasLoadedOnce = ref(false)
const activeRange = ref<{ start: Date; end: Date } | null>(null)
const events = ref<CalendarEventInput[]>([])
const suggestions = ref<ConflictSuggestion | null>(null)
const calendarRef = ref<any>(null)

const canUseRoom = computed(() => !!roomId.value)
const currentRoom = computed(() => rooms.value.find((item) => item.id === roomId.value) ?? null)
const showBlockingState = computed(() => !hasLoadedOnce.value && viewState.value === 'error')
const showInlineError = computed(() => hasLoadedOnce.value && viewState.value === 'error')
const contextHint = computed(() => {
  const suggest = String(route.query.suggest ?? '').toLowerCase()
  if (suggest === 'quiet') {
    return '已从运营看板进入预约建议视角，优先查看可用会议室与低峰时段。'
  }
  const focus = String(route.query.focus ?? '').toLowerCase()
  if (focus === 'today') {
    return '已按看板上下文切换到今日预约视角。'
  }
  return ''
})
const stateTitle = computed(() => '会议预约日历暂时不可用')
const stateDescription = computed(() => statusMessage.value || '当前无法获取会议预约日历，请稍后重试。')

const stats = computed(() => {
  const total = events.value.length
  const approved = events.value.filter((event) => event.extendedProps.status === 'APPROVED').length
  const pending = events.value.filter((event) => event.extendedProps.status === 'PENDING').length
  const today = events.value.filter((event) => {
    const start = new Date(event.start)
    const now = new Date()
    return (
      start.getFullYear() === now.getFullYear() &&
      start.getMonth() === now.getMonth() &&
      start.getDate() === now.getDate()
    )
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
  slotMinTime: '08:00:00',
  slotMaxTime: '18:00:00',
  allDaySlot: false,
  nowIndicator: false,
  height: 'auto',
  contentHeight: 'auto',
  expandRows: true,
  stickyHeaderDates: true,
  eventMinHeight: 58,
  eventShortHeight: 58,
  events: events.value,
  eventContent: renderCalendarEvent,
  datesSet: (arg: DatesSetArg) => {
    activeRange.value = { start: new Date(arg.start), end: new Date(arg.end) }
    void loadCalendar(arg.start, arg.end)
  },
  select: onSelect,
}))

function statusLabel(status: string) {
  if (status === 'APPROVED') return '已批准'
  if (status === 'PENDING') return '待审批'
  if (status === 'REJECTED') return '已拒绝'
  if (status === 'CANCELLED') return '已取消'
  return status || '未知'
}

function eventTone(status: string): EventTone {
  if (status === 'APPROVED') return 'approved'
  if (status === 'PENDING') return 'pending'
  if (status === 'REJECTED') return 'rejected'
  return 'muted'
}

function getFallbackRange() {
  const start = new Date()
  start.setDate(start.getDate() - 7)
  const end = new Date()
  end.setDate(end.getDate() + 7)
  return { start, end }
}

function applyRouteContext() {
  const suggest = String(route.query.suggest ?? '').toLowerCase()
  if (suggest === 'quiet' && rooms.value.length) {
    const preferredRoom = rooms.value.find((item) => (item.status ?? 'AVAILABLE') === 'AVAILABLE') ?? rooms.value[0]
    if (preferredRoom && roomId.value !== preferredRoom.id) {
      roomId.value = preferredRoom.id
      onRoomChange()
      return
    }
  }

  const focus = String(route.query.focus ?? '').toLowerCase()
  if (focus === 'today') {
    calendarRef.value?.getApi?.().gotoDate(new Date())
  }
}

function extractReservations(data: unknown): Reservation[] {
  if (Array.isArray(data)) {
    return data as Reservation[]
  }

  if (data && typeof data === 'object') {
    const envelope = data as ReservationEnvelope
    if (Array.isArray(envelope.list)) return envelope.list
    if (Array.isArray(envelope.records)) return envelope.records
  }

  return []
}

function fmt(date: Date) {
  const pad = (value: number) => (value < 10 ? `0${value}` : `${value}`)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function withinBusinessHours(start: Date, end: Date) {
  const startHour = start.getHours() + start.getMinutes() / 60
  const endHour = end.getHours() + end.getMinutes() / 60
  return startHour >= 8 && endHour <= 18
}

function hasConflict(start: Date, end: Date) {
  return events.value.some((event) => {
    const currentStart = new Date(event.start)
    const currentEnd = new Date(event.end)
    return start < currentEnd && end > currentStart
  })
}

function toEvent(reservation: Reservation): CalendarEventInput {
  const status = String(reservation.status ?? 'PENDING')
  const roomName = String(reservation.roomName ?? '会议室')
  const tone = eventTone(status)
  const color =
    tone === 'approved'
      ? '#5f7f6c'
      : tone === 'pending'
        ? '#9b875d'
        : tone === 'rejected'
          ? '#957173'
          : '#5f6368'

  return {
    id: String(reservation.id ?? ''),
    title: roomName,
    start: reservation.startTime,
    end: reservation.endTime,
    backgroundColor: color,
    borderColor: color,
    textColor: '#ffffff',
    extendedProps: {
      ...reservation,
      roomName,
      status,
      statusLabel: statusLabel(status),
      statusTone: tone,
    },
  }
}

function renderCalendarEvent(arg: EventContentArg) {
  const props = arg.event.extendedProps as Partial<CalendarEventMeta>
  const roomName = String(props.roomName ?? arg.event.title ?? '会议室')
  const badgeText = String(props.statusLabel ?? statusLabel(String(props.status ?? 'PENDING')))
  const tone = props.statusTone ?? eventTone(String(props.status ?? 'PENDING'))

  const wrapper = document.createElement('div')
  wrapper.className = `calendar-event__inner calendar-event__inner--${tone}`

  const title = document.createElement('div')
  title.className = 'calendar-event__title'
  title.textContent = roomName

  const badge = document.createElement('div')
  badge.className = `calendar-event__badge calendar-event__badge--${tone}`
  badge.textContent = badgeText

  wrapper.append(title, badge)
  return { domNodes: [wrapper] }
}

async function fetchCalendar(rangeStart: Date, rangeEnd: Date, preserveContent: boolean) {
  if (!roomId.value) {
    events.value = []
    hasLoadedOnce.value = true
    viewState.value = 'ready'
    statusMessage.value = ''
    return
  }

  try {
    const resp = await apiCalendar(roomId.value, fmt(rangeStart), fmt(rangeEnd))
    if (resp.code !== 0) {
      viewState.value = 'error'
      statusMessage.value = resp.message || '加载日历失败'
      if (!preserveContent) {
        events.value = []
      }
      return
    }

    events.value = extractReservations(resp.data).map(toEvent)
    hasLoadedOnce.value = true
    viewState.value = 'ready'
    statusMessage.value = ''
  } catch (error) {
    viewState.value = 'error'
    statusMessage.value = getRequestErrorMessage(error, '加载日历失败')
    if (!preserveContent) {
      events.value = []
    }
  }
}

async function loadRooms() {
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
        roomId.value = null
        events.value = []
        suggestions.value = null
      }
      return
    }

    rooms.value = Array.isArray(resp.data) ? resp.data : []
    if (!rooms.value.length) {
      roomId.value = null
      events.value = []
      suggestions.value = null
      hasLoadedOnce.value = true
      viewState.value = 'ready'
      return
    }

    if (!roomId.value || !rooms.value.some((item) => item.id === roomId.value)) {
      roomId.value = rooms.value[0].id
    }

    const range = activeRange.value ?? getFallbackRange()
    activeRange.value = range
    suggestions.value = null
    await fetchCalendar(range.start, range.end, preserveContent)
    void nextTick(() => {
      applyRouteContext()
    })
  } catch (error) {
    viewState.value = 'error'
    statusMessage.value = getRequestErrorMessage(error, '加载会议室失败')
    if (!preserveContent) {
      rooms.value = []
      roomId.value = null
      events.value = []
      suggestions.value = null
    }
  } finally {
    loading.value = false
  }
}

async function loadCalendar(rangeStart: Date, rangeEnd: Date) {
  if (!roomId.value) return

  const preserveContent = hasLoadedOnce.value
  activeRange.value = { start: new Date(rangeStart), end: new Date(rangeEnd) }
  suggestions.value = null
  loading.value = true
  statusMessage.value = ''

  if (!preserveContent) {
    viewState.value = 'loading'
  }

  try {
    await fetchCalendar(rangeStart, rangeEnd, preserveContent)
  } finally {
    loading.value = false
  }
}

async function onSelect(arg: DateSelectArg) {
  if (!roomId.value) return
  if (submitting.value) {
    ElMessage.warning('预约提交中，请稍候')
    return
  }

  const start = new Date(arg.start)
  const end = new Date(arg.end)
  const hours = (end.getTime() - start.getTime()) / 3600000

  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
    ElMessage.warning('时间格式异常，请重试')
    return
  }

  if (hours <= 0) {
    ElMessage.warning('请选择有效时间段')
    return
  }

  if (!withinBusinessHours(start, end)) {
    ElMessage.warning('仅支持 08:00 - 18:00 的预约时段')
    return
  }

  const startMinute = start.getMinutes()
  const endMinute = end.getMinutes()
  if (!([0, 30].includes(startMinute) && [0, 30].includes(endMinute))) {
    ElMessage.warning('当前按半小时预约，请选择 00 或 30 分钟起止时间')
    return
  }

  if (hours > 4) {
    ElMessage.warning('单次预约时长不能超过 4 小时')
    return
  }

  suggestions.value = null
  if (hasConflict(start, end)) {
    const suggestResp = await apiReservationSuggestions(roomId.value, fmt(start), fmt(end))
    if (suggestResp.code === 0) {
      suggestions.value = suggestResp.data
    }
    ElMessage.error('所选时段与现有排期冲突，请参考下方替代建议')
    return
  }

  submitting.value = true
  try {
    const room = rooms.value.find((item) => item.id === roomId.value)
    const resp = await apiCreateReservation({
      roomId: roomId.value,
      startTime: fmt(start),
      endTime: fmt(end),
      reason: `在线预约：${room?.name ?? ''}`,
    })

    if (resp.code !== 0) {
      ElMessage.error(resp.message)
      return
    }

    ElMessage.success(resp.data?.status === 'PENDING' ? '已提交申请，等待审批' : '预约成功')
    await loadCalendar(arg.view.activeStart, arg.view.activeEnd)
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '预约提交失败'))
  } finally {
    submitting.value = false
    arg.view.calendar.unselect()
  }
}

function onRoomChange() {
  if (!roomId.value) return
  const range = activeRange.value ?? getFallbackRange()
  activeRange.value = range
  void loadCalendar(range.start, range.end)
}

watch(
  () => `${String(route.query.focus ?? '')}|${String(route.query.suggest ?? '')}`,
  () => {
    void nextTick(() => {
      applyRouteContext()
    })
  },
  { immediate: true },
)

onMounted(loadRooms)
</script>

<template>
  <div class="page-wrap">
    <section class="page-hero cursor-card">
      <div class="page-hero__copy calendar-hero-copy">
        <div class="page-title-row calendar-title-row">
          <h2 class="page-title">会议室预约日历</h2>
          <el-select v-model="roomId" placeholder="选择会议室" class="calendar-filter-room" @change="onRoomChange">
            <el-option v-for="room in rooms" :key="room.id" :label="`${room.name}（容量 ${room.capacity}）`" :value="room.id" />
          </el-select>
        </div>
        <p class="page-subtitle calendar-page-subtitle">
          按时间轴查看会议室占用，拖拽即可快速发起预约申请，审批结果会实时同步到日历。
        </p>
      </div>
      <div class="hero-actions calendar-hero-actions">
        <el-button
          type="primary"
          class="btn-key-solid page-refresh-btn"
          :icon="RefreshRight"
          :loading="loading"
          circle
          title="刷新"
          aria-label="刷新会议室预约日历"
          @click="loadRooms"
        />
      </div>
    </section>

    <div v-if="contextHint" class="calendar-context-pill status-strip">{{ contextHint }}</div>

    <PageStatusPanel
      v-if="showBlockingState"
      tone="danger"
      :title="stateTitle"
      :description="stateDescription"
      action-text="重新加载"
      @action="loadRooms"
    />

    <template v-else>
      <PageStatusPanel
        v-if="showInlineError"
        tone="warning"
        title="已保留上次同步的日历数据"
        :description="statusMessage"
        action-text="重新加载"
        @action="loadRooms"
      />

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
            <div class="main-subtitle">
              {{ currentRoom ? `当前会议室：${currentRoom.name}` : '请选择会议室后查看可预约时段' }}
            </div>
          </div>
          <div class="legend-row" aria-label="预约状态图例">
            <span class="legend-item legend-item--approved"><i class="dot approved" />已批准</span>
            <span class="legend-item legend-item--pending"><i class="dot pending" />待审批</span>
            <span class="legend-item legend-item--rejected"><i class="dot rejected" />已拒绝</span>
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
        <div v-else class="calendar-wrap">
          <FullCalendar ref="calendarRef" :options="calendarOptions" />
        </div>
      </section>

      <section v-if="suggestions?.alternatives?.length" class="cursor-card table-card">
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
    </template>
  </div>
</template>

<style scoped>
.calendar-hero-copy {
  flex: 1 1 auto;
  min-width: 0;
}

.calendar-page-subtitle {
  max-width: none;
  white-space: nowrap;
}

.calendar-context-pill {
  margin-top: -2px;
  margin-bottom: 12px;
  max-width: min(100%, 72ch);
}

.calendar-main {
  border-color: var(--surface-panel-border);
  background:
    linear-gradient(180deg, var(--surface-panel-top), var(--surface-panel-bottom)),
    var(--bg-card);
  box-shadow:
    var(--surface-panel-shadow),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
  padding-bottom: 2px;
}

.main-title {
  color: var(--text-main);
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.main-subtitle {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.legend-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  padding: 8px;
  border: 1px solid var(--surface-nested-border);
  border-radius: calc(var(--radius-unified) + 2px);
  background: linear-gradient(180deg, var(--surface-nested-top), var(--surface-nested-bottom));
  color: var(--text-muted);
  font-size: 13px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(248, 248, 248, 0.92);
  border: 1px solid rgba(55, 63, 72, 0.1);
}

.dot {
  width: 9px;
  height: 9px;
  border-radius: 999px;
}

.dot.approved {
  background: #5f7f6c;
}

.dot.pending {
  background: #9b875d;
}

.dot.rejected {
  background: #957173;
}

.legend-item--approved {
  border-color: rgba(95, 127, 108, 0.32);
  background: rgba(95, 127, 108, 0.11);
  color: #4f695a;
}

.legend-item--pending {
  border-color: rgba(155, 135, 93, 0.34);
  background: rgba(155, 135, 93, 0.13);
  color: #6f6144;
}

.legend-item--rejected {
  border-color: rgba(149, 113, 115, 0.36);
  background: rgba(149, 113, 115, 0.13);
  color: #75595b;
}

.calendar-tip {
  margin-bottom: 14px;
}

:deep(.calendar-tip.el-alert) {
  border-radius: calc(var(--radius-unified) + 2px);
  border: 1px solid var(--nested-surface-border);
  background: linear-gradient(180deg, var(--nested-surface-top), var(--nested-surface-bottom)) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.calendar-wrap {
  overflow-x: auto;
  padding: 12px;
  border: 1px solid var(--surface-nested-border);
  border-radius: calc(var(--radius-unified) + 2px);
  background: linear-gradient(180deg, var(--surface-nested-top), var(--surface-nested-bottom));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.74),
    var(--surface-nested-shadow);
}

.calendar-title-row {
  align-items: center;
}

.calendar-hero-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  justify-content: flex-end;
  flex: 0 0 auto;
}

.calendar-filter-room {
  width: 260px;
}

:deep(.calendar-title-row .el-select__wrapper) {
  min-height: var(--control-height);
  height: var(--control-height);
  border-radius: var(--radius-unified);
}

:deep(.calendar-title-row .el-select__selected-item),
:deep(.calendar-title-row .el-select__placeholder) {
  font-size: 14px;
  line-height: 1.2;
}

:deep(.fc) {
  min-width: 920px;
}

:deep(.fc .fc-day-today) {
  background: rgba(31, 31, 31, 0.04) !important;
}

:deep(.fc .fc-timegrid-slot-label-cushion),
:deep(.fc .fc-col-header-cell-cushion) {
  color: var(--text-muted);
  font-size: 12px;
}

:deep(.fc .fc-scrollgrid),
:deep(.fc .fc-timegrid-slot),
:deep(.fc .fc-col-header-cell),
:deep(.fc .fc-timegrid-axis),
:deep(.fc-theme-standard td),
:deep(.fc-theme-standard th) {
  border-color: var(--line-soft) !important;
}

:deep(.fc .fc-toolbar) {
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px !important;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(72, 80, 88, 0.16);
}

:deep(.fc .fc-toolbar-title) {
  color: var(--text-main);
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.02em;
}

:deep(.fc .fc-button) {
  background: rgba(250, 252, 253, 0.94) !important;
  border-color: rgba(55, 63, 72, 0.12) !important;
  color: var(--text-main) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.68) !important;
  padding: 0.42em 0.72em !important;
  border-radius: var(--radius-unified) !important;
}

:deep(.fc .fc-button:hover) {
  background: rgba(241, 241, 241, 0.98) !important;
  border-color: rgba(31, 31, 31, 0.16) !important;
}

:deep(.fc .fc-button.fc-button-active),
:deep(.fc .fc-button:focus-visible) {
  background: var(--accent) !important;
  border-color: var(--accent) !important;
}

:deep(.fc .fc-event) {
  border-radius: 12px;
  padding: 0 !important;
  border: none !important;
  overflow: hidden;
  box-shadow: 0 8px 18px rgba(20, 24, 28, 0.16);
}

:deep(.fc .fc-event-main) {
  padding: 0 !important;
}

:deep(.fc .fc-event-main-frame) {
  min-height: 100%;
}

:deep(.fc .fc-timegrid-event-harness) {
  inset-inline-end: 2px !important;
}

:deep(.calendar-event__inner) {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 4px;
  padding: 7px 8px;
  color: #ffffff;
}

:deep(.calendar-event__title) {
  font-size: 11px;
  font-weight: 700;
  line-height: 1.24;
  white-space: normal;
  word-break: break-word;
  overflow-wrap: anywhere;
}

:deep(.calendar-event__badge) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 20px;
  padding: 2px 7px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.22);
  font-size: 10px;
  font-weight: 700;
  line-height: 1.1;
}

:deep(.calendar-event__inner--approved) {
  background: linear-gradient(180deg, #6f8d7a, #5f7668);
}

:deep(.calendar-event__inner--pending) {
  background: linear-gradient(180deg, #aa9467, #907b56);
}

:deep(.calendar-event__inner--rejected) {
  background: linear-gradient(180deg, #a38182, #8d6b6d);
}

:deep(.calendar-event__inner--muted) {
  background: linear-gradient(180deg, #5c6167, #4c5157);
}

:deep(.fc .fc-timegrid-slot) {
  height: 50px !important;
}

.calendar-stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 980px) {
  .calendar-page-subtitle {
    max-width: 65ch;
    white-space: normal;
  }

  .calendar-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .calendar-title-row {
    flex-wrap: wrap;
    align-items: flex-start;
  }

  .calendar-filter-room {
    width: min(280px, 100%);
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
    min-width: 820px;
  }
}

@media (max-width: 640px) {
  .calendar-filter-room {
    width: 100%;
  }
}
</style>
