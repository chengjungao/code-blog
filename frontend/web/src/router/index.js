import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/page/:pageNum', name: 'HomePage', component: () => import('../views/Home.vue') },
  { path: '/blog/:blogId', name: 'BlogDetail', component: () => import('../views/BlogDetail.vue') },
  { path: '/article/:blogId', name: 'Article', redirect: to => '/blog/' + to.params.blogId },
  { path: '/categories', name: 'Categories', component: () => import('../views/Categories.vue') },
  { path: '/category/:name/:page?', name: 'Category', component: () => import('../views/BlogList.vue') },
  { path: '/tag/:name/:page?', name: 'Tag', component: () => import('../views/BlogList.vue') },
  { path: '/search/:keyword/:page?', name: 'Search', component: () => import('../views/BlogList.vue') },
  { path: '/link', name: 'Links', component: () => import('../views/Links.vue') },
  { path: '/:subUrl', name: 'Page', component: () => import('../views/BlogDetail.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() { return { top: 0 } }
})

export default router
