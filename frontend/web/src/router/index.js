import { createRouter, createWebHistory } from 'vue-router'
import { trackPageview } from '../utils/tracker'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/portfolio', name: 'Portfolio', component: () => import('../views/Portfolio.vue') },
  { path: '/notes', name: 'Notes', component: () => import('../views/Notes.vue') },
  { path: '/notes/page/:pageNum', name: 'NotesPage', component: () => import('../views/Notes.vue') },
  { path: '/notes/:blogId', name: 'NoteDetail', component: () => import('../views/BlogDetail.vue') },
  { path: '/life', name: 'Life', component: () => import('../views/Life.vue') },
  { path: '/message', name: 'Message', component: () => import('../views/Message.vue') },
  { path: '/blog/:blogId', name: 'BlogDetail', redirect: to => '/notes/' + to.params.blogId },
  { path: '/article/:blogId', name: 'Article', redirect: to => '/notes/' + to.params.blogId },
  { path: '/page/:pageNum', name: 'LegacyHomePage', redirect: to => '/notes/page/' + to.params.pageNum },
  { path: '/categories', name: 'Categories', component: () => import('../views/Categories.vue') },
  { path: '/category/:name/:page?', name: 'Category', component: () => import('../views/BlogList.vue') },
  { path: '/tag/:name/:page?', name: 'Tag', component: () => import('../views/BlogList.vue') },
  { path: '/search/:keyword/:page?', name: 'Search', component: () => import('../views/BlogList.vue') },
  { path: '/link', name: 'Links', redirect: '/portfolio' },
  { path: '/about', name: 'About', redirect: '/' },
  { path: '/:subUrl', name: 'Page', component: () => import('../views/BlogDetail.vue') },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('../views/NotFound.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() { return { top: 0 } }
})

// 页面访问统计埋点：单发上报（无队列、无批量），削峰交给服务端有界队列
// 预渲染 / 无头浏览器环境由 tracker 内部自动关闭，不会污染统计
router.afterEach((to) => {
  trackPageview(to)
})

export default router
