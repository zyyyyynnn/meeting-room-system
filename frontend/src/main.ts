import { createApp } from 'vue'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import App from './App.vue'
import router from './router'
import { authStore } from './store/auth'
import { isChunkLoadError, tryRedirectToLoginOnce, tryReloadForChunkError } from './utils/chunkRecovery'
import './style.css'

window.addEventListener('unhandledrejection', (event) => {
  if (!isChunkLoadError(event.reason)) return

  event.preventDefault()

  if (tryReloadForChunkError()) return

  authStore.clear()
  if (tryRedirectToLoginOnce()) return

  ElMessage.error('Page assets failed to load, please refresh and try again')
})

createApp(App).use(router).mount('#app')
