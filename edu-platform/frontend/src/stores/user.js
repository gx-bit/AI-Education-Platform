import { defineStore } from 'pinia'
import { userApi } from '@/api/user'
import { ElMessage } from 'element-plus'
import router from '@/router'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: sessionStorage.getItem('token') || localStorage.getItem('token') || '',
    userInfo: (() => {
      try {
        const raw = sessionStorage.getItem('userInfo') || localStorage.getItem('userInfo');
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
    async login(credentials, remember = false) {
      const res = await userApi.login(credentials)
      this.token = res.data.accessToken
      this.userInfo = res.data.userInfo || res.data.user
      const storage = remember ? localStorage : sessionStorage
      const otherStorage = remember ? sessionStorage : localStorage
      storage.setItem('token', this.token)
      storage.setItem('userInfo', JSON.stringify(this.userInfo))
      otherStorage.removeItem('token')
      otherStorage.removeItem('userInfo')
      if (remember) localStorage.setItem('rememberLogin', 'true')
      else localStorage.removeItem('rememberLogin')
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
        localStorage.removeItem('rememberLogin')
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('userInfo')
        router.push('/login')
        ElMessage.success('已退出登录')
      }
    },

    async fetchProfile() {
      const res = await userApi.getProfile()
      this.userInfo = res.data
      this.persistUserInfo()
    },

    updateUserInfo(info) {
      this.userInfo = { ...this.userInfo, ...info }
      this.persistUserInfo()
    },

    persistUserInfo() {
      const storage = localStorage.getItem('rememberLogin') === 'true' ? localStorage : sessionStorage
      storage.setItem('userInfo', JSON.stringify(this.userInfo))
    }
  }
})
