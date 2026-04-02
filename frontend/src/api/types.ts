export type ApiResponse<T> = {
  code: number
  message: string
  data: T
}

export type MaintenanceSlot = {
  startTime: string
  endTime: string
  reason?: string
}

export type Room = {
  id: number
  name: string
  capacity: number
  equipment: string[]
  requireApproval: boolean
  status?: 'AVAILABLE' | 'MAINTENANCE' | 'DISABLED'
  maintenanceSlots?: MaintenanceSlot[]
}

export type Reservation = {
  id: number
  userId: number
  username: string
  roomId: number
  roomName: string
  startTime: string
  endTime: string
  status: string
  reason?: string
  adminComment?: string
  approvedBy?: number
  approvedAt?: string
}

export type TimeSlice = {
  reservationId: number
  startTime: string
  endTime: string
  status: string
}

export type RoomDayOccupancy = {
  roomId: number
  dayStart: string
  occupiedSlots: number[]
  slices: TimeSlice[]
}

export type AlternativeSlot = {
  roomId: number
  roomName: string
  startTime: string
  endTime: string
  tip: string
}

export type ConflictSuggestion = {
  conflictMessage: string
  alternatives: AlternativeSlot[]
}

export type OverviewStats = {
  totalRooms: number
  totalUsers: number
  todayReservations: number
  myUpcomingReservations: number
  pendingApprovals: number
  userBreakdown: {
    normalUsers: number
    adminUsers: number
    superAdminUsers: number
    disabledUsers: number
  }
}

export type DashboardWelcome = {
  roleLabel: string
  message: string
}

export type DashboardTaskItem = {
  key: string
  label: string
  value: number
  detail: string
  tone: string
  to: string
  query?: Record<string, string>
}

export type DashboardTaskSummary = {
  title: string
  subtitle: string
  items: DashboardTaskItem[]
}

export type DashboardMetric = {
  key: string
  label: string
  value: string
  detail: string
  tone: string
}

export type DashboardResourceSnapshot = {
  title: string
  subtitle: string
  metrics: DashboardMetric[]
}

export type DashboardHeatmapBucket = {
  label: string
  reservationCount: number
  activeRoomCount: number
  occupancyPercent: number
  load: 'low' | 'medium' | 'high' | string
}

export type DashboardTrendPoint = {
  day: string
  label: string
  reservationCount: number
  pendingCount: number
  riskCount: number
}

export type DashboardRiskDistributionItem = {
  key: string
  label: string
  value: number
  detail: string
  tone: string
}

export type DashboardQuickLinkContext = {
  label: string
  description: string
  to: string
  tone: string
  query?: Record<string, string>
}

export type DashboardStatsResponse = {
  adminView: boolean
  welcome: DashboardWelcome
  taskSummary: DashboardTaskSummary
  resourceSnapshot: DashboardResourceSnapshot
  todayHeatmap: DashboardHeatmapBucket[]
  weeklyTrend: DashboardTrendPoint[]
  riskDistribution: DashboardRiskDistributionItem[]
  quickLinkContext: DashboardQuickLinkContext[]
}

export type UserAccount = {
  id: number
  username: string
  role: 'SUPER_ADMIN' | 'ADMIN' | 'USER'
  enabled: boolean
}
