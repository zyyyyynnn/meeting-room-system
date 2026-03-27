import axios from 'axios'
import { ElMessage } from 'element-plus'
import { authStore } from '../store/auth'

export const http = axios.create({
  baseURL: '',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = authStore.state.token
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => resp,
  (err) => {
    const status = err?.response?.status
    const message = err?.response?.data?.message || err?.message || '请求失败'

    if (status === 401) {
      authStore.clear()
      ElMessage.error('登录已失效，请重新登录')
    } else {
      ElMessage.error(message)
    }

    return Promise.reject(err)
  },
)

