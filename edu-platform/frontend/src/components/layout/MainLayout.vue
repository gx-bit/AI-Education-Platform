<template>
  <el-container class="main-layout">
    <!-- 顶部导航 -->
    <el-header class="header">
      <div class="header-inner">
        <router-link to="/" class="logo">
          <el-icon size="28" color="#409eff"><Monitor /></el-icon>
          <span>云课堂</span>
        </router-link>

        <el-menu mode="horizontal" :default-active="activeMenu" router class="nav-menu" :ellipsis="false">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/courses">课程广场</el-menu-item>
          <el-menu-item v-if="userStore.isLoggedIn" index="/orders">我的学习</el-menu-item>
          <el-menu-item v-if="userStore.isLoggedIn" index="/favorites">我的收藏</el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/admin/dashboard">管理后台</el-menu-item>
        </el-menu>

        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <el-badge :value="unreadCount || undefined" class="notify-badge">
              <el-icon size="20" style="cursor:pointer" @click="showNotify = true"><Bell /></el-icon>
            </el-badge>
            <el-dropdown @command="handleCommand">
              <div class="user-avatar">
                <el-avatar :size="36" :src="userStore.avatar">{{ userStore.username[0] }}</el-avatar>
                <span class="username">{{ userStore.username }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                  <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="admin">管理后台</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button @click="$router.push('/auth/login')">登录</el-button>
            <el-button type="primary" @click="$router.push('/auth/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>

    <!-- 主内容 -->
    <el-main class="main-content">
      <router-view />
    </el-main>

    <!-- 页脚 -->
    <el-footer class="footer" height="60px">
      <span>© 2024 云课堂在线教育平台 · 用知识改变未来</span>
    </el-footer>

    <!-- 通知抽屉 -->
    <el-drawer v-model="showNotify" title="我的通知" size="380px">
      <NotificationPanel />
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { notifyApi } from '@/api/notify'
import NotificationPanel from '@/components/common/NotificationPanel.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const showNotify = ref(false)
const unreadCount = ref(0)

const activeMenu = computed(() => route.path)

async function loadUnreadCount() {
  if (userStore.isLoggedIn) {
    try {
      const res = await notifyApi.getUnreadCount()
      unreadCount.value = res.data || 0
    } catch {}
  }
}

function handleCommand(cmd) {
  const map = {
    profile: () => router.push('/profile'),
    orders: () => router.push('/orders'),
    favorites: () => router.push('/favorites'),
    admin: () => router.push('/admin/dashboard'),
    logout: () => userStore.logout()
  }
  map[cmd]?.()
}

loadUnreadCount()
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  flex-direction: column;
}
.header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, .08);
  padding: 0;
  height: 60px;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 0 16px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
  white-space: nowrap;
}
.nav-menu {
  flex: 1;
  border-bottom: none;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  white-space: nowrap;
}
.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.username {
  font-size: 14px;
  color: #303133;
}
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
  width: 100%;
}
.footer {
  background: #2c3e50;
  color: #bdc3c7;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}
.notify-badge {
  display: flex;
  align-items: center;
}
</style>
