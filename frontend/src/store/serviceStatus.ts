import { computed, reactive } from 'vue'

export type BackendStatus = 'idle' | 'checking' | 'unreachable'

const DEFAULT_UNAVAILABLE_MESSAGE = '后端服务暂未就绪，请稍后重试并确认后端已启动。'
const DEFAULT_RECONNECTING_MESSAGE = '正在重新连接后端服务，请稍候。'

const state = reactive({
  backendStatus: 'idle' as BackendStatus,
  backendMessage: '',
})

export const serviceStatusStore = {
  state,
  backendStatus: computed(() => state.backendStatus),
  backendMessage: computed(() => state.backendMessage),
  markChecking(message = DEFAULT_RECONNECTING_MESSAGE) {
    state.backendStatus = 'checking'
    state.backendMessage = message
  },
  markUnavailable(message = DEFAULT_UNAVAILABLE_MESSAGE) {
    state.backendStatus = 'unreachable'
    state.backendMessage = message
  },
  markHealthy() {
    state.backendStatus = 'idle'
    state.backendMessage = ''
  },
}

export function getDefaultUnavailableMessage() {
  return DEFAULT_UNAVAILABLE_MESSAGE
}
