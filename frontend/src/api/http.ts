import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { authStore } from '../store/auth'
import { getDefaultUnavailableMessage, serviceStatusStore } from '../store/serviceStatus'

type ErrorResponse = {
  message?: string
}

type RequestError = AxiosError<ErrorResponse> & {
  infrastructure?: boolean
  userMessage?: string
}

const INFRASTRUCTURE_STATUSES = new Set([502, 503, 504])
const INFRASTRUCTURE_MESSAGE = getDefaultUnavailableMessage()

export const http = axios.create({
  baseURL: '',
  timeout: 15000,
})

function resolveResponseMessage(error: RequestError | undefined) {
  const message = error?.response?.data?.message
  return typeof message === 'string' && message.trim() ? message.trim() : ''
}

function looksLikeInfrastructureError(error: RequestError) {
  const status = error.response?.status
  if (status && INFRASTRUCTURE_STATUSES.has(status)) return true
  if (error.code === 'ERR_CANCELED') return false
  if (!error.response) return true

  const raw = `${error.code ?? ''} ${error.message ?? ''}`
  return /network error|timeout|proxy|socket hang up|econnrefused|econnreset|failed to fetch/i.test(raw)
}

function decorateRequestError(error: unknown) {
  const requestError = error as RequestError
  const infrastructure = looksLikeInfrastructureError(requestError)
  const responseMessage = resolveResponseMessage(requestError)
  const fallbackMessage = typeof requestError.message === 'string' && requestError.message.trim() ? requestError.message.trim() : '请求失败'

  requestError.infrastructure = infrastructure
  requestError.userMessage = infrastructure ? INFRASTRUCTURE_MESSAGE : responseMessage || fallbackMessage
  return requestError
}

export function isInfrastructureRequestError(error: unknown) {
  return !!(error as RequestError | undefined)?.infrastructure
}

export function getRequestErrorMessage(error: unknown, fallback = '请求失败') {
  const requestError = error as RequestError | undefined
  return requestError?.userMessage || resolveResponseMessage(requestError) || requestError?.message || fallback
}

http.interceptors.request.use((config) => {
  const token = authStore.state.token
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    serviceStatusStore.markHealthy()
    return resp
  },
  (error) => {
    const requestError = decorateRequestError(error)
    const status = requestError.response?.status
    const message = requestError.userMessage || '请求失败'
    const method = requestError.config?.method?.toLowerCase()

    if (requestError.infrastructure) {
      serviceStatusStore.markUnavailable(message)
    } else {
      serviceStatusStore.markHealthy()
    }

    if (status === 401) {
      authStore.clear()
      serviceStatusStore.markHealthy()
      ElMessage.error('登录已失效，请重新登录')
      if (router.currentRoute.value.path !== '/login') {
        router.replace('/login').catch(() => {})
      }
    } else if (!requestError.infrastructure && method !== 'get') {
      ElMessage.error(message)
    }

    return Promise.reject(requestError)
  },
)
