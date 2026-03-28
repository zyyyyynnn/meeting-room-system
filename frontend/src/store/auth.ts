import { computed, reactive } from 'vue'

export type Role = 'SUPER_ADMIN' | 'ADMIN' | 'USER'

export type AuthState = {
  token: string | null
  userId: number | null
  username: string | null
  role: Role | null
}

const LS_KEY = 'mrs_auth'

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = payload + '='.repeat((4 - (payload.length % 4)) % 4)
    const json = atob(padded)
    const obj = JSON.parse(json)
    return obj && typeof obj === 'object' ? (obj as Record<string, unknown>) : null
  } catch {
    return null
  }
}

function tokenNotExpired(token: string): boolean {
  const payload = decodeJwtPayload(token)
  const exp = Number(payload?.exp)
  if (!Number.isFinite(exp) || exp <= 0) return false
  const nowSec = Math.floor(Date.now() / 1000)
  return exp > nowSec
}

function normalizeToken(raw: unknown): string | null {
  const token = String(raw ?? '').trim()
  if (!token || token === 'null' || token === 'undefined') return null
  // JWT tokens in this project are always xxx.yyy.zzz
  if (!/^[A-Za-z0-9\-_]+\.[A-Za-z0-9\-_]+\.[A-Za-z0-9\-_]+$/.test(token)) return null
  if (!tokenNotExpired(token)) return null
  return token
}

function normalizeUserId(raw: unknown): number | null {
  const value = Number(raw)
  if (!Number.isFinite(value) || value <= 0) return null
  return Math.trunc(value)
}

function normalizeUsername(raw: unknown): string | null {
  const value = String(raw ?? '').trim()
  if (!value || value === 'null' || value === 'undefined') return null
  return value
}

function normalizeRole(raw: unknown): Role | null {
  const role = String(raw ?? '').trim().toUpperCase()
  if (role === 'SUPER_ADMIN' || role === 'ADMIN' || role === 'USER') return role
  return null
}

function hasValidSession(next: AuthState) {
  return !!next.token && !!next.userId && !!next.role
}

function load(): AuthState {
  try {
    const raw = localStorage.getItem(LS_KEY)
    if (!raw) return { token: null, userId: null, username: null, role: null }
    const o = JSON.parse(raw) as AuthState
    const loaded: AuthState = {
      token: normalizeToken(o.token),
      userId: normalizeUserId(o.userId),
      username: normalizeUsername(o.username),
      role: normalizeRole(o.role),
    }
    return {
      token: hasValidSession(loaded) ? loaded.token : null,
      userId: hasValidSession(loaded) ? loaded.userId : null,
      username: hasValidSession(loaded) ? loaded.username : null,
      role: hasValidSession(loaded) ? loaded.role : null,
    }
  } catch {
    return { token: null, userId: null, username: null, role: null }
  }
}

const state = reactive<AuthState>(load())

function rank(role: Role | null) {
  if (role === 'SUPER_ADMIN') return 3
  if (role === 'ADMIN') return 2
  if (role === 'USER') return 1
  return 0
}

export const authStore = {
  state,
  isAuthed: computed(() => hasValidSession(state)),
  isAdmin: computed(() => rank(state.role) >= 2),
  isSuperAdmin: computed(() => state.role === 'SUPER_ADMIN'),
  hasRole(required: Role) {
    return rank(state.role) >= rank(required)
  },
  setAuth(next: AuthState) {
    state.token = normalizeToken(next.token)
    state.userId = normalizeUserId(next.userId)
    state.username = normalizeUsername(next.username)
    state.role = normalizeRole(next.role)

    if (!hasValidSession(state)) {
      localStorage.removeItem(LS_KEY)
      return
    }

    localStorage.setItem(
      LS_KEY,
      JSON.stringify({
        token: state.token,
        userId: state.userId,
        username: state.username,
        role: state.role,
      }),
    )
  },
  clear() {
    state.token = null
    state.userId = null
    state.username = null
    state.role = null
    localStorage.removeItem(LS_KEY)
  },
}
