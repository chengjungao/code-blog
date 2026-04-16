import axios from 'axios'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

// ===== URL 编码转换器（复用） =====
const urlEncodedTransformer = function (data) {
  return Object.keys(data)
    .filter(k => data[k] !== undefined && data[k] !== null)
    .map(k => encodeURIComponent(k) + '=' + encodeURIComponent(data[k]))
    .join('&')
}

// ===== 认证（走 AdminApiController /admin/api） =====
export function login(data) {
  return request({
    url: '/login',
    method: 'post',
    params: data
  })
}

export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}

export function getProfile() {
  return request({
    url: '/profile',
    method: 'get'
  })
}

// ===== Dashboard =====
export function getDashboardData() {
  return request({
    url: '/dashboard',
    method: 'get'
  })
}

// ===== 验证码（直接访问后端） =====
export function getKaptcha() {
  return axios.create({ baseURL: '', withCredentials: true }).get('/common/kaptcha', {
    responseType: 'blob'
  })
}

// ===== 密码与昵称（走 AdminApiController /admin/api） =====
export function updatePassword(data) {
  return request({
    url: '/profile/password',
    method: 'post',
    params: data
  })
}

export function updateName(data) {
  return request({
    url: '/profile/name',
    method: 'post',
    params: data
  })
}

// ===== 以下接口走原始 Controller（/admin/xxx），需要完整前缀 =====
const crud = axios.create({
  baseURL: '/admin',
  timeout: 15000,
  withCredentials: true,
  headers: { 'X-Requested-With': 'XMLHttpRequest' }
})

// CRUD 响应拦截：原始 Controller 返回 Result{resultCode, message, data}，格式相同
crud.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response) {
      const msg = error.response.data?.message || '网络错误'
      ElMessage.error(msg)
    } else {
      ElMessage.error('网络连接失败')
    }
    return Promise.reject(error)
  }
)

// ===== 博客管理 =====
export function getBlogList(params) {
  return crud({ url: '/blogs/list', method: 'get', params })
}

export function getBlogEditData(blogId) {
  return request({  // 走 AdminApiController
    url: '/blogs/edit',
    method: 'get',
    params: blogId ? { blogId } : {}
  })
}

export function saveBlog(data) {
  return crud({
    url: '/blogs/save', method: 'post', data,
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    transformRequest: [urlEncodedTransformer]
  })
}

export function updateBlog(data) {
  return crud({
    url: '/blogs/update', method: 'post', data,
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    transformRequest: [urlEncodedTransformer]
  })
}

export function deleteBlog(ids) {
  return crud({ url: '/blogs/delete', method: 'post', data: ids, headers: { 'Content-Type': 'application/json' } })
}

// ===== 分类管理 =====
export function getCategoryList(params) {
  return crud({ url: '/categories/list', method: 'get', params })
}

export function saveCategory(data) {
  return crud({ url: '/categories/save', method: 'post', params: data })
}

export function updateCategory(data) {
  return crud({ url: '/categories/update', method: 'post', params: data })
}

export function deleteCategory(ids) {
  return crud({ url: '/categories/delete', method: 'post', data: ids, headers: { 'Content-Type': 'application/json' } })
}

// ===== 标签管理 =====
export function getTagList(params) {
  return crud({ url: '/tags/list', method: 'get', params })
}

export function saveTag(data) {
  return crud({ url: '/tags/save', method: 'post', params: data })
}

export function deleteTag(ids) {
  return crud({ url: '/tags/delete', method: 'post', data: ids, headers: { 'Content-Type': 'application/json' } })
}

// ===== 评论管理 =====
export function getCommentList(params) {
  return crud({ url: '/comments/list', method: 'get', params })
}

export function checkDoneComments(ids) {
  return crud({ url: '/comments/checkDone', method: 'post', data: ids, headers: { 'Content-Type': 'application/json' } })
}

export function replyComment(data) {
  return crud({ url: '/comments/reply', method: 'post', params: data })
}

export function deleteComment(ids) {
  return crud({ url: '/comments/delete', method: 'post', data: ids, headers: { 'Content-Type': 'application/json' } })
}

// ===== 链接管理 =====
export function getLinkList(params) {
  return crud({ url: '/links/list', method: 'get', params })
}

export function getLinkInfo(id) {
  return crud({ url: `/links/info/${id}`, method: 'get' })
}

export function saveLink(data) {
  return crud({ url: '/links/save', method: 'post', params: data })
}

export function updateLink(data) {
  return crud({ url: '/links/update', method: 'post', params: data })
}

export function deleteLink(ids) {
  return crud({ url: '/links/delete', method: 'post', data: ids, headers: { 'Content-Type': 'application/json' } })
}

// ===== 系统配置（获取走 AdminApiController /admin/api，保存走原始 Controller /admin） =====
export function getConfigList() {
  return request({ url: '/configurations', method: 'get' })
}

export function saveWebsiteConfig(data) {
  return crud({
    url: '/configurations/website', method: 'post', data,
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    transformRequest: [urlEncodedTransformer]
  })
}

export function saveUserInfoConfig(data) {
  return crud({
    url: '/configurations/userInfo', method: 'post', data,
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    transformRequest: [urlEncodedTransformer]
  })
}

export function saveFooterConfig(data) {
  return crud({
    url: '/configurations/footer', method: 'post', data,
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    transformRequest: [urlEncodedTransformer]
  })
}

// ===== 文件上传 =====
export async function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  const res = await axios.create({
    baseURL: '/admin',
    withCredentials: true,
    timeout: 30000
  }).post('/upload/file', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data
}
