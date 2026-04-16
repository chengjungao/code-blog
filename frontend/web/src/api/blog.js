import request from '../utils/request'

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
