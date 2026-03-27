import { http } from './http'
import type { ApiResponse, ConflictSuggestion, Reservation, Room, RoomDayOccupancy, OverviewStats, UserAccount } from './types'

export async function apiRegister(username: string, password: string) {
  const { data } = await http.post<ApiResponse<null>>('/api/auth/register', { username, password })
  return data
}

export async function apiLogin(username: string, password: string) {
  const { data } = await http.post<
    ApiResponse<{ token: string; userId: number; username: string; role: 'SUPER_ADMIN' | 'ADMIN' | 'USER' }>
  >('/api/auth/login', { username, password })
  return data
}

export async function apiRooms() {
  const { data } = await http.get<ApiResponse<Room[]>>('/api/rooms')
  return data
}

export async function apiCreateRoom(payload: Partial<Room>) {
  const { data } = await http.post<ApiResponse<Room>>('/api/rooms', payload)
  return data
}

export async function apiUpdateRoom(id: number, payload: Partial<Room>) {
  const { data } = await http.put<ApiResponse<Room>>(`/api/rooms/${id}`, payload)
  return data
}

export async function apiDeleteRoom(id: number) {
  const { data } = await http.delete<ApiResponse<null>>(`/api/rooms/${id}`)
  return data
}

export async function apiUpdateRoomStatus(id: number, status: 'AVAILABLE' | 'MAINTENANCE' | 'DISABLED') {
  const { data } = await http.post<ApiResponse<null>>(`/api/rooms/${id}/status`, { status })
  return data
}

export async function apiAddRoomMaintenance(id: number, payload: { startTime: string; endTime: string; reason?: string }) {
  const { data } = await http.post<ApiResponse<null>>(`/api/rooms/${id}/maintenances`, payload)
  return data
}

export async function apiCreateReservation(payload: {
  roomId: number
  startTime: string
  endTime: string
  reason?: string
}) {
  const { data } = await http.post<ApiResponse<Reservation>>('/api/reservations', payload)
  return data
}

export async function apiCreateWeeklyBatch(payload: {
  roomId: number
  firstStartTime: string
  firstEndTime: string
  repeatWeeks: number
  reason?: string
}) {
  const { data } = await http.post<ApiResponse<Reservation[]>>('/api/reservations/batch/weekly', payload)
  return data
}

export async function apiCancelReservation(id: number) {
  const { data } = await http.post<ApiResponse<null>>(`/api/reservations/${id}/cancel`)
  return data
}

export async function apiDeleteReservation(id: number) {
  const { data } = await http.delete<ApiResponse<null>>(`/api/reservations/${id}`)
  return data
}

export async function apiMyRecent() {
  const { data } = await http.get<ApiResponse<Reservation[]>>('/api/reservations/mine/recent')
  return data
}

export async function apiCalendar(roomId: number, start: string, end: string) {
  const { data } = await http.get<ApiResponse<Reservation[]>>('/api/reservations/calendar', {
    params: { roomId, start, end },
  })
  return data
}

export async function apiRoomDayOccupancy(roomId: number, day: string) {
  const { data } = await http.get<ApiResponse<RoomDayOccupancy>>('/api/reservations/occupancy/day', {
    params: { roomId, day },
  })
  return data
}

export async function apiReservationSuggestions(roomId: number, start: string, end: string) {
  const { data } = await http.get<ApiResponse<ConflictSuggestion>>('/api/reservations/suggestions', {
    params: { roomId, start, end },
  })
  return data
}

export async function apiPending() {
  const { data } = await http.get<ApiResponse<Reservation[]>>('/api/admin/reservations/pending')
  return data
}

export async function apiRecentReviewed() {
  const { data } = await http.get<ApiResponse<Reservation[]>>('/api/admin/reservations/recent')
  return data
}

export async function apiApprove(id: number, adminComment?: string) {
  const { data } = await http.post<ApiResponse<null>>(`/api/admin/reservations/${id}/approve`, { adminComment })
  return data
}

export async function apiReject(id: number, adminComment?: string) {
  const { data } = await http.post<ApiResponse<null>>(`/api/admin/reservations/${id}/reject`, { adminComment })
  return data
}

export async function apiRevokeReview(id: number) {
  const { data } = await http.post<ApiResponse<null>>(`/api/admin/reservations/${id}/revoke`)
  return data
}

export async function apiNotifications() {
  const { data } = await http.get<ApiResponse<string[]>>('/api/reservations/notifications')
  return data
}

export async function apiMyAuditLogs() {
  const { data } = await http.get<ApiResponse<string[]>>('/api/reservations/audit/mine')
  return data
}

export async function apiAdminAuditLogs() {
  const { data } = await http.get<ApiResponse<string[]>>('/api/admin/reservations/audit')
  return data
}

export async function apiOverviewStats() {
  const { data } = await http.get<ApiResponse<OverviewStats>>('/api/stats/overview')
  return data
}

export async function apiUserList() {
  const { data } = await http.get<ApiResponse<UserAccount[]>>('/api/admin/users')
  return data
}

export async function apiUpdateUserRole(id: number, role: 'SUPER_ADMIN' | 'ADMIN' | 'USER') {
  const { data } = await http.post<ApiResponse<null>>(`/api/admin/users/${id}/role`, { role })
  return data
}

export async function apiUpdateUserEnabled(id: number, enabled: boolean) {
  const { data } = await http.post<ApiResponse<null>>(`/api/admin/users/${id}/enabled`, { enabled })
  return data
}
