import request, { chatRequest } from '../utils/request'

export const fetchConfig = () => request.get('/config')

export const fetchIndex = (page = 1) => request.get(`/index/${page}`)

export const fetchBlogDetail = (blogId, commentPage = 1) =>
  request.get(`/blog/${blogId}`, { params: { commentPage } })

export const fetchPageBySubUrl = (subUrl) => request.get(`/page/${subUrl}`)

export const fetchCategories = () => request.get('/categories')

export const fetchCategoryBlogs = (name, page = 1) => request.get(`/category/${name}/${page}`)

export const fetchTagBlogs = (name, page = 1) => request.get(`/tag/${name}/${page}`)

export const fetchSearchBlogs = (keyword, page = 1) => request.get(`/search/${keyword}/${page}`)

export const fetchLinks = () => request.get('/links')

export const submitComment = (data) => request.post('/comment', data)

export const fetchMessages = (page = 1) => request.get(`/messages/${page}`)

export const submitMessage = (data) => request.post('/message', data)

export const assistantChat = (message, history = []) =>
  chatRequest.post('/assistant', { message, history })

// 页面访问统计埋点（fire-and-forget，失败不影响页面）
export const reportVisit = (pagePath) =>
  request.post('/stat', { pagePath }).catch(() => {})

/**
 * 生成博客链接：优先使用自定义路径（blogSubUrl），否则使用 /notes/:blogId
 * @param {Object} blog - 博客对象，包含 blogId 和 blogSubUrl
 * @returns {string} 路由路径
 */
export const blogLink = (blog) => {
  if (blog?.blogSubUrl && blog.blogSubUrl.trim()) {
    return '/' + blog.blogSubUrl.trim()
  }
  return '/notes/' + blog.blogId
}
