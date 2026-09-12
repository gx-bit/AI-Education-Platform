import { defineStore } from 'pinia'
import { userApi } from '@/api/user'
import { ElMessage } from 'element-plus'
import router from '@/router'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: (() => {
      try {
        const raw = localStorage.getItem('userInfo');
        return raw && raw !== 'undefined' ? JSON.parse(raw) : null;
      } catch {
        return null;
      }
    })(),
  }),

  getters: {
    isLoggedIn: state => !!state.token,
    isAdmin: state => state.userInfo?.role === 'admin',
    isTeacher: state => state.userInfo?.role === 'teacher',
    username: state => state.userInfo?.username || '游客',
    avatar: state => state.userInfo?.avatar || ''
  },

  actions: {
    async login(credentials) {
      const res = await userApi.login(credentials)
      this.token = res.data.accessToken
      this.userInfo = res.data.userInfo
      localStorage.setItem('token', this.token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      ElMessage.success('登录成功')
    },

    async logout() {
      try {
        await userApi.logout()
      } finally {
        this.token = ''
        this.userInfo = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        router.push('/login')
        ElMessage.success('已退出登录')
      }
    },

    async fetchProfile() {
      const res = await userApi.getProfile()
      this.userInfo = res.data
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    },

    updateUserInfo(info) {
      this.userInfo = { ...this.userInfo, ...info }
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    }
  }
})
