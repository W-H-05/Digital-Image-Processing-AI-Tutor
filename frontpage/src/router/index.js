import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/login/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'home',
    component: () => import('../views/student/HomeView.vue')
  },
  {
    path: '/pack/:id',
    name: 'pack',
    component: () => import('../views/student/PackView.vue')
  },
  {
    path: '/self',
    name: 'self',
    component: () => import('../views/student/SelfView.vue')
  },
  {
    path: '/review',
    name: 'review',
    component: () => import('../views/student/ReviewView.vue')
  },
  {
    path: '/teacher',
    name: 'teacher',
    component: () => import('../views/teacher/TeacherLayout.vue'),
    children: [
      { path: '', redirect: '/teacher/dashboard' },
      { path: 'students', name: 'teacherStudents', component: () => import('../views/teacher/StudentsView.vue') },
      { path: 'packs', name: 'teacherPacks', component: () => import('../views/teacher/PacksView.vue') },
      { path: 'review', name: 'teacherReview', component: () => import('../views/teacher/ReviewManageView.vue') },
      { path: 'prepare', name: 'teacherPrepare', component: () => import('../views/teacher/PrepareView.vue') },
      { path: 'dashboard', name: 'teacherDashboard', component: () => import('../views/teacher/DashboardView.vue') },
      { path: 'operations', name: 'teacherOperations', component: () => import('../views/teacher/OperationsView.vue') },
      { path: 'rag', name: 'teacherRag', component: () => import('../views/teacher/RagView.vue') },
      { path: 'prompts', name: 'teacherPrompts', component: () => import('../views/teacher/PromptView.vue') },
      { path: 'config', name: 'teacherConfig', component: () => import('../views/teacher/ConfigView.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')
  if (to.meta.public) {
    next()
    return
  }
  if (!token) {
    next('/login')
    return
  }
  // 教师端路由保护
  if (to.path.startsWith('/teacher') && role !== 'TEACHER') {
    next('/home')
    return
  }
  next()
})

export default router
