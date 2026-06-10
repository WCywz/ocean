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
        component: () => import('../layout/ModelLayout.vue'),
        meta: { role: 'ADMIN' },
        children: [
          {
            path: '',
            name: 'Model',
            component: () => import('../views/model/ModelList.vue'),
            meta: { title: '模型管理', role: 'ADMIN' }
          },
          {
            path: ':id(\\d+)',
            name: 'ModelDetail',
            component: () => import('../views/model/ModelDetail.vue'),
            meta: { title: '模型详情', role: 'ADMIN' }
          },
          {
            path: 'schedule',
            name: 'ModelSchedule',
            component: () => import('../views/model/ScheduleOverview.vue'),
            meta: { title: '调度总览', role: 'ADMIN' }
          },
          {
            path: 'monitor',
            name: 'ModelMonitor',
            component: () => import('../views/model/RunMonitor.vue'),
            meta: { title: '运行监控', role: 'ADMIN' }
          },
          {
            path: 'alerts',
            name: 'ModelAlerts',
            component: () => import('../views/model/AlertManagement.vue'),
            meta: { title: '告警管理', role: 'ADMIN' }
          },
          {
            path: 'compare',
            name: 'ModelCompare',
            component: () => import('../views/model/ModelCompare.vue'),
            meta: { title: '模型对比', role: 'ADMIN' }
          }
        ]
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
        path: 'observation/sst',
        name: 'ObsSst',
        component: () => import('../views/observation/ObsSstView.vue'),
        meta: { title: '海表温度观测' }
      },
      {
        path: 'observation/chl',
        name: 'ObsCHL',
        component: () => import('../views/observation/ObsChlView.vue'),
        meta: { title: '叶绿素观测' }
      },
      {
        path: 'observation/history',
        name: 'ObsHistory',
        component: () => import('../views/observation/ObsHistoryView.vue'),
        meta: { title: '历史观测记录' }
      },
      {
        path: 'ocean-health',
        name: 'OceanHealth',
        component: () => import('../views/health/OceanHealthView.vue'),
        meta: { title: '海洋健康指数' }
      },
      {
        path: 'profile',
        component: () => import('../views/profile/ProfileLayout.vue'),
        redirect: '/app/profile/info',
        children: [
          {
            path: 'info',
            name: 'ProfileInfo',
            component: () => import('../views/profile/ProfileInfo.vue'),
            meta: { title: '个人信息' }
          },
          {
            path: 'security',
            name: 'ProfileSecurity',
            component: () => import('../views/profile/ProfileSecurity.vue'),
            meta: { title: '账户安全' }
          },
          {
            path: 'notifications',
            name: 'ProfileNotifications',
            component: () => import('../views/profile/ProfileNotifications.vue'),
            meta: { title: '通知设置' }
          },
          {
            path: 'announcements',
            name: 'ProfileAnnouncements',
            component: () => import('../views/profile/ProfileAnnouncements.vue'),
            meta: { title: '系统公告' }
          },
          {
            path: 'settings',
            name: 'ProfileSettings',
            component: () => import('../views/profile/ProfileSettings.vue'),
            meta: { title: '系统设置' }
          }
        ]
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
