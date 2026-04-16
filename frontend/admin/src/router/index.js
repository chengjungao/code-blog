import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('../layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'DataBoard' }
      },
      {
        path: 'blog/edit',
        name: 'BlogEdit',
        component: () => import('../views/BlogEdit.vue'),
        meta: { title: '发布博客', icon: 'Edit' }
      },
      {
        path: 'blog/edit/:id',
        name: 'BlogEditId',
        component: () => import('../views/BlogEdit.vue'),
        meta: { title: '编辑博客', icon: 'Edit' }
      },
      {
        path: 'blog/list',
        name: 'BlogList',
        component: () => import('../views/BlogList.vue'),
        meta: { title: '博客管理', icon: 'Document' }
      },
      {
        path: 'category',
        name: 'Category',
        component: () => import('../views/Category.vue'),
        meta: { title: '分类管理', icon: 'Folder' }
      },
      {
        path: 'tag',
        name: 'Tag',
        component: () => import('../views/Tag.vue'),
        meta: { title: '标签管理', icon: 'PriceTag' }
      },
      {
        path: 'comment',
        name: 'Comment',
        component: () => import('../views/Comment.vue'),
        meta: { title: '评论管理', icon: 'ChatDotRound' }
      },
      {
        path: 'link',
        name: 'Link',
        component: () => import('../views/Link.vue'),
        meta: { title: '链接管理', icon: 'Link' }
      },
      {
        path: 'config',
        name: 'Config',
        component: () => import('../views/Config.vue'),
        meta: { title: '系统配置', icon: 'Setting' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '个人中心', icon: 'User' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = to.meta.title + ' - 博客管理'
  }
  
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
