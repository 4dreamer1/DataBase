import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from './router'
import './styles/index.scss'
import ElementPlus from 'element-plus'
import { 
  User, Setting, Lock, Notification, 
  Menu, Document, Delete, Edit, Search,
  Upload, Download, ArrowLeft, ArrowRight,
  Odometer, Service, Box, List, Folder,
  DocumentCopy, Files, Timer, TrendCharts,
  AlarmClock, UserFilled, Avatar, Warning,
  Moon, Sunny, FullScreen, SwitchButton
} from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap-icons/font/bootstrap-icons.css'
import { useUserStore } from './store/modules/user'
import { useAuthStore } from './store/modules/auth'
import { useThemeStore } from './store/modules/theme'

// 引入Roboto字体
import '@/styles/fonts.css'

const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.use(ElementPlus, {
  size: 'default',
  zIndex: 3000
})

// 按需注册常用图标
const icons = {
  User, Setting, Lock, Notification, 
  Menu, Document, Delete, Edit, Search,
  Upload, Download, ArrowLeft, ArrowRight,
  Odometer, Service, Box, List, Folder,
  DocumentCopy, Files, Timer, TrendCharts,
  AlarmClock, UserFilled, Avatar, Warning,
  Moon, Sunny, FullScreen, SwitchButton
}

Object.entries(icons).forEach(([key, component]) => {
  app.component(key, component)
})

// 导入Bootstrap的JavaScript功能
import 'bootstrap/dist/js/bootstrap.bundle.min.js'

// 初始化用户状态
const userStore = useUserStore()
userStore.initUserState()

// 初始化认证状态
const authStore = useAuthStore()
// 从userStore同步头像信息到authStore
if (userStore.userInfo?.avatar && authStore.user) {
  authStore.updateUserAvatar(userStore.userInfo.avatar)
}

// 初始化主题状态
const themeStore = useThemeStore()
themeStore.initTheme()

app.mount('#app')
