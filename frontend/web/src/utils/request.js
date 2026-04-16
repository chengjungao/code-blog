import axios from 'axios'

const request = axios.create({
  baseURL: '/blog/api',
  timeout: 15000,
  withCredentials: true
})

request.interceptors.response.use(
  res => res.data,
  err => {
    console.error('请求错误:', err)
    return Promise.reject(err)
  }
)

export default request
