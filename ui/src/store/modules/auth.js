import { defineStore } from 'pinia'
import authService from '@/api/auth'
import router from '@/router'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    token: null,
    roles: []
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.roles.includes('ROLE_ADMIN'),
    userRoles: (state) => state.roles
  },
  
  actions: {
    async login(username, password) {
      try {
        const response = await authService.login(username, password)
        this.token = response.token
        this.user = {
          id: response.id,
          username: response.username,
          email: response.email,
          name: response.name,
          avatar: response.avatar
        }
        this.roles = response.roles
        
        return { success: true }
      } catch (error) {
        return { 
          success: false, 
          message: error.response?.data?.message || '登录失败，请检查用户名和密码' 
        }
      }
    },
    
    async register(userData) {
      try {
        await authService.register(userData)
        return { success: true }
      } catch (error) {
        return { 
          success: false, 
          message: error.response?.data?.message || '注册失败，请稍后再试' 
        }
      }
    },
    
    logout() {
      this.user = null
      this.token = null
      this.roles = []
      router.push('/login')
    },
    
    hasRole(role) {
      return this.roles.includes(role)
    },
    
    updateUserAvatar(avatarUrl) {
      if (this.user) {
        this.user.avatar = avatarUrl
      }
    }
  },
  
  persist: true
}) 