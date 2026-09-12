import { defineStore } from 'pinia'
import { userApi } from '@/api/user'
import { ElMessage } from 'element-plus'
import router from '@/router'

export const useUserStore = defineStore('user', {
  state: () => {
    const portal = location.pathname.startsWith('/admin') ? 'admin' : 'user'
    return ({
    portal,
    token: sessionStorage.getItem(`${portal}Token`) || localStorage.getItem(`${portal}Token`) || '',
    userInfo: (() => {
      try {
        const raw = sessionStorage.getItem(`${portal}UserInfo`) || localStorage.getItem(`${portal}UserInfo`);
        return raw && raw !== 'undefined' ? JSON.parse(raw) : null;
      } catch {
        return null;
      }
    })(),
  })},

  getters: {
    isLoggedIn: state => !!state.token,
    isAdmin: state => state.userInfo?.role === 'admin',
    isTeacher: state => state.userInfo?.role === 'teacher',
    username: state => state.userInfo?.username || '游客',
    avatar: state => state.userInfo?.avatar || ''
  },

  actions: {
    async login(credentials, remember = false, portal = 'user') {
      const res = await userApi.login(credentials)
      this.token = res.data.accessToken
      this.userInfo = res.data.userInfo || res.data.user
      this.portal = portal
      const storage = remember ? localStorage : sessionStorage
      const otherStorage = remember ? sessionStorage : localStorage
      storage.setItem(`${portal}Token`, this.token)
      storage.setItem(`${portal}UserInfo`, JSON.stringify(this.userInfo))
      otherStorage.removeItem(`${portal}Token`)
      otherStorage.removeItem(`${portal}UserInfo`)
      if (remember) localStorage.setItem(`${portal}RememberLogin`, 'true')
      else localStorage.removeItem(`${portal}RememberLogin`)
      ElMessage.success('登录成功')
    },

    async logout(target = '/login') {
      try {
        await userApi.logout()
      } finally {
        this.token = ''
        this.userInfo = null
        localStorage.removeItem(`${this.portal}Token`)
        localStorage.removeItem(`${this.portal}UserInfo`)
        localStorage.removeItem(`${this.portal}RememberLogin`)
        sessionStorage.removeItem(`${this.portal}Token`)
        sessionStorage.removeItem(`${this.portal}UserInfo`)
        router.push(target)
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
      const storage = localStorage.getItem(`${this.portal}RememberLogin`) === 'true' ? localStorage : sessionStorage
      storage.setItem(`${this.portal}UserInfo`, JSON.stringify(this.userInfo))
    }
  }
})
