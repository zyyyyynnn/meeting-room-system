import type { Role } from '../store/auth'

export function resolveHomeRoute(role: Role | null) {
  if (role === 'SUPER_ADMIN') return '/admin/users'
  if (role === 'ADMIN') return '/admin/approvals'
  return '/dashboard'
}

