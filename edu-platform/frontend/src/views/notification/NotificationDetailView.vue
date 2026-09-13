<template>
  <div class="notification-detail">
    <button class="back-link" type="button" @click="router.back()">
      <el-icon><ArrowLeft /></el-icon><span>返回</span>
    </button>

    <el-skeleton v-if="loading" :rows="8" animated class="detail-card" />
    <el-result v-else-if="!notification" icon="warning" title="通知不存在或已被删除">
      <template #extra><el-button type="primary" @click="router.push('/')">返回首页</el-button></template>
    </el-result>

    <article v-else class="detail-card">
      <header class="detail-header">
        <div class="type-icon" :class="notification.type">
          <el-icon><BellFilled /></el-icon>
        </div>
        <div class="header-copy">
          <div class="meta-row">
            <el-tag :type="tagType" effect="light" round>{{ typeLabel }}</el-tag>
            <span class="read-state"><i />已读</span>
          </div>
          <h1>{{ notification.title }}</h1>
          <time>{{ formatTime(notification.createdAt) }}</time>
        </div>
      </header>

      <div class="divider" />
      <section class="message-body">{{ notification.content }}</section>

      <footer class="detail-actions">
        <el-button v-if="notification.type === 'order'" type="primary" @click="router.push('/orders')">查看相关订单</el-button>
        <el-button @click="router.push('/')">返回首页</el-button>
        <el-button type="danger" plain @click="removeNotification">删除通知</el-button>
      </footer>
    </article>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, BellFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { notifyApi } from '@/api/notify'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const notification = ref(null)

const typeMap = {
  order: { label: '订单通知', tag: 'success' },
  course: { label: '课程通知', tag: 'warning' },
  system: { label: '系统通知', tag: 'primary' }
}
const typeLabel = computed(() => typeMap[notification.value?.type]?.label || '站内通知')
const tagType = computed(() => typeMap[notification.value?.type]?.tag || 'info')
const formatTime = value => value ? dayjs(value).format('YYYY年MM月DD日 HH:mm') : ''

async function loadDetail() {
  loading.value = true
  try {
    const res = await notifyApi.getNotificationDetail(String(route.params.id))
    notification.value = res.data
    if (notification.value && !notification.value.isRead) {
      await notifyApi.markAsRead(String(notification.value.id))
      notification.value.isRead = 1
    }
  } catch {
    notification.value = null
  } finally {
    loading.value = false
  }
}

async function removeNotification() {
  await ElMessageBox.confirm('删除后将无法恢复，确定继续吗？', '删除通知', { type: 'warning' })
  await notifyApi.deleteNotification(String(notification.value.id))
  ElMessage.success('通知已删除')
  router.replace('/')
}

onMounted(loadDetail)
</script>

<style scoped>
.notification-detail { max-width: 900px; margin: 8px auto 48px; }
.back-link { display:inline-flex;align-items:center;gap:6px;margin:0 0 18px;padding:8px 0;border:0;background:none;color:#606266;cursor:pointer;font-size:14px; }
.back-link:hover { color:#409eff; }
.detail-card { padding:34px 42px;background:#fff;border:1px solid #e8edf4;border-radius:18px;box-shadow:0 16px 45px rgba(31,45,61,.08); }
.detail-header { display:flex;align-items:flex-start;gap:20px; }
.type-icon { width:58px;height:58px;display:grid;place-items:center;flex:none;border-radius:16px;background:#ecf5ff;color:#409eff;font-size:27px; }
.type-icon.order { background:#eef9e9;color:#67c23a; }.type-icon.course { background:#fdf6ec;color:#e6a23c; }
.header-copy { min-width:0; }.meta-row { display:flex;align-items:center;gap:12px;margin-bottom:10px; }
.read-state { display:flex;align-items:center;gap:5px;color:#909399;font-size:13px; }.read-state i { width:6px;height:6px;border-radius:50%;background:#67c23a; }
h1 { margin:0 0 12px;color:#1f2937;font-size:28px;line-height:1.35;overflow-wrap:anywhere; }
time { color:#909399;font-size:13px; }.divider { height:1px;margin:28px 0;background:#edf0f5; }
.message-body { min-height:180px;color:#374151;font-size:16px;line-height:2;white-space:pre-wrap;overflow-wrap:anywhere; }
.detail-actions { display:flex;align-items:center;gap:10px;margin-top:34px;padding-top:22px;border-top:1px solid #edf0f5; }
.detail-actions .el-button:last-child { margin-left:auto; }
@media (max-width: 640px) { .detail-card { padding:24px 20px; }.detail-header { gap:14px; }.type-icon { width:46px;height:46px; }.detail-actions { flex-wrap:wrap; }.detail-actions .el-button:last-child { margin-left:0; } h1 { font-size:22px; } }
</style>
