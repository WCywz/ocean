import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/home/HeroV2View.vue'),
    meta: { title: '首页', noAuth: true }
  },
  {
    path: '/hero-v2',
    name: 'HeroV2',
    component: () => import('../views/home/HeroV2View.vue'),
    meta: { title: 'Hero V2', noAuth: true }
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
        path: 'forecast/sst',
        name: 'ForecastSst',
        component: () => import('../views/forecast/SstMapView.vue'),
        meta: { title: '海表温度预测' }
      },
      {
        path: 'forecast/chl',
        name: 'ForecastCHL',
        component: () => import('../views/forecast/ChxMapView.vue'),
        meta: { title: '叶绿素预测' }
      },
      {
        path: 'forecast/history',
        name: 'ForecastHistory',
        component: () => import('../views/forecast/HistoryView.vue'),
        meta: { title: '历史预报记录' }
      },
      {
        path: 'ocean-data',
        name: 'OceanData',
        component: () => import('../views/ocean/OceanDataView.vue'),
        meta: { title: '海洋观测数据' }
      },
      {
        path: 'ocean-health',
        name: 'OceanHealth',
        component: () => import('../views/health/OceanHealthView.vue'),
        meta: { title: '海洋健康指数' }
      }
    ]
  },
  {
    path: '/login',
    redirect: '/'
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

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `海洋环境预报系统 - ${to.meta.title}` : '海洋环境预报系统'

  const token = localStorage.getItem('token')

  if (to.meta.noAuth) {
    if (token && (to.path === '/login' || to.path === '/register' || to.path === '/')) {
      next('/app/dashboard')
      return
    }
    next()
    return
  }

  if (!token) {
    next('/')
    return
  }

  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
  if (to.meta.role === 'ADMIN' && userInfo?.role !== 'ADMIN') {
    next('/app/dashboard')
    return
  }

  next()
})

export default router
