<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiRegister } from '../api/mrs'
import AuthMeshLogo from '../components/AuthMeshLogo.vue'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

async function submit() {
  if (!form.username.trim() || !form.password.trim()) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    const resp = await apiRegister(form.username.trim(), form.password)
    if (resp.code !== 0) {
      ElMessage.error(resp.message)
      return
    }
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-shell register-shell">
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
          <h1 class="scene-title">创建账号</h1>
          <p class="form-subtitle">请填写账户信息完成注册</p>

          <el-form label-position="top" @submit.prevent>
            <el-form-item label="用户名" required>
              <el-input v-model="form.username" autocomplete="username" placeholder="请输入用户名" clearable />
            </el-form-item>

            <el-form-item label="密码" required>
              <el-input
                v-model="form.password"
                type="password"
                autocomplete="new-password"
                placeholder="请输入密码"
                show-password
              />
            </el-form-item>

            <el-form-item style="margin-top: 10px">
              <el-button type="primary" class="auth-btn auth-btn-solid" :loading="loading" @click="submit">创建账号</el-button>
            </el-form-item>

            <div class="auth-extra">
              <span class="auth-extra-label">已有账号？</span>
              <el-button text @click="$router.push('/login')">去登录</el-button>
            </div>
          </el-form>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.register-shell {
  --auth-shell-accent-1: rgba(104, 123, 114, 0.16);
  --auth-shell-accent-2: rgba(66, 84, 76, 0.11);
  --auth-shell-glow: rgba(255, 255, 255, 0.36);
  --auth-panel-bg: rgba(255, 255, 255, 0.46);
  --auth-visual-bg: rgba(243, 243, 243, 0.7);
  --auth-divider: rgba(40, 40, 40, 0.13);
}

.register-shell :deep(.el-form) {
  width: 100%;
  text-align: left;
}

.register-shell :deep(.el-form-item) {
  margin-bottom: 16px;
}

.register-shell :deep(.el-form-item__label) {
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 6px;
}

.register-shell :deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.74) !important;
}

.register-shell .auth-extra :deep(.el-button) {
  padding-inline: 4px;
  min-height: auto;
}

@media (max-width: 980px) {
  .register-shell .auth-panel {
    padding: 0;
    background: transparent;
    border: none;
  }
}
</style>
