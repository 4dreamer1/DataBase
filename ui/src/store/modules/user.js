import { defineStore } from 'pinia'
import userService from '@/api/user'
import router from '@/router'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: null,
    userInfo: null,
    pendingCount: 0
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => {
      if (!state.userInfo) return false;
      
      // 简化管理员判断逻辑
      if (state.userInfo.isAdmin === true) return true;
      
      if (Array.isArray(state.userInfo.roles)) {
        return state.userInfo.roles.some(role => 
          String(typeof role === 'object' ? role.name : role).toUpperCase().includes('ADMIN')
        );
      }
      
      return false;
    },
    username: (state) => state.userInfo?.username || '',
    nickname: (state) => state.userInfo?.name || state.userInfo?.username || '',
    avatar: (state) => state.userInfo?.avatar || '',
    role: (state) => state.userInfo?.roles?.[0] || '',
    hasPendingBorrows: (state) => state.pendingCount > 0
  },
  actions: {
    // 登录
    async login(loginForm) {
      try {
        const response = await userService.login(loginForm);
        const { token, id, username, email, name, roles } = response;
        
        this.token = token;
        this.userInfo = {
          id, username, email, name,
          roles: Array.isArray(roles) ? roles : [roles].filter(Boolean)
        };
        
        localStorage.setItem('token', token);
        localStorage.setItem('user-info', JSON.stringify(this.userInfo));
        
        return response;
      } catch (error) {
        return Promise.reject(error);
      }
    },
    
    // 注册
    async register(registerForm) {
      return userService.register(registerForm);
    },
    
    // 登出
    async logout() {
      try {
        await userService.logout();
      } catch (error) {
        console.error('登出时发生错误', error);
      } finally {
        this.token = null;
        this.userInfo = null;
        this.pendingCount = 0;
        
        localStorage.removeItem('token');
        localStorage.removeItem('user-info');
        localStorage.removeItem('user-store');

        router.push('/login');
      }
    },
    
    // 获取用户信息
    async fetchUserInfo() {
      try {
        const response = await userService.getCurrentUser();
        const userInfo = { ...response };
        
        // 标准化角色格式
        userInfo.roles = Array.isArray(userInfo.roles) ? 
          userInfo.roles.map(role => typeof role === 'object' ? role.name : role) : 
          [userInfo.roles].filter(Boolean);
          
        userInfo.isAdmin = userInfo.roles.some(role => 
          String(role).toUpperCase().includes('ADMIN')
        );
        
        this.userInfo = userInfo;
        localStorage.setItem('user-info', JSON.stringify(userInfo));
        
        return userInfo;
      } catch (error) {
        const cachedInfo = localStorage.getItem('user-info');
        this.userInfo = cachedInfo ? JSON.parse(cachedInfo) : null;
        return this.userInfo;
      }
    },
    
    // 更新用户头像
    updateUserAvatar(avatarUrl) {
      if (this.userInfo) {
        this.userInfo.avatar = avatarUrl;
        localStorage.setItem('user-info', JSON.stringify(this.userInfo));
      }
    },
    
    // 初始化用户状态
    initUserState() {
      const token = localStorage.getItem('token');
      if (token) {
        this.token = token;
        const userInfo = localStorage.getItem('user-info');
        this.userInfo = userInfo ? JSON.parse(userInfo) : null;
        
        if (!this.userInfo) {
          this.fetchUserInfo().catch(() => router.push('/login'));
        }
      }
    }
  },
  persist: {
    key: 'user-store',
    storage: localStorage,
    paths: ['token']
  }
})