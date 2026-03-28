import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { authStore, type Role } from '../store/auth'
import { resolveHomeRoute } from '../utils/authRoute'
import { clearChunkRecoveryMarkers, isChunkLoadError, tryRedirectToLoginOnce, tryReloadForChunkError } from '../utils/chunkRecovery'

const routes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('../views/LoginView.vue') },
  { path: '/register', component: () => import('../views/RegisterView.vue') },
  {
    path: '/',
    component: () => import('../views/LayoutView.vue'),
    children: [
      { path: '', redirect: () => (authStore.isAuthed.value ? resolveHomeRoute(authStore.state.role) : '/login') },
      { path: 'dashboard', component: () => import('../views/DashboardView.vue') },
      { path: 'calendar', component: () => import('../views/CalendarView.vue') },
      { path: 'rooms', component: () => import('../views/RoomsView.vue') },
      { path: 'mine', component: () => import('../views/MyReservationsView.vue') },
      { path: 'notifications', component: () => import('../views/NotificationsView.vue') },
      { path: 'admin/approvals', component: () => import('../views/AdminApprovalsView.vue'), meta: { role: 'ADMIN' } },
      { path: 'admin/users', component: () => import('../views/UserManagementView.vue'), meta: { role: 'ADMIN' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: () => (authStore.isAuthed.value ? resolveHomeRoute(authStore.state.role) : '/login') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.path === '/login' && String(to.query.force ?? '') === '1') {
    authStore.clear()
    return true
  }

  if (!authStore.isAuthed.value && (authStore.state.token || authStore.state.role || authStore.state.userId)) {
    authStore.clear()
  }

  const isAuthPage = to.path === '/login' || to.path === '/register'
  const authed = authStore.isAuthed.value
  if (!authed && !isAuthPage) return '/login'
  if (authed && isAuthPage) return resolveHomeRoute(authStore.state.role)

  const requiredRole = to.meta.role as Role | undefined
  if (requiredRole && !authStore.hasRole(requiredRole)) return resolveHomeRoute(authStore.state.role)
})

router.afterEach(() => {
  clearChunkRecoveryMarkers()
})

router.onError((error, to) => {
  if (!isChunkLoadError(error)) return

  const target = typeof to?.fullPath === 'string' ? to.fullPath : undefined
  if (tryReloadForChunkError(target)) return

  authStore.clear()
  if (tryRedirectToLoginOnce()) return

  console.error('[chunk-recovery] route chunk reload failed more than once', error)
})

export default router
