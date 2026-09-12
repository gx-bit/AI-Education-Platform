<template>
  <div class="notify-panel">
    <div class="notify-toolbar">
      <el-radio-group v-model="unreadOnly" size="small" @change="changeFilter">
        <el-radio-button :value="false">全部</el-radio-button><el-radio-button :value="true">未读</el-radio-button>
      </el-radio-group>
      <div><el-button link :icon="Refresh" :loading="loading" @click="loadNotifications">刷新</el-button>
        <el-button link type="primary" @click="markAll" :disabled="!unreadCount">全部已读</el-button></div>
    </div>
    <el-skeleton v-if="loading" :rows="5" animated />
    <template v-else>
      <el-empty v-if="!notifications.length" :description="unreadOnly ? '没有未读通知' : '暂无通知'" />
      <div v-else class="notify-list">
        <article v-for="item in notifications" :key="item.id" class="notify-card" :class="{ unread: !item.isRead }">
          <div class="notify-icon" :class="item.type"><el-icon><BellFilled /></el-icon></div>
          <div class="notify-main" @click="openNotification(item)">
            <div class="notify-title"><span>{{ item.title }}</span><i v-if="!item.isRead" /></div>
            <div class="notify-content">{{ item.content }}</div><time>{{ formatTime(item.createdAt) }}</time>
          </div>
          <el-dropdown trigger="click" @command="command => handleCommand(command, item)">
            <el-button link :icon="MoreFilled" @click.stop />
            <template #dropdown><el-dropdown-menu>
              <el-dropdown-item v-if="!item.isRead" command="read">标记已读</el-dropdown-item>
              <el-dropdown-item command="delete" divided>删除通知</el-dropdown-item>
            </el-dropdown-menu></template>
          </el-dropdown>
        </article>
      </div>
      <el-pagination v-if="total > pageSize" small layout="prev, pager, next" :total="total" :page-size="pageSize"
        v-model:current-page="page" @current-change="loadNotifications" />
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { notifyApi } from '@/api/notify'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Refresh, MoreFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const emit = defineEmits(['unread-change'])
const router = useRouter()
const notifications = ref([]), loading = ref(false), unreadOnly = ref(false), unreadCount = ref(0)
const page = ref(1), total = ref(0), pageSize = 10

async function loadNotifications() {
  loading.value = true
  try {
    const [listRes, countRes] = await Promise.all([
      notifyApi.getMyNotifications({ pageNum: page.value, pageSize, unreadOnly: unreadOnly.value }), notifyApi.getUnreadCount()
    ])
    notifications.value = listRes.data?.records || []
    total.value = Number(listRes.data?.total || 0)
    unreadCount.value = Number(countRes.data || 0)
    emit('unread-change', unreadCount.value)
  } finally { loading.value = false }
}
function changeFilter() { page.value = 1; loadNotifications() }
async function markRead(item) {
  await notifyApi.markAsRead(item.id); item.isRead = 1
  unreadCount.value = Math.max(0, unreadCount.value - 1); emit('unread-change', unreadCount.value)
  if (unreadOnly.value) await loadNotifications()
}
async function markAll() {
  await notifyApi.markAllAsRead(); notifications.value.forEach(n => { n.isRead = 1 })
  unreadCount.value = 0; emit('unread-change', 0)
  if (unreadOnly.value) await loadNotifications()
  ElMessage.success('全部通知已标记为已读')
}
async function remove(item) {
  await ElMessageBox.confirm('确定删除这条通知吗？', '删除通知', { type: 'warning' })
  await notifyApi.deleteNotification(item.id); await loadNotifications(); ElMessage.success('通知已删除')
}
async function handleCommand(command, item) { if (command === 'read') await markRead(item); if (command === 'delete') await remove(item) }
async function openNotification(item) { if (!item.isRead) await markRead(item); if (item.type === 'order') router.push('/orders') }
const formatTime = t => dayjs(t).format('YYYY-MM-DD HH:mm')
onMounted(loadNotifications)
</script>

<style scoped>
.notify-panel { padding:4px 0 20px; }.notify-toolbar { display:flex;align-items:center;justify-content:space-between;margin-bottom:16px; }
.notify-list { display:grid;gap:10px;margin-bottom:18px; }.notify-card { display:flex;gap:10px;padding:14px 10px;border:1px solid #ebeef5;border-radius:10px;background:#fff; }
.notify-card.unread { background:#f5f9ff;border-color:#c6e2ff; }.notify-icon { width:34px;height:34px;flex:none;border-radius:50%;display:grid;place-items:center;color:#409eff;background:#ecf5ff; }
.notify-icon.order { color:#67c23a;background:#f0f9eb; }.notify-main { flex:1;min-width:0;cursor:pointer; }.notify-title { display:flex;align-items:center;gap:7px;font-weight:600; }
.notify-title i { width:7px;height:7px;background:#f56c6c;border-radius:50%; }.notify-content { color:#606266;font-size:13px;line-height:1.6;margin:5px 0;overflow-wrap:anywhere; }
time { color:#a8abb2;font-size:12px; }.el-pagination { justify-content:center; }
</style>
