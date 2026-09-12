<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="sidebar">
      <div class="sidebar-logo">
        <span class="logo-mark"><el-icon><Grid /></el-icon></span>
        <div><strong>EDU OPS</strong><small>运营管理中枢</small></div>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="transparent"
        text-color="#91a0bc"
        active-text-color="#fff"
        class="sidebar-menu"
      >
        <el-menu-item index="/admin/dashboard"><el-icon><DataBoard /></el-icon><span>运营概览</span></el-menu-item>
        <el-sub-menu index="content"><template #title><el-icon><Collection /></el-icon><span>内容与用户</span></template>
          <el-menu-item index="/admin/courses">课程管理</el-menu-item><el-menu-item index="/admin/users">用户管理</el-menu-item></el-sub-menu>
        <el-sub-menu index="operations"><template #title><el-icon><Operation /></el-icon><span>运营中心</span></template>
          <el-menu-item index="/admin/orders">交易中心</el-menu-item><el-menu-item index="/admin/notifications">消息中心</el-menu-item><el-menu-item index="/admin/recommendation">推荐策略</el-menu-item></el-sub-menu>
        <el-sub-menu index="analysis"><template #title><el-icon><TrendCharts /></el-icon><span>数据与系统</span></template>
          <el-menu-item index="/admin/stats">数据分析</el-menu-item><el-menu-item index="/admin/monitor">运行监控</el-menu-item></el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部 -->
      <el-header class="admin-header">
        <div class="breadcrumb">
          <el-icon @click="router.back()" style="cursor:pointer"><ArrowLeft /></el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">管理后台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="admin-header-right">
          <span class="online"><i></i>系统运行正常</span>
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.avatar">{{ userStore.username[0] }}</el-avatar>
              <span>{{ userStore.username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const titleMap = {
  '/admin/dashboard': '运营概览',
  '/admin/courses': '课程管理',
  '/admin/users': '用户管理',
  '/admin/orders': '交易中心',
  '/admin/notifications': '消息中心',
  '/admin/recommendation': '推荐策略',
  '/admin/monitor': '运行监控',
  '/admin/stats': '数据分析'
}
const currentPageTitle = computed(() => titleMap[route.path] || '')

function handleCommand(cmd) {
  if (cmd === 'logout') userStore.logout('/admin/login')
}
</script>

<style scoped>
.admin-layout { height: 100vh; }
.sidebar {
  background: linear-gradient(180deg,#101a2f 0%,#0a1222 100%);
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  overflow-y: auto;
}
.sidebar-logo {
  height: 76px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255,255,255,.1);
}
.sidebar-logo div { display:flex; flex-direction:column; gap:3px; }
.sidebar-logo strong { letter-spacing:1px; }
.sidebar-logo small { font-size:10px; color:#71809e; font-weight:400; }
.logo-mark { width:36px; height:36px; display:grid; place-items:center; border-radius:10px; background:#3157e8; }
.sidebar-menu { border-right:none; padding:12px 10px; }
.sidebar-menu :deep(.el-menu-item), .sidebar-menu :deep(.el-sub-menu__title) { border-radius:8px; margin:3px 0; }
.sidebar-menu :deep(.el-menu-item.is-active) { background:linear-gradient(90deg,#3157e8,#5275f5); }
.admin-header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  margin-left: 220px;
  position: sticky;
  top: 0;
  z-index: 10;
}
.breadcrumb { display: flex; align-items: center; gap: 12px; }
.admin-header-right .user-info {
  display: flex; align-items: center; gap: 8px; cursor: pointer;
  font-size: 14px;
}
.admin-main {
  background: #f4f6fa;
  margin-left: 220px;
  min-height: calc(100vh - 60px);
  padding: 28px;
}
.online { display:flex; align-items:center; gap:7px; color:#667085; font-size:12px; padding-right:16px; border-right:1px solid #ebeef5; }
.online i { width:8px; height:8px; border-radius:50%; background:#22c55e; box-shadow:0 0 0 4px #dcfce7; }
</style>
