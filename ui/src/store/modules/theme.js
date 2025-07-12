import { defineStore } from 'pinia'

export const useThemeStore = defineStore('theme', {
  state: () => ({
    darkMode: false
  }),
  getters: {
    isDarkMode: (state) => state.darkMode
  },
  actions: {
    toggleDarkMode() {
      this.darkMode = !this.darkMode
      // 更新文档根元素的类名
      if (this.darkMode) {
        document.documentElement.classList.add('dark-mode')
      } else {
        document.documentElement.classList.remove('dark-mode')
      }
      // 保存到本地存储
      localStorage.setItem('darkMode', this.darkMode ? 'true' : 'false')
    },
    initTheme() {
      // 从本地存储中获取主题设置
      const savedTheme = localStorage.getItem('darkMode')
      if (savedTheme === 'true') {
        this.darkMode = true
        document.documentElement.classList.add('dark-mode')
      } else {
        this.darkMode = false
        document.documentElement.classList.remove('dark-mode')
      }
    }
  },
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'theme',
        storage: localStorage
      }
    ]
  }
}) 