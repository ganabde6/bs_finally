import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/',
  timeout: 30000
})

// 请求拦截:携带 token
request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = 'Bearer ' + userStore.token
    }
    return config
  },
  err => Promise.reject(err)
)

// 响应拦截:统一处理
request.interceptors.response.use(
  resp => {
    const data = resp.data
    if (data.code === 200) {
      return data
    }
    // 业务错误
    if (data.code === 401) {
      console.error('[401 Unauthorized] URL:', resp.config.url, 'Token:', localStorage.getItem('token')?.substring(0, 20))
      ElMessage.error('登录已失效,请重新登录')
      const userStore = useUserStore()
      userStore.clear()
      router.push('/login')
      return Promise.reject(data)
    }
    ElMessage.error(data.message || '操作失败')
    return Promise.reject(data)
  },
  err => {
    console.error('[Network Error] URL:', err.config?.url, 'Error:', err.message)
    ElMessage.error('网络异常: ' + err.message)
    return Promise.reject(err)
  }
)

export default request
