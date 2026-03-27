import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { authStore, type Role } from '../store/auth'

const routes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('../views/LoginView.vue') },
  { path: '/register', component: () => import('../views/RegisterView.vue') },
  {
    path: '/',
    component: () => import('../views/LayoutView.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('../views/DashboardView.vue') },
      { path: 'calendar', component: () => import('../views/CalendarView.vue') },
      { path: 'rooms', component: () => import('../views/RoomsView.vue') },
      { path: 'mine', component: () => import('../views/MyReservationsView.vue') },
      { path: 'notifications', component: () => import('../views/NotificationsView.vue') },
      { path: 'admin/approvals', component: () => import('../views/AdminApprovalsView.vue'), meta: { role: 'ADMIN' } },
      { path: 'admin/users', component: () => import('../views/UserManagementView.vue'), meta: { role: 'ADMIN' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const authed = authStore.isAuthed.value
  if (!authed && to.path !== '/login' && to.path !== '/register') return '/login'

  const requiredRole = to.meta.role as Role | undefined
  if (requiredRole && !authStore.hasRole(requiredRole)) return '/dashboard'
})

export default router
