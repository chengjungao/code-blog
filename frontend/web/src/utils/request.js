import axios from 'axios'

const request = axios.create({
  baseURL: '/blog/api',
  timeout: 15000,
  withCredentials: true
})

// AI 对话请求专用实例，超时 60 秒
export const chatRequest = axios.create({
  baseURL: '/blog/api',
  timeout: 60000,
  withCredentials: true
})

request.interceptors.response.use(
  res => res.data,
  err => {
    console.error('请求错误:', err)
    return Promise.reject(err)
  }
)

chatRequest.interceptors.response.use(
  res => res.data,
  err => {
    console.error('AI 请求错误:', err)
    return Promise.reject(err)
  }
)

export default request
