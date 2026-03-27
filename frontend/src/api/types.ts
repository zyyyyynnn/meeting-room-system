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

export type UserAccount = {
  id: number
  username: string
  role: 'SUPER_ADMIN' | 'ADMIN' | 'USER'
  enabled: boolean
}
