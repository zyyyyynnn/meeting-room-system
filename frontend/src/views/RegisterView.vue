<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiRegister } from '../api/mrs'

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
    const resp = await apiRegister(form.username, form.password)
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
      <p class="scene-kicker">Quiet Luxury Workspace</p>
      <h1 class="scene-title">创建账号</h1>
      <p class="form-subtitle">请填写账号信息完成注册。</p>

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
          <span>已有账号？</span>
          <el-button text @click="$router.push('/login')">去登录</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.register-shell {
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
  .register-shell {
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
