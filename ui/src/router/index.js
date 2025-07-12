import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/modules/user'

// 懒加载路由
const Login = () => import('@/views/Login.vue')
const Register = () => import('@/views/Register.vue')
const Layout = () => import('@/layout/index.vue')

// 路由配置
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { guest: true, title: '登录', hidden: true, icon: 'User' }
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { guest: true, title: '注册', hidden: true, icon: 'UserFilled' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { 
          requiresAuth: true, 
          title: '仪表盘',
          icon: 'Odometer' 
        }
      }
    ]
  },
  {
    path: '/ai-chat',
    component: Layout,
    children: [
      {
        path: '',
        name: 'AiChat',
        component: () => import('@/views/ai-chat/AiChat.vue'),
        meta: {
          requiresAuth: true,
          title: 'AI 助手',
          icon: 'Service'
        }
      }
    ]
  },
  {
    path: '/equipment',
    component: Layout,
    redirect: '/equipment/list',
    meta: { 
      title: '装备管理',
      icon: 'Box',
      requiresAuth: true
    },
    children: [
      {
        path: 'list',
        name: 'EquipmentList',
        component: () => import('@/views/equipment/list.vue'),
        meta: { 
          requiresAuth: true,
          title: '装备列表',
          icon: 'List'
        }
      },
      {
        path: 'category',
        name: 'EquipmentCategory',
        component: () => import('@/views/equipment/category.vue'),
        meta: { 
          requiresAuth: true, 
          requiresAdmin: true,
          title: '分类管理',
          icon: 'Folder'
        }
      },
      {
        path: 'edit/:id?',
        name: 'EquipmentEdit',
        component: () => import('@/views/equipment/edit.vue'),
        meta: { 
          requiresAuth: true, 
          requiresAdmin: true,
          title: '编辑装备',
          icon: 'Edit',
          hidden: true
        }
      },
      {
        path: ':id',
        name: 'EquipmentDetail',
        component: () => import('@/views/equipment/detail.vue'),
        meta: { 
          requiresAuth: true,
          title: '装备详情',
          icon: 'Document',
          hidden: true
        }
      }
    ]
  },
  {
    path: '/borrow',
    component: Layout,
    redirect: '/borrow/my',
    meta: { 
      title: '借用管理',
      icon: 'DocumentCopy',
      requiresAuth: true
    },
    children: [
      {
        path: 'my',
        name: 'MyBorrows',
        component: () => import('@/views/borrow/my.vue'),
        meta: { 
          requiresAuth: true,
          title: '我的借用',
          icon: 'User'
        }
      },
      {
        path: 'all',
        name: 'AllBorrows',
        component: () => import('@/views/borrow/list.vue'),
        meta: { 
          requiresAuth: true, 
          requiresAdmin: true,
          title: '所有借用',
          icon: 'Files'
        }
      },
      {
        path: 'pending',
        name: 'PendingBorrows',
        component: () => import('@/views/borrow/pending.vue'),
        meta: { 
          requiresAuth: true, 
          requiresAdmin: true,
          title: '待审批',
          icon: 'Timer'
        }
      },
      {
        path: 'stats',
        name: 'BorrowStats',
        component: () => import('@/views/borrow/stats.vue'),
        meta: { 
          requiresAuth: true,
          requiresAdmin: true,
          title: '借用统计',
          icon: 'TrendCharts'
        }
      },
      {
        path: 'overdue',
        name: 'OverdueManagement',
        component: () => import('@/views/borrow/overdue.vue'),
        meta: { 
          requiresAuth: true,
          requiresAdmin: true,
          title: '逾期管理',
          icon: 'AlarmClock'
        }
      },
      {
        path: ':id',
        name: 'BorrowDetail',
        component: () => import('@/views/borrow/detail.vue'),
        meta: { 
          requiresAuth: true,
          title: '借用详情',
          icon: 'Document',
          hidden: true
        }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    redirect: '/user/profile',
    meta: { 
      title: '用户管理',
      icon: 'UserFilled',
      requiresAuth: true
    },
    children: [
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/user/profile.vue'),
        meta: { 
          requiresAuth: true,
          title: '个人信息',
          icon: 'User'
        }
      },
      {
        path: 'list',
        name: 'UserList',
        component: () => import('@/views/user/list.vue'),
        meta: { 
          requiresAuth: true, 
          requiresAdmin: true,
          title: '用户列表',
          icon: 'Avatar'
        }
      }
    ]
  },
  {
    path: '/404',
    component: () => import('@/views/error/404.vue'),
    meta: { hidden: true, title: '404', icon: 'Warning' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
    meta: { hidden: true }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 导航守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const isLoggedIn = userStore.isLoggedIn
  const isAdmin = userStore.isAdmin
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 人员装备管理系统` : '人员装备管理系统'
  
  // 简化路由逻辑
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!isLoggedIn) {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      return
    }
    
    if (to.matched.some(record => record.meta.requiresAdmin) && !isAdmin) {
      const isDevelopment = process.env.NODE_ENV === 'development'
      next(isDevelopment ? undefined : { path: '/404' })
      return
    }
  } 
  else if (to.matched.some(record => record.meta.guest) && isLoggedIn) {
      next({ path: '/' })
    return
  } 
  
    next()
})

export default router 