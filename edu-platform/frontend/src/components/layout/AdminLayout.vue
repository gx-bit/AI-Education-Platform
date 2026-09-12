<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="sidebar">
      <div class="sidebar-logo">
        <el-icon size="24" color="#fff"><Monitor /></el-icon>
        <span>云课堂管理</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#2c3e50"
        text-color="#bdc3c7"
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
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.avatar">{{ userStore.username[0] }}</el-avatar>
              <span>{{ userStore.username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home">返回首页</el-dropdown-item>
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
  if (cmd === 'home') router.push('/')
  if (cmd === 'logout') userStore.logout()
}
</script>

<style scoped>
.admin-layout { height: 100vh; }
.sidebar {
  background: #2c3e50;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  overflow-y: auto;
}
.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255,255,255,.1);
}
.sidebar-menu { border-right: none; }
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
  background: #f5f7fa;
  margin-left: 220px;
  min-height: calc(100vh - 60px);
  padding: 24px;
}
</style>
