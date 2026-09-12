import axios from 'axios'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' }
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    NProgress.start()
    const token = sessionStorage.getItem('token') || localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    NProgress.done()
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    NProgress.done()
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('rememberLogin')
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('userInfo')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    NProgress.done()
    const status = error.response?.status
    const msgMap = {
      401: '登录已过期，请重新登录',
      403: '权限不足',
      404: '请求资源不存在',
      500: '服务器内部错误'
    }
    ElMessage.error(msgMap[status] || error.message || '网络请求失败')
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('rememberLogin')
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('userInfo')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default request
