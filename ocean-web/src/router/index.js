import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/home/HomeView.vue'),
    meta: { title: '首页', noAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/LoginView.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/register/RegisterView.vue'),
    meta: { title: '注册', noAuth: true }
  },
  {
    path: '/app',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/app/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/DashboardView.vue'),
        meta: { title: '首页仪表盘' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('../views/user/UserView.vue'),
        meta: { title: '用户管理', role: 'ADMIN' }
      },
      {
        path: 'model',
        name: 'Model',
        component: () => import('../views/model/ModelView.vue'),
        meta: { title: '预报模型管理', role: 'ADMIN' }
      },
      {
        path: 'forecast',
        name: 'Forecast',
        component: () => import('../views/forecast/ForecastView.vue'),
        meta: { title: '预报数据可视化' }
      },
      {
        path: 'ocean-data',
        name: 'OceanData',
        component: () => import('../views/ocean/OceanDataView.vue'),
        meta: { title: '海洋观测数据' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫 - 权限拦截
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `海洋环境预报系统 - ${to.meta.title}` : '海洋环境预报系统'

  const token = localStorage.getItem('token')

  // 无需认证的页面
  if (to.meta.noAuth) {
    if (token && (to.path === '/login' || to.path === '/register' || to.path === '/')) {
      next('/app/dashboard')
      return
    }
    next()
    return
  }

  // 未登录跳转首页
  if (!token) {
    next('/')
    return
  }

  // 角色权限检查
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
  if (to.meta.role === 'ADMIN' && userInfo?.role !== 'ADMIN') {
    next('/app/dashboard')
    return
  }

  next()
})

export default router
