<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiNotifications } from '../api/mrs'

const loading = ref(false)
const list = ref<string[]>([])

async function reload() {
  loading.value = true
  try {
    const resp = await apiNotifications()
    if (resp.code !== 0) return ElMessage.error(resp.message)
    list.value = Array.isArray(resp.data) ? resp.data : []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载通知失败')
    list.value = []
  } finally {
    loading.value = false
  }
}

onMounted(reload)
</script>

<template>
  <div class="page-wrap">
    <section class="page-hero cursor-card">
      <div>
        <h2 class="page-title">通知中心</h2>
        <p class="page-subtitle">集中查看预约相关提醒与系统通知。</p>
      </div>
      <el-button :loading="loading" @click="reload">刷新</el-button>
    </section>

    <section class="cursor-card table-card">
      <div class="section-head">
        <div class="section-title">最近通知</div>
        <div class="section-desc">按时间倒序展示消息，用于跟踪状态变化。</div>
      </div>

      <div v-if="list.length" class="notice-list">
        <article class="notice-item" v-for="(item, idx) in list" :key="`notice-${idx}`">
          <span class="notice-index">{{ idx + 1 }}</span>
          <span class="notice-text">{{ item }}</span>
        </article>
      </div>
      <el-empty v-else description="暂无通知，后续提醒会显示在这里。" />
    </section>
  </div>
</template>

<style scoped>
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notice-item {
  border: 1px solid var(--line-soft);
  background: rgba(255, 255, 255, 0.42);
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.notice-index {
  color: var(--text-weak);
  font-size: 12px;
  min-width: 18px;
}

.notice-text {
  color: var(--text-main);
  font-size: 13px;
  line-height: 1.66;
}
</style>
