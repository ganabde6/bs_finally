import { defineStore } from 'pinia'
import { getUserInfo } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}'),
    permissions: JSON.parse(localStorage.getItem('permissions') || '[]')
  }),
  getters: {
    isLogin: (state) => !!state.token,
    roleCode: (state) => state.userInfo.roleCode || '',
    homePath: (state) => {
      switch (state.userInfo.roleCode) {
        case 'STUDENT': return '/student/dashboard'
        case 'TEACHER': return '/teacher/dashboard'
        case 'SUPER_ADMIN':
        case 'SCHOOL_ADMIN': return '/admin/dashboard'
        default: return '/login'
      }
    }
  },
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    setUserInfo(info) {
      this.userInfo = info
      localStorage.setItem('userInfo', JSON.stringify(info))
    },
    setPermissions(perms) {
      this.permissions = perms || []
      localStorage.setItem('permissions', JSON.stringify(perms || []))
    },
    async fetchUserInfo() {
      const res = await getUserInfo()
      this.setUserInfo(res.data)
      this.setPermissions(res.data.permissions)
      return res.data
    },
    clear() {
      this.token = ''
      this.userInfo = {}
      this.permissions = []
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('permissions')
    }
  }
})
