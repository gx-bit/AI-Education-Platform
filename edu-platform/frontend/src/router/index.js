import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import NProgress from 'nprogress'

const routes = [
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/HomeView.vue') },
      { path: 'courses', name: 'CourseList', component: () => import('@/views/course/CourseListView.vue') },
      { path: 'courses/:id', name: 'CourseDetail', component: () => import('@/views/course/CourseDetailView.vue') },
      {
        path: 'orders',
        name: 'MyOrders',
        component: () => import('@/views/order/MyOrdersView.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/ProfileView.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'favorites',
        name: 'MyFavorites',
        component: () => import('@/views/course/MyFavoritesView.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'payment/result',
        name: 'PaymentResult',
        component: () => import('@/views/order/PaymentResultView.vue'),
        meta: { requiresAuth: true }
      }
    ]
  },
  {
    path: '/auth',
    component: () => import('@/components/layout/AuthLayout.vue'),
    children: [
      { path: 'login', name: 'Login', component: () => import('@/views/auth/LoginView.vue') },
      { path: 'register', name: 'Register', component: () => import('@/views/auth/RegisterView.vue') }
    ]
  },
  {
    path: '/login',
    redirect: '/auth/login'
  },
  {
    path: '/admin',
    component: () => import('@/components/layout/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/DashboardView.vue') },
      { path: 'courses', name: 'AdminCourses', component: () => import('@/views/admin/CourseManageView.vue') },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/UserManageView.vue') },
      { path: 'orders', name: 'AdminOrders', component: () => import('@/views/admin/OrderManageView.vue') },
      { path: 'notifications', name: 'AdminNotifications', component: () => import('@/views/admin/NotificationManageView.vue') },
      { path: 'recommendation', name: 'AdminRecommendation', component: () => import('@/views/admin/RecommendationManageView.vue') },
      { path: 'monitor', name: 'AdminMonitor', component: () => import('@/views/admin/SystemMonitorView.vue') },
      { path: 'stats', name: 'AdminStats', component: () => import('@/views/dashboard/DataDashboardView.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to, from, next) => {
  NProgress.start()
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/')
  } else {
    next()
  }
})

router.afterEach(() => NProgress.done())

export default router
