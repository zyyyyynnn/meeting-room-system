import { computed, reactive } from 'vue'

export type Role = 'SUPER_ADMIN' | 'ADMIN' | 'USER'

export type AuthState = {
  token: string | null
  userId: number | null
  username: string | null
  role: Role | null
}

const LS_KEY = 'mrs_auth'

function load(): AuthState {
  try {
    const raw = localStorage.getItem(LS_KEY)
    if (!raw) return { token: null, userId: null, username: null, role: null }
    const o = JSON.parse(raw) as AuthState
    return {
      token: o.token ?? null,
      userId: o.userId ?? null,
      username: o.username ?? null,
      role: (o.role as Role) ?? null,
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
  isAuthed: computed(() => !!state.token),
  isAdmin: computed(() => rank(state.role) >= 2),
  isSuperAdmin: computed(() => state.role === 'SUPER_ADMIN'),
  hasRole(required: Role) {
    return rank(state.role) >= rank(required)
  },
  setAuth(next: AuthState) {
    state.token = next.token
    state.userId = next.userId
    state.username = next.username
    state.role = next.role
    localStorage.setItem(LS_KEY, JSON.stringify(state))
  },
  clear() {
    state.token = null
    state.userId = null
    state.username = null
    state.role = null
    localStorage.removeItem(LS_KEY)
  },
}
