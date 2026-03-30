import type { Role } from '../store/auth'

export function resolveHomeRoute(_role: Role | null) {
  return '/dashboard'
}

