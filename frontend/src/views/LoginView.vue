<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiLogin } from '../api/mrs'
import AuthMeshLogo from '../components/AuthMeshLogo.vue'
import PageStatusPanel from '../components/PageStatusPanel.vue'
import { authStore } from '../store/auth'
import { serviceStatusStore } from '../store/serviceStatus'
import { resolveHomeRoute } from '../utils/authRoute'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const backendStatus = computed(() => serviceStatusStore.backendStatus.value)
const backendMessage = computed(() => serviceStatusStore.backendMessage.value)
const showServiceStatus = computed(() => backendStatus.value === 'checking' || backendStatus.value === 'unreachable')
const serviceTone = computed(() => (backendStatus.value === 'unreachable' ? 'warning' : 'loading'))
const serviceTitle = computed(() => (backendStatus.value === 'unreachable' ? '后端服务暂未就绪' : '正在连接后端服务'))
const serviceDescription = computed(() => {
  if (backendMessage.value) return backendMessage.value
  return backendStatus.value === 'unreachable' ? '请稍后重试，并确认后端、MySQL 与 Redis 已启动。' : '正在等待后端服务恢复响应。'
})

function normalizeRole(role: unknown): 'SUPER_ADMIN' | 'ADMIN' | 'USER' {
  const value = String(role ?? '').trim().toUpperCase()
  if (value === 'SUPER_ADMIN' || value === 'ADMIN' || value === 'USER') return value
  return 'USER'
}

const submitDisabled = computed(() => loading.value || !form.username.trim() || !form.password.trim())

async function submit() {
  if (submitDisabled.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  if (serviceStatusStore.state.backendStatus === 'unreachable') {
    serviceStatusStore.markChecking()
  }

  loading.value = true
  try {
    const resp = await apiLogin(form.username.trim(), form.password)
    if (resp.code !== 0) {
      ElMessage.error(resp.message)
      return
    }

    const normalizedRole = normalizeRole(resp.data.role)

    authStore.setAuth({
      token: resp.data.token,
      userId: resp.data.userId,
      username: resp.data.username,
      role: normalizedRole,
    })

    router.push(resolveHomeRoute(normalizedRole))
  } catch {
    // Infrastructure errors are rendered inline; non-infra HTTP errors are handled centrally.
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-shell login-shell">
    <section class="auth-single cursor-card">
      <div class="auth-layout">
        <div class="auth-visual">
          <div class="auth-logo-wrap">
            <AuthMeshLogo />
          </div>
          <p class="scene-kicker">Quiet Luxury Workspace</p>
          <p class="visual-hint">Conference Room Reservation System</p>
        </div>

        <div class="auth-panel">
          <h1 class="scene-title">登录系统</h1>
          <p class="form-subtitle">登录后将自动进入运营看板</p>
          <PageStatusPanel
            v-if="showServiceStatus"
            class="login-status-panel"
            :tone="serviceTone"
            :title="serviceTitle"
            :description="serviceDescription"
          />

          <el-form label-position="top" @submit.prevent="submit">
            <el-form-item label="用户名" required>
              <el-input v-model="form.username" autocomplete="username" placeholder="请输入用户名" clearable @keyup.enter="submit" />
            </el-form-item>

            <el-form-item label="密码" required>
              <el-input
                v-model="form.password"
                type="password"
                autocomplete="current-password"
                placeholder="请输入密码"
                show-password
                @keyup.enter="submit"
              />
            </el-form-item>

            <el-form-item style="margin-top: 10px">
              <el-button type="primary" class="auth-btn auth-btn-solid" :loading="loading" :disabled="submitDisabled" @click="submit">
                登录系统
              </el-button>
            </el-form-item>

            <div class="auth-extra">
              <span class="auth-extra-label">还没有账号？</span>
              <el-button text @click="$router.push('/register')">立即注册</el-button>
            </div>
          </el-form>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-shell {
  --auth-panel-bg: rgba(255, 255, 255, 0.5);
  --auth-visual-bg: rgba(243, 246, 249, 0.72);
  --auth-divider: rgba(44, 44, 44, 0.14);
}

.login-shell .auth-single {
  border-color: rgba(38, 38, 38, 0.1);
  box-shadow:
    0 26px 54px rgba(20, 24, 28, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.76);
}

.login-shell .auth-panel {
  padding: clamp(8px, 0.8vw, 10px);
}

.login-shell .scene-title {
  letter-spacing: -0.03em;
}

.login-shell .form-subtitle {
  line-height: 1.7;
}

.login-shell .auth-extra {
  justify-content: flex-start;
}

.login-shell .login-status-panel {
  margin-bottom: 16px;
}

.login-shell :deep(.el-form) {
  width: 100%;
  text-align: left;
}

.login-shell :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-shell :deep(.el-form-item__label) {
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 6px;
}

.login-shell :deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.76) !important;
}

.login-shell .auth-extra :deep(.el-button) {
  padding-inline: 4px;
  min-height: auto;
}

@media (max-width: 980px) {
  .login-shell .auth-panel {
    padding: 0;
    background: transparent;
    border: none;
  }
}

@media (max-width: 640px) {
  .login-shell .scene-title {
    font-size: 24px;
  }
}
</style>
