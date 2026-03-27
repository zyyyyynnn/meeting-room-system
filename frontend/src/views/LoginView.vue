<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiLogin } from '../api/mrs'
import { authStore } from '../store/auth'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const roleRedirectMap: Record<'SUPER_ADMIN' | 'ADMIN' | 'USER', string> = {
  SUPER_ADMIN: '/admin/users',
  ADMIN: '/admin/approvals',
  USER: '/dashboard',
}

const submitDisabled = computed(() => loading.value || !form.username.trim() || !form.password.trim())

async function submit() {
  if (submitDisabled.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    const resp = await apiLogin(form.username.trim(), form.password)
    if (resp.code !== 0) {
      ElMessage.error(resp.message)
      return
    }

    authStore.setAuth({
      token: resp.data.token,
      userId: resp.data.userId,
      username: resp.data.username,
      role: resp.data.role,
    })

    const role = resp.data.role
    const roleText = role === 'SUPER_ADMIN' ? '超级管理员' : role === 'ADMIN' ? '管理员' : '普通用户'
    ElMessage.success(`登录成功，欢迎${roleText}`)
    router.push(roleRedirectMap[role] || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-shell login-shell">
    <section class="auth-single cursor-card">
      <p class="scene-kicker">Quiet Luxury Workspace</p>
      <h1 class="scene-title">登录系统</h1>
      <p class="form-subtitle">登录后将自动进入你的工作台</p>

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
          <el-button type="primary" class="auth-btn auth-btn-solid" :loading="loading" :disabled="submitDisabled" @click="submit">登录系统</el-button>
        </el-form-item>

        <div class="auth-extra">
          <span>还没有账号？</span>
          <el-button text @click="$router.push('/register')">立即注册</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at 8% 10%, rgba(31, 31, 31, 0.035), transparent 32%),
    radial-gradient(circle at 92% 90%, rgba(31, 31, 31, 0.03), transparent 36%),
    var(--bg-base);
}

.auth-single {
  align-items: center;
  text-align: center;
}

:deep(.el-form) {
  width: min(420px, 100%);
  margin: 0 auto;
  text-align: left;
}

.auth-single {
  width: min(960px, calc(100vw - 48px));
  padding: 38px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.scene-kicker {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.7px;
  text-transform: uppercase;
  color: var(--text-weak);
}

.scene-title {
  margin: 10px 0 8px;
  font-size: 30px;
  line-height: 1.2;
  color: var(--text-main);
}

.form-subtitle {
  margin: 0 0 22px;
  color: var(--text-muted);
  font-size: 14px;
}

:deep(.el-form) {
  width: min(420px, 100%);
}

.auth-btn {
  width: 100%;
  height: 40px;
}

.auth-btn-solid {
  --el-button-bg-color: var(--accent) !important;
  --el-button-border-color: var(--accent) !important;
  --el-button-text-color: var(--bg-card-strong) !important;
  --el-button-hover-bg-color: var(--accent-strong) !important;
  --el-button-hover-border-color: var(--accent-strong) !important;
}

.auth-extra {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid var(--line-soft);
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 14px;
}

@media (max-width: 980px) {
  .login-shell {
    padding: 16px;
  }

  .auth-single {
    width: min(520px, calc(100vw - 32px));
    padding: 28px 24px;
  }

  .scene-title {
    font-size: 26px;
  }
}
</style>
