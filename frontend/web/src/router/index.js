import { createRouter, createWebHistory } from 'vue-router'
import { reportVisit } from '../api/blog'

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

// 页面访问统计埋点（记录前台页面路径，用于访问分析）
router.afterEach((to) => {
  reportVisit(to.path)
})

export default router
